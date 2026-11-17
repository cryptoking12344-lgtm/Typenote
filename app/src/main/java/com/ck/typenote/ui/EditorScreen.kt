package com.ck.typenote.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.withStyle
import com.ck.typenote.Graph
import com.ck.typenote.data.InkStyle
import com.ck.typenote.data.MAX_CHARS
import com.ck.typenote.data.Note
import com.ck.typenote.data.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.random.Random

// The hidden input field always holds one of these. Typing makes it longer,
// backspace makes it shorter; we read the difference, then put it back.
private const val SENT_A = "        "
private const val SENT_B = "        "

private val counterFormat: NumberFormat = NumberFormat.getIntegerInstance()

@Composable
fun EditorScreen(noteId: Long, onClose: () -> Unit) {
    val store = Graph.notes
    val feel = Graph.feel
    val settings by Graph.settings.settings.collectAsState()
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }

    val original = remember(noteId) { store.get(noteId) ?: Note(id = noteId) }
    var text by remember(noteId) { mutableStateOf(original.text) }
    var struck by remember(noteId) {
        mutableStateOf(original.struck.padEnd(original.text.length, '0').take(original.text.length))
    }
    var dirty by remember { mutableStateOf(false) }
    var showPaperSheet by remember { mutableStateOf(false) }
    var showLampSheet by remember { mutableStateOf(false) }

    fun current() = original.copy(text = text, struck = struck)
    fun saveNow() {
        if (text.isNotBlank()) store.save(current())
        dirty = false
    }
    fun close() {
        if (text.isBlank()) store.deleteForever(noteId) else store.save(current())
        keyboard?.hide()
        onClose()
    }
    BackHandler { close() }

    // Auto-save shortly after typing stops.
    LaunchedEffect(text, struck) {
        if (dirty) {
            delay(900)
            saveNow()
        }
    }
    DisposableEffect(noteId) { onDispose { if (text.isNotBlank()) store.save(current()) } }

    // ---- typing ------------------------------------------------------------
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var lastLineCount by remember { mutableIntStateOf(-1) }
    var bellLine by remember { mutableIntStateOf(-1) }
    var justTyped by remember { mutableStateOf(false) }
    var scrollBack by remember { mutableFloatStateOf(0f) }
    val roll = remember { Animatable(0f) }
    val lineHeightSp = settings.typeSize * 1.75f
    val lineHeightPx = with(density) { lineHeightSp.sp.toPx() }

    fun rollLine() {
        scope.launch { roll.animateTo(roll.value + lineHeightPx, tween(240)) }
    }

    fun type(c: Char) {
        if (text.length >= MAX_CHARS) {
            feel.thud(settings.haptics)
            return
        }
        text += c
        struck += '0'
        dirty = true
        justTyped = true
        scrollBack = 0f
        when (c) {
            '\n' -> {
                feel.carriageReturn(settings.keySounds, settings.haptics)
                bellLine = -1
            }
            ' ' -> feel.space(settings.keySounds, settings.haptics)
            else -> feel.key(settings.keySounds, settings.haptics)
        }
    }

    fun backspace() {
        if (text.isEmpty()) return
        scrollBack = 0f
        if (!settings.strikeThrough) {
            text = text.dropLast(1)
            struck = struck.dropLast(1)
        } else {
            var i = text.length - 1
            while (i >= 0 && struck[i] == '1') i--
            if (i < 0) return
            val c = text[i]
            if (c == ' ' || c == '\n') {
                // Blank space is simply backed over, but only at the very end.
                if (i != text.length - 1) return
                text = text.dropLast(1)
                struck = struck.dropLast(1)
            } else {
                struck = struck.substring(0, i) + '1' + struck.substring(i + 1)
            }
        }
        dirty = true
        feel.backspace(settings.keySounds, settings.haptics)
    }

    var sentinel by remember { mutableStateOf(SENT_A) }
    var field by remember { mutableStateOf(TextFieldValue(SENT_A, TextRange(SENT_A.length))) }

    LaunchedEffect(noteId) {
        delay(350)
        runCatching { focus.requestFocus() }
        keyboard?.show()
    }

    // ---- ink -----------------------------------------------------------------
    val inkColor = Ink
    val annotated = remember(text, struck, settings.ink) { inkText(text, struck, settings.ink, inkColor) }
    val typeFamily = if (settings.ink == InkStyle.Crisp) FontFamily.Monospace else TypeFont

    val blink = rememberInfiniteTransition(label = "caret")
    val caretAlpha by blink.animateFloat(
        initialValue = 1f, targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Reverse),
        label = "caretAlpha",
    )

    val paperColor = PaperColors.getOrElse(settings.paper) { PaperColors[0] }
    val speckles = remember { List(420) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat()) } }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(if (settings.lamp) Color(0xFF0E0C0A) else DeskTop, if (settings.lamp) Color(0xFF0E0C0A) else Desk)))
            .safeDrawingPadding()
    ) {
        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { close() }) {
                Icon(LineIcons.Back, contentDescription = "Back to notes", tint = Cream)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    SimpleDateFormat("EEE · d MMM", Locale.getDefault()).format(Date(original.created)).uppercase(),
                    color = Muted, fontSize = 11.sp, letterSpacing = 1.6.sp,
                )
                Text(
                    current().title, color = Cream, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "${counterFormat.format(text.length)} / ${counterFormat.format(MAX_CHARS)}",
                color = if (text.length >= MAX_CHARS) Ribbon else Muted,
                fontSize = 12.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = 72.dp),
            )
        }

        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val screenW = constraints.maxWidth.toFloat()
            val boxH = constraints.maxHeight.toFloat()
            val px = { v: Float -> with(density) { v.dp.toPx() } }
            val marginPx = px(settings.margins.dp.toFloat())
            val topPadPx = px(40f)
            val paperTopPx = px(46f)

            val lay = layout
            val caretOffset = lay?.let { text.length.coerceAtMost(it.layoutInput.text.length) } ?: 0
            val caretRect = lay?.getCursorRect(caretOffset)
            val caretBottom = caretRect?.bottom ?: lineHeightPx
            // Keep the typing line a few lines below the roller, like a real typewriter.
            val anchor = min(lineHeightPx * 4f, boxH - paperTopPx - topPadPx - px(96f)).coerceAtLeast(lineHeightPx)
            val targetShift = min(0f, anchor - caretBottom)
            val shift by animateFloatAsState(targetShift, tween(240), label = "paperShift")
            val maxBack = -targetShift

            val caretScreenX = px(14f) + marginPx + (caretRect?.left ?: 0f)
            val carriage = ((caretScreenX - px(30f)) / (screenW - px(60f))).coerceIn(0f, 1f)

            // Paper
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = 46.dp, start = 14.dp, end = 14.dp)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(paperColor)
                    .drawBehind {
                        speckles.forEach { (x, y, a) ->
                            drawCircle(
                                Color(0xFF785F3C).copy(alpha = 0.05f + a * 0.07f),
                                radius = 0.6f * this.density + a * this.density,
                                center = Offset(x * size.width, y * size.height),
                            )
                        }
                        if (settings.curved) {
                            drawRect(
                                Brush.horizontalGradient(
                                    0f to Color.Black.copy(alpha = 0.16f), 0.12f to Color.Transparent,
                                    0.88f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.16f),
                                )
                            )
                        }
                        drawRect(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = if (settings.curved) 0.38f else 0.25f),
                                1f to Color.Transparent,
                                startY = 0f, endY = 46f * this.density,
                            ),
                            size = Size(size.width, 46f * this.density),
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures {
                            runCatching { focus.requestFocus() }
                            keyboard?.show()
                        }
                    }
                    .draggable(
                        state = rememberDraggableState { delta ->
                            scrollBack = (scrollBack + delta).coerceIn(0f, maxBack.coerceAtLeast(0f))
                        },
                        orientation = Orientation.Vertical,
                    )
            ) {
                Text(
                    annotated,
                    style = TextStyle(
                        fontFamily = typeFamily,
                        fontSize = settings.typeSize.sp,
                        lineHeight = lineHeightSp.sp,
                        color = inkColor,
                    ),
                    onTextLayout = { result ->
                        layout = result
                        val lines = result.lineCount
                        if (lastLineCount in 1 until lines) rollLine()
                        lastLineCount = lines
                        if (justTyped) {
                            justTyped = false
                            val off = text.length.coerceAtMost(result.layoutInput.text.length)
                            val r = result.getCursorRect(off)
                            val line = result.getLineForOffset(off)
                            if (settings.bell && r.left > result.size.width * 0.86f && bellLine != line) {
                                bellLine = line
                                feel.bell(true, settings.haptics)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.Top, unbounded = true)
                        .padding(start = settings.margins.dp.dp, end = settings.margins.dp.dp, top = 40.dp, bottom = 120.dp)
                        .graphicsLayer { translationY = shift + scrollBack }
                        .drawWithContent {
                            drawContent()
                            val r = caretRect ?: return@drawWithContent
                            drawRect(
                                Ribbon.copy(alpha = caretAlpha),
                                topLeft = Offset(r.left + 1.5f * this.density, r.top + r.height * 0.12f),
                                size = Size(2f * this.density, r.height * 0.76f),
                            )
                        },
                )
            }

            // The platen roller sits over the top edge of the paper.
            Platen(
                roll = roll.value,
                carriage = carriage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = {
                            Graph.settings.update { it.copy(lamp = !it.lamp) }
                            feel.thud(settings.haptics)
                        })
                    }
            )

            if (settings.lamp) LampLight(settings, Modifier.fillMaxSize())

            // Hidden input that receives the keyboard.
            BasicTextField(
                value = field,
                onValueChange = { v ->
                    val t = v.text
                    if (t.length < sentinel.length) {
                        repeat(sentinel.length - t.length) { backspace() }
                    } else if (t.length > sentinel.length) {
                        val added = if (t.startsWith(sentinel)) t.substring(sentinel.length)
                        else t.takeLast(t.length - sentinel.length)
                        added.forEach { ch -> type(if (ch == '\r') '\n' else ch) }
                    }
                    sentinel = if (sentinel == SENT_A) SENT_B else SENT_A
                    field = TextFieldValue(sentinel, TextRange(sentinel.length))
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrectEnabled = false,
                ),
                modifier = Modifier
                    .size(1.dp)
                    .alpha(0f)
                    .focusRequester(focus),
            )

            // Glass quick bar
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xD9241F1A))
                    .border(1.dp, Cream.copy(alpha = 0.14f), RoundedCornerShape(50))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                BarButton(LineIcons.Paper, "Paper and ink") { showPaperSheet = true }
                BarButton(LineIcons.Lamp, "Lamp", active = settings.lamp) { showLampSheet = true }
                BarButton(if (settings.keySounds) LineIcons.Sound else LineIcons.Mute, if (settings.keySounds) "Mute key sounds" else "Turn key sounds on") {
                    Graph.settings.update { it.copy(keySounds = !it.keySounds) }
                }
                BarButton(LineIcons.Save, "Save note") {
                    if (text.isBlank()) {
                        Toast.makeText(context, "Type something first", Toast.LENGTH_SHORT).show()
                    } else {
                        saveNow()
                        Toast.makeText(context, "Note saved", Toast.LENGTH_SHORT).show()
                    }
                }
                BarButton(LineIcons.Share, "Share") {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, current().cleanText())
                    }
                    context.startActivity(Intent.createChooser(send, "Share note"))
                }
            }
        }
    }

    if (showPaperSheet) PaperInkSheet(settings, onDismiss = { showPaperSheet = false })
    if (showLampSheet) LampSheet(settings, onDismiss = { showLampSheet = false })
}

@Composable
private fun BarButton(icon: ImageVector, label: String, active: Boolean = false, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .width(52.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(if (active) Brass.copy(alpha = 0.18f) else Color.Transparent),
    ) {
        Icon(icon, contentDescription = label, tint = if (active) Brass else Cream)
    }
}

/** Warm pool of lamp light; everything outside the beam falls into darkness. */
@Composable
fun LampLight(settings: Settings, modifier: Modifier = Modifier) {
    val warm = lerp(Color(0xFFFFEED6), Color(0xFFFFB05A), settings.warmth / 100f)
    val bright = settings.brightness / 100f
    val beam = settings.beam / 100f
    Canvas(modifier) {
        val center = Offset(size.width / 2f, 60f * density)
        val radius = size.width * (0.55f + beam * 0.95f)
        drawRect(
            Brush.radialGradient(
                0f to warm.copy(alpha = 0.10f + 0.16f * bright),
                0.45f to warm.copy(alpha = 0.05f * bright),
                1f to Color.Transparent,
                center = center, radius = radius,
            )
        )
        drawRect(
            Brush.radialGradient(
                0f to Color.Transparent,
                0.35f to Color.Black.copy(alpha = 0.18f * (1.2f - bright)),
                1f to Color.Black.copy(alpha = 0.94f - 0.22f * bright),
                center = center, radius = radius,
            )
        )
    }
}

/** Builds the typed text with slightly uneven ink, like a real ribbon. */
private fun inkText(text: String, struck: String, ink: InkStyle, color: Color): AnnotatedString {
    val (minA, maxA, jitter, blur, shadowA) = when (ink) {
        InkStyle.Fresh -> InkSpec(0.80f, 1.0f, 0.025f, 1.2f, 0.45f)
        InkStyle.Faded -> InkSpec(0.42f, 0.72f, 0.030f, 1.6f, 0.18f)
        InkStyle.Smudged -> InkSpec(0.72f, 0.95f, 0.035f, 3.2f, 0.55f)
        InkStyle.Crisp -> InkSpec(1f, 1f, 0f, 0f, 0f)
    }
    return buildAnnotatedString {
        text.forEachIndexed { i, c ->
            if (c == '\n') {
                append(c)
                return@forEachIndexed
            }
            val h = hash(i, c)
            val a = minA + (maxA - minA) * h
            val shiftAmt = (hash(i + 7919, c) - 0.5f) * 2f * jitter
            val isStruck = struck.getOrNull(i) == '1'
            withStyle(
                SpanStyle(
                    color = color.copy(alpha = if (isStruck) a * 0.85f else a),
                    baselineShift = if (jitter > 0f) BaselineShift(shiftAmt) else null,
                    textDecoration = if (isStruck) TextDecoration.LineThrough else null,
                    shadow = if (blur > 0f) Shadow(color.copy(alpha = shadowA), Offset(0.4f, 0.3f), blur) else null,
                )
            ) { append(c) }
        }
    }
}

private data class InkSpec(val minA: Float, val maxA: Float, val jitter: Float, val blur: Float, val shadowA: Float)

private fun hash(i: Int, c: Char): Float {
    var x = i * 374761393 + c.code * 668265263
    x = (x xor (x ushr 13)) * 1274126177
    x = x xor (x ushr 16)
    return (x and 0xFFFF) / 65535f
}
