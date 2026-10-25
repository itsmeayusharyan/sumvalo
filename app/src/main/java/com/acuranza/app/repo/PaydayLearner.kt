package com.acuranza.app.repo

import com.acuranza.app.data.SumvaloDatabase
import com.acuranza.app.data.prefs.SettingsStore
import com.acuranza.app.finance.PayCycle
import com.acuranza.app.util.Time
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps `Settings.salaryDays` in step with the salary credits in the database, so each budget period starts on the
 * day salary actually arrived (see [PayCycle.learnPaydays]). Reacts to imports, edits and recategorising alike.
 */
object PaydayLearner {
    /** How far back salary days are kept: a year of periods and a little more. */
    private const val LOOKBACK_DAYS = 400L

    fun start(scope: CoroutineScope, db: SumvaloDatabase, settings: SettingsStore) {
        scope.launch {
            try {
                val since = Time.startOfDay(Time.today().minusDays(LOOKBACK_DAYS))
                combine(
                    settings.settings.map { it.monthStartDay }.distinctUntilChanged(),
                    db.transactions().observeSalaryCredits(since),
                ) { day, credits -> PayCycle(day).learnPaydays(credits.map { Time.toDate(it.timestamp) to it.amount }) }
                    .distinctUntilChanged()
                    .collect { days -> if (settings.current().salaryDays != days) settings.setSalaryDays(days) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Without learned days the weekend and holiday rule still applies.
            }
        }
    }
}
