@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.debtmanager.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val StatusPaid = Color(0xFF2E7D32)
val StatusOverdue = Color(0xFFC62828)
val StatusUpcoming = Color(0xFF1565C0)

data class ThemePalette(
    val primary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val tertiary: Color
)

fun paletteFor(key: String): ThemePalette = when (key) {
    "blue" -> ThemePalette(Color(0xFF1565C0), Color(0xFFBBDEFB), Color(0xFF0277BD), Color(0xFFFF8A65))
    "purple" -> ThemePalette(Color(0xFF6A1B9A), Color(0xFFE1BEE7), Color(0xFF8E24AA), Color(0xFFFFB74D))
    "green" -> ThemePalette(Color(0xFF2E7D32), Color(0xFFC8E6C9), Color(0xFF00897B), Color(0xFFFF8A65))
    "orange" -> ThemePalette(Color(0xFFE65100), Color(0xFFFFE0B2), Color(0xFFF57C00), Color(0xFF5C6BC0))
    "red" -> ThemePalette(Color(0xFFC62828), Color(0xFFFFCDD2), Color(0xFFD84315), Color(0xFF5C6BC0))
    "indigo" -> ThemePalette(Color(0xFF3949AB), Color(0xFFC5CAE9), Color(0xFF5C6BC0), Color(0xFFFF8A65))
    else -> ThemePalette(Color(0xFF0D7377), Color(0xFFB2DFDB), Color(0xFF14919B), Color(0xFFFF8A65)) // teal
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DebtManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColorKey: String = "teal",
    content: @Composable () -> Unit
) {
    val palette = paletteFor(themeColorKey)
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.primary.copy(alpha = 1f).let {
                // lighter primary for dark
                Color(
                    red = (it.red * 0.7f + 0.3f).coerceIn(0f, 1f),
                    green = (it.green * 0.7f + 0.3f).coerceIn(0f, 1f),
                    blue = (it.blue * 0.7f + 0.3f).coerceIn(0f, 1f)
                )
            },
            onPrimary = Color(0xFF003D40),
            primaryContainer = palette.primary.copy(alpha = 0.35f),
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            background = Color(0xFF0F1419),
            surface = Color(0xFF1A2332),
            surfaceVariant = Color(0xFF243040),
            onSurface = Color(0xFFE2E2E6),
            onSurfaceVariant = Color(0xFFBFC9C8),
            error = Color(0xFFF2B8B5),
            outline = Color(0xFF899391)
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = Color.White,
            primaryContainer = palette.primaryContainer,
            onPrimaryContainer = Color(0xFF002020),
            secondary = palette.secondary,
            onSecondary = Color.White,
            tertiary = palette.tertiary,
            background = Color(0xFFF5F7FA),
            surface = Color.White,
            surfaceVariant = Color(0xFFE8EEF2),
            onSurface = Color(0xFF1A1C1E),
            onSurfaceVariant = Color(0xFF3F4948),
            error = Color(0xFFB3261E),
            outline = Color(0xFF6F7978)
        )
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        typography = Typography(),
        content = content
    )
}

fun statusColor(status: com.debtmanager.app.data.entity.InstallmentStatus): Color = when (status) {
    com.debtmanager.app.data.entity.InstallmentStatus.PAID -> StatusPaid
    com.debtmanager.app.data.entity.InstallmentStatus.OVERDUE -> StatusOverdue
    com.debtmanager.app.data.entity.InstallmentStatus.UPCOMING -> StatusUpcoming
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
