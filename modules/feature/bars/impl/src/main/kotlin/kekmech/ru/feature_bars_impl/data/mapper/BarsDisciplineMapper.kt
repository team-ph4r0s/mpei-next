package kekmech.ru.feature_bars_impl.data.mapper

import kekmech.ru.ext_kotlin.capitalizeSafe
import kekmech.ru.feature_bars_impl.domain.AssessedDiscipline
import kekmech.ru.feature_bars_impl.domain.ControlActivity
import kekmech.ru.feature_bars_impl.domain.FinalGrade
import kekmech.ru.feature_bars_impl.domain.FinalGradeType
import kekmech.ru.lib_bars_parser.BarsActivityType
import kekmech.ru.lib_bars_parser.BarsDiscipline

/** The disciplines of the common BARS parser in the form that the Android screens and their cache have. */
internal object BarsDisciplineMapper {

    fun map(disciplines: List<BarsDiscipline>): List<AssessedDiscipline> =
        disciplines.map { discipline ->
            AssessedDiscipline(
                name = discipline.name,
                person = discipline.person,
                assessmentType = discipline.assessmentType.capitalizeSafe(),
                controlActivities = discipline.activities
                    .filter { it.type == BarsActivityType.CONTROL_ACTIVITY }
                    .map { activity ->
                        ControlActivity(
                            name = activity.name,
                            weight = activity.weight?.trim().orEmpty(),
                            deadline = activity.weekNum?.extractIntOrNull()?.toString().orEmpty(),
                            finalMark = activity.mark?.value ?: NO_MARK,
                            rawMark = activity.mark?.raw?.trim().orEmpty(),
                        )
                    },
                finalGrades = discipline.activities
                    .mapNotNull { activity ->
                        val type = activity.type.toFinalGradeType() ?: return@mapNotNull null
                        FinalGrade(
                            name = activity.name,
                            finalMark = activity.mark?.value ?: NO_MARK,
                            type = type,
                        )
                    },
            )
        }

    private fun BarsActivityType.toFinalGradeType(): FinalGradeType? =
        when (this) {
            BarsActivityType.CURRENT_SCORE -> FinalGradeType.CURRENT_SCORE
            BarsActivityType.CONTROL_WEEK -> FinalGradeType.CONTROL_WEEK
            BarsActivityType.INTERMEDIATE_MARK -> FinalGradeType.INTERMEDIATE_MARK
            BarsActivityType.FINAL_MARK -> FinalGradeType.FINAL_MARK
            BarsActivityType.CONTROL_ACTIVITY, BarsActivityType.UNDEFINED -> null
        }

    /** "15 (07.12.26-13.12.26)" is the week 15. */
    private fun String.extractIntOrNull() = toIntOrNull()
        ?: split("[\\s(/]+".toRegex())[0].toIntOrNull()

    /** The mark that the screens show as "no mark yet". */
    private const val NO_MARK = -1f
}
