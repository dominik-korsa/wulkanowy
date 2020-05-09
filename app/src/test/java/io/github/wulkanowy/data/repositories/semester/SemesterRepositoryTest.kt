package io.github.wulkanowy.data.repositories.semester

import com.github.pwittchen.reactivenetwork.library.rx2.internet.observing.InternetObservingSettings
import io.github.wulkanowy.data.db.entities.Semester
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.data.repositories.UnitTestInternetObservingStrategy
import io.github.wulkanowy.data.repositories.createSemesterEntity
import io.reactivex.Maybe
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.threeten.bp.LocalDate.now

class SemesterRepositoryTest {

    @Mock
    private lateinit var semesterRemote: SemesterRemote

    @Mock
    private lateinit var semesterLocal: SemesterLocal

    @Mock
    private lateinit var student: Student

    private lateinit var semesterRepository: SemesterRepository

    private val settings = InternetObservingSettings.builder()
        .strategy(UnitTestInternetObservingStrategy())
        .build()

    @Before
    fun initTest() {
        MockitoAnnotations.initMocks(this)
        semesterRepository = SemesterRepository(semesterRemote, semesterLocal, settings)
        doReturn("SCRAPPER").`when`(student).loginMode
    }

    @Test
    fun getSemesters_noSemesters() {
        val semesters = listOf(
            createSemesterEntity(1, 1, now().minusMonths(6), now().minusMonths(3)),
            createSemesterEntity(1, 2, now().minusMonths(3), now())
        )

        doReturn(Maybe.empty<Semester>()).`when`(semesterLocal).getSemesters(student)
        doReturn(Single.just(semesters)).`when`(semesterRemote).getSemesters(student)

        semesterRepository.getSemesters(student).blockingGet()

        verify(semesterLocal).deleteSemesters(emptyList())
        verify(semesterLocal).saveSemesters(semesters)
    }

    @Test
    fun getSemesters_invalidDiary_api() {
        doReturn("API").`when`(student).loginMode
        val badSemesters = listOf(
            createSemesterEntity(0, 1, now().minusMonths(6), now().minusMonths(3)),
            createSemesterEntity(0, 2, now().minusMonths(3), now())
        )

        doReturn(Maybe.just(badSemesters)).`when`(semesterLocal).getSemesters(student)

        val items = semesterRepository.getSemesters(student).blockingGet()
        assertEquals(2, items.size)
        assertEquals(0, items[0].diaryId)
    }

    @Test
    fun getSemesters_invalidDiary_scrapper() {
        doReturn("SCRAPPER").`when`(student).loginMode
        val badSemesters = listOf(
            createSemesterEntity(0, 1, now().minusMonths(6), now().minusMonths(3)),
            createSemesterEntity(0, 2, now().minusMonths(3), now())
        )

        val goodSemesters = listOf(
            createSemesterEntity(1, 1, now().minusMonths(6), now().minusMonths(3)),
            createSemesterEntity(1, 2, now().minusMonths(3), now())
        )

        doReturn(Maybe.just(badSemesters), Maybe.just(badSemesters), Maybe.just(goodSemesters)).`when`(semesterLocal).getSemesters(student)
        doReturn(Single.just(goodSemesters)).`when`(semesterRemote).getSemesters(student)

        val items = semesterRepository.getSemesters(student).blockingGet()
        assertEquals(2, items.size)
        assertNotEquals(0, items[0].diaryId)
    }

    @Test
    fun getSemesters_noCurrent() {
        val semesters = listOf(
            createSemesterEntity(1, 1, now().minusMonths(12), now().minusMonths(6)),
            createSemesterEntity(1, 2, now().minusMonths(6), now().minusMonths(1))
        )

        doReturn(Maybe.just(semesters)).`when`(semesterLocal).getSemesters(student)

        val items = semesterRepository.getSemesters(student).blockingGet()
        assertEquals(2, items.size)
    }

    @Test
    fun getSemesters_oneCurrent() {
        val semesters = listOf(
            createSemesterEntity(1, 1, now().minusMonths(6), now().minusMonths(3)),
            createSemesterEntity(1, 2, now().minusMonths(3), now())
        )

        doReturn(Maybe.just(semesters)).`when`(semesterLocal).getSemesters(student)

        val items = semesterRepository.getSemesters(student).blockingGet()
        assertEquals(2, items.size)
    }

    @Test
    fun getSemesters_doubleCurrent() {
        val semesters = listOf(
            createSemesterEntity(1, 1, now(), now()),
            createSemesterEntity(1, 2, now(), now())
        )

        doReturn(Maybe.just(semesters)).`when`(semesterLocal).getSemesters(student)

        val items = semesterRepository.getSemesters(student).blockingGet()
        assertEquals(2, items.size)
    }

    @Test
    fun getSemesters_noSemesters_refreshOnNoCurrent() {
        val semesters = listOf(
            createSemesterEntity(1, 1, now().minusMonths(6), now().minusMonths(3)),
            createSemesterEntity(1, 2, now().minusMonths(3), now())
        )

        doReturn(Maybe.empty<Semester>()).`when`(semesterLocal).getSemesters(student)
        doReturn(Single.just(semesters)).`when`(semesterRemote).getSemesters(student)

        semesterRepository.getSemesters(student, refreshOnNoCurrent = true).blockingGet()

        verify(semesterLocal).deleteSemesters(emptyList())
        verify(semesterLocal).saveSemesters(semesters)
    }

    @Test
    fun getSemesters_noCurrent_refreshOnNoCurrent() {
        val semesters = listOf(
            createSemesterEntity(1, 1, now().minusMonths(12), now().minusMonths(6)),
            createSemesterEntity(1, 2, now().minusMonths(6), now().minusMonths(1))
        )

        doReturn(Maybe.just(semesters)).`when`(semesterLocal).getSemesters(student)
        doReturn(Single.just(semesters)).`when`(semesterRemote).getSemesters(student)

        val items = semesterRepository.getSemesters(student, refreshOnNoCurrent = true).blockingGet()
        assertEquals(2, items.size)
    }

    @Test
    fun getSemesters_doubleCurrent_refreshOnNoCurrent() {
        val semesters = listOf(
            createSemesterEntity(1, 1, now(), now()),
            createSemesterEntity(1, 2, now(), now())
        )

        doReturn(Maybe.just(semesters)).`when`(semesterLocal).getSemesters(student)

        val items = semesterRepository.getSemesters(student, refreshOnNoCurrent = true).blockingGet()
        assertEquals(2, items.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun getCurrentSemester_doubleCurrent() {
        val semesters = listOf(
            createSemesterEntity(1, 1, now(), now()),
            createSemesterEntity(1, 1, now(), now())
        )

        doReturn(Maybe.just(semesters)).`when`(semesterLocal).getSemesters(student)

        semesterRepository.getCurrentSemester(student).blockingGet()
    }

    @Test(expected = RuntimeException::class)
    fun getCurrentSemester_emptyList() {
        doReturn(Maybe.just(emptyList<Semester>())).`when`(semesterLocal).getSemesters(student)

        semesterRepository.getCurrentSemester(student).blockingGet()
    }
}
