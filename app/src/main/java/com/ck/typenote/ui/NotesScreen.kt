package com.ck.typenote.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ck.typenote.Graph
import com.ck.typenote.data.Note
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
private val dayFormat = SimpleDateFormat("d MMM", Locale.getDefault())
private val numFormat = NumberFormat.getIntegerInstance()

private fun shortDate(ms: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = ms }
    val sameDay = now.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    return if (sameDay) "Today" else dayFormat.format(Date(ms))
}

@Composable
fun NotesScreen(onOpen: (Long) -> Unit, onNew: () -> Unit, onTrash: () -> Unit) {
    val all by Graph.notes.notes.collectAsState()
    var query by remember { mutableStateOf("") }
    val live = all.filter { !it.isDeleted && it.text.isNotBlank() }.sortedByDescending { it.updated }
    val shown = if (query.isBlank()) live else live.filter { it.cleanText().contains(query.trim(), ignoreCase = true) }
    val trashCount = all.count { it.isDeleted }
    val groups = shown.groupBy { monthFormat.format(Date(it.updated)) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DeskTop, Desk, Desk)))
            .safeDrawingPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 12.dp, top = 18.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Typenote",
                    style = TextStyle(fontFamily = TypeFont, fontSize = 32.sp, color = PaperColors[0]),
                )
                Text(".", style = TextStyle(fontFamily = TypeFont, fontSize = 32.sp, color = Ribbon))
                Spacer(Modifier.weight(1f))
                Text("${live.size} sheets", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(end = 10.dp))
            }

            // Search
            Row(
                Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Cream.copy(alpha = 0.07f))
                    .border(1.dp, Cream.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(LineIcons.Search, contentDescription = null, tint = Muted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search your sheets", color = Color(0xFF8F8679), fontSize = 15.sp)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Cream, fontSize = 15.sp),
                        cursorBrush = SolidColor(Ribbon),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(LineIcons.Close, contentDescription = "Clear search", tint = Muted, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (shown.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 90.dp, start = 40.dp, end = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (query.isBlank()) "A fresh sheet is waiting." else "No sheets match \"$query\".",
                        style = TextStyle(fontFamily = TypeFont, fontSize = 20.sp, color = PaperColors[0]),
                    )
                    Spacer(Modifier.height(8.dp))
                    if (query.isBlank()) Text("Tap New sheet to start typing.", color = Muted, fontSize = 14.sp)
                }
            }

            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                groups.forEach { (month, notes) ->
                    item(key = "h-$month") {
                        Text(
                            month.uppercase(),
                            color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.8.sp,
                            modifier = Modifier.padding(start = 2.dp, top = 12.dp),
                        )
                    }
                    items(notes, key = { it.id }) { note ->
                        NoteSlip(
                            note = note,
                            onOpen = { onOpen(note.id) },
                            menuLabel = "Move to Recently deleted",
                            onMenu = { Graph.notes.moveToTrash(note.id) },
                        )
                    }
                }
            }
        }

        // Recently deleted
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 22.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xCC241F1A))
                .border(1.dp, Cream.copy(alpha = 0.12f), RoundedCornerShape(50))
                .combinedClickableCompat(onTrash)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(LineIcons.Trash, contentDescription = null, tint = Label, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (trashCount > 0) "Deleted ($trashCount)" else "Deleted", color = Label, fontSize = 14.sp)
        }

        // New sheet
        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 22.dp)
                .height(56.dp)
                .shadow(10.dp, RoundedCornerShape(50))
                .clip(RoundedCornerShape(50))
                .background(Ribbon)
                .combinedClickableCompat(onNew)
                .padding(start = 18.dp, end = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(LineIcons.Plus, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("New sheet", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun TrashScreen(onBack: () -> Unit) {
    val all by Graph.notes.notes.collectAsState()
    val deleted = all.filter { it.isDeleted }.sortedByDescending { it.deletedAt ?: 0L }
    var confirmEmpty by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Desk)
            .safeDrawingPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(LineIcons.Back, contentDescription = "Back", tint = Cream) }
            Text("Recently deleted", color = Cream, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (deleted.isNotEmpty()) {
                TextButton(onClick = { confirmEmpty = true }) { Text("Empty", color = Ribbon) }
            }
        }
        Text(
            "Sheets stay here for 30 days. Tap one to restore it.",
            color = Muted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        )
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(deleted, key = { it.id }) { note ->
                NoteSlip(
                    note = note,
                    onOpen = { Graph.notes.restore(note.id) },
                    menuLabel = "Delete forever",
                    onMenu = { Graph.notes.deleteForever(note.id) },
                )
            }
        }
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            title = { Text("Delete all for good?") },
            text = { Text("These ${deleted.size} sheets can't be brought back.") },
            confirmButton = {
                TextButton(onClick = { Graph.notes.emptyTrash(); confirmEmpty = false }) { Text("Delete", color = Ribbon) }
            },
            dismissButton = { TextButton(onClick = { confirmEmpty = false }) { Text("Cancel", color = Cream) } },
            containerColor = Sheet,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteSlip(note: Note, onOpen: () -> Unit, menuLabel: String, onMenu: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val paper = PaperColors[(note.id % PaperColors.size).toInt().let { if (it < 0) 0 else it }]
    Box {
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(paper)
                .combinedClickable(onClick = onOpen, onLongClick = { menu = true }, onLongClickLabel = menuLabel)
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Text(
                note.preview.ifBlank { "…" },
                style = TextStyle(
                    fontFamily = TypeFont, fontSize = 16.sp, lineHeight = 24.sp, color = Ink,
                    shadow = Shadow(Ink.copy(alpha = 0.4f), Offset(0.4f, 0.3f), 1.2f),
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Row {
                Text(note.title, color = Color(0xFF6B6255), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("${shortDate(note.updated)} · ${numFormat.format(note.text.length)} chars", color = Color(0xFF6B6255), fontSize = 12.sp)
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Field) {
            DropdownMenuItem(text = { Text(menuLabel, color = Cream) }, onClick = { menu = false; onMenu() })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableCompat(onClick: () -> Unit): Modifier =
    this.combinedClickable(onClick = onClick)
