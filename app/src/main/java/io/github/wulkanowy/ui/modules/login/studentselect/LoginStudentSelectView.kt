package io.github.wulkanowy.ui.modules.login.studentselect

import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.ui.base.BaseView

interface LoginStudentSelectView : BaseView {

    fun initView()

    fun updateData(data: List<Pair<Student, Boolean>>)

    fun openMainView()

    fun showProgress(show: Boolean)

    fun showContent(show: Boolean)

    fun enableSignIn(enable: Boolean)

    fun showContact(show: Boolean)

    fun openDiscordInvite()

    fun openEmail(lastError: String)
}
