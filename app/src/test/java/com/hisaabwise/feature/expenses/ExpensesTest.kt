package com.hisaabwise.feature.expenses

import com.hisaabwise.core.data.ContentStore
import com.hisaabwise.core.data.InMemoryTokenStore
import com.hisaabwise.core.model.AuthoringCurrency
import com.hisaabwise.core.model.ContentResource
import com.hisaabwise.core.model.ErrorCode
import com.hisaabwise.core.model.LoadState
import com.hisaabwise.core.model.MoneyAmount
import com.hisaabwise.core.model.Picklists
import com.hisaabwise.core.network.ApiClient
import com.hisaabwise.core.network.ContentLoader
import com.hisaabwise.core.network.HWJson
import com.hisaabwise.core.network.HttpResponse
import com.hisaabwise.core.network.HttpTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.Request
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

private fun fixture(name: String): String =
    requireNotNull(ExpensesDecodingTest::class.java.classLoader!!.getResource("fixtures/$name")) { "missing fixture $name" }.readText()

private fun screen(name: String) = HWJson.decodeFromString(ExpensesScreen.serializer(), fixture(name))

private val INR = AuthoringCurrency(code = "INR", symbol = "₹", displayCode = "INR", exponent = 2)

class ExpensesDecodingTest {

    @Test fun decodesTheFullMonth() {
        val s = screen("expenses-inr.json")
        assertEquals("August", s.monthLabel)
        assertEquals("₹5,539", s.summary.total.display)
        assertEquals(30, s.wants.sharePercent)
        assertFalse(s.wants.isOver)
        assertEquals(7, s.categories.size)
        val transport = s.category("transport")!!
        assertEquals(ExpensesScreen.Kind.Log, transport.kind)
        assertEquals(ExpensesScreen.Field.TransportMode, transport.field)
        assertEquals("Today", transport.entries.first().dateLabel)
        val utilities = s.category("utilities")!!
        assertEquals(ExpensesScreen.Kind.Lines, utilities.kind)
        assertEquals(listOf(ExpensesScreen.Icon.Bolt, ExpensesScreen.Icon.Drop, ExpensesScreen.Icon.Signal), utilities.lines.map { it.icon })
        assertEquals(ExpensesScreen.Kind.Fixed, s.category("rent")!!.kind)
        assertNull(s.category("groceries")!!.field)
        assertEquals("+₹900", s.category("income")!!.total.display)
    }

    @Test fun decodesFirstRunAndOverBudget() {
        val first = screen("expenses-first-run.json")
        assertTrue(first.categories.all { it.entries.isEmpty() && it.lines.isEmpty() })
        val over = screen("expenses-over-budget.json")
        assertTrue(over.wants.isOver)
        assertEquals("108%", over.wants.percentageLabel)
    }

    @Test fun incomingCategoriesComeFirstThenServerOrder() {
        val ids = screen("expenses-inr.json").orderedCategories.map { it.id }
        assertEquals(listOf("income", "groceries", "transport", "entertainment", "other", "utilities", "rent"), ids)
    }

    @Test fun unknownPresentationValuesFallBackButUnknownKindFails() {
        val raw = fixture("expenses-inr.json")
            .replaceFirst("\"icon\": \"groceries\"", "\"icon\": \"sparkles\"")
            .replaceFirst("\"field\": \"transportMode\"", "\"field\": \"colour\"")
            .replaceFirst("\"flow\": \"out\"", "\"flow\": \"sideways\"")
        val s = HWJson.decodeFromString(ExpensesScreen.serializer(), raw)
        assertEquals(ExpensesScreen.Icon.Tag, s.category("groceries")!!.icon)
        assertEquals(ExpensesScreen.Flow.Outgoing, s.category("groceries")!!.flow)
        assertNull(s.category("transport")!!.field)

        val badKind = fixture("expenses-inr.json").replaceFirst("\"kind\": \"fixed\"", "\"kind\": \"subscription\"")
        assertTrue(runCatching { HWJson.decodeFromString(ExpensesScreen.serializer(), badKind) }.isFailure)
    }

    @Test fun decodesPicklists() {
        val lists = HWJson.decodeFromString(Picklists.serializer(), fixture("picklists.json"))
        assertEquals(22, lists.transport.size)
        assertEquals(20, lists.other.size)
        assertEquals(1, (lists.transport + lists.other).count { it.opensFreeText })
    }

    @Test fun offeredSharesFallBackToTheWholeRange() {
        val wants = screen("expenses-inr.json").wants
        assertEquals(WantsShare.entries, WantsShare.offered(wants))
        val withOptions = wants.copy(options = listOf(ExpensesScreen.ShareOption(20, wants.allowance), ExpensesScreen.ShareOption(30, wants.allowance), ExpensesScreen.ShareOption(33, wants.allowance)))
        assertEquals(listOf(WantsShare.Fifth, WantsShare.RuleOfThumb), WantsShare.offered(withOptions))
    }

    @Test fun writeBodiesEncodeAsTheContractSays() {
        val body = HWJson.encodeToString(NewExpense.serializer(), NewExpense("transport", MoneyAmount(12000, "INR"), optionId = "metro"))
        assertEquals("""{"categoryId":"transport","amount":{"minor":12000,"currency":"INR"},"optionId":"metro"}""", body)
        val lines = HWJson.encodeToString(
            BillLinesUpdate.serializer(),
            BillLinesUpdate(listOf(BillLinesUpdate.BillLine(null, "Gas", MoneyAmount(500, "INR")))),
        )
        assertEquals("""{"lines":[{"name":"Gas","amount":{"minor":500,"currency":"INR"}}]}""", lines)
    }
}

class ExpensesFormsTest {
    private val transport = screen("expenses-inr.json").category("transport")!!

    @Test fun refusesWhatIsMissingInOrder() {
        val draft = EntryDraft(categoryId = "transport", field = ExpensesScreen.Field.TransportMode)
        assertEquals(EntryValidation.Invalid(EntryFailure.AmountMissing), ExpensesForms.validate(draft, INR))
        assertEquals(EntryValidation.Invalid(EntryFailure.AmountMissing), ExpensesForms.validate(draft.copy(amount = "0"), INR))
        assertEquals(EntryValidation.Invalid(EntryFailure.OptionMissing), ExpensesForms.validate(draft.copy(amount = "12"), INR))
        assertEquals(
            EntryValidation.Invalid(EntryFailure.LabelMissing),
            ExpensesForms.validate(draft.copy(amount = "12", optionId = "other", needsFreeText = true, label = "  "), INR),
        )
        assertEquals(EntryValidation.NotReady, ExpensesForms.validate(draft.copy(amount = "12"), null))
    }

    @Test fun typedLabelIsRequiredExceptForSource() {
        val place = EntryDraft(categoryId = "entertainment", field = ExpensesScreen.Field.Place, amount = "300")
        assertEquals(EntryValidation.Invalid(EntryFailure.LabelMissing), ExpensesForms.validate(place, INR))
        val source = EntryDraft(categoryId = "income", field = ExpensesScreen.Field.Source, amount = "900")
        val valid = ExpensesForms.validate(source, INR) as EntryValidation.Valid
        assertNull(valid.body.label)
        assertEquals(MoneyAmount(90000, "INR"), valid.body.amount)
    }

    @Test fun validEntryCarriesTheOptionIdAndTrimmedLabel() {
        val draft = EntryDraft(categoryId = "transport", field = ExpensesScreen.Field.TransportMode, amount = "1,250.5", optionId = "metro", label = " ")
        val body = (ExpensesForms.validate(draft, INR) as EntryValidation.Valid).body
        assertEquals(NewExpense("transport", MoneyAmount(125050, "INR"), "metro", null), body)
        assertEquals("transport", transport.id)
    }

    @Test fun editModeFillsFromThePayloadAndSavesTheSet() {
        val utilities = screen("expenses-inr.json").category("utilities")!!
        val drafts = ExpensesForms.billDrafts(utilities)
        assertEquals(listOf("340", "48", "141"), drafts.map { it.amount })
        val edited = drafts.toMutableList().apply {
            this[1] = this[1].copy(name = "  ")
            add(BillDraft(null, "Gas", "", ExpensesScreen.Icon.Bolt))
        }
        val update = ExpensesForms.billLines(edited, INR)
        assertEquals(listOf("Electricity", "Phone / data", "Gas"), update.lines.map { it.name })
        assertNull(update.lines.last().id)
        assertEquals(0L, update.lines.last().amount.minor)

        val rent = screen("expenses-inr.json").category("rent")!!
        assertEquals("3000", ExpensesForms.fixedAmount(rent))
        assertEquals("", ExpensesForms.fixedAmount(screen("expenses-first-run.json").category("rent")!!))
        assertEquals(0L, ExpensesForms.fixedCost("", INR).amount.minor)
        assertEquals(450000L, ExpensesForms.fixedCost("4500", INR).amount.minor)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesViewModelTest {

    private class Recorded(val method: String, val path: String, val key: String?, val body: String?)

    /** Answers each request from [responder]; records what was sent. */
    private class FakeTransport(var responder: (Request) -> HttpResponse) : HttpTransport {
        val sent = mutableListOf<Recorded>()
        override suspend fun send(request: Request): HttpResponse {
            val body = request.body?.let { b -> Buffer().also { b.writeTo(it) }.readUtf8() }
            sent += Recorded(request.method, request.url.encodedPath, request.header("Idempotency-Key"), body)
            return responder(request)
        }
    }

    private class MemoryStore : ContentStore {
        override suspend fun data(resource: ContentResource): ByteArray? = null
        override suspend fun etag(resource: ContentResource): String? = null
        override suspend fun save(resource: ContentResource, data: ByteArray, etag: String?) {}
        override suspend fun remove(resource: ContentResource) {}
    }

    private fun ok(name: String) = HttpResponse(200, fixture(name).toByteArray(), emptyMap())
    private fun error(status: Int, code: String) = HttpResponse(status, """{"error":{"code":"$code","message":"x"}}""".toByteArray(), emptyMap())

    private lateinit var transport: FakeTransport
    private lateinit var vm: ExpensesViewModel

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        transport = FakeTransport { req ->
            if (req.url.encodedPath.startsWith("/v1/content/picklists")) ok("picklists.json") else ok("expenses-inr.json")
        }
        val api = ApiClient("http://localhost", transport, { "en" }, InMemoryTokenStore())
        vm = ExpensesViewModel(api, ContentLoader(api, MemoryStore()))
    }

    @After fun tearDown() = Dispatchers.resetMain()

    private fun typeTransportEntry() {
        vm.dispatch(ExpensesIntent.Open("transport"))
        vm.dispatch(ExpensesIntent.LoadPicklists)
        vm.dispatch(ExpensesIntent.EditAmount("120"))
        vm.dispatch(ExpensesIntent.Choose("metro"))
    }

    @Test fun loadsAndAddsWithTheServersAnswer() {
        assertTrue(vm.state.value.content is LoadState.Loaded)
        typeTransportEntry()
        assertEquals("Metro / subway", vm.state.value.chosenOptionName)
        vm.dispatch(ExpensesIntent.AddEntry)
        val post = transport.sent.last()
        assertEquals("POST", post.method)
        assertEquals("/v1/expenses", post.path)
        assertTrue(post.body!!.contains("\"optionId\":\"metro\""))
        assertEquals(ExpensesNotice.EntryAdded, vm.state.value.notice)
        assertTrue(vm.state.value.draft.isEmpty)
        assertEquals("transport", vm.state.value.draft.categoryId)
    }

    @Test fun anOfflineRetryReusesTheKeyAndKeepsTheDraft() {
        typeTransportEntry()
        transport.responder = { throw IOException("down") }
        vm.dispatch(ExpensesIntent.AddEntry)
        assertEquals(LoadState.Offline, vm.state.value.content)
        assertEquals("120", vm.state.value.draft.amount)
        val first = transport.sent.last().key
        assertNotNull(first)

        vm.dispatch(ExpensesIntent.AddEntry)
        assertEquals(first, transport.sent.last().key)
    }

    @Test fun monthClosedReloadsThenRefilesWithANewKey() {
        typeTransportEntry()
        transport.responder = { req -> if (req.method == "POST") error(409, "MONTH_CLOSED") else ok("expenses-first-run.json") }
        vm.dispatch(ExpensesIntent.AddEntry)
        val refused = transport.sent.last { it.method == "POST" }.key
        val offer = vm.state.value.refileOffer
        assertEquals(RefileOffer("transport", "August"), offer)

        transport.responder = { ok("expenses-inr.json") }
        vm.dispatch(ExpensesIntent.Refile)
        val refiled = transport.sent.last { it.method == "POST" }.key
        assertNotEquals(refused, refiled)
        assertNull(vm.state.value.refileOffer)
        assertEquals(ExpensesNotice.EntryAdded, vm.state.value.notice)
    }

    @Test fun otherWriteFailuresBecomeAFailedScreen() {
        transport.responder = { error(500, "INTERNAL") }
        vm.dispatch(ExpensesIntent.SetWantsShare(20))
        assertEquals(LoadState.Failed(ErrorCode("INTERNAL")), vm.state.value.content)
    }

    @Test fun choosingTheShareInForceSendsNothing() {
        val before = transport.sent.size
        vm.dispatch(ExpensesIntent.SetWantsShare(30))
        assertEquals(before, transport.sent.size)
        vm.dispatch(ExpensesIntent.SetWantsShare(25))
        assertEquals("PUT", transport.sent.last().method)
        assertEquals("""{"percent":25}""", transport.sent.last().body)
    }

    @Test fun billsEditAndSaveLeavesEditMode() {
        vm.dispatch(ExpensesIntent.Open("utilities"))
        vm.dispatch(ExpensesIntent.BeginEditing("utilities"))
        assertTrue(vm.state.value.isEditing)
        vm.dispatch(ExpensesIntent.AddBill)
        vm.dispatch(ExpensesIntent.EditBill(3, name = "Gas", amount = "50"))
        vm.dispatch(ExpensesIntent.SaveBills("utilities"))
        assertEquals("/v1/expenses/lines/utilities", transport.sent.last().path)
        assertTrue(transport.sent.last().body!!.contains("\"name\":\"Gas\",\"amount\":{\"minor\":5000"))
        assertFalse(vm.state.value.isEditing)
        assertEquals(ExpensesNotice.BillsUpdated, vm.state.value.notice)
    }

    @Test fun reopeningTheSameCategoryKeepsTheDraftButLeavesEditMode() {
        vm.dispatch(ExpensesIntent.Open("rent"))
        vm.dispatch(ExpensesIntent.BeginEditing("rent"))
        assertEquals("3000", vm.state.value.fixedAmount)
        vm.dispatch(ExpensesIntent.EditAmount("5"))
        vm.dispatch(ExpensesIntent.Open("rent"))
        assertFalse(vm.state.value.isEditing)
        assertEquals("5", vm.state.value.draft.amount)
        vm.dispatch(ExpensesIntent.Open("groceries"))
        assertEquals("", vm.state.value.draft.amount)
    }
}
