package com.acuranza.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.navigation.NavController
import com.acuranza.app.designsystem.components.AcuranzaCard
import com.acuranza.app.designsystem.components.PrimaryButton
import com.acuranza.app.designsystem.components.SecondaryButton
import com.acuranza.app.designsystem.theme.AcuranzaText
import com.acuranza.app.designsystem.theme.AcuranzaTheme
import com.acuranza.app.model.Money
import com.acuranza.app.ui.common.AcuranzaScreen
import com.acuranza.app.ui.common.LocalContainer
import com.acuranza.app.ui.common.LocalSettings
import com.acuranza.app.ui.nav.popBackSafely
import com.acuranza.app.util.Period
import com.acuranza.app.util.Time
import kotlinx.coroutines.launch

/**
 * The Savings Goal screen — the widget's real destination. Shows this period's net savings
 * against the configured target and lets the user set, change or clear the target. Editing the
 * target here preserves the salary the user entered in the Home plan sheet.
 */
@Composable
fun SavingsGoalScreen(nav: NavController) {
    val c = LocalContainer.current
    val settings = LocalSettings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }

    // Loaded once: the plan's current target (and the salary to preserve on save).
    var targetText by remember { mutableStateOf<String?>(null) }
    var salary by remember { mutableStateOf<Long?>(null) }
    var savedThisPeriod by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        val prefs = c.featurePrefs.current()
        salary = prefs.salary
        targetText = prefs.savingsTarget?.let { (it / 100).toString() }.orEmpty()
        val period = Period.containing(Time.today(), settings.payCycle)
        val txns = c.db.transactions().between(period.startMillis, period.endMillis)
        val f = com.acuranza.app.tools.WidgetFigures.of(period, txns, null)
        savedThisPeriod = f.saved
    }

    AcuranzaScreen(
        title = "Savings goal",
        subtitle = "What you want to set aside each period",
        onBack = { nav.popBackSafely() },
        snackbar = snackbar,
    ) { padding ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            val target = targetText?.filter { it.isDigit() }?.takeIf { it.isNotBlank() }?.toLongOrNull()?.times(100)
            AcuranzaCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Saved this period", style = AcuranzaText.sectionLabel, color = AcuranzaTheme.colors.subtleText)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        Money.format(savedThisPeriod, showPaise = false),
                        style = AcuranzaText.screenTitle,
                        color = if (savedThisPeriod < 0) AcuranzaTheme.colors.alert else AcuranzaTheme.colors.ink,
                    )
                    if (target != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "of ${Money.format(target, showPaise = false)} target · " +
                                "${if (target > 0) ((savedThisPeriod * 100) / target).toInt().coerceIn(0, 100) else 0}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = AcuranzaTheme.colors.subtleText,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            AcuranzaCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Monthly target", style = AcuranzaText.sectionLabel, color = AcuranzaTheme.colors.subtleText)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = targetText ?: "",
                        onValueChange = { v -> targetText = v.filter { it.isDigit() }.take(9) },
                        placeholder = { Text("e.g. 20000", style = MaterialTheme.typography.bodyMedium) },
                        prefix = { Text("₹") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        shape = RoundedCornerShape(16.dp),
                        textStyle = TextStyle(color = AcuranzaTheme.colors.ink),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = AcuranzaTheme.colors.cardSurface,
                            focusedContainerColor = AcuranzaTheme.colors.cardSurface,
                            unfocusedBorderColor = AcuranzaTheme.colors.hairline,
                            focusedBorderColor = AcuranzaTheme.colors.accent,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val haptic = com.acuranza.app.ui.common.rememberHaptic()
                        SecondaryButton(
                            "Remove",
                            onClick = {
                                haptic.confirm()
                                scope.launch {
                                    c.featurePrefs.setSalaryPlan(salary, null)
                                    android.widget.Toast.makeText(context, "Goal removed", android.widget.Toast.LENGTH_SHORT).show()
                                    nav.popBackSafely()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = salary != null || (target ?: 0L) > 0 || targetText?.isNotEmpty() == true,
                        )
                        PrimaryButton(
                            "Save",
                            onClick = {
                                haptic.confirm()
                                scope.launch {
                                    c.featurePrefs.setSalaryPlan(salary, target)
                                    android.widget.Toast.makeText(context, "Savings goal saved", android.widget.Toast.LENGTH_SHORT).show()
                                    nav.popBackSafely()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = target != null,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "The goal guides the savings widget and the safe-to-spend plan. Your salary entry " +
                    "from the Home plan is kept as it is.",
                style = MaterialTheme.typography.bodySmall,
                color = AcuranzaTheme.colors.subtleText,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
