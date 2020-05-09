package io.github.wulkanowy.ui.modules.grade.details

import io.github.wulkanowy.data.db.entities.Grade
import io.github.wulkanowy.data.repositories.grade.GradeRepository
import io.github.wulkanowy.data.repositories.preferences.PreferencesRepository
import io.github.wulkanowy.data.repositories.semester.SemesterRepository
import io.github.wulkanowy.data.repositories.student.StudentRepository
import io.github.wulkanowy.ui.base.BasePresenter
import io.github.wulkanowy.ui.base.ErrorHandler
import io.github.wulkanowy.ui.modules.grade.GradeAverageProvider
import io.github.wulkanowy.utils.FirebaseAnalyticsHelper
import io.github.wulkanowy.utils.SchedulersProvider
import timber.log.Timber
import javax.inject.Inject

class GradeDetailsPresenter @Inject constructor(
    schedulers: SchedulersProvider,
    errorHandler: ErrorHandler,
    studentRepository: StudentRepository,
    private val gradeRepository: GradeRepository,
    private val semesterRepository: SemesterRepository,
    private val preferencesRepository: PreferencesRepository,
    private val averageProvider: GradeAverageProvider,
    private val analytics: FirebaseAnalyticsHelper
) : BasePresenter<GradeDetailsView>(errorHandler, studentRepository, schedulers) {

    private var newGradesAmount: Int = 0

    private var currentSemesterId = 0

    private lateinit var lastError: Throwable

    override fun onAttachView(view: GradeDetailsView) {
        super.onAttachView(view)
        view.initView()
        errorHandler.showErrorMessage = ::showErrorViewOnError
    }

    fun onParentViewLoadData(semesterId: Int, forceRefresh: Boolean) {
        currentSemesterId = semesterId
        loadData(semesterId, forceRefresh)
    }

    fun onGradeItemSelected(grade: Grade, position: Int) {
        Timber.i("Select grade item ${grade.id}")
        view?.apply {
            showGradeDialog(grade, preferencesRepository.gradeColorTheme)
            if (!grade.isRead) {
                grade.isRead = true
                updateItem(grade, position)
                getHeaderOfItem(grade.subject).let { header ->
                    (header.value as GradeDetailsHeader).newGrades--
                    updateHeaderItem(header)
                }
                newGradesAmount--
                updateMarkAsDoneButton()
                updateGrade(grade)
            }
        }
    }

    fun onMarkAsReadSelected(): Boolean {
        Timber.i("Select mark grades as read")
        disposable.add(studentRepository.getCurrentStudent()
            .flatMap { semesterRepository.getSemesters(it) }
            .flatMap { gradeRepository.getUnreadGrades(it.first { item -> item.semesterId == currentSemesterId }) }
            .map { it.map { grade -> grade.apply { isRead = true } } }
            .flatMapCompletable {
                Timber.i("Mark as read ${it.size} grades")
                gradeRepository.updateGrades(it)
            }
            .subscribeOn(schedulers.backgroundThread)
            .observeOn(schedulers.mainThread)
            .subscribe({
                Timber.i("Mark as read result: Success")
                loadData(currentSemesterId, false)
            }, {
                Timber.i("Mark as read result: An exception occurred")
                errorHandler.dispatch(it)
            }))
        return true
    }

    fun onSwipeRefresh() {
        Timber.i("Force refreshing the grade details")
        view?.notifyParentRefresh()
    }

    fun onRetry() {
        view?.run {
            showErrorView(false)
            showProgress(true)
        }
        view?.notifyParentRefresh()
    }

    fun onDetailsClick() {
        view?.showErrorDetailsDialog(lastError)
    }

    fun onParentViewReselected() {
        view?.run {
            if (!isViewEmpty) {
                if (preferencesRepository.isGradeExpandable) collapseAllItems()
                scrollToStart()
            }
        }
    }

    fun onParentViewChangeSemester() {
        view?.run {
            showProgress(true)
            enableSwipe(false)
            showRefresh(false)
            showContent(false)
            showEmpty(false)
            clearView()
        }
        disposable.clear()
    }

    fun updateMarkAsDoneButton() {
        view?.enableMarkAsDoneButton(newGradesAmount > 0)
    }

    private fun loadData(semesterId: Int, forceRefresh: Boolean) {
        Timber.i("Loading grade details data started")
        disposable.add(studentRepository.getCurrentStudent()
            .flatMap { semesterRepository.getSemesters(it).map { semester -> it to semester } }
            .flatMap { (student, semesters) ->
                averageProvider.getGradeAverage(student, semesters, semesterId, forceRefresh).flatMap { averages ->
                    gradeRepository.getGrades(student, semesters.first { it.semesterId == semesterId }, forceRefresh)
                        .map { it.sortedByDescending { grade -> grade.date } }
                        .map { createGradeItems(it, averages) }
                }
            }
            .subscribeOn(schedulers.backgroundThread)
            .observeOn(schedulers.mainThread)
            .doFinally {
                view?.run {
                    showRefresh(false)
                    showProgress(false)
                    enableSwipe(true)
                    notifyParentDataLoaded(semesterId)
                }
            }
            .subscribe({ grades ->
                Timber.i("Loading grade details result: Success")
                newGradesAmount = grades
                    .filter { it.viewType == ViewType.HEADER }
                    .sumBy { item -> (item.value as GradeDetailsHeader).newGrades }
                updateMarkAsDoneButton()
                view?.run {
                    showEmpty(grades.isEmpty())
                    showErrorView(false)
                    showContent(grades.isNotEmpty())
                    updateData(
                        data = grades,
                        isGradeExpandable = preferencesRepository.isGradeExpandable,
                        gradeColorTheme = preferencesRepository.gradeColorTheme
                    )
                }
                analytics.logEvent("load_grade_details", "items" to grades.size, "force_refresh" to forceRefresh)
            }) {
                Timber.i("Loading grade details result: An exception occurred")
                errorHandler.dispatch(it)
            })
    }

    private fun showErrorViewOnError(message: String, error: Throwable) {
        view?.run {
            if (isViewEmpty) {
                lastError = error
                setErrorDetails(message)
                showErrorView(true)
                showEmpty(false)
            } else showError(message, error)
        }
    }

    private fun createGradeItems(items: List<Grade>, averages: List<Triple<String, Double, String>>): List<GradeDetailsItem> {
        return items.groupBy { grade -> grade.subject }.toSortedMap().map { (subject, grades) ->
            val subItems = grades.map {
                GradeDetailsItem(it, ViewType.ITEM)
            }

            listOf(GradeDetailsItem(GradeDetailsHeader(
                subject = subject,
                average = averages.singleOrNull { subject == it.first }?.second,
                pointsSum = averages.singleOrNull { subject == it.first }?.third,
                number = grades.size,
                newGrades = grades.filter { grade -> !grade.isRead }.size,
                grades = subItems
            ), ViewType.HEADER)) + if (preferencesRepository.isGradeExpandable) emptyList() else subItems
        }.flatten()
    }

    private fun updateGrade(grade: Grade) {
        Timber.i("Attempt to update grade ${grade.id}")
        disposable.add(gradeRepository.updateGrade(grade)
            .subscribeOn(schedulers.backgroundThread)
            .observeOn(schedulers.mainThread)
            .subscribe({
                Timber.i("Update grade result: Success")
            }) { error ->
                Timber.i("Update grade result: An exception occurred")
                errorHandler.dispatch(error)
            })
    }
}
