package kekmech.ru.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kekmech.ru.feature_notes_shared.Note
import kekmech.ru.feature_notes_shared.NotesController
import kekmech.ru.shared.schedule.dayTitle
import kekmech.ru.shared.schedule.formatTime

/** A note of a class: when the class is, its name and the text. Used on the home screen and in the list of the notes. */
@Composable
internal fun NoteCard(note: Note, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "${dayTitle(note.dateTime.date)} · ${formatTime(note.dateTime.time)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(note.classesName, style = MaterialTheme.typography.titleMedium)
            Text(NotesController.preview(note), style = MaterialTheme.typography.bodyMedium, maxLines = 3)
        }
    }
}
