package io.github.wulkanowy.data.repositories.timetable

import io.github.wulkanowy.api.Api
import io.github.wulkanowy.data.db.entities.Semester
import io.github.wulkanowy.data.db.entities.Timetable
import io.github.wulkanowy.utils.toLocalDate
import io.github.wulkanowy.utils.toLocalDateTime
import io.reactivex.Single
import org.threeten.bp.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimetableRemote @Inject constructor(private val api: Api) {

    fun getTimetable(semester: Semester, startDate: LocalDate, endDate: LocalDate): Single<List<Timetable>> {
        return Single.just(api.apply { diaryId = semester.diaryId })
            .flatMap { it.getTimetable(startDate, endDate) }
            .map { lessons ->
                lessons.map {
                    Timetable(
                        studentId = semester.studentId,
                        diaryId = semester.diaryId,
                        number = it.number,
                        start = it.start.toLocalDateTime(),
                        end = it.end.toLocalDateTime(),
                        date = it.date.toLocalDate(),
                        subject = it.subject,
                        subjectOld = it.subjectOld,
                        group = it.group,
                        room = it.room,
                        roomOld = it.roomOld,
                        teacher = it.teacher,
                        teacherOld = it.teacherOld,
                        info = it.info,
                        changes = it.changes,
                        canceled = it.canceled
                    )
                }
            }
    }
}
