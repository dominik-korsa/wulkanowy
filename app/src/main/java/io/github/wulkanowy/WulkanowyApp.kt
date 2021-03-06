package io.github.wulkanowy

import android.content.Context
import android.util.Log.INFO
import android.util.Log.VERBOSE
import androidx.multidex.MultiDex
import androidx.work.Configuration
import androidx.work.WorkManager
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.utils.Log
import io.github.wulkanowy.di.DaggerAppComponent
import io.github.wulkanowy.services.sync.SyncWorkerFactory
import io.github.wulkanowy.utils.ActivityLifecycleLogger
import io.github.wulkanowy.utils.AppInfo
import io.github.wulkanowy.utils.CrashlyticsTree
import io.github.wulkanowy.utils.DebugLogTree
import io.github.wulkanowy.utils.initCrashlytics
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class WulkanowyApp : DaggerApplication() {

    @Inject
    lateinit var workerFactory: SyncWorkerFactory

    @Inject
    lateinit var appInfo: AppInfo

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
        RxJavaPlugins.setErrorHandler(::onError)

        initWorkManager()
        initLogging()
        initCrashlytics(this, appInfo)
    }

    private fun initWorkManager() {
        WorkManager.initialize(this,
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setMinimumLoggingLevel(if (appInfo.isDebug) VERBOSE else INFO)
                .build())
    }

    private fun initLogging() {
        if (appInfo.isDebug) {
            Timber.plant(DebugLogTree())
            FlexibleAdapter.enableLogs(Log.Level.DEBUG)
        } else {
            Timber.plant(CrashlyticsTree())
        }
        registerActivityLifecycleCallbacks(ActivityLifecycleLogger())
    }

    private fun onError(error: Throwable) {
        //RxJava's too deep stack traces may cause SOE on older android devices
        val cause = error.cause
        if (error is UndeliverableException && cause is IOException || cause is InterruptedException || cause is StackOverflowError) {
            Timber.e(cause, "An undeliverable error occurred")
        } else throw error
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponent.factory().create(this)
    }
}
