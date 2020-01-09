package io.github.wulkanowy.ui.modules.settings

import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.work.WorkInfo
import com.readystatesoftware.chuck.api.ChuckCollector
import io.github.wulkanowy.data.repositories.preferences.PreferencesRepository
import io.github.wulkanowy.data.repositories.student.StudentRepository
import io.github.wulkanowy.services.sync.SyncManager
import io.github.wulkanowy.services.sync.works.Work
import io.github.wulkanowy.ui.base.BasePresenter
import io.github.wulkanowy.ui.base.ErrorHandler
import io.github.wulkanowy.utils.AppInfo
import io.github.wulkanowy.utils.FirebaseAnalyticsHelper
import io.github.wulkanowy.utils.SchedulersProvider
import io.github.wulkanowy.utils.isHolidays
import org.threeten.bp.LocalDate.now
import timber.log.Timber
import javax.inject.Inject

class SettingsPresenter @Inject constructor(
    schedulers: SchedulersProvider,
    errorHandler: ErrorHandler,
    studentRepository: StudentRepository,
    private val preferencesRepository: PreferencesRepository,
    private val analytics: FirebaseAnalyticsHelper,
    private val syncManager: SyncManager,
    private val chuckCollector: ChuckCollector,
    private val appInfo: AppInfo
) : BasePresenter<SettingsView>(errorHandler, studentRepository, schedulers) {

    override fun onAttachView(view: SettingsView) {
        super.onAttachView(view)
        Timber.i("Settings view was initialized")
        view.setServicesSuspended(preferencesRepository.serviceEnableKey, now().isHolidays)
        view.initView()
    }

    fun onSharedPreferenceChanged(key: String) {
        Timber.i("Change settings $key")

        with(preferencesRepository) {
            when (key) {
                serviceEnableKey -> with(syncManager) { if (isServiceEnabled) startPeriodicSyncWorker() else stopSyncWorker() }
                servicesIntervalKey, servicesOnlyWifiKey -> syncManager.startPeriodicSyncWorker(true)
                isDebugNotificationEnableKey -> chuckCollector.showNotification(isDebugNotificationEnable)
                appThemeKey -> view?.recreateView()
                appLanguageKey -> view?.run {
                    updateLanguage(if (appLanguage == "system") appInfo.systemLanguage else appLanguage)
                    recreateView()
                }
                else -> Unit
            }
        }
        analytics.logEvent("setting_changed", "name" to key)
    }

    fun onSyncNowClicked() {
        Timber.i("Setting sync now clicked")
        analytics.logEvent("sync_now_clicked")
        with(view) {
            if (this == null) return
            setSyncInProgress(true)
            syncManager.startOneTimeSyncWorker().observe(this.lifecycleOwner, Observer { workInfo ->
                if (workInfo != null) {
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        showSyncSuccess()
                        setSyncInProgress(false)
                    } else if (workInfo.state == WorkInfo.State.FAILED) {
                        showSyncFailed(Throwable(workInfo.outputData.getString("error")))
                        setSyncInProgress(false)
                    } else if (workInfo.state == WorkInfo.State.CANCELLED) {
                        setSyncInProgress(false)
                    }
                }
            })
        }
    }
}
