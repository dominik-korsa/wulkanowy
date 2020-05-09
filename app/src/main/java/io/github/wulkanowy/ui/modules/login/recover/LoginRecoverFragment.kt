package io.github.wulkanowy.ui.modules.login.recover

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.widget.doOnTextChanged
import io.github.wulkanowy.R
import io.github.wulkanowy.ui.base.BaseFragment
import io.github.wulkanowy.ui.modules.login.LoginActivity
import io.github.wulkanowy.ui.modules.login.form.LoginSymbolAdapter
import io.github.wulkanowy.utils.hideSoftInput
import io.github.wulkanowy.utils.showSoftInput
import kotlinx.android.synthetic.main.fragment_login_recover.*
import javax.inject.Inject

class LoginRecoverFragment : BaseFragment(), LoginRecoverView {

    @Inject
    lateinit var presenter: LoginRecoverPresenter

    companion object {
        fun newInstance() = LoginRecoverFragment()
    }

    private lateinit var hostKeys: Array<String>

    private lateinit var hostValues: Array<String>

    private lateinit var hostSymbols: Array<String>

    override val recoverHostValue: String
        get() = hostValues.getOrNull(hostKeys.indexOf(loginRecoverHost.text.toString())).orEmpty()

    override val formHostSymbol: String
        get() = hostSymbols.getOrNull(hostKeys.indexOf(loginRecoverHost.text.toString())).orEmpty()

    override val recoverNameValue: String
        get() = loginRecoverName.text.toString().trim()

    override val emailHintString: String
        get() = getString(R.string.login_email_hint)

    override val loginPeselEmailHintString: String
        get() = getString(R.string.login_login_pesel_email_hint)

    override val invalidEmailString: String
        get() = getString(R.string.login_invalid_email)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login_recover, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        presenter.onAttachView(this)
    }

    override fun initView() {
        loginRecoverWebView.setBackgroundColor(Color.TRANSPARENT)
        hostKeys = resources.getStringArray(R.array.hosts_keys)
        hostValues = resources.getStringArray(R.array.hosts_values)
        hostSymbols = resources.getStringArray(R.array.hosts_symbols)

        loginRecoverName.doOnTextChanged { _, _, _, _ -> presenter.onNameTextChanged() }
        loginRecoverHost.setOnItemClickListener { _, _, _, _ -> presenter.onHostSelected() }
        loginRecoverButton.setOnClickListener { presenter.onRecoverClick() }
        loginRecoverErrorRetry.setOnClickListener { presenter.onRecoverClick() }
        loginRecoverErrorDetails.setOnClickListener { presenter.onDetailsClick() }
        loginRecoverLogin.setOnClickListener { (activity as LoginActivity).switchView(0) }

        with(loginRecoverHost) {
            setText(hostKeys.getOrNull(0).orEmpty())
            setAdapter(LoginSymbolAdapter(context, R.layout.support_simple_spinner_dropdown_item, hostKeys))
            setOnClickListener { if (loginRecoverFormContainer.visibility == GONE) dismissDropDown() }
        }
    }

    override fun setDefaultCredentials(username: String) {
        loginRecoverName.setText(username)
    }

    override fun setErrorNameRequired() {
        with(loginRecoverNameLayout) {
            requestFocus()
            error = getString(R.string.login_field_required)
        }
    }

    override fun setUsernameHint(hint: String) {
        loginRecoverNameLayout.hint = hint
    }

    override fun setUsernameError(message: String) {
        with(loginRecoverNameLayout) {
            requestFocus()
            error = message
        }
    }

    override fun clearUsernameError() {
        loginRecoverNameLayout.error = null
    }

    override fun showProgress(show: Boolean) {
        loginRecoverProgress.visibility = if (show) VISIBLE else GONE
    }

    override fun showRecoverForm(show: Boolean) {
        loginRecoverFormContainer.visibility = if (show) VISIBLE else GONE
    }

    override fun showCaptcha(show: Boolean) {
        loginRecoverCaptchaContainer.visibility = if (show) VISIBLE else GONE
    }

    override fun showErrorView(show: Boolean) {
        loginRecoverError.visibility = if (show) VISIBLE else GONE
    }

    override fun setErrorDetails(message: String) {
        loginRecoverErrorMessage.text = message
    }

    override fun showSuccessView(show: Boolean) {
        loginRecoverSuccess.visibility = if (show) VISIBLE else GONE
    }

    override fun setSuccessTitle(title: String) {
        loginRecoverSuccessTitle.text = title
    }

    override fun setSuccessMessage(message: String) {
        loginRecoverSuccessMessage.text = message
    }

    override fun showSoftKeyboard() {
        activity?.showSoftInput()
    }

    override fun hideSoftKeyboard() {
        activity?.hideSoftInput()
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun loadReCaptcha(siteKey: String, url: String) {
        val html = """
            <div style="position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%);" id="recaptcha"></div>
            <script src="https://www.google.com/recaptcha/api.js?onload=cl&render=explicit&hl=pl" async defer></script>
            <script>var cl=()=>grecaptcha.render("recaptcha",{
            sitekey:'$siteKey',
            callback:e =>Android.captchaCallback(e)})</script>
        """.trimIndent()

        with(loginRecoverWebView) {
            settings.javaScriptEnabled = true
            webViewClient = object : WebViewClient() {
                private var recoverWebViewSuccess: Boolean = true

                override fun onPageFinished(view: WebView?, url: String?) {
                    if (recoverWebViewSuccess) {
                        showCaptcha(true)
                        showProgress(false)
                    } else {
                        showProgress(false)
                        showErrorView(true)
                    }
                }

                override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                    recoverWebViewSuccess = false
                }
            }

            loadDataWithBaseURL(url, html, "text/html", "UTF-8", null)
            addJavascriptInterface(object {
                @JavascriptInterface
                fun captchaCallback(reCaptchaResponse: String) {
                    activity?.runOnUiThread {
                        presenter.onReCaptchaVerified(reCaptchaResponse)
                    }
                }
            }, "Android")
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.updateFields()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loginRecoverWebView.destroy()
        presenter.onDetachView()
    }
}
