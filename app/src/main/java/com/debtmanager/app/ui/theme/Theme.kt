package com.debtmanager.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val StatusPaid = Color(0xFF2E7D32)
val StatusUpcoming = Color(0xFFF57C00)
val StatusOverdue = Color(0xFFC62828)
val PrimaryBlue = Color(0xFF1565C0)
val PrimaryBlueDark = Color(0xFF90CAF9)
val AccentTeal = Color(0xFF00838F)
val SurfaceGradientStart = Color(0xFFF0F4FF)
val SurfaceGradientEnd = Color(0xFFF5F7FA)

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    secondary = AccentTeal,
    secondaryContainer = Color(0xFFB2EBF2),
    background = Color(0xFFF5F7FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8EDF5),
    error = StatusOverdue
)

private val DarkColors = darkColorScheme(
    primary = PrimaryBlueDark,
    onPrimary = Color(0xFF0D47A1),
    primaryContainer = Color(0xFF1A3A5C),
    secondary = Color(0xFF4DD0E1),
    secondaryContainer = Color(0xFF004D56),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2A2A2A),
    error = Color(0xFFEF5350)
)

@Composable
fun DebtManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(
            bodyLarge = MaterialTheme.typography.bodyLarge,
            titleLarge = MaterialTheme.typography.titleLarge
        ),
        content = content
    )
}

fun statusColor(status: com.debtmanager.app.data.entity.InstallmentStatus): Color = when (status) {
    com.debtmanager.app.data.entity.InstallmentStatus.PAID -> StatusPaid
    com.debtmanager.app.data.entity.InstallmentStatus.UPCOMING -> StatusUpcoming
    com.debtmanager.app.data.entity.InstallmentStatus.OVERDUE -> StatusOverdue
}

fun checkStatusColor(status: String): Color = when (status) {
    "COLLECTED" -> StatusPaid
    "PENDING" -> StatusUpcoming
    "BOUNCED", "CANCELLED" -> StatusOverdue
    else -> StatusUpcoming
}

fun checkStatusLabel(status: String): String = when (status) {
    "PENDING" -> "در انتظار"
    "COLLECTED" -> "وصول‌شده"
    "BOUNCED" -> "برگشتی"
    "CANCELLED" -> "باطل"
    else -> status
}
