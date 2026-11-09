package kekmech.ru.feature_bars_impl.presentation.screen.main_compose.attendance

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import kekmech.ru.feature_bars_shared.AttendanceSummary
import kekmech.ru.feature_bars_shared.analyzeAttendance
import kekmech.ru.lib_bars_parser.BarsAttendanceRecord
import kekmech.ru.lib_bars_parser.BarsAttendanceStatus
import kekmech.ru.lib_elm.Resource
import kekmech.ru.res_strings.R.string as Strings
import kekmech.ru.ui_theme.theme.MpeixTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/** The tab "Посещаемость": light / dark theme x phone / tablet widths. Runs on the JVM (Robolectric). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AttendanceContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private var nextId = 0

    private fun record(discipline: String, status: BarsAttendanceStatus, day: Int, pair: Int = 7) = BarsAttendanceRecord(
        id = (++nextId).toString(),
        date = "${day.toString().padStart(2, '0')}.10.26",
        pair = pair,
        timeStart = "20:30",
        timeEnd = "22:00",
        kind = "лекция",
        teacher = "Петров П.П.",
        discipline = discipline,
        assessmentType = "экзамен",
        group = "А-01-26",
        status = status,
        badges = emptyList(),
        raw = "",
    )

    private val P = BarsAttendanceStatus.PRESENT
    private val A = BarsAttendanceStatus.ABSENT
    private val E = BarsAttendanceStatus.EXCUSED

    /** Physics: 3 of 4 skipped (critical), Chemistry: 1 skip of 6 (yellow), Math: all visited (green) */
    private val summary: AttendanceSummary = analyzeAttendance(
        listOf(P, A, A, A).mapIndexed { i, s -> record("Физика", s, i + 1) } +
            listOf(P, P, P, P, P, A).mapIndexed { i, s -> record("Химия", s, i + 1) } +
            listOf(P, P, E).mapIndexed { i, s -> record("Математика", s, i + 1) },
    )

    private fun show(
        attendance: Resource<AttendanceSummary>,
        darkTheme: Boolean = false,
        width: Dp = 360.dp,
        onRetry: () -> Unit = {},
    ) {
        composeRule.setContent {
            Box(Modifier.requiredSize(width, 1600.dp)) {
                MpeixTheme(darkTheme = darkTheme) {
                    var expanded by remember { mutableStateOf(emptySet<String>()) }
                    LazyColumn {
                        attendanceItems(
                            attendance = attendance,
                            expanded = expanded,
                            onToggle = { name -> expanded = if (name in expanded) expanded - name else expanded + name },
                            onRetry = onRetry,
                        )
                    }
                }
            }
        }
    }

    private fun summaryShowsTheDisciplinesAndTheLevels(darkTheme: Boolean, width: Dp) {
        show(Resource.Data(summary), darkTheme, width)

        composeRule.onNodeWithText(context.getString(Strings.attendance_semester_title)).assertExists()
        for (name in listOf("Физика", "Химия", "Математика")) {
            composeRule.onNodeWithText(name).assertExists()
        }
        // the level is a word, not a color only
        composeRule.onNodeWithText(context.getString(Strings.attendance_risk_critical)).assertExists()
        composeRule.onNodeWithText(context.getString(Strings.attendance_risk_warning)).assertExists()
        composeRule.onNodeWithText(context.getString(Strings.attendance_risk_ok)).assertExists()
        // the percentage of the whole semester is shown in the ring
        composeRule.onNodeWithText("${summary.percent}%").assertExists()
    }

    @Test
    fun summary_light_phone() = summaryShowsTheDisciplinesAndTheLevels(darkTheme = false, width = 360.dp)

    @Test
    fun summary_dark_phone() = summaryShowsTheDisciplinesAndTheLevels(darkTheme = true, width = 360.dp)

    @Test
    fun summary_light_tablet() = summaryShowsTheDisciplinesAndTheLevels(darkTheme = false, width = 520.dp)

    @Test
    fun summary_dark_tablet() = summaryShowsTheDisciplinesAndTheLevels(darkTheme = true, width = 520.dp)

    @Test
    fun theWorstDisciplineGoesFirst() {
        assertEquals(listOf("Физика", "Химия", "Математика"), summary.disciplines.map { it.name })
        assertEquals(overallLevel(), summary.disciplines.first().risk)
    }

    private fun overallLevel() = summary.overallRisk()

    // the card is clickable, so its texts are merged into one node: the badges are counted in the unmerged tree
    private fun count(text: String) =
        composeRule.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().size

    @Test
    fun aCardOpensItsClassesOnClick() {
        show(Resource.Data(summary))
        val skip = context.getString(Strings.attendance_status_absent)
        val present = context.getString(Strings.attendance_status_present)

        // the header and the cards say "Missed" / "Present" too: count what a click adds, not the absolute number
        val skipClosed = count(skip)
        val presentClosed = count(present)

        composeRule.onNodeWithText("Физика").performClick()

        // Physics has three skips (three badges) and one visit (one badge)
        assertEquals(skipClosed + 3, count(skip))
        assertEquals(presentClosed + 1, count(present))

        composeRule.onNodeWithText("Физика").performClick()
        assertEquals(skipClosed, count(skip))
        assertEquals(presentClosed, count(present))
    }

    @Test
    fun noRecordsIsAWordNotAnEmptyScreen() {
        show(Resource.Data(analyzeAttendance(emptyList())))

        composeRule.onNodeWithText(context.getString(Strings.attendance_empty)).assertExists()
    }

    @Test
    fun anErrorOffersToTryAgain() {
        var retried = 0
        show(Resource.Error(IOException("no network")), onRetry = { retried++ })

        composeRule.onNodeWithText(context.getString(Strings.bars_summary_error_retry)).performClick()

        assertEquals(1, retried)
    }
}
