package kekmech.ru.feature_bars_shared

import kekmech.ru.lib_bars_parser.BarsAttendanceRecord
import kekmech.ru.lib_bars_parser.BarsAttendanceStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AttendanceAnalysisTest {

    private var nextId = 0

    private fun record(
        discipline: String,
        status: BarsAttendanceStatus,
        date: String = "01.10.26",
        pair: Int = 1,
        id: String = (++nextId).toString(),
    ) = BarsAttendanceRecord(
        id = id,
        date = date,
        pair = pair,
        timeStart = null,
        timeEnd = null,
        kind = "лекция",
        teacher = "Петров П.П.",
        discipline = discipline,
        assessmentType = "экзамен",
        group = "А-01-26",
        status = status,
        badges = emptyList(),
        raw = "",
    )

    private fun days(discipline: String, vararg statuses: BarsAttendanceStatus) =
        statuses.mapIndexed { index, status -> record(discipline, status, date = "${(index + 1).toString().padStart(2, '0')}.10.26") }

    private val P = BarsAttendanceStatus.PRESENT
    private val A = BarsAttendanceStatus.ABSENT
    private val E = BarsAttendanceStatus.EXCUSED

    @Test
    fun `counts the classes of every discipline and of the semester`() {
        val summary = analyzeAttendance(days("Физика", P, P, A, E) + days("Химия", P, P))

        assertEquals(4, summary.present)
        assertEquals(1, summary.absent)
        assertEquals(1, summary.excused)
        assertEquals(6, summary.total)
        // the excused skip does not count against the student: 5 of 6
        assertEquals(83, summary.percent)
        val physics = summary.disciplines.single { it.name == "Физика" }
        assertEquals(75, physics.percent)
        assertEquals("экзамен", physics.assessmentType)
    }

    @Test
    fun `a discipline without skips is green, with a skip yellow`() {
        val summary = analyzeAttendance(days("Химия", P, P, P, P, E) + days("Физика", P, P, P, P, P, A))

        assertEquals(AttendanceRisk.OK, summary.disciplines.single { it.name == "Химия" }.risk)
        assertEquals(AttendanceRisk.WARNING, summary.disciplines.single { it.name == "Физика" }.risk)
    }

    @Test
    fun `a quarter of the classes skipped is critical`() {
        val summary = analyzeAttendance(days("Физика", P, A, P, P))

        val physics = summary.disciplines.single()
        assertEquals(AttendanceRisk.CRITICAL, physics.risk)
        assertEquals(0.25, physics.skipShare)
    }

    @Test
    fun `one skip of two classes is not enough records to be critical`() {
        val summary = analyzeAttendance(days("Физика", P, A))

        assertEquals(AttendanceRisk.WARNING, summary.disciplines.single().risk)
    }

    @Test
    fun `three skips in a row are critical even among many classes`() {
        val summary = analyzeAttendance(days("Физика", P, P, P, P, P, P, P, P, P, P, A, A, A))

        val physics = summary.disciplines.single()
        assertEquals(AttendanceRisk.CRITICAL, physics.risk)
        assertEquals(3, physics.longestSkipStreak)
        assertEquals(3, physics.currentSkipStreak)
    }

    @Test
    fun `a visit breaks the row of skips and the classes of a day go by their numbers`() {
        val records = listOf(
            record("Физика", A, date = "05.10.26", pair = 7),
            record("Физика", P, date = "05.10.26", pair = 6),
            record("Физика", A, date = "04.10.26", pair = 1),
            record("Физика", A, date = "03.10.26", pair = 1),
            record("Физика", P, date = "02.10.26", pair = 1),
            record("Физика", P, date = "01.10.26", pair = 1),
            record("Физика", P, date = "30.09.26", pair = 1),
            record("Физика", P, date = "29.09.26", pair = 1),
            record("Физика", P, date = "28.09.26", pair = 1),
            record("Физика", P, date = "27.09.26", pair = 1),
            record("Физика", P, date = "26.09.26", pair = 1),
            record("Физика", P, date = "25.09.26", pair = 1),
            record("Физика", P, date = "24.09.26", pair = 1),
        )

        val physics = analyzeAttendance(records).disciplines.single()

        // 03.10, 04.10 skipped, then 05.10 pair 6 visited, 05.10 pair 7 skipped
        assertEquals(2, physics.longestSkipStreak)
        assertEquals(1, physics.currentSkipStreak)
        assertEquals(AttendanceRisk.WARNING, physics.risk)
        // newest first
        assertEquals("05.10.26", physics.records.first().date)
        assertEquals(7, physics.records.first().pair)
    }

    @Test
    fun `the worst discipline goes first and the unknown records are left out`() {
        val summary = analyzeAttendance(
            days("Химия", P, P, P, P, P, A) + days("Физика", P, A, A, A) + listOf(record("Прочее", BarsAttendanceStatus.UNKNOWN)),
        )

        assertEquals(listOf("Физика", "Химия"), summary.disciplines.map { it.name })
    }

    @Test
    fun `an empty list is full attendance with nothing to show`() {
        val summary = analyzeAttendance(emptyList())

        assertEquals(100, summary.percent)
        assertEquals(false, summary.hasRecords)
    }

    // region: the changes

    @Test
    fun `the first check has nothing new`() {
        val summary = analyzeAttendance(days("Физика", P, A, A, A))

        val changes = detectAttendanceChanges(previous = null, semesterId = "28", summary = summary)

        assertTrue(changes.isEmpty)
    }

    @Test
    fun `a new skip is news, an old one is not`() {
        val before = analyzeAttendance(days("Физика", P, P, P, P, P, P, P, P, P, A))
        val snapshot = before.toSnapshot("28")
        val after = analyzeAttendance(
            before.disciplines.flatMap { it.records } + record("Физика", A, date = "20.10.26", id = "new"),
        )

        val changes = detectAttendanceChanges(snapshot, "28", after)

        assertEquals(listOf("new"), changes.newSkips.map { it.id })
        assertTrue(changes.newCritical.isEmpty())
    }

    @Test
    fun `a visit that turned into a skip is news`() {
        val records = days("Физика", P, P, P, P, P, P, P)
        val snapshot = analyzeAttendance(records).toSnapshot("28")
        val changed = records.map { if (it.date == "03.10.26") it.withStatus(A) else it }

        val changes = detectAttendanceChanges(snapshot, "28", analyzeAttendance(changed))

        assertEquals(listOf("03.10.26"), changes.newSkips.map { it.date })
    }

    @Test
    fun `an excused skip is not news`() {
        val records = days("Физика", P, P, P, P, P)
        val snapshot = analyzeAttendance(records).toSnapshot("28")

        val changes = detectAttendanceChanges(snapshot, "28", analyzeAttendance(records + record("Физика", E, date = "20.10.26")))

        assertTrue(changes.isEmpty)
    }

    @Test
    fun `crossing the threshold is news once`() {
        val before = analyzeAttendance(days("Физика", P, P, P, P, P, P, A))
        val grown = before.disciplines.flatMap { it.records } + record("Физика", A, date = "20.10.26", id = "x1") +
            record("Физика", A, date = "21.10.26", id = "x2")
        val after = analyzeAttendance(grown)

        val first = detectAttendanceChanges(before.toSnapshot("28"), "28", after)
        assertEquals(listOf("Физика"), first.newCritical.map { it.name })
        assertEquals(listOf("x1", "x2"), first.newSkips.map { it.id })

        // the next check, nothing happened in between: the discipline is already known as critical
        val second = detectAttendanceChanges(after.toSnapshot("28"), "28", after)
        assertTrue(second.isEmpty)
    }

    @Test
    fun `another semester starts anew`() {
        val summary = analyzeAttendance(days("Физика", P, A, A, A))

        val changes = detectAttendanceChanges(summary.toSnapshot("27"), "28", summary)

        assertTrue(changes.isEmpty)
    }

    @Test
    fun `the snapshot survives the storage`() {
        val summary = analyzeAttendance(days("Физика", P, A, A, A) + days("Химия", P, E))
        val snapshot = summary.toSnapshot("28")

        val restored = AttendanceSnapshot.decode(snapshot.encode())

        assertEquals(snapshot.semesterId, restored?.semesterId)
        assertEquals(snapshot.statuses, restored?.statuses)
        assertEquals(snapshot.critical, restored?.critical)
    }

    @Test
    fun `a damaged text is no snapshot`() {
        assertNull(AttendanceSnapshot.decode(null))
        assertNull(AttendanceSnapshot.decode(""))
        assertNull(AttendanceSnapshot.decode("hello"))
        assertNull(AttendanceSnapshot.decode("V1\t28\nS\t1\tNOPE"))
    }

    // endregion
}
