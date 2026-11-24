# HisaabWise Android: architecture

A Kotlin + Jetpack Compose port of the iOS app (`../HisaabWiseIOS`), talking to the same backend (`../HisaabWiseBackend`). Feature-for-feature parity with iOS is the goal. Where this file and iOS disagree on behaviour, iOS wins.

## Ground rules (from the workspace `CLAUDE.md` invariants)

1. **The server decides, the client renders.** Never format, sum, convert or round money on the device. Show `Money.display` as-is. Dates and labels like "Today" come from the server.
2. Every screen has **one endpoint** that returns exactly what it draws (`GET /v1/screens/<tab>`). Every write returns the whole updated screen payload. Replace the state with it; don't patch.
3. Learn grading is the **one** client-side computation (`LessonRun`). The results are always submitted and the server re-grades.
4. Online-only writes. A write that fails offline shows the offline/refused state; it is never queued.
5. Accessibility parity: TalkBack labels (use server `accessibilityLabel` fields), 44 dp touch targets, font scaling, and `LocalReduceMotion` (no confetti or ambient loops; fades only).

## Stack

| Concern | Choice |
|---|---|
| Language / UI | Kotlin 2.4, Jetpack Compose (Material 3, themed) |
| Architecture | **MVI**: `State` + `Intent` + `Effect` per screen, `MviViewModel` |
| Navigation | Navigation Compose. Pre-auth stack in `RootScreen`; one `NavHost` per tab |
| Networking | OkHttp + kotlinx.serialization, wrapped by `ApiClient` (port of iOS `APIClient`) |
| Storage | Refresh token encrypted with an Android Keystore AES-GCM key; SharedPreferences for 2 prefs; file cache for content |
| DI | Manual container `AppContainer` (port of iOS `AppEnvironment`), exposed as `LocalAppContainer` |
| Build | AGP 9.5, Gradle 9.9, compileSdk/targetSdk 37, minSdk 26 |

## Layout

```
app/src/main/java/com/hisaabwise/
├── HisaabWiseApplication.kt   builds AppContainer; foreground revalidation
├── MainActivity.kt            single AppCompatActivity (per-app language), splash, edge-to-edge
├── app/
│   ├── AppContainer.kt        object graph + LocalAppContainer
│   ├── RootScreen.kt          signed-in → AppShell, else pre-auth NavHost; privacy cover; launch splash
│   ├── AppShell.kt            5 tabs, floating tab bar, HideTabBar(), tour start
│   └── WalkthroughOverlay.kt  17-step spotlight tour
├── core/
│   ├── mvi/                   MviViewModel, UiState/UiIntent/UiEffect, loadState(), attempt()
│   ├── model/                 Money, MoneyAmount, AuthoringCurrency, CurrencyToken, Stat, LoadState, ErrorCode,
│   │                          AppLanguage, AppTab, Session payloads, Reference lists, Picklists, Tips,
│   │                          ContentResource, TypedAmount, PasswordStrength, Validation
│   ├── network/               ApiClient, ApiError, AccessToken, Endpoint, HttpTransport, ContentLoader, NetworkLogger
│   ├── data/                  TokenStore (Keystore / in-memory), AppPreferences, FileContentStore
│   ├── session/               SessionCoordinator, ScreenRepaint
│   ├── i18n/                  LanguageManager
│   ├── walkthrough/           WalkthroughModel, WalkthroughTarget, Modifier.walkthroughTarget()
│   └── designsystem/          HWColors, UnitTint, VerdictTint, HWType, HWRadius, HWMotion, HisaabWiseTheme, LocalReduceMotion
│       └── components/        SurfaceGround, BrandGround, HWMark, HWCard, HWHeroCard, HWTray, HWEyebrowRow, HWTopBar,
│                              HWButton, HWIconButton, HWTextField, HWMoneyField, HWComboField, HWCheckbox, HWStepBar,
│                              HWStrengthMeter, StateView, ErrorCopy, HWToast, HWBanner, HWBackBar, HWSheet,
│                              HWPickerSheet, HWDonut, HWCategoryKey, HWVerdictBadge, HWSavingsMeter, HWBudgetBar,
│                              HWSplitBar, HWStatTile, HWChip
└── feature/
    ├── auth/      Landing, Sign in, Registration (3 steps), Forgot password
    ├── home/      Home, Article
    ├── expenses/  Expenses, Expense category
    ├── learn/     Learn map, Lesson player, Lesson completion
    ├── reports/   Reports archive, Month report
    └── account/   Account, Personal, Language, Currency, Password
```

Each feature folder holds:

```
feature/<name>/
├── <Name>Models.kt        @Serializable payloads for this feature's endpoints (port of iOS Models/)
├── <Screen>Contract.kt    State, Intent, Effect for one screen
├── <Screen>ViewModel.kt   MviViewModel subclass
├── <Screen>Screen.kt      XRoute (wires VM) + stateless XScreen(state, onIntent)
├── components/            feature-only composables
└── <Name>Tab.kt           the tab's NavHost (tabs only)
```

## MVI in one example

```kotlin
data class HomeState(val content: LoadState<HomeScreen> = LoadState.Loading, val isolated: String? = null) : UiState
sealed interface HomeIntent : UiIntent { data object Load : HomeIntent; data class Isolate(val id: String?) : HomeIntent }
sealed interface HomeEffect : UiEffect { data object GoToExpenses : HomeEffect }

class HomeViewModel(private val api: ApiClient) : MviViewModel<HomeState, HomeIntent, HomeEffect>(HomeState()) {
    init { dispatch(HomeIntent.Load) }
    override suspend fun handle(intent: HomeIntent) = when (intent) {
        HomeIntent.Load -> reduce { copy(content = LoadState.Loading) }.also {
            val loaded = loadState { api.get<HomeScreen>(Endpoint.SCREEN_HOME) }
            reduce { copy(content = loaded) }
        }
        is HomeIntent.Isolate -> reduce { copy(isolated = intent.id) }
    }
}

@Composable fun HomeRoute(onAddExpense: () -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel { HomeViewModel(container.api) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(vm) { vm.effects.collect { if (it is HomeEffect.GoToExpenses) onAddExpense() } }
    HomeScreen(state, vm::dispatch)
}
```

Rules:
- Only `reduce { copy(...) }` changes state. Screens are stateless and call `onIntent(...)`.
- Fetches go through `loadState { }` (the only place `ApiError` becomes `LoadState`). Writes go through `attempt { }`, which gives a `Result`.
- One-shot things (navigate, open URL, haptic, close sheet) are `Effect`s.
- Pure-UI ephemera (scroll position, a text field's focus) may stay in `remember`. Anything the logic reads belongs in State.
- Tab view models live in the shell's ViewModelStore, so they are rebuilt per session. Build them with `viewModel { … }` from `LocalAppContainer.current`.
- After a currency or language change, call `container.repaint.repaintEveryScreen()`. Tab VMs collect `container.repaint.requests` and reload.

## Networking

```kotlin
api.get<HomeScreen>(Endpoint.SCREEN_HOME)
api.post<NewExpense, ExpensesScreen>(Endpoint.EXPENSES, body, idempotencyKey = draft.key)
api.put<SavingsGoalUpdate, JsonObject>(Endpoint.GOAL, body)
api.delete<ExpensesScreen>(Endpoint.expense(id))
content.load<Curriculum>(ContentResource.Curriculum)   // ETag cache, works offline once fetched
```

- Every POST gets an `Idempotency-Key`. Pass your own key to reuse it across retries of the same user intent (adding an expense, completing a lesson).
- Auth: access JWT in memory, refresh token in the Keystore. Refresh is single-flight, and a 401 triggers one refresh and one retry. The server ending the session flips `SessionCoordinator.state.isSignedIn`.
- JSON: camelCase, `ignoreUnknownKeys`, `coerceInputValues`. **Give enum properties a default** so an unknown server value falls back instead of failing the screen.
- `Accept-Language` is the chosen app language. The server localises labels and money with it.

## Strings

`res/values*/strings_ios.xml` is **generated** from the iOS `Localizable.xcstrings` (508 keys × en, ar, hi, fil, ne, ur) by `tools/convert_strings.py`. Don't edit it. The key `home.greeting %@ %@` becomes `R.string.home_greeting`, and `%@` becomes `%s`. Android-only copy goes in `res/values/strings_<feature>.xml`.

## Build

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug          # debug → http://localhost:8080 (backend `npm run dev`; run `adb reverse tcp:8080 tcp:8080`)
./gradlew :app:assembleStaging        # staging → Render
./gradlew :app:testDebugUnitTest
```

`tools/compile-check.sh [path]` compiles one build at a time and filters errors to a path.

| Build type | API base URL | App id |
|---|---|---|
| debug | `http://localhost:8080` (via `adb reverse`) | `com.hw.hisaabwise.debug` |
| staging | `https://hisaabwise-backend-0l9b.onrender.com` | `com.hw.hisaabwise.staging` |
| release | `https://hisaabwise-backend-0l9b.onrender.com` | `com.hw.hisaabwise` |

Sample payloads for every screen (copied from the iOS fixture corpus) are in `app/src/test/resources/fixtures/`.
