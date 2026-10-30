package com.modkey.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

val ExpressiveAppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

/**
 * Material 3 Expressive connected list shape.
 * Groups items into rounded visual blocks with subtle inner radiuses.
 */
fun connectedItemShape(
    index: Int,
    count: Int,
    outer: Dp = 24.dp,
    inner: Dp = 6.dp
): Shape {
    if (count <= 1) return RoundedCornerShape(outer)
    val top = if (index == 0) outer else inner
    val bottom = if (index == count - 1) outer else inner
    return RoundedCornerShape(
        topStart = top,
        topEnd = top,
        bottomStart = bottom,
        bottomEnd = bottom
    )
}

fun Morph.toComposePath(progress: Float, path: Path = Path()): Path {
    var isFirst = true
    path.rewind()
    forEachCubic(progress) { cubic: Cubic ->
        if (isFirst) {
            path.moveTo(cubic.anchor0X, cubic.anchor0Y)
            isFirst = false
        }
        path.cubicTo(
            cubic.control0X, cubic.control0Y,
            cubic.control1X, cubic.control1Y,
            cubic.anchor1X, cubic.anchor1Y
        )
    }
    path.close()
    return path
}

class MorphShape(
    private val morph: Morph,
    private val progress: Float
) : Shape {
    private val path = Path()
    private val matrix = Matrix()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        path.rewind()
        morph.toComposePath(progress = progress, path = path)

        matrix.reset()
        matrix.scale(size.width / 2f, size.height / 2f)
        matrix.translate(1f, 1f)
        path.transform(matrix)

        return Outline.Generic(path)
    }
}

object ExpressivePolygons {
    val squircle by lazy {
        RoundedPolygon(
            numVertices = 4,
            rounding = CornerRounding(0.45f, smoothing = 0.2f)
        )
    }

    val softHexagon by lazy {
        RoundedPolygon(
            numVertices = 6,
            rounding = CornerRounding(0.35f, smoothing = 0.15f)
        )
    }
}
