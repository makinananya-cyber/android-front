package com.hisaabwise.feature.reports

import com.hisaabwise.core.network.HWJson
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ReportsPayloadTest {

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("fixtures/$name")) { "missing fixture $name" }.readText()

    private fun archive(name: String) = HWJson.decodeFromString<ReportsScreenPayload>(fixture(name))
    private fun month(name: String) = HWJson.decodeFromString<ReportsMonthPayload>(fixture(name))

    @Test fun decodesTheSixMonthArchive() {
        val screen = archive("reports-inr.json")
        assertEquals(6, screen.summary.monthCount.value)
        assertEquals(6, screen.trend.bars.size)
        assertEquals("2026-02", screen.trend.bars.first().monthKey)
        assertEquals(Verdict.Near, screen.trend.bars.first().verdict)
        assertEquals(6, screen.allMonths.size)
        // The archive reads newest first.
        assertEquals("2026-07", screen.allMonths.first().monthKey)
        assertTrue(screen.allMonths.all { it.spent.display.isNotBlank() })
    }

    @Test fun decodesTheEmptyArchive() {
        val screen = archive("reports-empty.json")
        assertTrue(screen.years.isEmpty())
        assertTrue(screen.trend.bars.isEmpty())
    }

    @Test fun decodesTwoYears() {
        val screen = archive("reports-two-years.json")
        assertEquals(2, screen.years.size)
        assertTrue(screen.years.all { it.months.isNotEmpty() })
    }

    @Test fun decodesAMonthInEachCurrency() {
        for (name in listOf("reports-month-inr.json", "reports-month-aed.json")) {
            val m = month(name)
            assertEquals(ReportsMonthPayload.Portion.entries, m.split.segments.map { it.portion })
            assertEquals(6, m.facts.size)
            assertNotNull(m.segment(ReportsMonthPayload.Portion.Needs))
            assertTrue(m.groups.isNotEmpty())
        }
        // Two reads of the same month: the story (verdict) is the same, only the figures convert.
        assertEquals(month("reports-month-inr.json").verdict, month("reports-month-aed.json").verdict)
        assertEquals(month("reports-month-inr.json").monthKey, month("reports-month-aed.json").monthKey)
    }

    @Test fun decodesAQuietMonth() {
        val m = month("reports-month-quiet.json")
        assertEquals(6, m.facts.size)
        assertNull(m.facts.first { it.kind == ReportsMonthPayload.Kind.BiggestCost }.note)
    }

    @Test fun anIncomingGroupDecodesItsFlow() {
        val m = month("reports-month-inr.json")
        assertEquals(Flow.Incoming, m.groups.last().flow)
        assertTrue(m.groups.dropLast(1).all { it.flow == Flow.Outgoing })
    }

    @Test fun undecodableFactsAreDropped() {
        val json = fixture("reports-month-inr.json").replace("\"kind\": \"salary\"", "\"kind\": \"lottery\"")
            .replace("\"kind\":\"salary\"", "\"kind\":\"lottery\"")
        val m = HWJson.decodeFromString<ReportsMonthPayload>(json)
        assertEquals(5, m.facts.size)
        assertTrue(m.facts.none { it.kind == ReportsMonthPayload.Kind.Salary })
    }

    @Test fun aSplitThatIsNotTheFourPortionsFails() {
        val json = fixture("reports-month-inr.json").replace("\"portion\": \"surplus\"", "\"portion\": \"leftover\"")
            .replace("\"portion\":\"surplus\"", "\"portion\":\"leftover\"")
        try {
            HWJson.decodeFromString<ReportsMonthPayload>(json)
            fail("expected the split to be refused")
        } catch (e: SerializationException) {
            // expected
        }
    }

    @Test fun anUnknownVerdictFallsBack() {
        val json = fixture("reports-month-inr.json").replaceFirst("\"verdict\": \"near\"", "\"verdict\": \"stellar\"")
        assertEquals(Verdict.Miss, HWJson.decodeFromString<ReportsMonthPayload>(json).verdict)
    }
}
