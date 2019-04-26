package io.github.wulkanowy.ui.modules.login

import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.ui.base.BasePresenter
import io.github.wulkanowy.ui.base.ErrorHandler
import timber.log.Timber
import javax.inject.Inject

class LoginPresenter @Inject constructor(errorHandler: ErrorHandler) : BasePresenter<LoginView>(errorHandler) {

    override fun onAttachView(view: LoginView) {
        super.onAttachView(view)
        view.run {
            initAdapter()
            showActionBar(false)
        }
        Timber.i("Login view is attached")
    }

    fun onFormViewAccountLogged(students: List<Student>, loginData: Triple<String, String, String>) {
        view?.apply {
            if (students.isEmpty()) {
                Timber.i("Switch to symbol form")
                notifyInitSymbolFragment(loginData)
                switchView(1)
            } else {
                Timber.i("Switch to student select")
                notifyInitStudentSelectFragment(students)
                switchView(2)
            }
        }
    }

    fun onSymbolViewAccountLogged(students: List<Student>) {
        view?.apply {
            Timber.i("Switch to student select")
            notifyInitStudentSelectFragment(students)
            switchView(2)
        }
    }

    fun onViewSelected(index: Int) {
        view?.apply {
            when (index) {
                0, 1 -> showActionBar(false)
                2 -> showActionBar(true)
            }
        }
    }

    fun onBackPressed(default: () -> Unit) {
        Timber.i("Back pressed in login view")
        view?.apply {
            when (currentViewIndex) {
                1, 2 -> switchView(0)
                else -> default()
            }
        }
    }
}
