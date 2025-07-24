package com.example.expenses
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.flow.firstOrNull

class BalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BalanceWidget()
}

class BalanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = BalanceRepository(context)
        // Fetch the latest balance from our repository
        val balance = repository.balanceFlow.firstOrNull()

        provideContent {
            GlanceTheme {
                WidgetContent(balance = balance)
            }
        }
    }

    @Composable
    private fun WidgetContent(balance: Double?) {
        val balanceColor = if ((balance ?: 0.0) >= 0) GlanceTheme.colors.onSurface else GlanceTheme.colors.error

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Current Balance",
                style = TextStyle(fontSize = 16.sp, color = GlanceTheme.colors.onSurfaceVariant)
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = balance?.let { String.format("₹%.2f", it) } ?: "Not Set",
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor
                )
            )
        }
    }
}
