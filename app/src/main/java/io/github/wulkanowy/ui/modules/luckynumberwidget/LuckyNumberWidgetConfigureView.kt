package io.github.wulkanowy.ui.modules.luckynumberwidget

import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.ui.base.BaseView

interface LuckyNumberWidgetConfigureView : BaseView {

    fun initView()

    fun showThemeDialog()

    fun updateData(data: List<Pair<Student, Boolean>>)

    fun updateLuckyNumberWidget(widgetId: Int)

    fun setSuccessResult(widgetId: Int)

    fun finishView()

    fun openLoginView()
}
