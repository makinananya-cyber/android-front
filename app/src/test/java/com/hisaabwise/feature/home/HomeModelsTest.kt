package com.hisaabwise.feature.home

import com.hisaabwise.core.designsystem.VerdictTint
import com.hisaabwise.core.model.Money
import com.hisaabwise.core.model.Tip
import com.hisaabwise.core.model.TipList
import com.hisaabwise.core.network.HWJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeModelsTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResource("/fixtures/$name")) { "missing fixture $name" }.readText()

    private val inr by lazy { HWJson.decodeFromString<HomeScreenPayload>(fixture("home-inr.json")) }
    private val firstRun by lazy { HWJson.decodeFromString<HomeScreenPayload>(fixture("home-first-run.json")) }

    private fun money(minor: Long, display: String) = Money(minor, "INR", 2, display)

    // MARK: - Decoding

    @Test fun decodesHomeWithSpending() {
        assertEquals("Good morning", inr.greeting)
        assertEquals("Ananya", inr.name)
        assertFalse(inr.spending.isFirstRun)
        assertEquals(6, inr.spending.categories.size)
        assertEquals("rent", inr.spending.categories.first().id)
        assertEquals("₹5,539", inr.spending.total.display)
        assertEquals("9% of pay", inr.spending.shareOfPayLabel)
        assertEquals(HomeScreenPayload.Verdict.Met, inr.savings.verdict)
        assertNull(inr.savings.remaining)
        assertNull(inr.savings.goalNudge)
        assertEquals(4, inr.learning.streak)
        assertEquals(listOf(ArticleIcon.Shield, ArticleIcon.Globe, ArticleIcon.Steps), inr.articles.map { it.icon })
        assertEquals(listOf(3, 2, 4), inr.articles.map { it.accent })
    }

    @Test fun decodesFirstRun() {
        assertTrue(firstRun.spending.isFirstRun)
        assertTrue(firstRun.spending.categories.isEmpty())
        assertEquals(HomeScreenPayload.Verdict.Low, firstRun.savings.verdict)
        assertEquals("₹13,000", firstRun.savings.remaining?.display)
    }

    @Test fun unknownVerdictAndIconFallBack() {
        val json = fixture("home-inr.json")
            .replace("\"verdict\": \"met\"", "\"verdict\": \"stellar\"")
            .replace("\"icon\": \"shield\"", "\"icon\": \"rocket\"")
        val screen = HWJson.decodeFromString<HomeScreenPayload>(json)
        assertEquals(HomeScreenPayload.Verdict.OnTrack, screen.savings.verdict)
        assertEquals(ArticleIcon.Lightbulb, screen.articles.first().icon)
    }

    @Test fun decodesGoalNudge() {
        val json = fixture("home-inr.json").replace(
            "\"remaining\": null",
            "\"remaining\": null, \"goalNudge\": {\"suggested\": {\"minor\": 2000000, \"currency\": \"INR\", \"exponent\": 2, \"display\": \"₹20,000\"}, " +
                "\"current\": {\"minor\": 1300000, \"currency\": \"INR\", \"exponent\": 2, \"display\": \"₹13,000\"}}",
        )
        val nudge = HWJson.decodeFromString<HomeScreenPayload>(json).savings.goalNudge
        assertEquals("₹20,000", nudge?.suggested?.display)
        assertEquals("₹13,000", nudge?.current?.display)
    }

    @Test fun decodesArticle() {
        val body = HWJson.decodeFromString<ArticleBody>(fixture("article-scams.json"))
        assertEquals("scams", body.id)
        assertTrue(body.sections.isNotEmpty())
        // Absent blocks decode as empty rather than failing.
        val first = body.sections.first()
        assertTrue(first.paragraphs.isNotEmpty())
        assertEquals(5, first.entries.size)
        assertTrue(body.sources.isNotEmpty())
        assertTrue(body.sources.all { it.url.startsWith("http") })
    }

    @Test fun decodesTipPool() {
        val pool = HWJson.decodeFromString<TipList>(fixture("tips.json"))
        assertEquals(49, pool.tips.size)
        assertTrue(pool.tips.any { "{c}" in it.text })
    }

    // MARK: - Tips

    @Test fun dayTipResolvesCurrencyToken() {
        assertEquals(
            "To find 10% of a number, move the dot one step left. ₹6,400 becomes ₹640. Double it and there is your 20% savings target.",
            inr.tip.resolvedText,
        )
    }

    @Test fun nextTipAdvancesAndWraps() {
        val pool = listOf(Tip("a", "A"), Tip("b", "B"), Tip("c", "C"))
        assertEquals("b", HomeLogic.nextTip(pool, "a")?.id)
        assertEquals("a", HomeLogic.nextTip(pool, "c")?.id)
        assertEquals("a", HomeLogic.nextTip(pool, "tip-07")?.id)
        assertNull(HomeLogic.nextTip(emptyList(), "a"))
    }

    @Test fun substitutedTipKeepsThePayloadsToken() {
        val shown = HomeLogic.tipShown(inr, Tip("tip-02", "Pay {c}100"))
        assertEquals("tip-02", shown.id)
        assertEquals("Pay ₹100", shown.resolvedText)
        assertEquals(inr.tip.dayKey, shown.dayKey)
        assertEquals(inr.tip, HomeLogic.tipShown(inr, null))
    }

    // MARK: - Foot line and verdict

    @Test fun footLineFollowsTheServerVerdict() {
        assertEquals(HomeLogic.FootLine.Met("₹23,000"), HomeLogic.footLine(inr.savings))
        assertEquals(HomeLogic.FootLine.Nothing, HomeLogic.footLine(firstRun.savings))
        val onTrack = inr.savings.copy(verdict = HomeScreenPayload.Verdict.OnTrack, saved = money(500000, "₹5,000"), remaining = money(800000, "₹8,000"))
        assertEquals(HomeLogic.FootLine.Remaining("₹5,000", "₹8,000"), HomeLogic.footLine(onTrack))
        // Never substitutes the goal for a missing remaining.
        assertEquals(HomeLogic.FootLine.Met("₹5,000"), HomeLogic.footLine(onTrack.copy(remaining = null)))
    }

    @Test fun verdictTints() {
        assertEquals(VerdictTint.Miss, HomeLogic.verdictTint(HomeScreenPayload.Verdict.Low))
        assertEquals(VerdictTint.Near, HomeLogic.verdictTint(HomeScreenPayload.Verdict.OnTrack))
        assertEquals(VerdictTint.Hit, HomeLogic.verdictTint(HomeScreenPayload.Verdict.Met))
    }

    // MARK: - Isolation

    @Test fun isolationTogglesAndSurvivesOnlyIfPresent() {
        assertEquals("rent", HomeLogic.toggleIsolation(null, "rent"))
        assertNull(HomeLogic.toggleIsolation("rent", "rent"))
        assertEquals("groceries", HomeLogic.toggleIsolation("rent", "groceries"))
        assertEquals("rent", HomeLogic.reconcileIsolation("rent", inr))
        assertNull(HomeLogic.reconcileIsolation("rent", firstRun))
        assertEquals("₹3,000", HomeLogic.isolatedCategory("rent", inr)?.amount?.display)
        assertNull(HomeLogic.isolatedCategory(null, inr))
    }

    // MARK: - Greeting

    @Test fun nameRangeTintsOnlyTheName() {
        assertEquals(14..19, HomeLogic.nameRange("Good morning, Ananya", "Ananya"))
        assertNull(HomeLogic.nameRange("Good morning, ", ""))
        assertNull(HomeLogic.nameRange("صباح الخير", "Ananya"))
    }

    // MARK: - Markdown

    @Test fun markdownBoldAndItalic() {
        assertEquals(
            listOf(HWMarkdown.Run("You pay "), HWMarkdown.Run("no tax", bold = true), HWMarkdown.Run(" here.")),
            HWMarkdown.runs("You pay **no tax** here."),
        )
        assertEquals(
            listOf(HWMarkdown.Run("No bank will "), HWMarkdown.Run("ever", italic = true), HWMarkdown.Run(" ask.")),
            HWMarkdown.runs("No bank will *ever* ask."),
        )
        assertEquals(listOf(HWMarkdown.Run("2 * 3 = 6")), HWMarkdown.runs("2 * 3 = 6"))
        assertEquals(listOf(HWMarkdown.Run("a **b")), HWMarkdown.runs("a **b"))
    }
}
