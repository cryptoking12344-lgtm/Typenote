package com.ck.typenote.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.ck.typenote.R

val Desk = Color(0xFF191613)
val DeskTop = Color(0xFF2A241E)
val Sheet = Color(0xFF1D1915)
val Field = Color(0xFF26211C)
val FieldBorder = Color(0xFF3F372E)
val Cream = Color(0xFFEDE6DA)
val Muted = Color(0xFFA79E91)
val Label = Color(0xFFB7AE9F)
val Ink = Color(0xFF24211D)
val Ribbon = Color(0xFFB8432E)
val Brass = Color(0xFFE0BE7E)

val PaperColors = listOf(
    Color(0xFFF3EDE1), // ivory
    Color(0xFFF7F3EA), // white
    Color(0xFFEDE3CF), // aged
    Color(0xFFE9E4DA), // grey
)
val PaperNames = listOf("Ivory", "White", "Aged", "Grey")

val TypeFont = FontFamily(Font(R.font.special_elite))

@Composable
fun TypenoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Ribbon,
            onPrimary = Color.White,
            secondary = Brass,
            background = Desk,
            surface = Sheet,
            onSurface = Cream,
            onBackground = Cream,
            surfaceVariant = Field,
            onSurfaceVariant = Label,
        ),
        content = content,
    )
}
