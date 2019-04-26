package io.github.wulkanowy.ui.modules.attendance.summary

import io.github.wulkanowy.data.db.entities.AttendanceSummary
import io.github.wulkanowy.data.db.entities.Subject
import io.github.wulkanowy.data.repositories.attendancesummary.AttendanceSummaryRepository
import io.github.wulkanowy.data.repositories.semester.SemesterRepository
import io.github.wulkanowy.data.repositories.student.StudentRepository
import io.github.wulkanowy.data.repositories.subject.SubjectRepository
import io.github.wulkanowy.ui.base.session.BaseSessionPresenter
import io.github.wulkanowy.ui.base.session.SessionErrorHandler
import io.github.wulkanowy.utils.FirebaseAnalyticsHelper
import io.github.wulkanowy.utils.SchedulersProvider
import io.github.wulkanowy.utils.calculatePercentage
import io.github.wulkanowy.utils.getFormattedName
import timber.log.Timber
import java.lang.String.format
import java.util.Locale.FRANCE
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject

class AttendanceSummaryPresenter @Inject constructor(
    private val errorHandler: SessionErrorHandler,
    private val attendanceSummaryRepository: AttendanceSummaryRepository,
    private val subjectRepository: SubjectRepository,
    private val studentRepository: StudentRepository,
    private val semesterRepository: SemesterRepository,
    private val schedulers: SchedulersProvider,
    private val analytics: FirebaseAnalyticsHelper
) : BaseSessionPresenter<AttendanceSummaryView>(errorHandler) {

    private var subjects = emptyList<Subject>()

    var currentSubjectId = -1
        private set

    fun onAttachView(view: AttendanceSummaryView, subjectId: Int?) {
        super.onAttachView(view)
        Timber.i("Attendance summary view is attached with subject id ${subjectId ?: -1}")
        view.initView()
        loadData(subjectId ?: -1)
        loadSubjects()
    }

    fun onSwipeRefresh() {
        Timber.i("Force refreshing the attendance summary")
        loadData(currentSubjectId, true)
    }

    fun onSubjectSelected(name: String) {
        Timber.i("Select attendance summary subject $name")
        view?.run {
            showContent(false)
            showProgress(true)
            enableSwipe(false)
            clearView()
        }
        (subjects.singleOrNull { it.name == name }?.realId ?: -1).let {
            if (it != currentSubjectId) loadData(it)
        }
    }

    private fun loadData(subjectId: Int, forceRefresh: Boolean = false) {
        Timber.i("Loading attendance summary data started")
        currentSubjectId = subjectId
        disposable.apply {
            clear()
            add(studentRepository.getCurrentStudent()
                .delay(200, MILLISECONDS)
                .flatMap { semesterRepository.getCurrentSemester(it) }
                .flatMap { attendanceSummaryRepository.getAttendanceSummary(it, subjectId, forceRefresh) }
                .map { createAttendanceSummaryItems(it) to AttendanceSummaryScrollableHeader(formatPercentage(it.calculatePercentage())) }
                .subscribeOn(schedulers.backgroundThread)
                .observeOn(schedulers.mainThread)
                .doFinally {
                    view?.run {
                        hideRefresh()
                        showProgress(false)
                        enableSwipe(true)
                    }
                }
                .subscribe({
                    Timber.i("Loading attendance summary result: Success")
                    view?.apply {
                        showEmpty(it.first.isEmpty())
                        showContent(it.first.isNotEmpty())
                        updateDataSet(it.first, it.second)
                    }
                    analytics.logEvent("load_attendance_summary", "items" to it.first.size, "force_refresh" to forceRefresh, "item_id" to subjectId)
                }) {
                    Timber.i("Loading attendance summary result: An exception occurred")
                    view?.run { showEmpty(isViewEmpty) }
                    errorHandler.dispatch(it)
                }
            )
        }
    }

    private fun loadSubjects() {
        Timber.i("Loading attendance summary subjects started")
        disposable.add(studentRepository.getCurrentStudent()
            .flatMap { semesterRepository.getCurrentSemester(it) }
            .flatMap { subjectRepository.getSubjects(it) }
            .doOnSuccess { subjects = it }
            .map { ArrayList(it.map { subject -> subject.name }) }
            .subscribeOn(schedulers.backgroundThread)
            .observeOn(schedulers.mainThread)
            .subscribe({
                Timber.i("Loading attendance summary subjects result: Success")
                view?.run {
                    view?.updateSubjects(it)
                    showSubjects(true)
                }
            }, {
                Timber.i("Loading attendance summary subjects result: An exception occurred")
                errorHandler.dispatch(it)
            })
        )
    }

    private fun createAttendanceSummaryItems(attendanceSummary: List<AttendanceSummary>): List<AttendanceSummaryItem> {
        return attendanceSummary.sortedByDescending { it.id }.map {
            AttendanceSummaryItem(
                month = it.month.getFormattedName(),
                percentage = formatPercentage(it.calculatePercentage()),
                present = it.presence.toString(),
                absence = it.absence.toString(),
                excusedAbsence = it.absenceExcused.toString(),
                schoolAbsence = it.absenceForSchoolReasons.toString(),
                exemption = it.exemption.toString(),
                lateness = it.lateness.toString(),
                excusedLateness = it.latenessExcused.toString()
            )
        }
    }

    private fun formatPercentage(percentage: Double): String {
        return if (percentage == 0.0) "0%"
        else "${format(FRANCE, "%.2f", percentage)}%"
    }
}
