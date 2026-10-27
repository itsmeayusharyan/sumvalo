package com.acuranza.app.di

import android.content.Context
import com.acuranza.app.data.DemoData
import com.acuranza.app.data.SumvaloDatabase
import com.acuranza.app.data.local.ReleaseCache
import com.acuranza.app.data.prefs.SettingsStore
import com.acuranza.app.notify.Notifications
import com.acuranza.app.repo.AccountRepository
import com.acuranza.app.repo.BillRepository
import com.acuranza.app.repo.BudgetRepository
import com.acuranza.app.repo.CategoryRepository
import com.acuranza.app.repo.GoalRepository
import com.acuranza.app.repo.InsightRepository
import com.acuranza.app.repo.NetWorthRepository
import com.acuranza.app.repo.TransactionRepository
import com.acuranza.app.security.BackupManager
import com.acuranza.app.work.WorkScheduler

/** Manual dependency container; get it with `(context.applicationContext as SumvaloApp).container`. */
/**
 * [sandbox] is set for the decoy profile: an in-memory database, separate settings and no SMS access, so
 * nothing real can ever be shown or written there.
 */
class AppContainer(context: Context, sandbox: com.acuranza.app.security.DecoySandbox? = null) {
    private val app = context.applicationContext

    val db: SumvaloDatabase = sandbox?.db ?: SumvaloDatabase.create(app)
    val settings = sandbox?.settings ?: SettingsStore(app)
    // The offline edition has no INTERNET permission; it never tries to fetch NPS prices.
    val notifications = Notifications(app)
    // Changelog cache: initialised in SumvaloApp.onCreate(); available to all builds.
    val releaseCache: ReleaseCache = ReleaseCache.apply { init(app) }

    val accounts = AccountRepository(db)
    val loans = com.acuranza.app.repo.LoanRepository(db)
    val categories = CategoryRepository(db)
    val transactions = TransactionRepository(db)
    val bills = BillRepository(db, transactions)
    val featurePrefs = com.acuranza.app.data.prefs.featurePrefsFor(app, decoy = sandbox != null)
    val budgets = BudgetRepository(db, settings, featurePrefs)
    val planningPrefs = com.acuranza.app.data.prefs.PlanningPrefs(app)
    val goals = GoalRepository(db, settings, planningPrefs)
    val insights = InsightRepository(db, accounts, settings, budgets, planningPrefs)
    val notificationCenter = com.acuranza.app.repo.NotificationCenter(bills, insights, transactions, settings)
    val netWorth = NetWorthRepository(db, accounts, loans)
    val goalOutlooks = com.acuranza.app.repo.GoalSimulations(goals, planningPrefs)

    /** Receipt files, encrypted in app storage. */
    val attachmentStore = com.acuranza.app.data.AttachmentStore(app)
    val backup = BackupManager(app, db, attachmentStore)
    val recurring = com.acuranza.app.repo.RecurringRepository(db, featurePrefs)
    val debts = com.acuranza.app.repo.DebtRepository(db)
    val attachments = com.acuranza.app.repo.AttachmentRepository(db, attachmentStore)
    val statementImporter = com.acuranza.app.repo.StatementImporter(db)
    // SMS transaction tracking: the decoy profile gets the bookkeeping but never an inbox reader.
    val smsImports = com.acuranza.app.repo.SmsImportRepository(db, attachments)
    // The full variant's inbox runner; in the standard build the READ_SMS grant never exists,
    // so the gate inside turns every scan into a recorded no-op.
    val smsScanRunner = if (sandbox == null) {
        com.acuranza.app.data.sms.SmsScanRunner(app, smsImports, settings)
    } else {
        null
    }
    /** True when this container is the decoy profile, which must never touch real data. */
    val decoy = sandbox != null
    val reports = com.acuranza.app.repo.ReportRepository(db, settings, transactions, recurring)
    val demoData = DemoData(db)

    /** Runs the full daily refresh (SMS, net worth) once in the background. */
    fun syncNow() = WorkScheduler.syncNow(app)
}

