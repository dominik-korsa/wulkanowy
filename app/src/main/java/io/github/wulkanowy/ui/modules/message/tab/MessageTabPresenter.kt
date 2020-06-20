package io.github.wulkanowy.ui.modules.message.tab

import io.github.wulkanowy.data.db.entities.Message
import io.github.wulkanowy.data.repositories.message.MessageFolder
import io.github.wulkanowy.data.repositories.message.MessageRepository
import io.github.wulkanowy.data.repositories.semester.SemesterRepository
import io.github.wulkanowy.data.repositories.student.StudentRepository
import io.github.wulkanowy.ui.base.BasePresenter
import io.github.wulkanowy.ui.base.ErrorHandler
import io.github.wulkanowy.utils.FirebaseAnalyticsHelper
import io.github.wulkanowy.utils.SchedulersProvider
import io.github.wulkanowy.utils.toFormattedString
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.rx2.rxSingle
import me.xdrop.fuzzywuzzy.FuzzySearch
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.pow

class MessageTabPresenter @Inject constructor(
    schedulers: SchedulersProvider,
    errorHandler: ErrorHandler,
    studentRepository: StudentRepository,
    private val messageRepository: MessageRepository,
    private val semesterRepository: SemesterRepository,
    private val analytics: FirebaseAnalyticsHelper
) : BasePresenter<MessageTabView>(errorHandler, studentRepository, schedulers) {

    lateinit var folder: MessageFolder

    private lateinit var lastError: Throwable

    private var lastSearchQuery = ""

    private var messages = emptyList<Message>()

    private val searchQuery = PublishSubject.create<String>()

    fun onAttachView(view: MessageTabView, folder: MessageFolder) {
        super.onAttachView(view)
        view.initView()
        initializeSearchStream()
        errorHandler.showErrorMessage = ::showErrorViewOnError
        this.folder = folder
    }

    fun onSwipeRefresh() {
        Timber.i("Force refreshing the $folder message")
        onParentViewLoadData(true)
    }

    fun onRetry() {
        view?.run {
            showErrorView(false)
            showProgress(true)
        }
        loadData(true)
    }

    fun onDetailsClick() {
        view?.showErrorDetailsDialog(lastError)
    }

    fun onDeleteMessage() {
        loadData(false)
    }

    fun onParentViewLoadData(forceRefresh: Boolean) {
        loadData(forceRefresh)
    }

    fun onMessageItemSelected(message: Message, position: Int) {
        Timber.i("Select message ${message.id} item (position: $position)")
        view?.run {
            openMessage(message)
            if (message.unread) {
                message.unread = false
                updateItem(message, position)
            }
        }
    }

    private fun loadData(forceRefresh: Boolean) {
        Timber.i("Loading $folder message data started")
        disposable.add(rxSingle { studentRepository.getCurrentStudent() }
            .flatMap { student ->
                rxSingle { semesterRepository.getCurrentSemester(student) }
                    .flatMap { rxSingle { messageRepository.getMessages(student, it, folder, forceRefresh) } }
            }
            .subscribeOn(schedulers.backgroundThread)
            .observeOn(schedulers.mainThread)
            .doFinally {
                view?.run {
                    showRefresh(false)
                    showProgress(false)
                    enableSwipe(true)
                    notifyParentDataLoaded()
                }
            }
            .subscribe({
                Timber.i("Loading $folder message result: Success")
                messages = it
                view?.updateData(getFilteredData(lastSearchQuery))
                analytics.logEvent(
                    "load_data",
                    "type" to "messages",
                    "items" to it.size,
                    "folder" to folder.name
                )
            }) {
                Timber.i("Loading $folder message result: An exception occurred")
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

    fun onSearchQueryTextChange(query: String) {
        if (query != searchQuery.toString())
            searchQuery.onNext(query)
    }

    private fun initializeSearchStream() {
        disposable.add(searchQuery
            .debounce(250, TimeUnit.MILLISECONDS)
            .map { query ->
                lastSearchQuery = query
                getFilteredData(query)
            }
            .subscribeOn(schedulers.backgroundThread)
            .observeOn(schedulers.mainThread)
            .subscribe({
                Timber.d("Applying filter. Full list: ${messages.size}, filtered: ${it.size}")
                updateData(it)
            }) { Timber.e(it) })
    }

    private fun getFilteredData(query: String): List<Message> {
        return if (query.trim().isEmpty()) {
            messages.sortedByDescending { it.date }
        } else {
            messages
                .map { it to calculateMatchRatio(it, query) }
                .sortedByDescending { it.second }
                .filter { it.second > 5000 }
                .map { it.first }
        }
    }

    private fun updateData(data: List<Message>) {
        view?.run {
            showEmpty(data.isEmpty())
            showContent(data.isNotEmpty())
            showErrorView(false)
            updateData(data)
            resetListPosition()
        }
    }

    private fun calculateMatchRatio(message: Message, query: String): Int {
        val subjectRatio = FuzzySearch.tokenSortPartialRatio(
            query.toLowerCase(Locale.getDefault()),
            message.subject
        )

        val senderOrRecipientRatio = FuzzySearch.tokenSortPartialRatio(
            query.toLowerCase(Locale.getDefault()),
            if (message.sender.isNotEmpty()) message.sender.toLowerCase(Locale.getDefault())
            else message.recipient.toLowerCase(Locale.getDefault())
        )

        val dateRatio = listOf(
            FuzzySearch.ratio(
                query.toLowerCase(Locale.getDefault()),
                message.date.toFormattedString("dd.MM").toLowerCase(Locale.getDefault())
            ),
            FuzzySearch.ratio(
                query.toLowerCase(Locale.getDefault()),
                message.date.toFormattedString("dd.MM.yyyy").toLowerCase(Locale.getDefault())
            ),
            FuzzySearch.ratio(
                query.toLowerCase(Locale.getDefault()),
                message.date.toFormattedString("d MMMM").toLowerCase(Locale.getDefault())
            ),
            FuzzySearch.ratio(
                query.toLowerCase(Locale.getDefault()),
                message.date.toFormattedString("d MMMM yyyy").toLowerCase(Locale.getDefault())
            )
        ).max() ?: 0


        return (subjectRatio.toDouble().pow(2)
            + senderOrRecipientRatio.toDouble().pow(2)
            + dateRatio.toDouble().pow(2) * 2
            ).toInt()
    }
}
