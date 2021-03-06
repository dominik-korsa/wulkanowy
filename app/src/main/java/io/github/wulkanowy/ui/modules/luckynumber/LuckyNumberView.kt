package io.github.wulkanowy.ui.modules.luckynumber

import io.github.wulkanowy.data.db.entities.LuckyNumber
import io.github.wulkanowy.ui.base.BaseView

interface LuckyNumberView : BaseView {

    fun initView()

    fun updateData(data: LuckyNumber)

    fun hideRefresh()

    fun showEmpty(show: Boolean)

    fun showProgress(show: Boolean)

    fun enableSwipe(enable: Boolean)

    fun showContent(show: Boolean)

    fun isViewEmpty(): Boolean
}
