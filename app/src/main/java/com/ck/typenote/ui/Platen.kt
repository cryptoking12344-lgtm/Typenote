package com.ck.typenote.ui

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect

/**
 * The platen: the black rubber roller the paper wraps around, with chrome knobs,
 * the metal scale bar under it and the red carriage marker riding along the bar.
 *
 * [roll] is the roller's turn in px (grows each line feed) so the grooves move.
 * [carriage] is 0..1, the current typing column across the page.
 */
@Composable
fun Platen(roll: Float, carriage: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val d = density
        val w = size.width
        val knobW = 20 * d
        val knobH = 48 * d
        val knobTop = 9 * d

        // Knobs
        drawKnob(Offset(4 * d, knobTop), Size(knobW, knobH), mirrored = false, roll = roll)
        drawKnob(Offset(w - 4 * d - knobW, knobTop), Size(knobW, knobH), mirrored = true, roll = roll)

        // Rubber cylinder
        val cylLeft = 22 * d
        val cylTop = 12 * d
        val cylH = 42 * d
        val cylW = w - 44 * d
        // drop shadow onto the paper
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(cylLeft, cylTop + 6 * d),
            size = Size(cylW, cylH),
            cornerRadius = CornerRadius(10 * d),
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                0f to Color(0xFF050505),
                0.20f to Color(0xFF262320),
                0.33f to Color(0xFF57514A),
                0.50f to Color(0xFF2A2723),
                0.78f to Color(0xFF121110),
                1f to Color(0xFF030303),
                startY = cylTop, endY = cylTop + cylH,
            ),
            topLeft = Offset(cylLeft, cylTop),
            size = Size(cylW, cylH),
            cornerRadius = CornerRadius(10 * d),
        )
        // Rubber grooves that slide as the roller turns
        clipRect(cylLeft, cylTop + 2 * d, cylLeft + cylW, cylTop + cylH - 2 * d) {
            val gap = 7 * d
            val phase = ((roll % gap) + gap) % gap
            var y = cylTop + phase
            while (y < cylTop + cylH) {
                val rel = (y - cylTop) / cylH
                // brighter near the highlight band, fading toward the edges of the curve
                val a = (0.10f * (1f - kotlin.math.abs(rel - 0.33f) * 2.2f)).coerceAtLeast(0.015f)
                drawLine(Color.White.copy(alpha = a), Offset(cylLeft, y), Offset(cylLeft + cylW, y), strokeWidth = 1 * d)
                y += gap
            }
        }

        // Scale bar (paper guide)
        val barLeft = 30 * d
        val barRight = w - 30 * d
        val barTop = 50 * d
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(barLeft, barTop + 2 * d),
            size = Size(barRight - barLeft, 14 * d),
            cornerRadius = CornerRadius(2 * d),
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFFE4E0D8), Color(0xFFA8A399)), startY = barTop, endY = barTop + 14 * d,
            ),
            topLeft = Offset(barLeft, barTop),
            size = Size(barRight - barLeft, 14 * d),
            cornerRadius = CornerRadius(2 * d),
        )
        var x = barLeft + 4 * d
        var i = 0
        while (x < barRight - 4 * d) {
            val top = if (i % 5 == 0) barTop + 3 * d else barTop + 7 * d
            drawLine(Color(0xFF28231E).copy(alpha = 0.75f), Offset(x, top), Offset(x, barTop + 13 * d), strokeWidth = 1 * d)
            x += 8 * d
            i++
        }

        // Carriage marker
        val markerW = 12 * d
        val mx = barLeft + carriage.coerceIn(0f, 1f) * (barRight - barLeft) - markerW / 2
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.5f),
            topLeft = Offset(mx, 44 * d),
            size = Size(markerW, 26 * d),
            cornerRadius = CornerRadius(4 * d),
        )
        drawRoundRect(
            color = Ribbon,
            topLeft = Offset(mx, 42 * d),
            size = Size(markerW, 26 * d),
            cornerRadius = CornerRadius(4 * d),
        )
        drawLine(Color.White.copy(alpha = 0.25f), Offset(mx + 3 * d, 45 * d), Offset(mx + 3 * d, 64 * d), strokeWidth = 1 * d)
    }
}

private fun DrawScope.drawKnob(topLeft: Offset, size: Size, mirrored: Boolean, roll: Float) {
    val d = density
    val stops = if (!mirrored) {
        arrayOf(0f to Color(0xFF5E5A55), 0.38f to Color(0xFFECE9E3), 0.70f to Color(0xFF9C988F), 1f to Color(0xFF45413C))
    } else {
        arrayOf(0f to Color(0xFF45413C), 0.30f to Color(0xFF9C988F), 0.62f to Color(0xFFECE9E3), 1f to Color(0xFF5E5A55))
    }
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.5f),
        topLeft = topLeft + Offset(0f, 4 * d),
        size = size,
        cornerRadius = CornerRadius(7 * d),
    )
    drawRoundRect(
        brush = Brush.horizontalGradient(*stops, startX = topLeft.x, endX = topLeft.x + size.width),
        topLeft = topLeft,
        size = size,
        cornerRadius = CornerRadius(7 * d),
    )
    // knurled ridges, turning with the roller
    clipRect(topLeft.x, topLeft.y + 3 * d, topLeft.x + size.width, topLeft.y + size.height - 3 * d) {
        val gap = 4 * d
        val phase = ((roll % gap) + gap) % gap
        var y = topLeft.y + phase
        while (y < topLeft.y + size.height) {
            drawLine(Color.Black.copy(alpha = 0.22f), Offset(topLeft.x, y), Offset(topLeft.x + size.width, y), strokeWidth = 1 * d)
            y += gap
        }
    }
}
