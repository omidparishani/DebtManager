package com.debtmanager.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.debtmanager.app.MainActivity
import com.debtmanager.app.R
import com.debtmanager.app.data.database.AppDatabase
import com.debtmanager.app.util.CurrencyUtil
import com.debtmanager.app.util.PersianDateUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FinanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, id)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.finance_widget)
            views.setTextViewText(R.id.widget_title, "حسابداری شخصی")
            views.setTextViewText(R.id.widget_balance, "…")
            views.setTextViewText(R.id.widget_debt, "…")
            views.setTextViewText(R.id.widget_today, "…")

            val intent = Intent(context, MainActivity::class.java)
            val pending = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            manager.updateAppWidget(appWidgetId, views)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val accounts = db.bankAccountDao().getAllOnce()
                    val debts = db.debtDao().getAllDebtsOnce()
                    val balance = accounts.sumOf { it.balance }
                    val remaining = debts.sumOf { (it.totalAmount - it.paidAmount).coerceAtLeast(0) }
                    val now = System.currentTimeMillis()
                    val start = PersianDateUtil.startOfDay(now)
                    val end = PersianDateUtil.endOfDay(now)
                    val todayDebts = debts.filter { it.date in start..end }
                    val todayText = if (todayDebts.isEmpty()) "سررسید امروز: موردی نیست"
                    else "امروز: ${todayDebts.size} مورد مرتبط"

                    views.setTextViewText(R.id.widget_balance, "موجودی حساب‌ها: ${CurrencyUtil.format(balance)}")
                    views.setTextViewText(R.id.widget_debt, "مانده بدهی/طلب: ${CurrencyUtil.format(remaining)}")
                    views.setTextViewText(R.id.widget_today, todayText)
                    manager.updateAppWidget(appWidgetId, views)
                } catch (_: Exception) {
                    views.setTextViewText(R.id.widget_balance, "برای مشاهده اپ را باز کنید")
                    manager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
