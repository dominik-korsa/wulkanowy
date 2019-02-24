package io.github.wulkanowy.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "Semesters", indices = [Index(value = ["student_id", "diary_id", "semester_id"], unique = true)])
data class Semester(

    @ColumnInfo(name = "student_id")
    val studentId: Int,

    @ColumnInfo(name = "diary_id")
    val diaryId: Int,

    @ColumnInfo(name = "diary_name")
    val diaryName: String,

    @ColumnInfo(name = "semester_id")
    val semesterId: Int,

    @ColumnInfo(name = "semester_name")
    val semesterName: Int,

    @ColumnInfo(name = "is_current")
    val isCurrent: Boolean,

    @ColumnInfo(name = "class_id")
    val classId: Int,

    @ColumnInfo(name = "unit_id")
    val unitId: Int
) {

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}
