package kekmech.ru.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kekmech.ru.feature_notes_shared.Note
import kekmech.ru.feature_notes_shared.NotesController
import kekmech.ru.feature_schedule_shared.Classes
import kekmech.ru.feature_schedule_shared.ClassesType
import kekmech.ru.shared.schedule.formatTime
import kekmech.ru.ui_theme.theme.MpeixTheme

/**
 * A class on the schedule and on the home screen: the same card as in the Android app. The top line has the number of
 * the class in a circle, the time and the badge with the kind of the class; then the name, the room with the teacher
 * and the groups, and the note.
 *
 * @param caption a line above the card (the day, "going now")
 * @param highlighted the class that goes now
 */
@Composable
internal fun ClassCard(
    classes: Classes,
    note: Note?,
    modifier: Modifier = Modifier,
    caption: String? = null,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val palette = MpeixTheme.palette
    Surface(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = RoundedCornerShape(16.dp),
        color = palette.surfacePlus1,
        border = BorderStroke(if (highlighted) 1.5.dp else 1.dp, if (highlighted) palette.primary else palette.outline.copy(alpha = 0.25f)),
    ) {
        Column(Modifier.padding(12.dp)) {
            if (caption != null) {
                Text(
                    caption,
                    style = MpeixTheme.typography.labelNormal,
                    color = if (highlighted) palette.primary else palette.contentVariant,
                )
                Spacer(Modifier.height(4.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f, fill = false), verticalAlignment = Alignment.CenterVertically) {
                    if (classes.number > 0) {
                        Box(
                            Modifier.size(22.dp).clip(CircleShape).background(palette.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                classes.number.toString(),
                                style = MpeixTheme.typography.paragraphNormal.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = palette.primary,
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        "${formatTime(classes.time.start)} - ${formatTime(classes.time.end)}",
                        style = MpeixTheme.typography.paragraphNormal.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                        color = palette.content,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(8.dp))
                TypeBadge(classes)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                classes.name,
                style = MpeixTheme.typography.header4.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                color = palette.content,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            val place = classes.place.takeIf { it.isNotBlank() }
            val person = classes.person.takeIf { it.isNotBlank() }
            if (place != null || person != null) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (place != null) {
                        Text(
                            place,
                            Modifier.weight(1f, fill = false),
                            style = MpeixTheme.typography.paragraphNormal.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                            color = palette.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (place != null && person != null) {
                        Text(
                            "•",
                            Modifier.padding(horizontal = 6.dp),
                            style = MpeixTheme.typography.paragraphNormal.copy(fontSize = 10.sp),
                            color = palette.contentDisabled,
                        )
                    }
                    if (person != null) {
                        Text(
                            person,
                            Modifier.weight(1.5f, fill = false),
                            style = MpeixTheme.typography.paragraphNormal.copy(fontSize = 12.sp),
                            color = palette.contentVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (classes.groups.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    classes.groups,
                    style = MpeixTheme.typography.paragraphNormal.copy(fontSize = 11.sp),
                    color = palette.contentDisabled,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (note != null) {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(palette.surfacePlus3).padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(
                        NotesController.preview(note),
                        style = MpeixTheme.typography.paragraphNormal,
                        color = palette.content,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** The badge with the kind of the class ("Лекция") in its color, as on Android. */
@Composable
private fun TypeBadge(classes: Classes) {
    val label = typeTitle(classes.type, classes.rawType).ifBlank { "Занятие" }
    val color = classes.type.color()
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(
            label,
            style = MpeixTheme.typography.paragraphNormal.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun typeTitle(type: ClassesType, raw: String?): String = when (type) {
    ClassesType.LECTURE -> "Лекция"
    ClassesType.PRACTICE -> "Практика"
    ClassesType.LAB -> "Лабораторная"
    ClassesType.COURSE -> "Курсовая"
    ClassesType.EXAM -> "Экзамен"
    ClassesType.CONSULTATION -> "Консультация"
    ClassesType.UNDEFINED -> raw.orEmpty()
}

/** The colors of the kinds are the ones of the palette of the Android app. */
@Composable
private fun ClassesType.color(): Color {
    val palette = MpeixTheme.palette
    return when (this) {
        ClassesType.LECTURE -> palette.classesTypeLecture
        ClassesType.PRACTICE, ClassesType.COURSE -> palette.classesTypePractice
        ClassesType.LAB -> palette.classesTypeLab
        ClassesType.EXAM -> palette.redMarkColor
        ClassesType.CONSULTATION -> palette.primary
        ClassesType.UNDEFINED -> palette.contentDisabled
    }
}
