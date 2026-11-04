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
    primary = Color(0xFF00D4FF),
    primaryContainer = Color(0xFF00D4FF).copy(alpha = 0.12f),
    onPrimary = Color(0xFF000000),
    background = Color(0xFF0A0A0F),
    surface = Color(0xFF13131A),
    surfaceContainer = Color(0xFF1A1A22),
    surfaceContainerLow = Color(0xFF16161D),
    surfaceContainerHigh = Color(0xFF22222C),
    surfaceContainerHighest = Color(0xFF2A2A35),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF9494A3),
    onSurfaceDisabled = Color(0xFF5A5A6B),
    outline = Color(0xFF2A2A35),
    outlineVariant = Color(0xFF1F1F29),
    error = Color(0xFFFF4D4F),
    errorContainer = Color(0xFFFF4D4F).copy(alpha = 0.12f),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFFFFFFFF),
    success = Color(0xFF00C896),
    successContainer = Color(0xFF00C896).copy(alpha = 0.12f),
    warning = Color(0xFFFFA940),
    warningContainer = Color(0xFFFFA940).copy(alpha = 0.12f),
    divider = Color(0xFF1F1F29)
)

val LightModkeyPalette = ModkeyPalette(
    primary = Color(0xFF00A0CB),
    primaryContainer = Color(0xFF00D4FF).copy(alpha = 0.18f),
    onPrimary = Color(0xFF000000),
    background = Color(0xFFF4F6FA),
    surface = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF2F4F9),
    surfaceContainerLow = Color(0xFFF8F9FC),
    surfaceContainerHigh = Color(0xFFEAEEF4),
    surfaceContainerHighest = Color(0xFFDFE5ED),
    onSurface = Color(0xFF0E1319),
    onSurfaceVariant = Color(0xFF5C6673),
    onSurfaceDisabled = Color(0xFFA6ADB9),
    outline = Color(0xFFD7DDE7),
    outlineVariant = Color(0xFFE7EBF1),
    error = Color(0xFFE5484D),
    errorContainer = Color(0xFFE5484D).copy(alpha = 0.12f),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF93201F),
    success = Color(0xFF0EA47E),
    successContainer = Color(0xFF0EA47E).copy(alpha = 0.12f),
    warning = Color(0xFFE8930C),
    warningContainer = Color(0xFFE8930C).copy(alpha = 0.14f),
    divider = Color(0xFFE7EBF1)
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

    // ── Shapes ──
    object Shapes {
        val extraSmall = RoundedCornerShape(8.dp)
        val small = RoundedCornerShape(12.dp)
        val medium = RoundedCornerShape(16.dp)
        val large = RoundedCornerShape(24.dp)
        val extraLarge = RoundedCornerShape(32.dp)
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
