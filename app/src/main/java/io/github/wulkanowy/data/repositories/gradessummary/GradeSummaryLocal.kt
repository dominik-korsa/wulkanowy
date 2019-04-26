package io.github.wulkanowy.data.repositories.gradessummary

import io.github.wulkanowy.data.db.dao.GradeSummaryDao
import io.github.wulkanowy.data.db.entities.GradeSummary
import io.github.wulkanowy.data.db.entities.Semester
import io.reactivex.Maybe
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GradeSummaryLocal @Inject constructor(private val gradeSummaryDb: GradeSummaryDao) {

    fun saveGradesSummary(gradesSummary: List<GradeSummary>) {
        gradeSummaryDb.insertAll(gradesSummary)
    }

    fun deleteGradesSummary(gradesSummary: List<GradeSummary>) {
        gradeSummaryDb.deleteAll(gradesSummary)
    }

    fun getGradesSummary(semester: Semester): Maybe<List<GradeSummary>> {
        return gradeSummaryDb.loadAll(semester.semesterId, semester.studentId).filter { it.isNotEmpty() }
    }
}
