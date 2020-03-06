package io.github.wulkanowy.ui.modules.timetable

import io.github.wulkanowy.data.db.entities.Timetable
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime

class TimetableItemDisplayState(lesson: Timetable, previousLessonEnd: LocalDateTime?) {
    val justFinished : Boolean = lesson.end.isBefore(LocalDateTime.now()) && lesson.end.plusSeconds(15).isAfter(LocalDateTime.now()) && !lesson.canceled

    val timeLeft : Duration? = when {
        lesson.canceled -> null
        lesson.end.isAfter(LocalDateTime.now()) && lesson.start.isBefore(LocalDateTime.now()) -> Duration.between(LocalDateTime.now(), lesson.end)
        else -> null
    }

    val timeUntil : Duration = Duration.between(LocalDateTime.now(), lesson.start)

    val showTimeUntil : Boolean

    val code : String?

    init {
        showTimeUntil =
            when {
                !lesson.isStudentPlan -> false
                lesson.canceled -> false
                LocalDateTime.now().isAfter(lesson.start) -> false
                previousLessonEnd != null && LocalDateTime.now().isBefore(previousLessonEnd) -> false
                else -> timeUntil <= Duration.ofMinutes(60)
            }
        code = when {
            !lesson.isStudentPlan -> null
            justFinished -> "just finished"
            showTimeUntil ->
                if (timeUntil.seconds <= 60) "in ${timeUntil.seconds.toInt()} s"
                else "in ${timeUntil.toMinutes()} m"
            timeLeft == null -> null
            timeLeft.seconds <= 60 -> "left ${timeLeft.seconds.toInt()} s"
            else -> "left ${timeLeft.toMinutes()} m"
        }
    }
}