package kekmech.ru.lib_bars_parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * The markup of "Посещение занятий" is the one of the page of bars.mpei.ru (saved from the portal, the names of the
 * people are replaced); the lines of the rows are made up in the same form.
 */
class BarsAttendanceParserTest {

    @Test
    fun `reads the fields of a line`() {
        val record = BarsHtmlParser.parseAttendancePage(PAGE).records.first()

        assertEquals("32560103", record.id)
        assertEquals("28.10.26", record.date)
        assertEquals(7, record.pair)
        assertEquals("20:30", record.timeStart)
        assertEquals("22:00", record.timeEnd)
        assertEquals("лекция", record.kind)
        assertEquals("Петров П.П.", record.teacher)
        assertEquals("Аппаратные средства вычислительной техники", record.discipline)
        assertEquals("экзамен", record.assessmentType)
        assertEquals("ИЭозс-45-25", record.group)
        assertEquals(listOf("пропуск"), record.badges)
        assertEquals(BarsAttendanceStatus.ABSENT, record.status)
    }

    @Test
    fun `a skip badge makes an absence, no badge leaves the status unknown`() {
        val records = BarsHtmlParser.parseAttendancePage(PAGE).records

        assertEquals(BarsAttendanceStatus.ABSENT, records[0].status)
        assertEquals(BarsAttendanceStatus.UNKNOWN, records.single { it.id == "32458300" }.status)
    }

    @Test
    fun `brackets inside the assessment and commas inside the name of the discipline stay in it`() {
        val records = BarsHtmlParser.parseAttendancePage(PAGE).records

        val nested = records.single { it.id == "32539713" }
        assertEquals("Культурология", nested.discipline)
        assertEquals("зачёт (без оценки)", nested.assessmentType)

        val comma = records.single { it.id == "32458300" }
        assertEquals("Математика, часть 1", comma.discipline)
        assertEquals("экзамен", comma.assessmentType)
        assertEquals("практическое занятие", comma.kind)
        assertEquals("ИЭозс-45-25", comma.group)
    }

    @Test
    fun `reads the semesters without the all-semesters item`() {
        val page = BarsHtmlParser.parseAttendancePage(PAGE)

        assertEquals(listOf("28", "27"), page.semesters.map { it.id })
        assertEquals("2026/2027, Осень", page.selectedSemester?.name)
    }

    @Test
    fun `reads the pager`() {
        val page = BarsHtmlParser.parseAttendancePage(PAGE)

        assertEquals(26, page.total)
        assertEquals(1, page.page)
        assertEquals(4, page.records.size)
    }

    @Test
    fun `a page without a pager has no total`() {
        val page = BarsHtmlParser.parseAttendancePage(PAGE.substringBefore("""<div id="st_les__Pager""") + "</div>")

        assertNull(page.total)
        assertEquals(1, page.page)
    }

    @Test
    fun `the part that the ajax request gives is read as well`() {
        val part = PAGE.substringAfter("""<div class="table-responsive">""")
            .let { """<div class="table-responsive">$it""" }

        assertEquals(4, BarsHtmlParser.parseAttendancePage(part).records.size)
    }

    @Test
    fun `a line that is not in the known form is not lost`() {
        val page = BarsHtmlParser.parseAttendancePage(
            """<table id="tbl__PartialListStudent__Lesson__Student"><tbody>
               <tr data-ls-id="1"><td><label>Что-то новое</label><span class="badge bg-danger">пропуск</span></td></tr>
               </tbody></table>""",
        )

        val record = page.records.single()
        assertEquals("Что-то новое", record.discipline)
        assertEquals("Что-то новое", record.raw)
        assertEquals(BarsAttendanceStatus.ABSENT, record.status)
    }

    @Test
    fun `a good reason makes the skip an excused one`() {
        val page = BarsHtmlParser.parseAttendancePage(
            """<table id="tbl__PartialListStudent__Lesson__Student"><tbody>
               <tr data-ls-id="1"><td><label>20.10.26, 1 пара (08:00-09:30), лекция (Петров П.П.), Физика (экзамен), А-01-26</label>
               <span class="badge bg-danger">пропуск</span><span class="badge bg-warning">по уважительной причине</span></td></tr>
               <tr data-ls-id="2"><td><label>20.10.26, 2 пара (09:45-11:15), лекция (Петров П.П.), Физика (экзамен), А-01-26</label>
               <span class="badge bg-danger">пропуск</span><span class="badge bg-secondary">без уважительной причины</span></td></tr>
               </tbody></table>""",
        )

        assertEquals(listOf(BarsAttendanceStatus.EXCUSED, BarsAttendanceStatus.ABSENT), page.records.map { it.status })
    }

    @Test
    fun `not an attendance page is refused`() {
        assertFailsWith<BarsParseException> { BarsHtmlParser.parseAttendancePage("<html><body></body></html>") }
    }

    @Test
    fun `the date is a key for sorting`() {
        assertEquals(20261028, attendanceDateKey("28.10.26"))
        assertEquals(20261028, attendanceDateKey("28.10.2026"))
        assertEquals(0, attendanceDateKey("вчера"))

        val records = BarsHtmlParser.parseAttendancePage(PAGE).records
        // the classes of a day go by their numbers
        val sorted = records.sortedBy { it.orderKey }
        assertEquals("32458300", sorted.first().id)
        assertEquals("32560103", sorted.last().id)
    }

    private companion object {
        val PAGE = """
<div id="div-Student__Lesson">
<div class="border bg-light">
    <div class="row p-1">
        <div class="col-12 col-md-10 col-lg-8">
            <div class="input-group">
                <select class="form-control ddl_LessonStudent_FilterSemester" id="ddl_LessonStudent_FilterSemester" name="ddl_LessonStudent_FilterSemester"><option value="*">все семестры</option>
<option selected="selected" value="28">2026/2027, Осень</option>
<option value="27">2025/2026, Весна</option>
</select>
            </div>
        </div>
    </div>
    <div class="table-responsive">
        <table id="tbl__PartialListStudent__Lesson__Student"
               data-q-id="11111111-2222-3333-4444-555555555555"
               data-q-sort-order="CustomSort1 desc"
               data-q-page="1"
               data-q-page-size="10"
               class="table table-sm table-striped table-bordered table-hover mb-0">
            <thead><tr><th><label><input type="checkbox" class="form-check-input" /></label>
                <a href="#" data-col-name="CustomSort1" class="text-dark">Занятие</a></th><th class="w-1"></th></tr></thead>
            <tbody>
                <tr data-ls-id="32560103">
                    <td>
                        <label style="font-weight: normal">
                            <input type="checkbox" class="form-check-input" />

                            28.10.26, 7 пара (20:30-22:00), лекция (Петров П.П.), Аппаратные средства вычислительной техники (экзамен), ИЭозс-45-25
                        </label>

                        <span class="badge bg-danger" style="font-size: 12px;">пропуск</span>

                        <br />
                        <span class="ls-header">Запись занятия создана:</span>
                        <span class="ls-item"> 22.10.2026 20:11:36 Петров П.П.</span>
                    </td>
                    <td><div class="btn-group"></div></td>
                </tr>
                <tr data-ls-id="32560081">
                    <td>
                        <label style="font-weight: normal">
                            <input type="checkbox" class="form-check-input" />
                            28.10.26, 6 пара (18:55-20:25), лекция (Петров П.П.), Аппаратные средства вычислительной техники (экзамен), ИЭозс-45-25
                        </label>
                        <span class="badge bg-danger" style="font-size: 12px;">пропуск</span>
                    </td>
                    <td><div class="btn-group"></div></td>
                </tr>
                <tr data-ls-id="32539713">
                    <td>
                        <label style="font-weight: normal">
                            <input type="checkbox" class="form-check-input" />
                            21.10.26, 7 пара (20:30-22:00), лекция (Кузнецов К.К.), Культурология (зачёт (без оценки)), ИЭозс-45-25
                        </label>
                        <span class="badge bg-danger" style="font-size: 12px;">пропуск</span>
                    </td>
                    <td><div class="btn-group"></div></td>
                </tr>
                <tr data-ls-id="32458300">
                    <td>
                        <label style="font-weight: normal">
                            <input type="checkbox" class="form-check-input" />
                            16.10.26, 5 пара (17:20-18:50), практическое занятие (Орлов О.О.), Математика, часть 1 (экзамен), ИЭозс-45-25
                        </label>
                    </td>
                    <td><div class="btn-group"></div></td>
                </tr>
            </tbody>
        </table>
    </div>
</div>
<div id="st_les__Pager" class="d-flex">
    <div class='pager-size'><span>Отображены 1-10 записи из 26</span><br/><span>Отображать <select><option value='10' selected>10</option><option value='25'>25</option></select> записей</span></div>
    <div class="ms-auto flex-shrink-1"><ul class="pagination justify-content-end flex-wrap"><li class="page-item active"><span class="page-link">1</span></li><li class="page-item"><a class="page-link" href="2">2</a></li></ul></div>
</div>
</div>
"""
    }
}
