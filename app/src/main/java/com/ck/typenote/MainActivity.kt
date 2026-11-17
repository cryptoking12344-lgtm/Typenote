package com.ck.typenote

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.ck.typenote.data.NoteStore
import com.ck.typenote.data.SettingsStore
import com.ck.typenote.ui.EditorScreen
import com.ck.typenote.ui.NotesScreen
import com.ck.typenote.ui.TrashScreen
import com.ck.typenote.ui.TypenoteTheme

/** App-wide singletons, created once per process. */
object Graph {
    lateinit var notes: NoteStore
    lateinit var settings: SettingsStore
    lateinit var feel: TypeFeel

    fun init(context: Context) {
        if (::notes.isInitialized) return
        val app = context.applicationContext
        notes = NoteStore(app)
        settings = SettingsStore(app)
        feel = TypeFeel(app)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Graph.init(this)
        enableEdgeToEdge()
        setContent {
            TypenoteTheme {
                App()
            }
        }
    }
}

@Composable
private fun App() {
    // -1 = notes list, -2 = recently deleted, otherwise the open note id.
    var screen by rememberSaveable { mutableStateOf(-1L) }

    when (screen) {
        -1L -> NotesScreen(
            onOpen = { screen = it },
            onNew = { screen = Graph.notes.create().id },
            onTrash = { screen = -2L },
        )
        -2L -> {
            BackHandler { screen = -1L }
            TrashScreen(onBack = { screen = -1L })
        }
        else -> {
            val id = screen
            EditorScreen(noteId = id, onClose = { screen = -1L })
        }
    }
}
