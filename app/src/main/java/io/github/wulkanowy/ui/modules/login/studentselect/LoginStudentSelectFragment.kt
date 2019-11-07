package io.github.wulkanowy.ui.modules.login.studentselect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import io.github.wulkanowy.R
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.ui.base.BaseFragment
import io.github.wulkanowy.ui.modules.main.MainActivity
import io.github.wulkanowy.utils.AppInfo
import io.github.wulkanowy.utils.openEmailClient
import io.github.wulkanowy.utils.openInternetBrowser
import io.github.wulkanowy.utils.setOnItemClickListener
import kotlinx.android.synthetic.main.fragment_login_student_select.*
import java.io.Serializable
import javax.inject.Inject

class LoginStudentSelectFragment : BaseFragment(), LoginStudentSelectView {

    @Inject
    lateinit var presenter: LoginStudentSelectPresenter

    @Inject
    lateinit var loginAdapter: FlexibleAdapter<AbstractFlexibleItem<*>>

    @Inject
    lateinit var appInfo: AppInfo

    companion object {
        const val SAVED_STUDENTS = "STUDENTS"

        fun newInstance() = LoginStudentSelectFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login_student_select, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        presenter.onAttachView(this, savedInstanceState?.getSerializable(SAVED_STUDENTS))
    }

    override fun initView() {
        loginStudentSelectSignIn.setOnClickListener { presenter.onSignIn() }
        loginAdapter.apply { setOnItemClickListener { presenter.onItemSelected(it) } }
        loginStudentSelectContactDiscord.setOnClickListener { presenter.onDiscordClick() }
        loginStudentSelectContactEmail.setOnClickListener { presenter.onEmailClick() }

        loginStudentSelectRecycler.apply {
            adapter = loginAdapter
            layoutManager = SmoothScrollLinearLayoutManager(context)
        }
    }

    override fun updateData(data: List<LoginStudentSelectItem>) {
        loginAdapter.updateDataSet(data)
    }

    override fun openMainView() {
        activity?.let { startActivity(MainActivity.getStartIntent(context = it, clear = true)) }
    }

    override fun showProgress(show: Boolean) {
        loginStudentSelectProgress.visibility = if (show) VISIBLE else GONE
    }

    override fun showContent(show: Boolean) {
        loginStudentSelectContent.visibility = if (show) VISIBLE else GONE
    }

    override fun enableSignIn(enable: Boolean) {
        loginStudentSelectSignIn.isEnabled = enable
    }

    fun onParentInitStudentSelectFragment(students: List<Student>) {
        presenter.onParentInitStudentSelectView(students)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(SAVED_STUDENTS, presenter.students as Serializable)
    }

    override fun showContact(show: Boolean) {
        loginStudentSelectContact.visibility = if (show) VISIBLE else GONE
    }

    override fun onDestroyView() {
        presenter.onDetachView()
        super.onDestroyView()
    }

    override fun openDiscordInvite() {
        context?.openInternetBrowser("https://discord.gg/vccAQBr", ::showMessage)
    }

    override fun openEmail() {
        context?.openEmailClient(
            requireContext().getString(R.string.login_email_intent_title),
            "wulkanowyinc@gmail.com",
            requireContext().getString(R.string.login_email_subject),
            requireContext().getString(R.string.login_email_text, appInfo.systemModel, appInfo.systemVersion.toString(), appInfo.versionName)
        )
    }
}
