package io.github.wulkanowy.ui.modules.timetable

import android.annotation.SuppressLint
import android.graphics.Paint
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import io.github.wulkanowy.R
import io.github.wulkanowy.data.db.entities.Timetable
import io.github.wulkanowy.utils.getThemeAttrColor
import io.github.wulkanowy.utils.toFormattedString
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.item_timetable.*
import org.threeten.bp.LocalDateTime
import kotlinx.android.synthetic.main.item_timetable_small.*

class TimetableItem(val lesson: Timetable, private val showWholeClassPlan: String, private val previousLessonEnd: LocalDateTime?) :
    AbstractFlexibleItem<TimetableItem.ViewHolder>() {

    override fun getLayoutRes() = when {
        showWholeClassPlan == "small" && !lesson.isStudentPlan -> R.layout.item_timetable_small
        else -> R.layout.item_timetable
    }

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<*>>): ViewHolder {
        return ViewHolder(view, adapter)
    }

    private var lastTimeLeftShownCode: String? = null

    @SuppressLint("SetTextI18n")
    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<*>>, holder: ViewHolder, position: Int, payloads: MutableList<Any>?) {
        when (itemViewType) {
            R.layout.item_timetable_small -> bindSmallView(holder)
            R.layout.item_timetable -> bindNormalView(holder)
        }
    }

    private fun bindSmallView(holder: ViewHolder) {
        with(holder) {
            timetableSmallItemNumber.text = lesson.number.toString()
            timetableSmallItemSubject.text = lesson.subject
            timetableSmallItemTimeStart.text = lesson.start.toFormattedString("HH:mm")
            timetableSmallItemRoom.text = lesson.room
            timetableSmallItemTeacher.text = lesson.teacher

            updateSubjectStyle(timetableSmallItemSubject)
            updateSmallDescription(this)
            updateSmallColors(this)
        }
    }

    private fun bindNormalView(holder: ViewHolder) {
        with(holder) {
            timetableItemNumber.text = lesson.number.toString()
            timetableItemSubject.text = lesson.subject
            timetableItemRoom.text = lesson.room
            timetableItemTeacher.text = lesson.teacher
            timetableItemTimeStart.text = lesson.start.toFormattedString("HH:mm")
            timetableItemTimeFinish.text = lesson.end.toFormattedString("HH:mm")

            updateSubjectStyle(timetableItemSubject)
            updateNormalDescription(this)
            updateNormalColors(this)
        }
        updateTimeLeft(holder)
    }

    fun getTimeNeedsUpdate(): Boolean {
        return TimetableItemDisplayState(lesson, previousLessonEnd).code != lastTimeLeftShownCode
    }

    private fun updateTimeLeft(holder: ViewHolder) {
        val displayState = TimetableItemDisplayState(lesson, previousLessonEnd)

        lastTimeLeftShownCode = displayState.code

        with(holder) {
            when {
                displayState.justFinished -> {
                    timetableItemTimeUntil.visibility = GONE
                    timetableItemTimeLeft.visibility = VISIBLE
                    timetableItemTimeLeft.text = view.context.getString(R.string.timetable_finished)
                }
                displayState.showTimeUntil -> {
                    timetableItemTimeUntil.visibility = VISIBLE
                    timetableItemTimeLeft.visibility = GONE
                    timetableItemTimeUntil.text = view.context.getString(
                        R.string.timetable_time_until,
                        if (displayState.timeUntil.seconds <= 60) {
                            view.context.getString(R.string.timetable_seconds, displayState.timeUntil.seconds.toString(10))
                        } else {
                            view.context.getString(R.string.timetable_minutes, displayState.timeUntil.toMinutes().toString(10))
                        }
                    )
                }
                displayState.timeLeft == null -> {
                    timetableItemTimeUntil.visibility = GONE
                    timetableItemTimeLeft.visibility = GONE
                }
                else -> {
                    timetableItemTimeUntil.visibility = GONE
                    timetableItemTimeLeft.visibility = VISIBLE
                    timetableItemTimeLeft.text = view.context.getString(
                        R.string.timetable_time_left,
                        if (displayState.timeLeft.seconds <= 60) {
                            view.context.getString(R.string.timetable_seconds, displayState.timeLeft.seconds.toString(10))
                        } else {
                            view.context.getString(R.string.timetable_minutes, displayState.timeLeft.toMinutes().toString(10))
                        }
                    )
                }
            }
        }
    }

    private fun updateSubjectStyle(subjectView: TextView) {
        subjectView.paintFlags = if (lesson.canceled) subjectView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else subjectView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
    }

    private fun updateSmallDescription(holder: ViewHolder) {
        with(holder) {
            if (lesson.info.isNotBlank() && !lesson.changes) {
                timetableSmallItemDescription.visibility = VISIBLE
                timetableSmallItemDescription.text = lesson.info

                timetableSmallItemRoom.visibility = GONE
                timetableSmallItemTeacher.visibility = GONE

                timetableSmallItemDescription.setTextColor(holder.view.context.getThemeAttrColor(
                    if (lesson.canceled) R.attr.colorPrimary
                    else R.attr.colorTimetableChange
                ))
            } else {
                timetableSmallItemDescription.visibility = GONE
                timetableSmallItemRoom.visibility = VISIBLE
                timetableSmallItemTeacher.visibility = VISIBLE
            }
        }
    }

    private fun updateNormalDescription(holder: ViewHolder) {
        with(holder) {
            if (lesson.info.isNotBlank() && !lesson.changes) {
                timetableItemDescription.visibility = VISIBLE
                timetableItemDescription.text = lesson.info

                timetableItemRoom.visibility = GONE
                timetableItemTeacher.visibility = GONE

                timetableItemDescription.setTextColor(holder.view.context.getThemeAttrColor(
                    if (lesson.canceled) R.attr.colorPrimary
                    else R.attr.colorTimetableChange
                ))
            } else {
                timetableItemDescription.visibility = GONE
                timetableItemRoom.visibility = VISIBLE
                timetableItemTeacher.visibility = VISIBLE
            }
        }
    }

    private fun updateSmallColors(holder: ViewHolder) {
        with(holder) {
            if (lesson.canceled) {
                updateNumberAndSubjectCanceledColor(timetableSmallItemNumber, timetableSmallItemSubject)
            } else {
                updateNumberColor(timetableSmallItemNumber)
                updateSubjectColor(timetableSmallItemSubject)
                updateRoomColor(timetableSmallItemRoom)
                updateTeacherColor(timetableSmallItemTeacher)
            }
        }
    }

    private fun updateNormalColors(holder: ViewHolder) {
        with(holder) {
            if (lesson.canceled) {
                updateNumberAndSubjectCanceledColor(timetableItemNumber, timetableItemSubject)
            } else {
                updateNumberColor(timetableItemNumber)
                updateSubjectColor(timetableItemSubject)
                updateRoomColor(timetableItemRoom)
                updateTeacherColor(timetableItemTeacher)
            }
        }
    }

    private fun updateNumberAndSubjectCanceledColor(numberView: TextView, subjectView: TextView) {
        numberView.setTextColor(numberView.context.getThemeAttrColor(R.attr.colorPrimary))
        subjectView.setTextColor(subjectView.context.getThemeAttrColor(R.attr.colorPrimary))
    }

    private fun updateNumberColor(numberView: TextView) {
        numberView.setTextColor(numberView.context.getThemeAttrColor(
            if (lesson.changes || lesson.info.isNotBlank()) R.attr.colorTimetableChange
            else android.R.attr.textColorPrimary
        ))
    }

    private fun updateSubjectColor(subjectView: TextView) {
        subjectView.setTextColor(subjectView.context.getThemeAttrColor(
            if (lesson.subjectOld.isNotBlank() && lesson.subjectOld != lesson.subject) R.attr.colorTimetableChange
            else android.R.attr.textColorPrimary
        ))
    }

    private fun updateRoomColor(roomView: TextView) {
        roomView.setTextColor(roomView.context.getThemeAttrColor(
            if (lesson.roomOld.isNotBlank() && lesson.roomOld != lesson.room) R.attr.colorTimetableChange
            else android.R.attr.textColorSecondary
        ))
    }

    private fun updateTeacherColor(teacherTextView: TextView) {
        teacherTextView.setTextColor(teacherTextView.context.getThemeAttrColor(
            if (lesson.teacherOld.isNotBlank() && lesson.teacherOld != lesson.teacher) R.attr.colorTimetableChange
            else android.R.attr.textColorSecondary
        ))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TimetableItem

        if (lesson != other.lesson) return false
        return true
    }

    override fun hashCode(): Int {
        var result = lesson.hashCode()
        result = 31 * result + lesson.id.toInt()
        return result
    }

    class ViewHolder(val view: View, adapter: FlexibleAdapter<*>) :
        FlexibleViewHolder(view, adapter), LayoutContainer {
        override val containerView: View
            get() = contentView
    }
}
