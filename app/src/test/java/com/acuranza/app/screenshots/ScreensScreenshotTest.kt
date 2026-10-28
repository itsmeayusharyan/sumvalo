package com.acuranza.app.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.acuranza.app.data.BudgetEntity
import com.acuranza.app.data.GoalEntity
import com.acuranza.app.designsystem.components.SumvaloCard
import com.acuranza.app.designsystem.components.SumvaloLogo
import com.acuranza.app.designsystem.components.SegmentedTabs
import com.acuranza.app.designsystem.components.LogoShape
import com.acuranza.app.designsystem.theme.SumvaloText
import com.acuranza.app.designsystem.theme.SumvaloTheme as SumvaloThemeObject
import com.acuranza.app.finance.Budgets
import com.acuranza.app.finance.Goals
import com.acuranza.app.model.TxnType
import com.acuranza.app.repo.BudgetStatus
import com.acuranza.app.repo.NotificationItem
import com.acuranza.app.repo.NotificationItem.Kind
import com.acuranza.app.repo.GoalStatus
import com.acuranza.app.ui.SumvaloBottomBar
import com.acuranza.app.ui.notifications.NotificationsContent
import com.acuranza.app.ui.bills.BillsContent
import com.acuranza.app.ui.bills.BillsState
import com.acuranza.app.ui.common.Pill
import com.acuranza.app.ui.home.EditHomeContent
import com.acuranza.app.ui.home.HomeContent
import com.acuranza.app.ui.home.HomeLayout
import com.acuranza.app.ui.home.HomeState
import com.acuranza.app.ui.nav.Routes
import com.acuranza.app.ui.planning.PlanningContent
import com.acuranza.app.ui.planning.PlanningState
import com.acuranza.app.ui.about.AboutContent
import com.acuranza.app.ui.settings.RowTone
import com.acuranza.app.ui.settings.SettingsRow
import com.acuranza.app.ui.settings.SettingsSection
import com.acuranza.app.ui.settings.SettingsToggle
import com.acuranza.app.ui.transactions.AddTransactionContent
import com.acuranza.app.ui.transactions.DayGroup
import com.acuranza.app.ui.transactions.EditorRefs
import com.acuranza.app.ui.transactions.TransactionsContent
import com.acuranza.app.ui.transactions.TxnListState
import com.acuranza.app.util.Period
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TheaterComedy

class ScreensScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6, maxPercentDifference = 0.1)

    private val today = LocalDate.of(2026, 10, 14)

    /** A phone frame: screen content above the real bottom bar, like the running app. */
    @Composable
    private fun Frame(dark: Boolean = false, tab: String? = null, content: @Composable () -> Unit) {
        com.acuranza.app.designsystem.theme.SumvaloTheme(darkTheme = dark) {
          androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalInspectionMode provides true) {
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                Box(Modifier.weight(1f)) {
                    content()
                }
                if (tab != null) SumvaloBottomBar(tab, onTab = {})
            }
          }
        }
    }

    private fun homeState() = HomeState(
        loading = false,
        netWorth = FakeData.netWorth,
        trend = FakeData.trend,
        summary = FakeData.summary,
        unreviewed = 3,
        bills = FakeData.bills,
        billsDue = FakeData.bills.size,
        insights = FakeData.insights,
        recent = FakeData.recent,
        accounts = FakeData.accounts,
    )

    /** Stress: dynamic text at the extremes — long titles, subtitles and values must stay
     *  inside the card, ellipsize gracefully, and never starve the title column. */
    @Test
    fun settings_row_long_text() = paparazzi.snapshot {
        com.acuranza.app.designsystem.theme.SumvaloTheme(darkTheme = true) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                SumvaloCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    com.acuranza.app.ui.settings.SettingsRow(
                        title = "Recurring transactions with an unusually long setting name that keeps going and going",
                        subtitle = "A description far longer than one line, which used to hard-clip mid-word at the row's edge before the ellipsis sweep; it now wraps within its two lines",
                        icon = androidx.compose.material.icons.Icons.AutoMirrored.Outlined.ReceiptLong,
                        value = "Quarterly starting on the fifteenth of the month",
                        onClick = {},
                    )
                    com.acuranza.app.ui.settings.SettingsRow(
                        title = "Short title",
                        subtitle = "Subtitle",
                        icon = Icons.Outlined.Bolt,
                        value = "Standard",
                        divider = false,
                        onClick = {},
                    )
                }
            }
        }
    }

    /** Stress: a long profile name wraps the greeting to two lines without overlapping the
     *  avatar, the privacy eye, or the notification bell. */
    @Test
    fun home_header_long_name() = paparazzi.snapshot {
        Frame(tab = Routes.HOME) {
            HomeContent(homeState().copy(profileName = "Alexandria Federica Ocasio de Montenegro y Pilar"), hidden = false)
        }
    }

    @Test
    fun home_light() = paparazzi.snapshot { Frame(tab = Routes.HOME) { HomeContent(homeState(), hidden = false) } }

    @Test
    fun home_left_to_spend() = paparazzi.snapshot {
        val safe = com.acuranza.app.repo.SafeToSpend.plan(
            today = java.time.LocalDate.of(2026, 10, 15), cycle = com.acuranza.app.finance.PayCycle(25), liquid = 40_000_000L, cardDues = 0,
            upcoming = listOf(com.acuranza.app.finance.BudgetMath.Due(java.time.LocalDate.of(2026, 10, 18), 200_000L, "SIP")),
            overallBudget = null, incomeThisPeriod = 10_000_000L, usualMonthlySpend = null, spentThisPeriod = 3_000_000L,
            salary = 10_000_000L, savingsTarget = 2_000_000L,
        )
        Frame(tab = Routes.HOME) { HomeContent(homeState().copy(safeToSpend = safe), hidden = false) }
    }

    /** The header's compact Safe-to-spend chip: renders only when the user hasn't given the
     *  Left-to-spend section a slot (no duplication) — sections here have SAFE removed. */
    @Test
    fun home_header_chip() = paparazzi.snapshot {
        val safe = com.acuranza.app.repo.SafeToSpend.plan(
            today = java.time.LocalDate.of(2026, 10, 15), cycle = com.acuranza.app.finance.PayCycle(25), liquid = 40_000_000L, cardDues = 0,
            upcoming = listOf(com.acuranza.app.finance.BudgetMath.Due(java.time.LocalDate.of(2026, 10, 18), 200_000L, "SIP")),
            overallBudget = null, incomeThisPeriod = 10_000_000L, usualMonthlySpend = null, spentThisPeriod = 3_000_000L,
            salary = 10_000_000L, savingsTarget = 2_000_000L,
        )
        val s = homeState().copy(safeToSpend = safe, profileName = "Ayush")
            .let { it.copy(sections = it.sections.filterNot { sec -> sec == com.acuranza.app.ui.home.HomeSection.SAFE }) }
        Frame(tab = Routes.HOME) { HomeContent(s, hidden = false) }
    }

    /** The same chip in privacy mode — the amount must read as the masked placeholder. */
    @Test
    fun home_header_chip_privacy() = paparazzi.snapshot {
        val safe = com.acuranza.app.repo.SafeToSpend.plan(
            today = java.time.LocalDate.of(2026, 10, 15), cycle = com.acuranza.app.finance.PayCycle(25), liquid = 40_000_000L, cardDues = 0,
            upcoming = listOf(com.acuranza.app.finance.BudgetMath.Due(java.time.LocalDate.of(2026, 10, 18), 200_000L, "SIP")),
            overallBudget = null, incomeThisPeriod = 10_000_000L, usualMonthlySpend = null, spentThisPeriod = 3_000_000L,
            salary = 10_000_000L, savingsTarget = 2_000_000L,
        )
        val s = homeState().copy(safeToSpend = safe, profileName = "Ayush")
            .let { it.copy(sections = it.sections.filterNot { sec -> sec == com.acuranza.app.ui.home.HomeSection.SAFE }) }
        Frame(tab = Routes.HOME) { HomeContent(s, hidden = true) }
    }

    @Test
    fun home_dark() = paparazzi.snapshot { Frame(dark = true, tab = Routes.HOME) { HomeContent(homeState(), hidden = false) } }

    @Test
    fun home_privacy() = paparazzi.snapshot { Frame(tab = Routes.HOME) { HomeContent(homeState(), hidden = true) } }

    @Test
    fun activity() = paparazzi.snapshot {
        val groups = FakeData.recent.groupBy { it.date }.map { (d, items) -> DayGroup(d, items, items.sumOf { it.signedAmount }) }
        Frame(tab = Routes.TRANSACTIONS) {
            TransactionsContent(
                TxnListState(
                    period = Period(today.withDayOfMonth(1), today.withDayOfMonth(1).plusMonths(1)),
                    groups = groups,
                    income = FakeData.summary.income,
                    expense = FakeData.summary.expense,
                    loaded = true,
                ),
                hidden = false,
            )
        }
    }

    @Test
    fun add_transaction() = paparazzi.snapshot {
        Frame {
            AddTransactionContent(
                refs = EditorRefs(FakeData.accounts.map { it.account }, FakeData.categories),
                editId = null,
                initialType = TxnType.EXPENSE,
                initialAccountId = FakeData.hdfc.id,
                initialAmount = "642",
                initialCategoryKey = "food",
                initialMerchant = "Swiggy",
            )
        }
    }

    private fun budget(key: String?, limit: Long, spent: Long) = BudgetStatus(
        BudgetEntity(id = (key ?: "all").hashCode().toLong(), categoryKey = key, monthlyLimit = limit * 100),
        key?.let(FakeData::cat),
        spent * 100,
        Budgets.project(limit * 100, spent * 100, today),
    )

    private fun planState() = PlanningState(
        budgets = listOf(
            budget(null, 70_000, 61_240),
            budget("food", 10_000, 9_820),
            budget("shopping", 12_000, 8_450),
            budget("transport", 6_000, 4_120),
        ),
        goals = listOf(
            GoalStatus(
                GoalEntity(id = 1, name = "Emergency fund", target = 6_00_000_00, saved = 3_80_000_00, deadline = today.plusMonths(10).toEpochDay()),
                Goals.plan(6_00_000_00, 3_80_000_00, today.plusMonths(10), today, 7.0),
            ),
            GoalStatus(
                GoalEntity(id = 2, name = "Japan trip", target = 2_50_000_00, saved = 60_000_00, deadline = today.plusMonths(14).toEpochDay(), icon = "flight"),
                Goals.plan(2_50_000_00, 60_000_00, today.plusMonths(14), today, 6.0),
            ),
        ),
        categories = FakeData.categories,
        loaded = true,
    )

    @Test
    fun plan_budgets() = paparazzi.snapshot { Frame { PlanningContent(planState(), tab = 0) } }

    @Test
    fun plan_goals() = paparazzi.snapshot { Frame { PlanningContent(planState(), tab = 1) } }

    @Test
    fun card_bills() = paparazzi.snapshot {
        Frame {
            BillsContent(
                BillsState(bills = FakeData.bills, cards = listOf(FakeData.kotakCard, FakeData.dbsCard), banks = listOf(FakeData.hdfc), loaded = true),
                hidden = false,
            )
        }
    }

    /** Settings is assembled from the same row widgets the More screen uses. */
    @Test
    fun settings() = paparazzi.snapshot { Frame(tab = Routes.SETTINGS) { settingsBody() } }

    /** Dark is the default theme; the settings screen must read well in it.
     *  Device-height bounded: covers header → Notifications (the tail has its own golden). */
    @Test
    fun settings_dark() = paparazzi.snapshot {
        com.acuranza.app.designsystem.theme.SumvaloTheme(darkTheme = true) { settingsBody() }
    }

    /** The profile editor with a configured name — dark is the shipped default. */
    @Test
    fun profile_dark() = paparazzi.snapshot {
        Frame(dark = true) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                com.acuranza.app.ui.settings.ProfileContent(
                    savedName = "Ayush",
                    savedAvatar = null,
                    name = "Ayush Aryan",
                    stagedAvatar = null,
                    nameError = null,
                    onNameChange = {},
                    onPickImage = {},
                    onRemoveImage = {},
                    onSave = {},
                    onCancel = {},
                    saveEnabled = true,
                )
            }
        }
    }

    /** Untouched profile: initials fallback, empty name field, Save disabled. */
    @Test
    fun profile_default_light() = paparazzi.snapshot {
        Frame {
            Column(Modifier.padding(horizontal = 20.dp)) {
                com.acuranza.app.ui.settings.ProfileContent(
                    savedName = null,
                    savedAvatar = null,
                    name = "",
                    stagedAvatar = null,
                    nameError = null,
                    onNameChange = {},
                    onPickImage = {},
                    onRemoveImage = {},
                    onSave = {},
                    onCancel = {},
                    saveEnabled = false,
                )
            }
        }
    }

    /** The real hub: actual header, profile card and section tiles — not a hand-built replica.
     *  Tall frame so every section (About included) is captured, not cut at the viewport fold. */
    @Composable
    private fun settingsBody() {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                SumvaloLogo(size = 44.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Settings", style = SumvaloText.screenTitle)
                    Text("Your money, on your phone", style = MaterialTheme.typography.bodySmall, color = SumvaloThemeObject.colors.subtleText)
                }
            }
            com.acuranza.app.ui.settings.ProfileCard(name = null, avatarPath = null, onClick = {})
            com.acuranza.app.ui.settings.SettingsHubLayout.visibleFlat(null).chunked(2).forEach { pair ->
                com.acuranza.app.ui.settings.SettingsGridRow(pair, onNavigate = {})
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    @Test
    fun bottom_bar_analytics_active() = paparazzi.snapshot {
        Frame(tab = Routes.INSIGHTS) { Box(Modifier.fillMaxSize()) }
    }

    @Test
    fun about() = paparazzi.snapshot {
        com.acuranza.app.designsystem.theme.SumvaloTheme(darkTheme = true) {
            AboutContent(
                onBack = {},
                onPrivacy = {},
                onTerms = {},
                onLicence = {},
            )
        }
    }

    @Test
    fun categories() = paparazzi.snapshot {
        com.acuranza.app.designsystem.theme.SumvaloTheme(darkTheme = true) {
            com.acuranza.app.ui.settings.CategoriesContent(
                cats = listOf(
                    com.acuranza.app.data.CategoryEntity(key = "food", name = "Food & Dining", type = TxnType.EXPENSE, icon = "restaurant", colorArgb = 0xFFFF7A59),
                    com.acuranza.app.data.CategoryEntity(key = "custom_goa", name = "Goa trip", type = TxnType.EXPENSE, icon = "flight", colorArgb = 0xFF3BC9DB, isDefault = false),
                    com.acuranza.app.data.CategoryEntity(key = "salary", name = "Salary", type = TxnType.INCOME, icon = "payments", colorArgb = 0xFF2F9E44),
                ),
            )
        }
    }

    @Test
    fun edit_home() = paparazzi.snapshot {
        com.acuranza.app.designsystem.theme.SumvaloTheme(darkTheme = true) {
            EditHomeContent(
                layout = HomeLayout.decode("quick,!heat,stats"),
            )
        }
    }

    @Test
    fun notifications() = paparazzi.snapshot {
        com.acuranza.app.designsystem.theme.SumvaloTheme(darkTheme = true) {
            NotificationsContent(
                items = listOf(
                    NotificationItem(
                        id = "bill_1",
                        kind = Kind.BILL,
                        title = "Axis Card bill overdue",
                        message = "₹12,345 was due 12 Oct.",
                        whenLabel = "7d overdue",
                        route = "bills",
                        epochDay = 0,
                        unread = true,
                    ),
                    NotificationItem(
                        id = "bill_2",
                        kind = Kind.BILL,
                        title = "HDFC Card bill due in 2 days",
                        message = "₹4,500 is due on 21 Oct.",
                        whenLabel = "In 2 days",
                        route = "bills",
                        epochDay = 0,
                        unread = true,
                    ),
                    NotificationItem(
                        id = "insight_a",
                        kind = Kind.INSIGHT,
                        title = "Dining is up 40% this month",
                        message = "You've spent more on dining than usual.",
                        whenLabel = "Today",
                        route = "insights",
                        epochDay = 0,
                        unread = false,
                    ),
                ),
                hidden = false,
            )
        }
    }

    // ── v1.13.21 coverage additions: consent, insights, dark variants ────────────────────────
    @Test
    fun sms_consent() = paparazzi.snapshot {
        Frame { com.acuranza.app.ui.settings.SmsConsentContent(Modifier, onAgree = {}) }
    }

    @Test
    fun insights_empty() = paparazzi.snapshot {
        Frame(tab = Routes.INSIGHTS) {
            com.acuranza.app.ui.insights.InsightsContent(insights = emptyList(), hidden = false)
        }
    }

    private fun insight(id: String, severity: com.acuranza.app.finance.Severity, title: String, message: String) =
        com.acuranza.app.finance.Insight(id = id, severity = severity, title = title, message = message)

    @Test
    fun insights_populated() = paparazzi.snapshot {
        Frame(tab = Routes.INSIGHTS) {
            com.acuranza.app.ui.insights.InsightsContent(
                insights = listOf(
                    insight("i1", com.acuranza.app.finance.Severity.ALERT, "Overspending on Food & Dining",
                        "You've spent 82% of this month's food budget with 9 days to go."),
                    insight("i2", com.acuranza.app.finance.Severity.WARNING, "Rising transport costs",
                        "Transport spending is 34% above your 3-month average."),
                    insight("i3", com.acuranza.app.finance.Severity.TIP, "Idle cash in HDFC ••4521",
                        "₹18,400 has been idle for 45 days — a recurring SIP could put it to work."),
                    insight("i4", com.acuranza.app.finance.Severity.GOOD, "Savings rate up",
                        "You saved 22% of income this month, up from 14% last month."),
                ),
                hidden = false,
            )
        }
    }

    @Test
    fun activity_dark() = paparazzi.snapshot {
        val groups = FakeData.recent.groupBy { it.date }.map { (d, items) -> DayGroup(d, items, items.sumOf { it.signedAmount }) }
        Frame(dark = true, tab = Routes.TRANSACTIONS) {
            TransactionsContent(
                TxnListState(
                    period = Period(today.withDayOfMonth(1), today.withDayOfMonth(1).plusMonths(1)),
                    groups = groups,
                    income = FakeData.summary.income,
                    expense = FakeData.summary.expense,
                    loaded = true,
                ),
                hidden = false,
            )
        }
    }

    @Test
    fun add_transaction_dark() = paparazzi.snapshot {
        Frame(dark = true) {
            AddTransactionContent(
                refs = EditorRefs(FakeData.accounts.map { it.account }, FakeData.categories),
                editId = null,
                initialType = TxnType.EXPENSE,
                initialAccountId = FakeData.hdfc.id,
                initialAmount = "642",
                initialCategoryKey = "food",
                initialMerchant = "Swiggy",
            )
        }
    }

    /** The redesigned first-launch welcome: logo, value proposition, three props, Get started. */
    @Test
    fun onboarding_welcome() = paparazzi.snapshot {
        Frame(dark = true) {
            com.acuranza.app.ui.onboarding.Welcome(onNext = {})
        }
    }

    /** The navigation-tabs editor: live preview of the real bar + customization list. */
    @Test
    fun nav_tabs_editor() = paparazzi.snapshot {
        Frame(dark = true) {
            com.acuranza.app.ui.settings.NavTabsEditorContent(
                layout = com.acuranza.app.ui.nav.NavTabs.decodeLayout("home,insights,!transactions,settings"),
                labels = emptyMap(),
                icons = emptyMap(),
                showLabels = true,
                floating = true,
            )
        }
    }

    /** The Customize-settings edit mode: sections with move/hide/rename, hidden list, reset. */
    @Test
    fun settings_edit() = paparazzi.snapshot {
        Frame(dark = true) {
            com.acuranza.app.ui.settings.SettingsEditContent(
                layout = com.acuranza.app.ui.settings.SettingsHubLayout.decodeWithHidden(
                    "appearance>Appearance & personalization|appearance;!widgets;financial>Tools|financial",
                ),
            )
        }
    }

    @Test
    fun about_dark() = paparazzi.snapshot {
        Frame(dark = true) {
            AboutContent(
                onBack = {},
                onPrivacy = {},
                onTerms = {},
                onLicence = {},
            )
        }
    }

    @Test
    fun splash_and_logo() = paparazzi.snapshot {
        com.acuranza.app.designsystem.theme.SumvaloTheme {
            Column(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))
                SumvaloLogo(size = 88.dp, shape = LogoShape.RoundedSquare)
                Spacer(Modifier.height(12.dp))
                Text("Sumvalo", fontSize = 20.sp, style = MaterialTheme.typography.titleLarge, color = SumvaloThemeObject.colors.ink)
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    SumvaloLogo(size = 96.dp, shape = LogoShape.RoundedSquare)
                    SumvaloLogo(size = 64.dp, shape = LogoShape.Circle)
                    SumvaloLogo(size = 40.dp, shape = LogoShape.RoundedSquare)
                }
                Spacer(Modifier.height(48.dp))
                Box(Modifier.fillMaxWidth())
            }
        }
    }

}
