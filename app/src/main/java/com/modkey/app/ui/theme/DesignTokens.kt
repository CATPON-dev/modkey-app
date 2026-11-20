package com.modkey.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.modkey.app.R

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  DESIGN SYSTEM — replaces MaterialTheme / MaterialExpressiveTheme entirely
//  No Material 3 Expressive imports. All custom tokens.
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

/** Nata Sans — the Modkey brand typeface (variable font, weight axis mapped). */
val NataSans = FontFamily(
    Font(R.font.nata_sans_variable, FontWeight.Normal),
    Font(R.font.nata_sans_variable, FontWeight.Medium),
    Font(R.font.nata_sans_variable, FontWeight.SemiBold),
    Font(R.font.nata_sans_variable, FontWeight.Bold),
    Font(R.font.nata_sans_variable, FontWeight.ExtraBold)
)

/**
 * Modkey color palette. Both variants share the same semantic names,
 * only values differ between dark and light modes.
 */
data class ModkeyPalette(
    val primary: Color,
    val primaryContainer: Color,
    val onPrimary: Color,
    val background: Color,
    val surface: Color,
    val surfaceContainer: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val onSurfaceDisabled: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val errorContainer: Color,
    val onError: Color,
    val onErrorContainer: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val divider: Color
)

val DarkModkeyPalette = ModkeyPalette(
    primary = Color(0xFF3B82F6),
    primaryContainer = Color(0xFF10223E),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFF090A0D),
    surface = Color(0xFF0E1014),
    surfaceContainer = Color(0xFF121419),
    surfaceContainerLow = Color(0xFF0E1014),
    surfaceContainerHigh = Color(0xFF161920),
    surfaceContainerHighest = Color(0xFF1E222B),
    onSurface = Color(0xFFF4F6FA),
    onSurfaceVariant = Color(0xFF8A92A2),
    onSurfaceDisabled = Color(0xFF525866),
    outline = Color(0xFF232732),
    outlineVariant = Color(0xFF1B1F28),
    error = Color(0xFFEF4444),
    errorContainer = Color(0xFFEF4444).copy(alpha = 0.14f),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFFFCA5A5),
    success = Color(0xFF22C55E),
    successContainer = Color(0xFF22C55E).copy(alpha = 0.14f),
    warning = Color(0xFFF59E0B),
    warningContainer = Color(0xFFF59E0B).copy(alpha = 0.14f),
    divider = Color(0xFF1B1F28)
)

val LightModkeyPalette = ModkeyPalette(
    primary = Color(0xFF2563EB),
    primaryContainer = Color(0xFFDBEAFE),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFFAFAFC),
    surface = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF6F7F9),
    surfaceContainerHigh = Color(0xFFF1F3F6),
    surfaceContainerHighest = Color(0xFFE5E8EE),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B),
    onSurfaceDisabled = Color(0xFF94A3B8),
    outline = Color(0xFFE2E6ED),
    outlineVariant = Color(0xFFEBEEF3),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFDC2626).copy(alpha = 0.10f),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF991B1B),
    success = Color(0xFF16A34A),
    successContainer = Color(0xFF16A34A).copy(alpha = 0.10f),
    warning = Color(0xFFD97706),
    warningContainer = Color(0xFFD97706).copy(alpha = 0.12f),
    divider = Color(0xFFEBEEF3)
)

object ModkeyDesign {

    // ── Live palette (switched by ModkeyTheme; light / dark) ──
    internal val palette = androidx.compose.runtime.mutableStateOf(DarkModkeyPalette)

    fun setPalette(isDark: Boolean) {
        palette.value = if (isDark) DarkModkeyPalette else LightModkeyPalette
    }

    // ── Colors (semantic, theme-aware) ──
    object Colors {
        // Brand
        val primary get() = palette.value.primary
        val primaryContainer get() = palette.value.primaryContainer
        val onPrimary get() = palette.value.onPrimary

        // Surface / Background
        val background get() = palette.value.background
        val surface get() = palette.value.surface
        val surfaceContainer get() = palette.value.surfaceContainer
        val surfaceContainerLow get() = palette.value.surfaceContainerLow
        val surfaceContainerHigh get() = palette.value.surfaceContainerHigh
        val surfaceContainerHighest get() = palette.value.surfaceContainerHighest

        // Text
        val onSurface get() = palette.value.onSurface
        val onSurfaceVariant get() = palette.value.onSurfaceVariant
        val onSurfaceDisabled get() = palette.value.onSurfaceDisabled

        // Border
        val outline get() = palette.value.outline
        val outlineVariant get() = palette.value.outlineVariant

        // Semantic
        val error get() = palette.value.error
        val errorContainer get() = palette.value.errorContainer
        val onError get() = palette.value.onError
        val onErrorContainer get() = palette.value.onErrorContainer

        val success get() = palette.value.success
        val successContainer get() = palette.value.successContainer

        val warning get() = palette.value.warning
        val warningContainer get() = palette.value.warningContainer

        // Divider
        val divider get() = palette.value.divider
    }

    // ── Typography (Nata Sans) ──
    object Typography {
        private val fam = NataSans

        fun displayLarge() = TextStyle(
            fontFamily = fam,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 40.sp,
            lineHeight = 48.sp,
            letterSpacing = (-0.5).sp
        )
        fun headlineLarge() = TextStyle(
            fontFamily = fam,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = (-0.25).sp
        )
        fun titleLarge() = TextStyle(
            fontFamily = fam,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 30.sp,
            letterSpacing = 0.sp
        )
        fun titleMedium() = TextStyle(
            fontFamily = fam,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.sp
        )
        fun titleSmall() = TextStyle(
            fontFamily = fam,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.1.sp
        )
        fun bodyLarge() = TextStyle(
            fontFamily = fam,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.25.sp
        )
        fun bodyMedium() = TextStyle(
            fontFamily = fam,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.2.sp
        )
        fun bodySmall() = TextStyle(
            fontFamily = fam,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.3.sp
        )
        fun labelLarge() = TextStyle(
            fontFamily = fam,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.1.sp
        )
        fun labelMedium() = TextStyle(
            fontFamily = fam,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.3.sp
        )
        fun labelSmall() = TextStyle(
            fontFamily = fam,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    }

    // ── Shapes (Crisp SaaS Panel Radii) ──
    object Shapes {
        val extraSmall = RoundedCornerShape(6.dp)
        val small = RoundedCornerShape(8.dp)
        val medium = RoundedCornerShape(12.dp)
        val large = RoundedCornerShape(14.dp)
        val extraLarge = RoundedCornerShape(16.dp)
        val full = CircleShape
    }

    // ── Spacing ──
    object Spacing {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 20.dp
        val xxl = 24.dp
        val xxxl = 32.dp
        val screen = 16.dp
    }

    // ── Motion (custom spring, no M3E) ──
    object Motion {
        val pressScale = 0.96f
        val springDamping = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
        val springStiffness = androidx.compose.animation.core.Spring.StiffnessMedium
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
//  THEME MODE
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

enum class ThemeMode(val label: String) {
    SYSTEM("Системная"),
    LIGHT("Светлая"),
    DARK("Тёмная")
}
