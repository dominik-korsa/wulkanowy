package io.github.wulkanowy.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.wulkanowy.data.db.entities.Message
import io.reactivex.Maybe

@Dao
interface MessagesDao {

    @Insert
    fun insertAll(messages: List<Message>)

    @Delete
    fun deleteAll(messages: List<Message>)

    @Update
    fun updateAll(messages: List<Message>)

    @Query("SELECT * FROM Messages WHERE student_id = :studentId AND folder_id = :folder AND removed = 0 ORDER BY date DESC")
    fun loadAll(studentId: Int, folder: Int): Maybe<List<Message>>

    @Query("SELECT * FROM Messages WHERE id = :id")
    fun load(id: Long): Maybe<Message>

    @Query("SELECT * FROM Messages WHERE student_id = :studentId AND removed = 1 ORDER BY date DESC")
    fun loadDeleted(studentId: Int): Maybe<List<Message>>
}
