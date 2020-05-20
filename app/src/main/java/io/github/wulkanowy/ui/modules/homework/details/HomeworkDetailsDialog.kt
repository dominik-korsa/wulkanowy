package io.github.wulkanowy.ui.modules.homework.details

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.wulkanowy.R
import io.github.wulkanowy.data.db.entities.Homework
import io.github.wulkanowy.databinding.DialogHomeworkBinding
import io.github.wulkanowy.ui.base.BaseDialogFragment
import io.github.wulkanowy.ui.modules.homework.HomeworkFragment
import io.github.wulkanowy.utils.openInternetBrowser
import javax.inject.Inject

class HomeworkDetailsDialog : BaseDialogFragment<DialogHomeworkBinding>(), HomeworkDetailsView {

    @Inject
    lateinit var presenter: HomeworkDetailsPresenter

    @Inject
    lateinit var detailsAdapter: HomeworkDetailsAdapter

    private lateinit var homework: Homework

    companion object {
        private const val ARGUMENT_KEY = "Item"

        fun newInstance(homework: Homework): HomeworkDetailsDialog {
            return HomeworkDetailsDialog().apply {
                arguments = Bundle().apply { putSerializable(ARGUMENT_KEY, homework) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        arguments?.run {
            homework = getSerializable(ARGUMENT_KEY) as Homework
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return DialogHomeworkBinding.inflate(inflater).apply { binding = this }.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        presenter.onAttachView(this)
    }

    @SuppressLint("SetTextI18n")
    override fun initView() {
        with(binding) {
            homeworkDialogRead.text = view?.context?.getString(if (homework.isDone) R.string.homework_mark_as_undone else R.string.homework_mark_as_done)
            homeworkDialogRead.setOnClickListener { presenter.toggleDone(homework) }
            homeworkDialogClose.setOnClickListener { dismiss() }
        }

        with(binding.homeworkDialogRecycler) {
            layoutManager = LinearLayoutManager(context)
            adapter = detailsAdapter.apply {
                onAttachmentClickListener = { context.openInternetBrowser(it, ::showMessage) }
                onFullScreenClickListener = { dialog?.window?.setLayout(MATCH_PARENT, MATCH_PARENT) }
                onFullScreenExitClickListener = { dialog?.window?.setLayout(WRAP_CONTENT, WRAP_CONTENT) }
                homework = this@HomeworkDetailsDialog.homework
            }
        }
    }

    override fun updateMarkAsDoneLabel(isDone: Boolean) {
        (parentFragment as? HomeworkFragment)?.onReloadList()
        binding.homeworkDialogRead.text = view?.context?.getString(if (isDone) R.string.homework_mark_as_undone else R.string.homework_mark_as_done)
    }

    override fun onDestroyView() {
        presenter.onDetachView()
        super.onDestroyView()
    }
}
