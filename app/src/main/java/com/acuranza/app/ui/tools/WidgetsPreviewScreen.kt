package com.acuranza.app.ui.tools

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.acuranza.app.designsystem.components.PrimaryButton
import com.acuranza.app.designsystem.theme.SumvaloTheme
import com.acuranza.app.model.TxnType
import com.acuranza.app.tools.CategorySlice
import com.acuranza.app.tools.GoalProgress
import com.acuranza.app.tools.MonthWidgetProvider
import com.acuranza.app.tools.QuickAddWidgetProvider
import com.acuranza.app.tools.SavingsGoalWidgetProvider
import com.acuranza.app.tools.CategoryDonutWidgetProvider
import com.acuranza.app.tools.ShortcutsWidgetProvider
import com.acuranza.app.tools.WidgetFigures
import com.acuranza.app.tools.WidgetViews
import com.acuranza.app.ui.common.SumvaloScreen
import com.acuranza.app.ui.nav.popBackSafely
import com.acuranza.app.ui.settings.SettingsRow
import com.acuranza.app.util.Period
import com.acuranza.app.util.Time

/**
 * The Add Widgets screen: every widget previewed with its ACTUAL RemoteViews rendering — the same
 * WidgetViews builders the providers ship — filled with clearly-labelled sample data. "Add" uses
 * the system's pin request, which places the real widget on the home screen.
 */
@Composable
fun WidgetsPreviewScreen(nav: NavController) {
    val context = LocalContext.current
    val manager = remember { AppWidgetManager.getInstance(context) }

    SumvaloScreen(
        title = "Add widgets",
        subtitle = "Preview, then pin to your home screen",
        onBack = { nav.popBackSafely() },
    ) { padding ->
        Column(
            Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Text(
                "Previews use sample data. Colors follow your system's light or dark mode, exactly " +
                    "like the real widgets.",
                style = MaterialTheme.typography.bodySmall,
                color = SumvaloTheme.colors.subtleText,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
            WidgetEntry(
                name = "This month",
                description = "Net savings, savings rate, income and spending, with a progress ring and budget status.",
                sizes = "3×2 · resizable",
                pinnedSupported = manager.isRequestPinAppWidgetSupported,
                onAdd = { pin(context, MonthWidgetProvider::class.java) },
            ) { frame ->
                val f = WidgetFigures(income = 52_400_00, spent = 31_900_00, barPercent = 61)
                frame.addView(
                    WidgetViews.month(
                        context,
                        f,
                        show = true,
                        title = "This month",
                        ringFraction = 0.61f,
                        budgetCaption = "61% of ₹50,000 budget",
                    ).apply(context, frame),
                )
            }
            WidgetEntry(
                name = "Savings goal",
                description = "This period's savings against your target, on a ring. Tap opens the goal screen.",
                sizes = "3×2 · resizable",
                pinnedSupported = manager.isRequestPinAppWidgetSupported,
                onAdd = { pin(context, SavingsGoalWidgetProvider::class.java) },
            ) { frame ->
                frame.addView(
                    WidgetViews.goal(context, GoalProgress(saved = 28_600_00, target = 40_000_00), show = true)
                        .apply(context, frame),
                )
            }
            WidgetEntry(
                name = "Spending by category",
                description = "A donut of the period's top spending categories with amounts. Tap opens Reports.",
                sizes = "3×2 · resizable",
                pinnedSupported = manager.isRequestPinAppWidgetSupported,
                onAdd = { pin(context, CategoryDonutWidgetProvider::class.java) },
            ) { frame ->
                val slices = listOf(
                    CategorySlice("Food", 12_600_00, 0.40f, 0xFF2563EB.toInt()),
                    CategorySlice("Transport", 6_300_00, 0.20f, 0xFF0D9488.toInt()),
                    CategorySlice("Bills", 5_700_00, 0.18f, 0xFF9333EA.toInt()),
                    CategorySlice("Other", 6_100_00, 0.22f, 0xFF94A3B8.toInt()),
                )
                frame.addView(WidgetViews.categories(context, slices, show = true).apply(context, frame))
            }
            WidgetEntry(
                name = "Quick add",
                description = "Log an expense or income, start a transfer, or open your transactions.",
                sizes = "4×2 · resizable",
                pinnedSupported = manager.isRequestPinAppWidgetSupported,
                onAdd = { pin(context, QuickAddWidgetProvider::class.java) },
            ) { frame ->
                frame.addView(WidgetViews.quickAdd(context).apply(context, frame))
            }
            WidgetEntry(
                name = "Shortcuts",
                description = "Bills, budgets, reports and accounts, one tap away.",
                sizes = "4×2 · resizable",
                pinnedSupported = manager.isRequestPinAppWidgetSupported,
                onAdd = { pin(context, ShortcutsWidgetProvider::class.java) },
            ) { frame ->
                frame.addView(WidgetViews.shortcuts(context).apply(context, frame))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun pin(context: android.content.Context, provider: Class<*>) {
    val manager = AppWidgetManager.getInstance(context)
    runCatching { manager.requestPinAppWidget(ComponentName(context, provider), null, null) }
}

/** One preview card: the live widget rendering, its name, description, sizes, and the Add action. */
@Composable
private fun WidgetEntry(
    name: String,
    description: String,
    sizes: String,
    pinnedSupported: Boolean,
    onAdd: () -> Unit,
    preview: (FrameLayout) -> Unit,
) {
    val context = LocalContext.current
    Text(
        name,
        style = MaterialTheme.typography.titleMedium,
        color = SumvaloTheme.colors.ink,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
    )
    androidx.compose.foundation.layout.Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .height(150.dp),
    ) {
        // The real widget rendering — RemoteViews applied into a plain container.
        AndroidView(
            factory = { ctx -> FrameLayout(ctx) },
            update = { frame ->
                frame.removeAllViews()
                preview(frame)
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Text(
        "$description · $sizes",
        style = MaterialTheme.typography.bodySmall,
        color = SumvaloTheme.colors.subtleText,
        modifier = Modifier.padding(top = 6.dp, start = 4.dp),
    )
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        if (pinnedSupported) {
            PrimaryButton("Add to home screen", onClick = onAdd)
        } else {
            Text(
                "Add it from your launcher: long-press the home screen → Widgets → Sumvalo.",
                style = MaterialTheme.typography.bodySmall,
                color = SumvaloTheme.colors.subtleText,
            )
        }
    }
    Spacer(Modifier.height(10.dp))
}
