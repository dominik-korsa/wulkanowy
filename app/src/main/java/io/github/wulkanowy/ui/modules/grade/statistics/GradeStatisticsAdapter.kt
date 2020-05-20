package io.github.wulkanowy.ui.modules.grade.statistics

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import io.github.wulkanowy.R
import io.github.wulkanowy.data.db.entities.GradePointsStatistics
import io.github.wulkanowy.data.db.entities.GradeStatistics
import io.github.wulkanowy.data.pojos.GradeStatisticsItem
import io.github.wulkanowy.databinding.ItemGradeStatisticsBarBinding
import io.github.wulkanowy.databinding.ItemGradeStatisticsPieBinding
import io.github.wulkanowy.utils.getThemeAttrColor
import javax.inject.Inject

class GradeStatisticsAdapter @Inject constructor() :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var items = emptyList<GradeStatisticsItem>()

    var theme: String = "vulcan"

    var showAllSubjectsOnList: Boolean = false

    private val vulcanGradeColors = listOf(
        6 to R.color.grade_vulcan_six,
        5 to R.color.grade_vulcan_five,
        4 to R.color.grade_vulcan_four,
        3 to R.color.grade_vulcan_three,
        2 to R.color.grade_vulcan_two,
        1 to R.color.grade_vulcan_one
    )

    private val materialGradeColors = listOf(
        6 to R.color.grade_material_six,
        5 to R.color.grade_material_five,
        4 to R.color.grade_material_four,
        3 to R.color.grade_material_three,
        2 to R.color.grade_material_two,
        1 to R.color.grade_material_one
    )

    private val gradePointsColors = listOf(
        Color.parseColor("#37c69c"),
        Color.parseColor("#d8b12a")
    )

    private val gradeLabels = listOf(
        "6, 6-", "5, 5-, 5+", "4, 4-, 4+", "3, 3-, 3+", "2, 2-, 2+", "1, 1+"
    )

    override fun getItemCount() = if (showAllSubjectsOnList) items.size else (if (items.isEmpty()) 0 else 1)

    override fun getItemViewType(position: Int) = items[position].type.id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ViewType.PARTIAL.id, ViewType.SEMESTER.id -> PieViewHolder(ItemGradeStatisticsPieBinding.inflate(inflater, parent, false))
            ViewType.POINTS.id -> BarViewHolder(ItemGradeStatisticsBarBinding.inflate(inflater, parent, false))
            else -> throw IllegalStateException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PieViewHolder -> bindPieChart(holder, items[position].partial)
            is BarViewHolder -> bindBarChart(holder, items[position].points!!)
        }
    }

    private fun bindPieChart(holder: PieViewHolder, partials: List<GradeStatistics>) {
        with(holder.binding.gradeStatisticsPieTitle) {
            text = partials.firstOrNull()?.subject
            visibility = if (items.size == 1 || !showAllSubjectsOnList) GONE else VISIBLE
        }

        val gradeColors = when (theme) {
            "vulcan" -> vulcanGradeColors
            else -> materialGradeColors
        }

        val dataset = PieDataSet(partials.map {
            PieEntry(it.amount.toFloat(), it.grade.toString())
        }, "Legenda")

        with(dataset) {
            valueTextSize = 12f
            sliceSpace = 1f
            valueTextColor = Color.WHITE
            setColors(partials.map {
                gradeColors.single { color -> color.first == it.grade }.second
            }.toIntArray(), holder.binding.root.context)
        }

        with(holder.binding.gradeStatisticsPie) {
            setTouchEnabled(false)
            if (partials.size == 1) animateXY(1000, 1000)
            data = PieData(dataset).apply {
                setValueFormatter(object : ValueFormatter() {
                    override fun getPieLabel(value: Float, pieEntry: PieEntry): String {
                        return resources.getQuantityString(R.plurals.grade_number_item, value.toInt(), value.toInt())
                    }
                })
            }
            with(legend) {
                textColor = context.getThemeAttrColor(android.R.attr.textColorPrimary)
                setCustom(gradeLabels.mapIndexed { i, it ->
                    LegendEntry().apply {
                        label = it
                        formColor = ContextCompat.getColor(context, gradeColors[i].second)
                        form = Legend.LegendForm.SQUARE
                    }
                })
            }

            minAngleForSlices = 25f
            description.isEnabled = false
            centerText = partials.fold(0) { acc, it -> acc + it.amount }
                .let { resources.getQuantityString(R.plurals.grade_number_item, it, it) }

            setHoleColor(context.getThemeAttrColor(android.R.attr.windowBackground))
            setCenterTextColor(context.getThemeAttrColor(android.R.attr.textColorPrimary))
            invalidate()
        }
    }

    private fun bindBarChart(holder: BarViewHolder, points: GradePointsStatistics) {
        with(holder.binding.gradeStatisticsBarTitle) {
            text = points.subject
            visibility = if (items.size == 1) GONE else VISIBLE
        }

        val dataset = BarDataSet(listOf(
            BarEntry(1f, points.others.toFloat()),
            BarEntry(2f, points.student.toFloat())
        ), "Legenda")

        with(dataset) {
            valueTextSize = 12f
            valueTextColor = holder.binding.root.context.getThemeAttrColor(android.R.attr.textColorPrimary)
            valueFormatter = object : ValueFormatter() {
                override fun getBarLabel(barEntry: BarEntry) = "${barEntry.y}%"
            }
            colors = gradePointsColors
        }

        with(holder.binding.gradeStatisticsBar) {
            setTouchEnabled(false)
            if (items.size == 1) animateXY(1000, 1000)
            data = BarData(dataset).apply {
                barWidth = 0.5f
                setFitBars(true)
            }
            legend.setCustom(listOf(
                LegendEntry().apply {
                    label = "Średnia klasy"
                    formColor = gradePointsColors[0]
                    form = Legend.LegendForm.SQUARE
                },
                LegendEntry().apply {
                    label = "Uczeń"
                    formColor = gradePointsColors[1]
                    form = Legend.LegendForm.SQUARE
                }
            ))
            legend.textColor = context.getThemeAttrColor(android.R.attr.textColorPrimary)

            description.isEnabled = false

            holder.binding.root.context.getThemeAttrColor(android.R.attr.textColorPrimary).let {
                axisLeft.textColor = it
                axisRight.textColor = it
            }
            xAxis.setDrawLabels(false)
            xAxis.setDrawGridLines(false)
            with(axisLeft) {
                axisMinimum = 0f
                axisMaximum = 100f
                labelCount = 11
            }
            with(axisRight) {
                axisMinimum = 0f
                axisMaximum = 100f
                labelCount = 11
            }
            invalidate()
        }
    }

    private class PieViewHolder(val binding: ItemGradeStatisticsPieBinding) :
        RecyclerView.ViewHolder(binding.root)

    private class BarViewHolder(val binding: ItemGradeStatisticsBarBinding) :
        RecyclerView.ViewHolder(binding.root)
}
