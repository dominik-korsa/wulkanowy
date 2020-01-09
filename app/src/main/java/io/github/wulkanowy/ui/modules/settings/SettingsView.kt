package io.github.wulkanowy.ui.modules.settings

import androidx.lifecycle.LifecycleOwner
import io.github.wulkanowy.ui.base.BaseView

interface SettingsView : BaseView {

    val lifecycleOwner: LifecycleOwner

    fun initView()

    fun recreateView()

    fun updateLanguage(langCode: String)

    fun setServicesSuspended(serviceEnablesKey: String, isHolidays: Boolean)

    fun showSyncSuccess()

    fun showSyncFailed(error: Throwable)

    fun setSyncInProgress(inProgress: Boolean)
}
