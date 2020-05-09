package io.github.wulkanowy.utils

import io.github.wulkanowy.getTimetableEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.threeten.bp.LocalDateTime.now

class TimetableExtensionTest {

    @Test
    fun isShowTimeUntil() {
        assertFalse(getTimetableEntity().isShowTimeUntil(null))
        assertFalse(getTimetableEntity(isStudentPlan = false).isShowTimeUntil(null))
        assertFalse(getTimetableEntity(isStudentPlan = true, canceled = true).isShowTimeUntil(null))
        assertFalse(getTimetableEntity(isStudentPlan = true, canceled = false, start = now().minusSeconds(1)).isShowTimeUntil(null))
        assertFalse(getTimetableEntity(isStudentPlan = true, canceled = false, start = now().plusMinutes(5)).isShowTimeUntil(now().plusMinutes(5)))
        assertFalse(getTimetableEntity(isStudentPlan = true, canceled = false, start = now().plusMinutes(61)).isShowTimeUntil(now().minusMinutes(5)))

        assertTrue(getTimetableEntity(isStudentPlan = true, canceled = false, start = now().plusMinutes(60)).isShowTimeUntil(now().minusMinutes(5)))
        assertTrue(getTimetableEntity(isStudentPlan = true, canceled = false, start = now().plusMinutes(60)).isShowTimeUntil(null))

        assertFalse(getTimetableEntity(isStudentPlan = true, canceled = false, start = now().minusSeconds(1)).isShowTimeUntil(null))
    }

    @Test
    fun getLeft() {
        assertEquals(null, getTimetableEntity(canceled = true).left)
        assertEquals(null, getTimetableEntity(start = now().plusMinutes(5), end = now().plusMinutes(50)).left)
        assertNotEquals(null, getTimetableEntity(start = now().minusMinutes(1), end = now().plusMinutes(44)).left)
    }

    @Test
    fun isJustFinished() {
        assertFalse(getTimetableEntity(end = now().minusSeconds(16)).isJustFinished)
        assertTrue(getTimetableEntity(end = now().minusSeconds(14)).isJustFinished)
        assertTrue(getTimetableEntity(end = now().minusSeconds(1)).isJustFinished)
        assertFalse(getTimetableEntity(end = now().plusSeconds(1)).isJustFinished)
    }
}
