package io.github.wulkanowy.data.db.dao

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update

interface BaseDao<T> {

    @Insert
    suspend fun insertAll(items: List<T>): List<Long>

    @Update
    suspend fun updateAll(items: List<T>)

    @Delete
    suspend fun deleteAll(items: List<T>)
}
