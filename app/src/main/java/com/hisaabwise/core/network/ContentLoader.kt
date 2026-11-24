package com.hisaabwise.core.network

import com.hisaabwise.core.data.ContentStore
import com.hisaabwise.core.model.ContentResource
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

/**
 * Cacheable content (reference lists, tips, picklists, curriculum, articles), revalidated by ETag.
 * Port of iOS `ContentLoader`:
 * - sends `If-None-Match` only when cached bytes exist; a 304 uses the cache;
 * - offline or a server error falls back to cached bytes;
 * - a cache entry that no longer decodes is deleted and refetched.
 */
class ContentLoader(private val client: ApiClient, private val store: ContentStore) {

    suspend inline fun <reified T> load(resource: ContentResource): T = load(resource, serializer<T>())

    suspend fun <T> load(resource: ContentResource, serializer: KSerializer<T>): T {
        val stored = store.data(resource)
        val etag = if (stored == null) null else store.etag(resource)
        val response = try {
            client.content(resource.path, etag)
        } catch (e: ApiError) {
            val cached = stored?.let { decodeOrNull(it, serializer) } ?: throw e
            return cached
        }
        return when (response) {
            ApiClient.ContentResponse.NotModified ->
                stored?.let { decodeOrNull(it, serializer) } ?: run {
                    store.remove(resource)
                    fetch(resource, serializer)
                }
            is ApiClient.ContentResponse.Fetched -> {
                store.save(resource, response.data, response.etag)
                decodeOrNull(response.data, serializer) ?: run {
                    store.remove(resource)
                    throw ApiError.MalformedResponse
                }
            }
        }
    }

    private suspend fun <T> fetch(resource: ContentResource, serializer: KSerializer<T>): T {
        val response = client.content(resource.path, null) as? ApiClient.ContentResponse.Fetched
            ?: throw ApiError.MalformedResponse
        store.save(resource, response.data, response.etag)
        return decodeOrNull(response.data, serializer) ?: throw ApiError.MalformedResponse
    }

    private fun <T> decodeOrNull(data: ByteArray, serializer: KSerializer<T>): T? =
        runCatching { HWJson.decodeFromString(serializer, data.decodeToString()) }.getOrNull()
}
