package com.hisaabwise.feature.home

import com.hisaabwise.core.model.CurrencyToken
import com.hisaabwise.core.model.Money
import com.hisaabwise.core.model.MoneyAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `GET /v1/screens/home` — everything Home draws, fully computed by the server (ADR-0020).
 * Port of iOS `HomeScreen`. Nothing here needs arithmetic on the client.
 */
@Serializable
data class HomeScreenPayload(
    /** "Good morning" — chosen against the server-owned day boundary. */
    val greeting: String,
    val name: String,
    /** "Tuesday, 11 August" — a label, not a date. */
    val dateLabel: String,
    /** "August". */
    val monthLabel: String,
    val spending: Spending,
    val savings: Savings,
    val tip: DayTip,
    val learning: Learning,
    /** Teasers only; bodies come from the cacheable article endpoint. */
    val articles: List<ArticleTeaser> = emptyList(),
) {
    @Serializable
    data class Spending(
        val total: Money,
        /** "68% of pay" — computed against the salary the server holds (defect D1). */
        val shareOfPayLabel: String,
        /** In the order they are drawn. */
        val categories: List<Category> = emptyList(),
        /** The first-run case, said by the server rather than inferred from an empty array. */
        val isFirstRun: Boolean = false,
    )

    @Serializable
    data class Category(
        val id: String,
        val name: String,
        val amount: Money,
        /** 0…1, geometry only. */
        val share: Double,
        /** "38%". */
        val shareLabel: String,
        /** Colour slot 1…6. */
        val slot: Int,
    )

    @Serializable
    data class Savings(
        val saved: Money,
        val goal: Money,
        val zeroLabel: String,
        /** "56% of goal". */
        val percentageLabel: String,
        /** 0…1, already clamped. */
        val position: Double,
        val verdict: Verdict = Verdict.OnTrack,
        /** What is left to save, or null once the goal is met. */
        val remaining: Money? = null,
        /** Non-null only when a pay rise has left the goal behind. */
        val goalNudge: GoalNudge? = null,
    )

    @Serializable
    data class GoalNudge(val suggested: Money, val current: Money)

    /** An unknown verdict falls back to [OnTrack], as on iOS. */
    @Serializable
    enum class Verdict {
        @SerialName("low") Low,
        @SerialName("onTrack") OnTrack,
        @SerialName("met") Met,
    }

    /** The tip of the day, already chosen. `{c}` left verbatim; [currencyToken] is what it becomes. */
    @Serializable
    data class DayTip(
        val id: String,
        val dayKey: String,
        val text: String,
        val currencyToken: String,
    ) {
        val resolvedText: String get() = CurrencyToken(currencyToken).resolve(text)
    }

    @Serializable
    data class Learning(
        val streak: Int,
        /** "120 XP · Needs vs. Wants". */
        val summary: String,
        /** The lesson Continue goes to. */
        val nextLesson: String,
    )

    @Serializable
    data class ArticleTeaser(
        val id: String,
        val short: String,
        val icon: ArticleIcon = ArticleIcon.Lightbulb,
        /** Accent slot 1…5. */
        val accent: Int = 4,
    )
}

/** The design's five article glyphs, as a closed set. Unknown names fall back to [Lightbulb]. */
@Serializable
enum class ArticleIcon {
    @SerialName("shield") Shield,
    @SerialName("globe") Globe,
    @SerialName("steps") Steps,
    @SerialName("lightbulb") Lightbulb,
    @SerialName("alert") Alert;

    companion object {
        fun fromName(name: String?): ArticleIcon = entries.firstOrNull { it.name == name } ?: Lightbulb
    }
}

/** `PUT /v1/me/goal` body. */
@Serializable
data class SavingsGoalUpdate(val savingsGoal: MoneyAmount)

/**
 * `GET /v1/content/articles/:id` — one article body. Port of iOS `ArticleBody`.
 * Every block of a section is optional; the draw order is fixed: paragraphs, entries, steps, callout.
 */
@Serializable
data class ArticleBody(
    val id: String,
    val title: String,
    val lede: String,
    val sections: List<Section> = emptyList(),
    /** Official sources only. */
    val sources: List<Source> = emptyList(),
) {
    @Serializable
    data class Section(
        val heading: String,
        val paragraphs: List<String> = emptyList(),
        val entries: List<Entry> = emptyList(),
        val steps: List<String> = emptyList(),
        val callout: String? = null,
    )

    @Serializable
    data class Entry(val term: String, val detail: String)

    @Serializable
    data class Source(val title: String, val url: String)
}
