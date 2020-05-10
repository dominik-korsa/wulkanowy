package io.github.wulkanowy.ui.modules.timetable

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.recyclerview.widget.LinearLayoutManager
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import io.github.wulkanowy.R
import io.github.wulkanowy.data.db.entities.Timetable
import io.github.wulkanowy.databinding.FragmentTimetableBinding
import io.github.wulkanowy.ui.base.BaseFragment
import io.github.wulkanowy.ui.modules.main.MainActivity
import io.github.wulkanowy.ui.modules.main.MainView
import io.github.wulkanowy.ui.modules.timetable.completed.CompletedLessonsFragment
import io.github.wulkanowy.ui.widgets.DividerItemDecoration
import io.github.wulkanowy.utils.SchooldaysRangeLimiter
import io.github.wulkanowy.utils.dpToPx
import org.threeten.bp.LocalDate
import javax.inject.Inject

class TimetableFragment : BaseFragment<FragmentTimetableBinding>(R.layout.fragment_timetable),
    TimetableView, MainView.MainChildView, MainView.TitledView {

    @Inject
    lateinit var presenter: TimetablePresenter

    @Inject
    lateinit var timetableAdapter: TimetableAdapter

    companion object {
        private const val SAVED_DATE_KEY = "CURRENT_DATE"

        fun newInstance() = TimetableFragment()
    }

    override val titleStringId get() = R.string.timetable_title

    override val isViewEmpty get() = timetableAdapter.items.isEmpty()

    override val currentStackSize get() = (activity as? MainActivity)?.currentStackSize

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentTimetableBinding.bind(view)
        messageContainer = binding.timetableRecycler
        presenter.onAttachView(this, savedInstanceState?.getLong(SAVED_DATE_KEY))
    }

    override fun initView() {
        timetableAdapter.onClickListener = presenter::onTimetableItemSelected

        with(binding.timetableRecycler) {
            layoutManager = LinearLayoutManager(context)
            adapter = timetableAdapter
            addItemDecoration(DividerItemDecoration(context))
        }

        with(binding) {
            timetableSwipe.setOnRefreshListener(presenter::onSwipeRefresh)
            timetableErrorRetry.setOnClickListener { presenter.onRetry() }
            timetableErrorDetails.setOnClickListener { presenter.onDetailsClick() }

            timetablePreviousButton.setOnClickListener { presenter.onPreviousDay() }
            timetableNavDate.setOnClickListener { presenter.onPickDate() }
            timetableNextButton.setOnClickListener { presenter.onNextDay() }

            timetableNavContainer.setElevationCompat(requireContext().dpToPx(8f))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.action_menu_timetable, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.timetableMenuCompletedLessons) presenter.onCompletedLessonsSwitchSelected()
        else false
    }

    override fun updateData(data: List<Timetable>, showWholeClassPlanType: String, showTimetableTimers: Boolean) {
        with(timetableAdapter) {
            items = data.toMutableList()
            showTimers = showTimetableTimers
            showWholeClassPlan = showWholeClassPlanType
            notifyDataSetChanged()
        }
    }

    override fun clearData() {
        with(timetableAdapter) {
            items = mutableListOf()
            notifyDataSetChanged()
        }
    }

    override fun updateNavigationDay(date: String) {
        binding.timetableNavDate.text = date
    }

    override fun hideRefresh() {
        binding.timetableSwipe.isRefreshing = false
    }

    override fun resetView() {
        binding.timetableRecycler.smoothScrollToPosition(0)
    }

    override fun onFragmentReselected() {
        if (::presenter.isInitialized) presenter.onViewReselected()
    }

    override fun popView() {
        (activity as? MainActivity)?.popView()
    }

    override fun showEmpty(show: Boolean) {
        binding.timetableEmpty.visibility = if (show) VISIBLE else GONE
    }

    override fun showErrorView(show: Boolean) {
        binding.timetableError.visibility = if (show) VISIBLE else GONE
    }

    override fun setErrorDetails(message: String) {
        binding.timetableErrorMessage.text = message
    }

    override fun showProgress(show: Boolean) {
        binding.timetableProgress.visibility = if (show) VISIBLE else GONE
    }

    override fun enableSwipe(enable: Boolean) {
        binding.timetableSwipe.isEnabled = enable
    }

    override fun showContent(show: Boolean) {
        binding.timetableRecycler.visibility = if (show) VISIBLE else GONE
    }

    override fun showPreButton(show: Boolean) {
        binding.timetablePreviousButton.visibility = if (show) VISIBLE else View.INVISIBLE
    }

    override fun showNextButton(show: Boolean) {
        binding.timetableNextButton.visibility = if (show) VISIBLE else View.INVISIBLE
    }

    override fun showTimetableDialog(lesson: Timetable) {
        (activity as? MainActivity)?.showDialogFragment(TimetableDialog.newInstance(lesson))
    }

    override fun showDatePickerDialog(currentDate: LocalDate) {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            presenter.onDateSet(year, month + 1, dayOfMonth)
        }
        val datePickerDialog = DatePickerDialog.newInstance(dateSetListener,
            currentDate.year, currentDate.monthValue - 1, currentDate.dayOfMonth)

        with(datePickerDialog) {
            setDateRangeLimiter(SchooldaysRangeLimiter())
            version = DatePickerDialog.Version.VERSION_2
            scrollOrientation = DatePickerDialog.ScrollOrientation.VERTICAL
            show(this@TimetableFragment.parentFragmentManager, null)
        }
    }

    override fun openCompletedLessonsView() {
        (activity as? MainActivity)?.pushView(CompletedLessonsFragment.newInstance())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(SAVED_DATE_KEY, presenter.currentDate.toEpochDay())
    }

    override fun onDestroyView() {
        presenter.onDetachView()
        super.onDestroyView()
    }
}
