package io.github.wulkanowy.utils

import io.github.wulkanowy.data.db.entities.Timetable
import org.threeten.bp.Duration
import org.threeten.bp.Duration.between
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalDateTime.now

fun Timetable.isShowTimeUntil(previousLessonEnd: LocalDateTime?) = when {
    !isStudentPlan -> false
    canceled -> false
    now().isAfter(start) -> false
    previousLessonEnd != null && now().isBefore(previousLessonEnd) -> false
    else -> between(now(), start) <= Duration.ofMinutes(60)
}

inline val Timetable.left: Duration?
    get() = when {
        canceled -> null
        end.isAfter(now()) && start.isBefore(now()) -> between(now(), end)
        else -> null
    }

inline val Timetable.until: Duration
    get() = between(now(), start)

inline val Timetable.isJustFinished: Boolean
    get() = end.isBefore(now()) && end.plusSeconds(15).isAfter(now()) && !canceled
