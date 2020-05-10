package io.github.wulkanowy.ui.modules.schoolandteachers

import android.os.Bundle
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import io.github.wulkanowy.R
import io.github.wulkanowy.databinding.FragmentSchoolandteachersBinding
import io.github.wulkanowy.ui.base.BaseFragment
import io.github.wulkanowy.ui.base.BaseFragmentPagerAdapter
import io.github.wulkanowy.ui.modules.main.MainView
import io.github.wulkanowy.ui.modules.schoolandteachers.school.SchoolFragment
import io.github.wulkanowy.ui.modules.schoolandteachers.teacher.TeacherFragment
import io.github.wulkanowy.utils.dpToPx
import io.github.wulkanowy.utils.setOnSelectPageListener
import javax.inject.Inject

class SchoolAndTeachersFragment :
    BaseFragment<FragmentSchoolandteachersBinding>(R.layout.fragment_schoolandteachers),
    SchoolAndTeachersView, MainView.TitledView {

    @Inject
    lateinit var presenter: SchoolAndTeachersPresenter

    @Inject
    lateinit var pagerAdapter: BaseFragmentPagerAdapter

    companion object {
        fun newInstance() = SchoolAndTeachersFragment()
    }

    override val titleStringId: Int get() = R.string.schoolandteachers_title

    override val currentPageIndex get() = binding.schoolandteachersViewPager.currentItem

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSchoolandteachersBinding.bind(view)
        presenter.onAttachView(this)
    }

    override fun initView() {
        with(pagerAdapter) {
            containerId = binding.schoolandteachersViewPager.id
            addFragmentsWithTitle(mapOf(
                SchoolFragment.newInstance() to getString(R.string.school_title),
                TeacherFragment.newInstance() to getString(R.string.teachers_title)
            ))
        }

        with(binding.schoolandteachersViewPager) {
            adapter = pagerAdapter
            offscreenPageLimit = 2
            setOnSelectPageListener(presenter::onPageSelected)
        }

        with(binding.schoolandteachersTabLayout) {
            setupWithViewPager(binding.schoolandteachersViewPager)
            setElevationCompat(context.dpToPx(4f))
        }
    }

    override fun showContent(show: Boolean) {
        with(binding) {
            schoolandteachersViewPager.visibility = if (show) VISIBLE else INVISIBLE
            schoolandteachersTabLayout.visibility = if (show) VISIBLE else INVISIBLE
        }
    }

    override fun showProgress(show: Boolean) {
        binding.schoolandteachersProgress.visibility = if (show) VISIBLE else INVISIBLE
    }

    fun onChildFragmentLoaded() {
        presenter.onChildViewLoaded()
    }

    override fun notifyChildLoadData(index: Int, forceRefresh: Boolean) {
        (pagerAdapter.getFragmentInstance(index) as? SchoolAndTeachersChildView)?.onParentLoadData(forceRefresh)
    }

    override fun onDestroyView() {
        presenter.onDetachView()
        super.onDestroyView()
    }
}
