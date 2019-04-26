package io.github.wulkanowy.data.repositories.timetable

import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.github.pwittchen.reactivenetwork.library.rx2.internet.observing.InternetObservingSettings
import io.github.wulkanowy.data.db.entities.Semester
import io.github.wulkanowy.data.db.entities.Timetable
import io.github.wulkanowy.utils.friday
import io.github.wulkanowy.utils.monday
import io.reactivex.Single
import org.threeten.bp.LocalDate
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimetableRepository @Inject constructor(
    private val settings: InternetObservingSettings,
    private val local: TimetableLocal,
    private val remote: TimetableRemote
) {

    fun getTimetable(semester: Semester, start: LocalDate, end: LocalDate, forceRefresh: Boolean = false): Single<List<Timetable>> {
        return Single.fromCallable { start.monday to end.friday }.flatMap { (monday, friday) ->
            local.getTimetable(semester, monday, friday).filter { !forceRefresh }
                .switchIfEmpty(ReactiveNetwork.checkInternetConnectivity(settings).flatMap {
                    if (it) remote.getTimetable(semester, monday, friday)
                    else Single.error(UnknownHostException())
                }.flatMap { newTimetable ->
                    local.getTimetable(semester, monday, friday)
                        .toSingle(emptyList())
                        .doOnSuccess { oldTimetable ->
                            local.deleteTimetable(oldTimetable - newTimetable)
                            local.saveTimetable((newTimetable - oldTimetable).map { item ->
                                item.apply {
                                    oldTimetable.singleOrNull { this.start == it.start }?.let {
                                        return@map copy(
                                            room = if (room.isEmpty()) it.room else room,
                                            teacher = if (teacher.isEmpty()) it.teacher else teacher
                                        )
                                    }
                                }
                            })
                        }
                }.flatMap {
                    local.getTimetable(semester, monday, friday).toSingle(emptyList())
                }).map { list -> list.filter { it.date in start..end } }
        }
    }
}
