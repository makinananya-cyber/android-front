package com.hisaabwise.feature.home

import com.hisaabwise.core.designsystem.VerdictTint
import com.hisaabwise.core.model.Tip

/**
 * The few pure decisions Home makes. None of them is about money: they pick which copy to show,
 * which tip is on screen, and which slice is isolated. Port of the static helpers on iOS
 * `HomeView` / `HomeViewModel`.
 */
object HomeLogic {

    /** Which of the three `.mf-l` sentences the savings foot line shows. */
    sealed interface FootLine {
        data class Met(val saved: String) : FootLine
        data object Nothing : FootLine
        data class Remaining(val saved: String, val remaining: String) : FootLine
    }

    /**
     * The server's verdict chooses; the catalogue words it. A month with nothing saved reads the same
     * whatever the verdict (the design keys on `saved <= 0`). `remaining` or nothing — never the goal.
     */
    fun footLine(savings: HomeScreenPayload.Savings): FootLine = when {
        savings.verdict == HomeScreenPayload.Verdict.Met -> FootLine.Met(savings.saved.display)
        savings.saved.minor == 0L -> FootLine.Nothing
        else -> savings.remaining?.let { FootLine.Remaining(savings.saved.display, it.display) }
            ?: FootLine.Met(savings.saved.display)
    }

    /** low | onTrack | met → Miss | Near | Hit. */
    fun verdictTint(verdict: HomeScreenPayload.Verdict): VerdictTint = when (verdict) {
        HomeScreenPayload.Verdict.Low -> VerdictTint.Miss
        HomeScreenPayload.Verdict.OnTrack -> VerdictTint.Near
        HomeScreenPayload.Verdict.Met -> VerdictTint.Hit
    }

    /** Tapping the isolated slice again returns to the whole ring. */
    fun toggleIsolation(current: String?, tapped: String?): String? = if (current == tapped) null else tapped

    /** An isolation the new payload does not contain is dropped, so the ring never comes back uniformly faded. */
    fun reconcileIsolation(current: String?, screen: HomeScreenPayload): String? =
        current?.takeIf { id -> screen.spending.categories.any { it.id == id } }

    /** The category the centre readout describes, or null for the total. */
    fun isolatedCategory(isolated: String?, screen: HomeScreenPayload): HomeScreenPayload.Category? =
        isolated?.let { id -> screen.spending.categories.firstOrNull { it.id == id } }

    /**
     * **Show me another** — the pool entry after whatever is showing, wrapping. If the showing tip is not in
     * the pool, the first entry. Null for an empty pool.
     */
    fun nextTip(pool: List<Tip>, showingId: String): Tip? {
        if (pool.isEmpty()) return null
        val index = pool.indexOfFirst { it.id == showingId }
        return pool[if (index < 0) 0 else (index + 1) % pool.size]
    }

    /**
     * What the tip card draws: the substitute (with **this payload's** currency token, since the cacheable
     * pool cannot carry one user's currency), else the day's own.
     */
    fun tipShown(screen: HomeScreenPayload, substituted: Tip?): HomeScreenPayload.DayTip =
        substituted?.let { screen.tip.copy(id = it.id, text = it.text) } ?: screen.tip

    /**
     * Where the name sits in the resolved greeting line, so only that range is tinted. The line is resolved
     * whole (a translation may reorder it); a name that cannot be found leaves the line in ink.
     */
    fun nameRange(line: String, name: String): IntRange? {
        if (name.isEmpty()) return null
        val start = line.indexOf(name)
        return if (start < 0) null else start until start + name.length
    }
}
