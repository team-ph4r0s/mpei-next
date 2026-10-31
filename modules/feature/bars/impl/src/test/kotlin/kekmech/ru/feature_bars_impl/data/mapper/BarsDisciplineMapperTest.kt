package kekmech.ru.feature_bars_impl.data.mapper

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kekmech.ru.feature_bars_impl.domain.FinalGradeType
import kekmech.ru.lib_bars_parser.BarsActivity
import kekmech.ru.lib_bars_parser.BarsActivityType
import kekmech.ru.lib_bars_parser.BarsDiscipline
import kekmech.ru.lib_bars_parser.BarsMark

class BarsDisciplineMapperTest : StringSpec({

    fun mark(raw: String, value: Float?) =
        BarsMark(raw = raw, value = value, date = null, retakenFrom = null, retakenDate = null, note = null)

    fun activity(type: BarsActivityType, name: String, weight: String? = null, weekNum: String? = null, mark: BarsMark? = null) =
        BarsActivity(type = type, name = name, weight = weight, weekNum = weekNum, mark = mark, criteria = null)

    val discipline = BarsDiscipline(
        name = "Физика",
        person = "Иванов И.И.",
        assessmentType = "экзамен",
        units = "4ЗЕ",
        deadline = null,
        notes = emptyList(),
        activities = listOf(
            activity(
                BarsActivityType.CONTROL_ACTIVITY,
                name = "КМ-1",
                weight = " 30 ",
                weekNum = "15 (07.12.26-13.12.26)",
                mark = mark("4  (06.06.26)", 4f),
            ),
            activity(BarsActivityType.CONTROL_ACTIVITY, name = "КМ-2", weekNum = "7", mark = null),
            activity(BarsActivityType.CURRENT_SCORE, name = "Балл текущего контроля", mark = mark("2,00", 2f)),
            activity(BarsActivityType.FINAL_MARK, name = "Итоговая оценка", mark = null),
            activity(BarsActivityType.UNDEFINED, name = "Что-то ещё", mark = mark("5", 5f)),
        ),
    )

    "the control events keep the weight, the week of the deadline and the mark of the cell" {
        val mapped = BarsDisciplineMapper.map(listOf(discipline)).single()

        mapped.name shouldBe "Физика"
        mapped.person shouldBe "Иванов И.И."
        mapped.assessmentType shouldBe "Экзамен"
        val first = mapped.controlActivities.first()
        first.weight shouldBe "30"
        first.deadline shouldBe "15"
        first.finalMark shouldBe 4f
        first.rawMark shouldBe "4  (06.06.26)"
    }

    "a control event without a mark has no mark" {
        val second = BarsDisciplineMapper.map(listOf(discipline)).single().controlActivities[1]

        second.finalMark shouldBe -1f
        second.rawMark shouldBe ""
        second.deadline shouldBe "7"
    }

    "the totals become final grades and the unknown rows are dropped" {
        val grades = BarsDisciplineMapper.map(listOf(discipline)).single().finalGrades

        grades.map { it.type } shouldBe listOf(FinalGradeType.CURRENT_SCORE, FinalGradeType.FINAL_MARK)
        grades.map { it.finalMark } shouldBe listOf(2f, -1f)
    }
})
