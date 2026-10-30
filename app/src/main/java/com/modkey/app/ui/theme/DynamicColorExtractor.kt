package com.modkey.app.ui.theme

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlin.math.abs
import kotlin.math.pow

object DynamicColorExtractor {
    val defaultSeedColor = Color(0xFF1E88E5) // Modkey brand cyan-blue

    fun extractSeedColor(bitmap: Bitmap?): Color? {
        if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) return null

        val marginX = (bitmap.width * 0.12f).toInt()
        val marginY = (bitmap.height * 0.15f).toInt()
        val left = marginX.coerceAtMost(bitmap.width - 1)
        val top = marginY.coerceAtMost(bitmap.height - 1)
        val right = (bitmap.width - marginX).coerceAtLeast(left + 1)
        val bottom = (bitmap.height - marginY).coerceAtLeast(top + 1)

        val palette = Palette.from(bitmap)
            .maximumColorCount(24)
            .clearFilters()
            .setRegion(left, top, right, bottom)
            .generate()

        val swatches = palette.swatches
        if (swatches.isEmpty()) return null

        val maxPop = swatches.maxOf { it.population }.coerceAtLeast(1).toFloat()
        var bestSwatch: Palette.Swatch? = null
        var bestScore = -1f

        for (swatch in swatches) {
            val hsl = swatch.hsl
            val saturation = hsl[1]
            val lightness = hsl[2]

            if (lightness < 0.08f || lightness > 0.92f) continue

            val popWeight = (swatch.population / maxPop).pow(0.35f)
            val satWeight = when {
                saturation >= 0.35f -> 1.4f + saturation * 0.6f
                saturation >= 0.18f -> 0.9f + saturation
                else -> 0.25f * saturation
            }
            val lightWeight = 1f - (abs(lightness - 0.50f) * 1.25f).coerceIn(0f, 0.85f)
            val score = popWeight * satWeight * lightWeight

            if (score > bestScore) {
                bestScore = score
                bestSwatch = swatch
            }
        }

        val chosen = bestSwatch
            ?: palette.vibrantSwatch
            ?: palette.dominantSwatch
            ?: swatches.maxByOrNull { it.population }
            ?: return null

        val hsl = chosen.hsl.clone()
        if (hsl[1] in 0.08f..0.45f) {
            hsl[1] = (hsl[1] * 1.35f).coerceAtMost(0.78f)
        }
        hsl[2] = hsl[2].coerceIn(0.32f, 0.68f)

        return Color(ColorUtils.HSLToColor(hsl))
    }
}
