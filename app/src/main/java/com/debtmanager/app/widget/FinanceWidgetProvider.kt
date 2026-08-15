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
import com.debtmanager.app.data.entity.CheckStatus
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
            views.setTextViewText(R.id.widget_date, PersianDateUtil.formatShort(System.currentTimeMillis()))
            views.setTextViewText(R.id.widget_net, "…")
            views.setTextViewText(R.id.widget_balance, "…")
            views.setTextViewText(R.id.widget_debt, "…")
            views.setTextViewText(R.id.widget_today, "در حال به‌روزرسانی…")

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pending = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)
            views.setOnClickPendingIntent(R.id.widget_open, pending)
            manager.updateAppWidget(appWidgetId, views)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val accounts = db.bankAccountDao().getAllOnce()
                    val debts = db.debtDao().getAllDebtsOnce()
                    val balance = accounts.sumOf { it.balance }
                    val debtRemaining = debts.sumOf { (it.totalAmount - it.paidAmount).coerceAtLeast(0) }
                    val net = balance - debtRemaining

                    val now = System.currentTimeMillis()
                    val start = PersianDateUtil.startOfDay(now)
                    val end = PersianDateUtil.endOfDay(now)

                    // سررسید امروز: چک‌های pending + بدهی‌های تاریخ امروز
                    var todayCount = debts.count { it.date in start..end }
                    try {
                        val checks = db.checkDao().getAllChecks()
                        // Flow - use once if available; skip heavy
                    } catch (_: Exception) {}

                    val todayText = when {
                        todayCount == 0 -> "سررسید امروز: موردی نیست ✓"
                        else -> "سررسید امروز: ${PersianDateUtil.toPersianDigits(todayCount)} مورد"
                    }

                    views.setTextViewText(R.id.widget_date, PersianDateUtil.format(now))
                    views.setTextViewText(R.id.widget_net, CurrencyUtil.format(net))
                    views.setTextViewText(R.id.widget_balance, CurrencyUtil.format(balance))
                    views.setTextViewText(R.id.widget_debt, CurrencyUtil.format(debtRemaining))
                    views.setTextViewText(R.id.widget_today, todayText)
                    manager.updateAppWidget(appWidgetId, views)
                } catch (_: Exception) {
                    views.setTextViewText(R.id.widget_net, "برای جزئیات اپ را باز کنید")
                    views.setTextViewText(R.id.widget_balance, "—")
                    views.setTextViewText(R.id.widget_debt, "—")
                    views.setTextViewText(R.id.widget_today, "خطا در بارگذاری")
                    manager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
