package io.github.wulkanowy.ui.modules.login

import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import io.github.wulkanowy.di.scopes.PerActivity
import io.github.wulkanowy.di.scopes.PerFragment
import io.github.wulkanowy.ui.base.BaseFragmentPagerAdapter
import io.github.wulkanowy.ui.modules.login.form.LoginFormFragment
import io.github.wulkanowy.ui.modules.login.studentselect.LoginStudentSelectFragment
import io.github.wulkanowy.ui.modules.login.symbol.LoginSymbolFragment

@Module
internal abstract class LoginModule {

    @Module
    companion object {

        @JvmStatic
        @PerActivity
        @Provides
        fun provideLoginAdapter(activity: LoginActivity) = BaseFragmentPagerAdapter(activity.supportFragmentManager)
    }

    @PerFragment
    @ContributesAndroidInjector
    abstract fun bindLoginFormFragment(): LoginFormFragment

    @PerFragment
    @ContributesAndroidInjector
    abstract fun bindLoginSymbolFragment(): LoginSymbolFragment

    @PerFragment
    @ContributesAndroidInjector
    abstract fun bindLoginSelectStudentFragment(): LoginStudentSelectFragment
}
