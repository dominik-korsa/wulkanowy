package io.github.wulkanowy.ui.modules.grade

import io.github.wulkanowy.data.db.entities.Grade
import io.github.wulkanowy.data.db.entities.GradeSummary
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.data.repositories.createSemesterEntity
import io.github.wulkanowy.data.repositories.grade.GradeRepository
import io.github.wulkanowy.data.repositories.preferences.PreferencesRepository
import io.github.wulkanowy.createSemesterEntity
import io.github.wulkanowy.sdk.Sdk
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.threeten.bp.LocalDate.now
import org.threeten.bp.LocalDate.of
import org.threeten.bp.LocalDateTime

class GradeAverageProviderTest {

    @Mock
    lateinit var preferencesRepository: PreferencesRepository

    @Mock
    lateinit var semesterRepository: SemesterRepository

    @Mock
    lateinit var gradeRepository: GradeRepository

    private lateinit var gradeAverageProvider: GradeAverageProvider

    private val student = Student("", "", "", "SCRAPPER", "", "", false, "", "", "", 101, 0, "", "", "", "", "", 1, true, LocalDateTime.now())

    private val semesters = mutableListOf(
        createSemesterEntity(10, 21, of(2019, 1, 31), of(2019, 6, 23)),
        createSemesterEntity(11, 22, of(2019, 9, 1), of(2020, 1, 31)),
        createSemesterEntity(11, 23, of(2020, 2, 1), now(), semesterName = 2)
    )

    private val firstGrades = listOf(
        // avg: 3.5
        getGrade(22, "Matematyka", 4.0),
        getGrade(22, "Matematyka", 3.0),

        // avg: 3.5
        getGrade(22, "Fizyka", 6.0),
        getGrade(22, "Fizyka", 1.0)
    )

    private val firstSummaries = listOf(
        getSummary(semesterId = 22, subject = "Matematyka", average = 3.9),
        getSummary(semesterId = 22, subject = "Fizyka", average = 3.1)
    )

    private val secondGrades = listOf(
        // avg: 2.5
        getGrade(23, "Matematyka", 2.0),
        getGrade(23, "Matematyka", 3.0),

        // avg: 3.0
        getGrade(23, "Fizyka", 4.0),
        getGrade(23, "Fizyka", 2.0)
    )

    private val secondSummaries = listOf(
        getSummary(semesterId = 23, subject = "Matematyka", average = 2.9),
        getSummary(semesterId = 23, subject = "Fizyka", average = 3.4)
    )

    private val secondGradeWithModifier = listOf(
        // avg: 3.375
        getGrade(24, "Język polski", 3.0, -0.50),
        getGrade(24, "Język polski", 4.0, 0.25)
    )

    private val secondSummariesWithModifier = listOf(
        getSummary(24, "Język polski", 3.49)
    )

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        `when`(preferencesRepository.gradeMinusModifier).thenReturn(.33)
        `when`(preferencesRepository.gradePlusModifier).thenReturn(.33)
        `when`(preferencesRepository.gradeAverageForceCalc).thenReturn(false)
        `when`(semesterRepository.getSemesters(student)).thenReturn(Single.just(semesters))

        gradeAverageProvider = GradeAverageProvider(semesterRepository, gradeRepository, preferencesRepository)
    }

    @Test
    fun onlyOneSemesterTest() {
        `when`(preferencesRepository.gradeAverageForceCalc).thenReturn(true)
        `when`(preferencesRepository.gradeAverageMode).thenReturn("only_one_semester")
        `when`(gradeRepository.getGrades(student, semesters[2])).thenReturn(Single.just(secondGrades to secondSummaries))

        val items = gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId).blockingGet()

        assertEquals(2, items.size)
        assertEquals(2.5, items.single { it.subject == "Matematyka" }.average, .0)
        assertEquals(3.0, items.single { it.subject == "Fizyka" }.average, .0)
    }

    @Test
    fun onlyOneSemester_gradesWithModifiers_default() {
        `when`(preferencesRepository.gradeAverageForceCalc).thenReturn(true)
        `when`(preferencesRepository.gradeAverageMode).thenReturn("only_one_semester")
        `when`(gradeRepository.getGrades(student, semesters[2])).thenReturn(Single.just(secondGradeWithModifier to secondSummariesWithModifier))

        val items = gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId).blockingGet()

        assertEquals(3.5, items.single { it.subject == "Język polski" }.average, .0)
    }

    @Test
    fun onlyOneSemester_gradesWithModifiers_api() {
        val student = student.copy(loginMode = Sdk.Mode.API.name)

        `when`(preferencesRepository.gradeAverageForceCalc).thenReturn(true)
        `when`(preferencesRepository.gradeAverageMode).thenReturn("only_one_semester")
        `when`(semesterRepository.getSemesters(student)).thenReturn(Single.just(semesters))
        `when`(gradeRepository.getGrades(student, semesters[2])).thenReturn(Single.just(secondGradeWithModifier to secondSummariesWithModifier))

        val items = gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId).blockingGet()

        assertEquals(3.375, items.single { it.subject == "Język polski" }.average, .0)
    }

    @Test
    fun onlyOneSemester_gradesWithModifiers_scrapper() {
        val student = student.copy(loginMode = Sdk.Mode.SCRAPPER.name)

        `when`(preferencesRepository.gradeAverageForceCalc).thenReturn(true)
        `when`(preferencesRepository.gradeAverageMode).thenReturn("only_one_semester")
        `when`(semesterRepository.getSemesters(student)).thenReturn(Single.just(semesters))
        `when`(gradeRepository.getGrades(student, semesters[2])).thenReturn(Single.just(secondGradeWithModifier to secondSummariesWithModifier))

        val items = gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId).blockingGet()

        assertEquals(3.5, items.single { it.subject == "Język polski" }.average, .0)
    }

    @Test
    fun onlyOneSemester_gradesWithModifiers_hybrid() {
        val student = student.copy(loginMode = Sdk.Mode.HYBRID.name)

        `when`(preferencesRepository.gradeAverageForceCalc).thenReturn(true)
        `when`(preferencesRepository.gradeAverageMode).thenReturn("only_one_semester")
        `when`(semesterRepository.getSemesters(student)).thenReturn(Single.just(semesters))
        `when`(gradeRepository.getGrades(student, semesters[2])).thenReturn(Single.just(secondGradeWithModifier to secondSummariesWithModifier))

        val items = gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId).blockingGet()

        assertEquals(3.375, items.single { it.subject == "Język polski" }.average, .0)
    }

    @Test
    fun allYearFirstSemesterTest() {
        `when`(preferencesRepository.gradeAverageForceCalc).thenReturn(true)
        `when`(preferencesRepository.gradeAverageMode).thenReturn("all_year")
        `when`(gradeRepository.getGrades(student, semesters[1])).thenReturn(Single.just(firstGrades to firstSummaries))

        val items = gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[1].semesterId).blockingGet()

        assertEquals(2, items.size)
        assertEquals(3.5, items.single { it.subject == "Matematyka" }.average, .0)
        assertEquals(3.5, items.single { it.subject == "Fizyka" }.average, .0)
    }

    @Test
    fun allYearSecondSemesterTest() {
        `when`(preferencesRepository.gradeAverageForceCalc).thenReturn(true)
        `when`(preferencesRepository.gradeAverageMode).thenReturn("all_year")
        `when`(gradeRepository.getGrades(student, semesters[1])).thenReturn(Single.just(firstGrades to firstSummaries))
        `when`(gradeRepository.getGrades(student, semesters[2])).thenReturn(Single.just(secondGrades to secondSummaries))

        val items = gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId).blockingGet()

        assertEquals(2, items.size)
        assertEquals(3.0, items.single { it.subject == "Matematyka" }.average, .0)
        assertEquals(3.25, items.single { it.subject == "Fizyka" }.average, .0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun incorrectAverageModeTest() {
        `when`(preferencesRepository.gradeAverageMode).thenReturn("test_mode")

        gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId, true).blockingGet()
    }

    @Test
    fun allYearSemester_averageFromSummary() {
        `when`(preferencesRepository.gradeAverageMode).thenReturn("all_year")
        `when`(preferencesRepository.gradeAverageForceCalc).thenReturn(false)
        `when`(gradeRepository.getGrades(student, semesters[1])).thenReturn(Single.just(firstGrades to listOf(
            getSummary(22, "Matematyka", 3.0),
            getSummary(22, "Fizyka", 3.5)
        )))
        `when`(gradeRepository.getGrades(student, semesters[2])).thenReturn(Single.just(secondGrades to listOf(
            getSummary(22, "Matematyka", 3.5),
            getSummary(22, "Fizyka", 4.0)
        )))

        val items = gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId).blockingGet()

        assertEquals(2, items.size)
        assertEquals(3.25, items.single { it.subject == "Matematyka" }.average, .0)
        assertEquals(3.75, items.single { it.subject == "Fizyka" }.average, .0)
    }

    @Test
    fun onlyOneSemester_averageFromSummary_forceCalc() {
        `when`(preferencesRepository.gradeAverageMode).thenReturn("all_year")
        `when`(preferencesRepository.gradeAverageForceCalc).thenReturn(true)
        `when`(gradeRepository.getGrades(student, semesters[1])).thenReturn(Single.just(firstGrades to firstSummaries))
        `when`(gradeRepository.getGrades(student, semesters[2])).thenReturn(Single.just(secondGrades to listOf(
            getSummary(22, "Matematyka", 1.1),
            getSummary(22, "Fizyka", 7.26)
        )))

        val items = gradeAverageProvider.getGradesDetailsWithAverage(student, semesters[2].semesterId).blockingGet()

        assertEquals(2, items.size)
        assertEquals(3.0, items.single { it.subject == "Matematyka" }.average, .0)
        assertEquals(3.25, items.single { it.subject == "Fizyka" }.average, .0)
    }

    private fun getGrade(semesterId: Int, subject: String, value: Double, modifier: Double = 0.0): Grade {
        return Grade(
            studentId = 101,
            semesterId = semesterId,
            subject = subject,
            value = value,
            modifier = modifier,
            weightValue = 1.0,
            teacher = "",
            date = now(),
            weight = "",
            gradeSymbol = "",
            entry = "",
            description = "",
            comment = "",
            color = ""
        )
    }

    private fun getSummary(semesterId: Int, subject: String, average: Double): GradeSummary {
        return GradeSummary(
            studentId = 101,
            semesterId = semesterId,
            subject = subject,
            average = average,
            pointsSum = "",
            proposedPoints = "",
            finalPoints = "",
            finalGrade = "",
            predictedGrade = "",
            position = 0
        )
    }
}
