package com.hisaabwise.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hisaabwise.R
import com.hisaabwise.app.HideTabBar
import com.hisaabwise.app.LocalAppContainer
import com.hisaabwise.core.designsystem.HWColors
import com.hisaabwise.core.designsystem.HWRadius
import com.hisaabwise.core.designsystem.HWType
import com.hisaabwise.core.designsystem.MinTouchTarget
import com.hisaabwise.core.designsystem.UnitAccent
import com.hisaabwise.core.designsystem.UnitTint
import com.hisaabwise.core.designsystem.components.HWBackBar
import com.hisaabwise.core.designsystem.components.StateView
import com.hisaabwise.core.designsystem.components.SurfaceGround
import com.hisaabwise.feature.home.components.cornerGlow
import com.hisaabwise.feature.home.components.vector

/** The pushed article page. Hides the tab bar; sources open in the browser. */
@Composable
fun ArticleRoute(id: String, title: String, icon: ArticleIcon, accent: Int, onBack: () -> Unit) {
    HideTabBar()
    val container = LocalAppContainer.current
    val vm = viewModel(key = "article-$id") { ArticleViewModel(id, title, icon, accent, container.content) }
    val state by vm.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(vm) {
        vm.effects.collect { effect ->
            when (effect) {
                is ArticleEffect.OpenUrl -> runCatching { uriHandler.openUri(effect.url) }
                ArticleEffect.Close -> onBack()
            }
        }
    }
    ArticleScreen(state, vm::dispatch)
}

/** Port of iOS `ArticleView`: a gradient hero, sections (paragraphs, terms, steps, callout), then sources. */
@Composable
fun ArticleScreen(state: ArticleState, onIntent: (ArticleIntent) -> Unit) {
    val tint = UnitTint.forIndex(state.accent.coerceAtLeast(1)).accent
    SurfaceGround {
        Column(Modifier.fillMaxSize()) {
            HWBackBar(state.title, onBack = { onIntent(ArticleIntent.Back) }, backDescription = stringResource(R.string.article_back))
            StateView(
                state = state.content,
                onRetry = { onIntent(ArticleIntent.Load) },
                empty = {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.article_empty), style = HWType.Body, color = HWColors.InkSecondary, textAlign = TextAlign.Center)
                    }
                },
            ) { body ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp)
                        .padding(top = 8.dp, bottom = 18.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    Hero(body, state.icon, tint)
                    body.sections.forEach { Section(it, tint) }
                    if (body.sources.isNotEmpty()) Sources(body.sources) { onIntent(ArticleIntent.OpenSource(it)) }
                }
            }
        }
    }
}

@Composable
private fun Hero(body: ArticleBody, icon: ArticleIcon, tint: UnitAccent) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(14.dp, HWRadius.Card, ambientColor = HWColors.Ink.copy(alpha = 0.18f), spotColor = HWColors.Ink.copy(alpha = 0.18f))
            .clip(HWRadius.Card)
            .background(Brush.linearGradient(listOf(tint.base, tint.deep)))
            .cornerGlow(HWColors.BrandInk.copy(alpha = 0.28f), radius = 240f)
            .semantics(mergeDescendants = true) { heading() }
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier
                .padding(bottom = 4.dp)
                .size(46.dp)
                .clip(HWRadius.Medium)
                .background(HWColors.BrandInk.copy(alpha = 0.22f))
                .border(1.dp, HWColors.BrandInk.copy(alpha = 0.30f), HWRadius.Medium),
            contentAlignment = Alignment.Center,
        ) { Icon(icon.vector(), null, tint = HWColors.BrandInk, modifier = Modifier.size(24.dp)) }
        Text(body.title, style = HWType.Heading, color = HWColors.BrandInk)
        Text(body.lede, style = HWType.Body.copy(lineHeight = 23.sp), color = HWColors.BrandInk.copy(alpha = 0.92f))
    }
}

private val ReadingBody = HWType.Body.copy(lineHeight = 25.sp)

@Composable
private fun Section(section: ArticleBody.Section, tint: UnitAccent) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Text(section.heading, style = HWType.Subheading, color = HWColors.Ink, modifier = Modifier.semantics { heading() })
        section.paragraphs.forEach { Text(HWMarkdown.annotated(it), style = ReadingBody, color = HWColors.InkSecondary) }
        section.entries.forEach { Entry(it, tint) }
        if (section.steps.isNotEmpty()) Steps(section.steps, tint)
        section.callout?.let { Callout(it, tint) }
    }
}

private fun Modifier.raisedBox(): Modifier = this
    .fillMaxWidth()
    .shadow(4.dp, HWRadius.Medium, ambientColor = HWColors.Ink.copy(alpha = 0.06f), spotColor = HWColors.Ink.copy(alpha = 0.06f))
    .clip(HWRadius.Medium)
    .background(HWColors.Surface)
    .border(1.dp, HWColors.Separator, HWRadius.Medium)
    .padding(horizontal = 13.dp, vertical = 12.dp)

@Composable
private fun Entry(entry: ArticleBody.Entry, tint: UnitAccent) {
    Row(Modifier.semantics(mergeDescendants = true) {}.raisedBox()) {
        Box(Modifier.padding(top = 7.dp).size(8.dp).clip(HWRadius.Pill).background(tint.base))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(HWMarkdown.annotated(entry.term), style = HWType.Body.copy(fontWeight = FontWeight.ExtraBold), color = HWColors.Ink)
            Text(HWMarkdown.annotated(entry.detail), style = HWType.Body.copy(lineHeight = 23.sp), color = HWColors.InkSecondary)
        }
    }
}

@Composable
private fun Steps(steps: List<String>, tint: UnitAccent) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        steps.forEachIndexed { index, step ->
            val label = stringResource(R.string.article_step_accessibilityLabel, (index + 1).toString(), steps.size.toString())
            val plain = HWMarkdown.runs(step).joinToString("") { it.text }
            Row(Modifier.clearAndSetSemantics { contentDescription = "$label. $plain" }.raisedBox()) {
                Box(Modifier.size(24.dp).clip(HWRadius.Small).background(tint.soft), contentAlignment = Alignment.Center) {
                    Text((index + 1).toString(), style = HWType.Caption.copy(fontWeight = FontWeight.ExtraBold), color = tint.deep)
                }
                Spacer(Modifier.width(11.dp))
                Text(HWMarkdown.annotated(step), style = HWType.Body.copy(lineHeight = 23.sp), color = HWColors.InkSecondary, modifier = Modifier.weight(1f).padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun Callout(text: String, tint: UnitAccent) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(HWRadius.Large)
            .background(tint.soft)
            .border(1.dp, tint.base.copy(alpha = 0.28f), HWRadius.Large)
            .semantics(mergeDescendants = true) {}
            .padding(14.dp),
    ) {
        Icon(Icons.Outlined.ErrorOutline, null, tint = tint.deep, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(11.dp))
        Text(HWMarkdown.annotated(text), style = HWType.Body.copy(lineHeight = 23.sp), color = HWColors.Ink, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Sources(sources: List<ArticleBody.Source>, onOpen: (String) -> Unit) {
    val hint = stringResource(R.string.article_sources_hint)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(HWRadius.Large)
            .background(HWColors.BackgroundSecondary)
            .border(1.dp, HWColors.Separator, HWRadius.Large)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            stringResource(R.string.article_sources_caption).uppercase(),
            style = HWType.Eyebrow,
            color = HWColors.AccentMuted,
            modifier = Modifier.padding(bottom = 2.dp).semantics { heading() },
        )
        sources.forEach { source ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = MinTouchTarget)
                    .clip(HWRadius.Small)
                    .clickable { onOpen(source.url) }
                    .clearAndSetSemantics {
                        contentDescription = source.title
                        role = Role.Button
                        onClick(label = hint) { onOpen(source.url); true }
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Link, null, tint = HWColors.Accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(source.title, style = HWType.Body.copy(fontWeight = FontWeight.Medium), color = HWColors.Accent, modifier = Modifier.weight(1f))
            }
        }
    }
}
