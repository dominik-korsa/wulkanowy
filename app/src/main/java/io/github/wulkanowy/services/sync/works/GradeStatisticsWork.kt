package io.github.wulkanowy.services.sync.works

import io.github.wulkanowy.data.db.entities.Semester
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.data.repositories.gradestatistics.GradeStatisticsRepository
import io.reactivex.Completable
import kotlinx.coroutines.rx2.rxCompletable
import javax.inject.Inject

class GradeStatisticsWork @Inject constructor(private val gradeStatisticsRepository: GradeStatisticsRepository) : Work {

    override fun create(student: Student, semester: Semester): Completable {
        return rxCompletable { gradeStatisticsRepository.getGradesStatistics(student, semester, "Wszystkie", false, forceRefresh = true) }
    }
}

