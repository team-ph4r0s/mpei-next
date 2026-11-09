package kekmech.ru.lib_bars_client

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Headers
import kekmech.ru.lib_bars_parser.BarsAttendanceStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** A fake BARS with "Посещение занятий": the markup is the one of bars.mpei.ru, the lines are made up. */
class BarsAttendanceClientTest {

    private val log = mutableListOf<HttpRequestData>()

    @Test
    fun `the status comes from the filter that returned the record`() = runTest {
        val client = BarsClient(engine = fakeAttendance(log))

        val attendance = client.loadAttendance(studentId = STUDENT)

        assertEquals("28", attendance.semesterId)
        assertEquals(listOf("28", "27"), attendance.semesters.map { it.id })
        val byId = attendance.records.associate { it.id to it.status }
        assertEquals(BarsAttendanceStatus.ABSENT, byId["1"])
        assertEquals(BarsAttendanceStatus.EXCUSED, byId["2"])
        assertEquals(BarsAttendanceStatus.ABSENT, byId["3"])
        assertEquals(BarsAttendanceStatus.PRESENT, byId["10"])
        assertEquals(BarsAttendanceStatus.PRESENT, byId["11"])
        assertEquals(5, attendance.records.size)
    }

    @Test
    fun `asks for the page and then for the three filters with the query of the page`() = runTest {
        val client = BarsClient(engine = fakeAttendance(log))

        client.loadAttendance(studentId = STUDENT)

        assertEquals("/bars_web/ST_Study/Lesson__Student/ListStudent__Lesson__Student", log[0].url.encodedPath)
        assertEquals(STUDENT, log[0].url.parameters["studentID"])
        val parts = log.drop(1)
        assertEquals(3, parts.size)
        for (part in parts) {
            assertEquals("/bars_web/ST_Study/Lesson__Student/_PartialListStudent__Lesson__Student", part.url.encodedPath)
            assertEquals(STUDENT, part.url.parameters["studentID"])
            assertEquals("XMLHttpRequest", part.headers["X-Requested-With"])
        }
        val queries = parts.map { it.url.parameters["query"].orEmpty() }
        assertEquals(
            listOf(
                """"FilterLessonSkipType":{"Value":"1"},"FilterLessonSkipReason":{"Value":"*"}""",
                """"FilterLessonSkipType":{"Value":"1"},"FilterLessonSkipReason":{"Value":"2"}""",
                """"FilterLessonSkipType":{"Value":"2"},"FilterLessonSkipReason":{"Value":"*"}""",
            ),
            queries.map { it.substringAfter(""""FilterSemester":{"Value":"28"},""").removeSuffix("}") },
        )
        assertEquals(true, """"ID":"$STUDENT"""" in queries.first())
        assertEquals(true, """"PageSize":"500"""" in queries.first())
    }

    @Test
    fun `another semester is asked for with its id`() = runTest {
        val client = BarsClient(engine = fakeAttendance(log))

        val attendance = client.loadAttendance(semesterId = "27", studentId = STUDENT)

        assertEquals("27", attendance.semesterId)
        assertEquals(true, log.drop(1).all { """"FilterSemester":{"Value":"27"}""" in it.url.parameters["query"].orEmpty() })
    }

    @Test
    fun `reads the next page of the pager when the records do not fit in one`() = runTest {
        val client = BarsClient(engine = fakeAttendance(log, skipsInTwoPages = true))

        val attendance = client.loadAttendance(studentId = STUDENT)

        val skipQueries = log.map { it.url.parameters["query"].orEmpty() }
            .filter { """"FilterLessonSkipType":{"Value":"1"},"FilterLessonSkipReason":{"Value":"*"}""" in it }
        assertEquals(listOf("1", "2"), skipQueries.map { it.substringAfter(""""Page":"""").substringBefore('"') })
        assertEquals(setOf("1", "2", "3"), attendance.records.filter { it.status != BarsAttendanceStatus.PRESENT }.map { it.id }.toSet())
    }

    @Test
    fun `a filter that matches nothing is no records`() = runTest {
        val client = BarsClient(engine = fakeAttendance(log, noVisits = true))

        val attendance = client.loadAttendance(studentId = STUDENT)

        assertEquals(0, attendance.records.count { it.status == BarsAttendanceStatus.PRESENT })
        assertEquals(3, attendance.records.size)
    }

    @Test
    fun `the sign-in page instead of the attendance says that the session is gone`() = runTest {
        val client = BarsClient(engine = MockEngine { respond(LOGIN, HttpStatusCode.OK, htmlHeaders()) })

        assertFailsWith<BarsSessionExpiredException> { client.loadAttendance(studentId = STUDENT) }
    }

    private companion object {
        const val STUDENT = "11111111-2222-3333-4444-555555555555"

        fun htmlHeaders() = Headers.build { append(HttpHeaders.ContentType, "text/html; charset=utf-8") }

        const val LOGIN = """<html><body><form action="/bars_web/" method="post">
<input name="__RequestVerificationToken" type="hidden" value="tok1"><input name="Account" type="text"><input name="Password" type="password">
</form></body></html>"""

        fun row(id: String, line: String) = """<tr data-ls-id="$id"><td><label>$line</label></td></tr>"""

        fun table(rows: List<String>, total: Int, page: Int = 1) = """
<div id="div-Student__Lesson">
<select id="ddl_LessonStudent_FilterSemester"><option value="*">все семестры</option><option selected="selected" value="28">2026/2027, Осень</option><option value="27">2025/2026, Весна</option></select>
<table id="tbl__PartialListStudent__Lesson__Student"><tbody>${rows.joinToString("\n")}</tbody></table>
</div>
<div id="st_les__Pager" class="d-flex"><div class='pager-size'><span>Отображены записи из $total</span></div>
<div><ul class="pagination"><li class="page-item active"><span class="page-link">$page</span></li></ul></div></div>
"""

        private const val LINE = "20.10.26, 1 пара (08:00-09:30), лекция (Петров П.П.), Физика (экзамен), А-01-26"

        fun fakeAttendance(
            log: MutableList<HttpRequestData>,
            skipsInTwoPages: Boolean = false,
            noVisits: Boolean = false,
        ) = MockEngine { request ->
            log += request
            val path = request.url.encodedPath.lowercase()
            val query = request.url.parameters["query"].orEmpty()
            val body = when {
                path.endsWith("/listStudent__lesson__student".lowercase()) -> table(listOf(row("1", LINE)), total = 3)
                path.endsWith("/_partialliststudent__lesson__student") -> when {
                    """"FilterLessonSkipType":{"Value":"2"}""" in query ->
                        if (noVisits) "<div>Нет данных</div>" else table(listOf(row("10", LINE), row("11", LINE)), total = 2)
                    """"FilterLessonSkipReason":{"Value":"2"}""" in query -> table(listOf(row("2", LINE)), total = 1)
                    skipsInTwoPages && """"Page":"2"""" in query -> table(listOf(row("3", LINE)), total = 3, page = 2)
                    skipsInTwoPages -> table(listOf(row("1", LINE), row("2", LINE)), total = 3)
                    else -> table(listOf(row("1", LINE), row("2", LINE), row("3", LINE)), total = 3)
                }
                else -> ""
            }
            respond(body, HttpStatusCode.OK, htmlHeaders())
        }
    }
}
