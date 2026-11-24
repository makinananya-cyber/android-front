package com.hisaabwise.feature.reports

import com.hisaabwise.core.model.Money
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder

/**
 * `GET /v1/screens/reports` — the archive of closed months and the savings-goal trend, fully computed
 * (port of iOS `ReportsScreen`). No percentage arrives as a number: a verdict is a [Verdict] and a
 * percentage is a sentence. The trend reads oldest → newest, the archive newest first; both orders are sent.
 */
@Serializable
data class ReportsScreenPayload(
    val summary: Summary,
    val trend: Trend,
    val years: List<Year>,
) {
    val allMonths: List<Month> get() = years.flatMap { it.months }

    @Serializable
    data class Summary(
        val goalsMetLabel: String,
        val monthCount: Count,
        val averageSpend: Money,
        val totalSaved: Money,
    )

    @Serializable
    data class Count(val value: Int, val display: String)

    @Serializable
    data class Trend(val goalPosition: Double, val bars: List<Bar>)

    @Serializable
    data class Bar(
        val monthKey: String,
        val label: String,
        val fill: Double,
        val verdict: Verdict = Verdict.Miss,
        val percentageLabel: String,
        val accessibilityLabel: String,
    )

    @Serializable
    data class Year(val label: String, val totalSaved: Money, val months: List<Month>)

    @Serializable
    data class Month(
        val monthKey: String,
        val label: String,
        val spent: Money,
        val percentageLabel: String,
        val verdict: Verdict = Verdict.Miss,
        val segments: List<Segment>,
    )

    @Serializable
    data class Segment(val slot: Int, val share: Double)
}

/** §4.2's one threshold table, server-computed (hit ≥ 100%, near ≥ 70%, miss). */
@Serializable
enum class Verdict {
    @SerialName("hit") Hit,
    @SerialName("near") Near,
    @SerialName("miss") Miss,
}

/**
 * `GET /v1/screens/reports/{monthKey}` — one closed month in full (port of iOS `ReportsMonthScreen`).
 * Archived months are immutable with a pinned FX set (invariant 7): the client draws strings only.
 *
 * - [split] decodes only when its segments are exactly needs, wants, saved, surplus in that order;
 *   anything else fails the whole payload, as on iOS.
 * - An undecodable entry in [facts] is dropped rather than failing the screen.
 */
@Serializable
data class ReportsMonthPayload(
    val monthKey: String,
    val title: String,
    val verdict: Verdict = Verdict.Miss,
    val percentageLabel: String,
    val totals: Totals,
    val spending: Spending,
    val savings: Savings,
    val wants: Wants,
    val split: Split,
    val groups: List<Group>,
    @Serializable(with = FailableFactListSerializer::class)
    val facts: List<Fact>,
) {
    fun segment(portion: Portion): Segment? = split.segments.firstOrNull { it.portion == portion }
    fun group(id: String): Group? = groups.firstOrNull { it.id == id }

    @Serializable
    data class Totals(
        val spent: Money,
        val fixed: Money,
        val variable: Money,
        val additionalIncome: Money,
        val isAdapted: Boolean,
    )

    @Serializable
    data class Spending(
        val shareOfIncomeLabel: String,
        val categoryCountLabel: String,
        val categories: List<Category>,
    )

    @Serializable
    data class Category(
        val id: String,
        val name: String,
        val amount: Money,
        val share: Double,
        val shareLabel: String,
        val slot: Int,
    )

    @Serializable
    data class Savings(
        val saved: Money,
        val goal: Money,
        val zeroLabel: String,
        val position: Double,
        val shareOfIncomeLabel: String,
        val remaining: Money? = null,
        val surplus: Money? = null,
    )

    @Serializable
    data class Wants(
        val used: Money,
        val allowance: Money,
        val percentageLabel: String,
        val fill: Double,
        val isOver: Boolean,
        val remaining: Money? = null,
        val excess: Money? = null,
    )

    @Serializable(with = SplitSerializer::class)
    data class Split(val income: Money, val segments: List<Segment>)

    data class Segment(val portion: Portion, val amount: Money, val share: Double, val target: Money?)

    enum class Portion(val wire: String) { Needs("needs"), Wants("wants"), Saved("saved"), Surplus("surplus") }

    @Serializable
    data class Group(
        val id: String,
        val name: String,
        val slot: Int,
        val icon: GroupIcon = GroupIcon.Tag,
        val flow: Flow = Flow.Outgoing,
        val total: Money,
        val summaryLabel: String,
        val entries: List<Entry>,
    )

    /** An archived entry carries no id; rows are identified by position. */
    @Serializable
    data class Entry(val label: String, val dateLabel: String, val amount: Money)

    @Serializable
    data class Fact(val kind: Kind = Kind.Unknown, val value: String, val note: String? = null)

    @Serializable
    enum class Kind {
        @SerialName("salary") Salary,
        @SerialName("goal") Goal,
        @SerialName("saved") Saved,
        @SerialName("biggestCost") BiggestCost,
        @SerialName("needs") Needs,
        @SerialName("leftOver") LeftOver,

        /** Not sent by the server: an unknown kind decodes to this and the fact is dropped (iOS fails it). */
        Unknown,
    }
}

/** Port of iOS `ExpensesScreen.Icon`; unknown values fall back to [Tag], as on iOS. */
@Serializable
enum class GroupIcon {
    @SerialName("groceries") Groceries,
    @SerialName("transport") Transport,
    @SerialName("entertainment") Entertainment,
    @SerialName("other") Other,
    @SerialName("income") Income,
    @SerialName("utilities") Utilities,
    @SerialName("rent") Rent,
    @SerialName("bolt") Bolt,
    @SerialName("drop") Drop,
    @SerialName("signal") Signal,
    @SerialName("tag") Tag,
}

/** Port of iOS `ExpensesScreen.Flow`; unknown values fall back to outgoing. */
@Serializable
enum class Flow {
    @SerialName("out") Outgoing,
    @SerialName("in") Incoming,
}

/** Thrown when the split is not exactly the four portions, in order. */
class NotTheFourPortions(val portions: List<String>) : SerializationException("split portions were $portions")

@Serializable
private data class SplitWire(val income: Money, val segments: List<SegmentWire>)

@Serializable
private data class SegmentWire(val portion: String, val amount: Money, val share: Double, val target: Money? = null)

internal object SplitSerializer : KSerializer<ReportsMonthPayload.Split> {
    private val wire = SplitWire.serializer()
    override val descriptor: SerialDescriptor = wire.descriptor

    override fun deserialize(decoder: Decoder): ReportsMonthPayload.Split {
        val raw = decoder.decodeSerializableValue(wire)
        val expected = ReportsMonthPayload.Portion.entries
        if (raw.segments.map { it.portion } != expected.map { it.wire }) {
            throw NotTheFourPortions(raw.segments.map { it.portion })
        }
        return ReportsMonthPayload.Split(
            income = raw.income,
            segments = raw.segments.mapIndexed { i, s -> ReportsMonthPayload.Segment(expected[i], s.amount, s.share, s.target) },
        )
    }

    override fun serialize(encoder: Encoder, value: ReportsMonthPayload.Split) {
        encoder.encodeSerializableValue(
            wire,
            SplitWire(value.income, value.segments.map { SegmentWire(it.portion.wire, it.amount, it.share, it.target) }),
        )
    }
}

/** Decodes each fact on its own and drops the ones that fail (iOS `FailableFact`). */
internal object FailableFactListSerializer : KSerializer<List<ReportsMonthPayload.Fact>> {
    private val list = ListSerializer(ReportsMonthPayload.Fact.serializer())
    override val descriptor: SerialDescriptor = list.descriptor

    override fun deserialize(decoder: Decoder): List<ReportsMonthPayload.Fact> {
        val json = (decoder as? JsonDecoder) ?: return decoder.decodeSerializableValue(list)
        val array = json.decodeJsonElement() as? JsonArray ?: throw SerializationException("facts is not an array")
        return array.mapNotNull { element ->
            runCatching { json.json.decodeFromJsonElement(ReportsMonthPayload.Fact.serializer(), element) }
                .getOrNull()
                ?.takeIf { it.kind != ReportsMonthPayload.Kind.Unknown }
        }
    }

    override fun serialize(encoder: Encoder, value: List<ReportsMonthPayload.Fact>) =
        encoder.encodeSerializableValue(list, value)
}
