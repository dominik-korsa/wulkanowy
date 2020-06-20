package io.github.wulkanowy.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import io.github.wulkanowy.data.db.entities.LuckyNumber
import org.threeten.bp.LocalDate
import javax.inject.Singleton

@Singleton
@Dao
interface LuckyNumberDao : BaseDao<LuckyNumber> {

    @Query("SELECT * FROM LuckyNumbers WHERE student_id = :studentId AND date = :date")
    suspend fun load(studentId: Int, date: LocalDate): LuckyNumber
}
