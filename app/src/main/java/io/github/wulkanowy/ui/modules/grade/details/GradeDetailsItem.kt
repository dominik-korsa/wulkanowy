package io.github.wulkanowy.ui.modules.grade.details

import android.annotation.SuppressLint
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import io.github.wulkanowy.R
import io.github.wulkanowy.data.db.entities.Grade
import io.github.wulkanowy.utils.toFormattedString
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_grade_details.*

class GradeDetailsItem(
    val grade: Grade,
    private val valueBgColor: Int,
    private val weightString: String,
    private val noDescriptionString: String
) : AbstractFlexibleItem<GradeDetailsItem.ViewHolder>() {

    override fun getLayoutRes() = R.layout.item_grade_details

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<*>>): ViewHolder {
        return ViewHolder(view, adapter)
    }

    @SuppressLint("SetTextI18n")
    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<*>>, holder: ViewHolder, position: Int, payloads: MutableList<Any>?) {
        holder.run {
            gradeItemValue.run {
                text = grade.entry
                setBackgroundResource(valueBgColor)
            }
            gradeItemDescription.text = when {
                grade.description.isNotBlank() -> grade.description
                grade.gradeSymbol.isNotBlank() -> grade.gradeSymbol
                else -> noDescriptionString
            }
            gradeItemDate.text = grade.date.toFormattedString()
            gradeItemWeight.text = "$weightString: ${grade.weight}"
            gradeItemNote.visibility = if (!grade.isRead) VISIBLE else GONE
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GradeDetailsItem

        if (grade != other.grade) return false
        if (grade.id != other.grade.id) return false
        if (weightString != other.weightString) return false
        if (valueBgColor != other.valueBgColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = grade.hashCode()
        result = 31 * result + grade.id.toInt()
        result = 31 * result + weightString.hashCode()
        result = 31 * result + valueBgColor
        return result
    }

    class ViewHolder(view: View, adapter: FlexibleAdapter<*>) : FlexibleViewHolder(view, adapter), LayoutContainer {
        override val containerView: View
            get() = contentView
    }
}
