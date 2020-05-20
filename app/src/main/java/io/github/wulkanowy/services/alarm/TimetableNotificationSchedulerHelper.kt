package io.github.wulkanowy.services.alarm

import android.app.AlarmManager
import android.app.AlarmManager.RTC_WAKEUP
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import android.content.Context
import android.content.Intent
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationManagerCompat
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.data.db.entities.Timetable
import io.github.wulkanowy.data.repositories.preferences.PreferencesRepository
import io.github.wulkanowy.services.alarm.TimetableNotificationReceiver.Companion.LESSON_END
import io.github.wulkanowy.services.alarm.TimetableNotificationReceiver.Companion.LESSON_NEXT_ROOM
import io.github.wulkanowy.services.alarm.TimetableNotificationReceiver.Companion.LESSON_NEXT_TITLE
import io.github.wulkanowy.services.alarm.TimetableNotificationReceiver.Companion.LESSON_ROOM
import io.github.wulkanowy.services.alarm.TimetableNotificationReceiver.Companion.LESSON_START
import io.github.wulkanowy.services.alarm.TimetableNotificationReceiver.Companion.LESSON_TITLE
import io.github.wulkanowy.services.alarm.TimetableNotificationReceiver.Companion.LESSON_TYPE
import io.github.wulkanowy.services.alarm.TimetableNotificationReceiver.Companion.NOTIFICATION_ID
import io.github.wulkanowy.services.alarm.TimetableNotificationReceiver.Companion.NOTIFICATION_TYPE_CURRENT
import io.github.wulkanowy.services.alarm.TimetableNotificationReceiver.Companion.NOTIFICATION_TYPE_LAST_LESSON_CANCELLATION
import io.github.wulkanowy.services.alarm.TimetableNotificationReceiver.Companion.NOTIFICATION_TYPE_UPCOMING
import io.github.wulkanowy.services.alarm.TimetableNotificationReceiver.Companion.STUDENT_ID
import io.github.wulkanowy.services.alarm.TimetableNotificationReceiver.Companion.STUDENT_NAME
import io.github.wulkanowy.ui.modules.main.MainView
import io.github.wulkanowy.utils.toTimestamp
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalDateTime.now
import timber.log.Timber
import javax.inject.Inject

class TimetableNotificationSchedulerHelper @Inject constructor(
    private val context: Context,
    private val alarmManager: AlarmManager,
    private val preferencesRepository: PreferencesRepository
) {

    private fun getRequestCode(time: LocalDateTime, studentId: Int) = (time.toTimestamp() * studentId).toInt()

    private fun getUpcomingLessonTime(index: Int, day: List<Timetable>, lesson: Timetable): LocalDateTime {
        return day.getOrNull(index - 1)?.end ?: lesson.start.minusMinutes(30)
    }

    fun cancelScheduled(lessons: List<Timetable>, studentId: Int = 1) {
        lessons.sortedBy { it.start }.forEachIndexed { index, lesson ->
            val upcomingTime = getUpcomingLessonTime(index, lessons, lesson)
            cancelScheduledTo(upcomingTime..lesson.start, getRequestCode(upcomingTime, studentId))
            cancelScheduledTo(lesson.start..lesson.end, getRequestCode(lesson.start, studentId))

            Timber.d("TimetableNotification canceled: type 1 & 2, subject: ${lesson.subject}, start: ${lesson.start}, student: $studentId")
        }
    }

    private fun cancelScheduledTo(range: ClosedRange<LocalDateTime>, requestCode: Int) {
        if (now() in range) cancelNotification()
        alarmManager.cancel(PendingIntent.getBroadcast(context, requestCode, Intent(), FLAG_CANCEL_CURRENT))
    }

    fun cancelNotification() = NotificationManagerCompat.from(context).cancel(MainView.Section.TIMETABLE.id)

    fun scheduleNotifications(lessons: List<Timetable>, student: Student) {
        if (!preferencesRepository.isUpcomingLessonsNotificationsEnable) return cancelScheduled(lessons, student.studentId)

        lessons.groupBy { it.date }
            .map { it.value.sortedBy { lesson -> lesson.start } }
            .map { it.filter { lesson -> !lesson.canceled && lesson.isStudentPlan } }
            .map { day ->
                day.forEachIndexed { index, lesson ->
                    val intent = createIntent(student, lesson, day.getOrNull(index + 1))

                    if (lesson.start > now()) {
                        scheduleBroadcast(intent, student.studentId, NOTIFICATION_TYPE_UPCOMING, getUpcomingLessonTime(index, day, lesson))
                    }

                    if (lesson.end > now()) {
                        scheduleBroadcast(intent, student.studentId, NOTIFICATION_TYPE_CURRENT, lesson.start)
                        if (day.lastIndex == index) {
                            scheduleBroadcast(intent, student.studentId, NOTIFICATION_TYPE_LAST_LESSON_CANCELLATION, lesson.end)
                        }
                    }
                }
            }
    }

    private fun createIntent(student: Student, lesson: Timetable, nextLesson: Timetable?): Intent {
        return Intent(context, TimetableNotificationReceiver::class.java).apply {
            putExtra(STUDENT_ID, student.studentId)
            putExtra(STUDENT_NAME, student.studentName)
            putExtra(LESSON_ROOM, lesson.room)
            putExtra(LESSON_START, lesson.start.toTimestamp())
            putExtra(LESSON_END, lesson.end.toTimestamp())
            putExtra(LESSON_TITLE, lesson.subject)
            putExtra(LESSON_NEXT_TITLE, nextLesson?.subject)
            putExtra(LESSON_NEXT_ROOM, nextLesson?.room)
        }
    }

    private fun scheduleBroadcast(intent: Intent, studentId: Int, notificationType: Int, time: LocalDateTime) {
        AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, RTC_WAKEUP, time.toTimestamp(),
            PendingIntent.getBroadcast(context, getRequestCode(time, studentId), intent.also {
                it.putExtra(NOTIFICATION_ID, MainView.Section.TIMETABLE.id)
                it.putExtra(LESSON_TYPE, notificationType)
            }, FLAG_CANCEL_CURRENT)
        )
        Timber.d("TimetableNotification scheduled: type: $notificationType, subject: ${intent.getStringExtra(LESSON_TITLE)}, start: $time, student: $studentId")
    }
}
