package io.github.wulkanowy.ui.modules.settings

import androidx.work.WorkInfo
import com.readystatesoftware.chuck.api.ChuckCollector
import io.github.wulkanowy.data.repositories.preferences.PreferencesRepository
import io.github.wulkanowy.data.repositories.student.StudentRepository
import io.github.wulkanowy.services.sync.SyncManager
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
        view?.showForceSyncDialog()
    }

    fun onForceSyncDialogSubmit() {
        view?.run {
            Timber.i("Setting sync now started")
            analytics.logEvent("sync_now_started")
            setSyncInProgress(true)
            disposable.add(syncManager.startOneTimeSyncWorker()
                .subscribe({ workInfo ->
                    when (workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            showSyncSuccess()
                            setSyncInProgress(false)
                        }
                        WorkInfo.State.FAILED -> {
                            showSyncFailed(Throwable(workInfo.outputData.getString("error")))
                            setSyncInProgress(false)
                        }
                        WorkInfo.State.CANCELLED -> {
                            setSyncInProgress(false)
                        }
                        else -> {
                        }
                    }
                }, {
                    Timber.e("Force sync failed")
                    Timber.e(it)
                })
            )
        }
    }
}
