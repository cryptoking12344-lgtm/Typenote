package com.ck.typenote.ui

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Thin stroke icons matching the design mockups. */
object LineIcons {
    private fun icon(name: String, vararg paths: String): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        paths.forEach { d ->
            b.addPath(
                pathData = addPathNodes(d),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    val Back = icon("back", "M15 18l-6-6 6-6")
    val Paper = icon("paper", "M6 3h9l4 4v14H6z", "M15 3v4h4", "M9 12h7M9 16h5")
    val Lamp = icon("lamp", "M8 3h8l3 8H5z", "M12 11v7", "M8 21h8")
    val Sound = icon("sound", "M4 9v6h4l5 4V5L8 9z", "M16.5 8.5a5 5 0 0 1 0 7", "M19 6a8.5 8.5 0 0 1 0 12")
    val Mute = icon("mute", "M4 9v6h4l5 4V5L8 9z", "M17 9l5 6M22 9l-5 6")
    val Save = icon("save", "M5 3h11l3 3v15H5z", "M8 3v5h7V3", "M8 21v-7h8v7")
    val Share = icon("share", "M12 15V3", "M7 8l5-5 5 5", "M5 13v7h14v-7")
    val Trash = icon("trash", "M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3")
    val Search = icon("search", "M11 4a7 7 0 1 1 0 14a7 7 0 1 1 0-14z", "M20 20l-3.5-3.5")
    val Plus = icon("plus", "M12 5v14M5 12h14")
    val Minus = icon("minus", "M6 12h12")
    val Restore = icon("restore", "M4 12a8 8 0 1 0 2.3-5.7", "M4 4v4h4")
    val Close = icon("close", "M6 6l12 12M18 6L6 18")
}
