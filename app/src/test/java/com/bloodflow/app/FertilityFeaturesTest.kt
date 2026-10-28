package com.bloodflow.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FertilityFeaturesTest {

    @Test
    fun testAssessFertilityNoPeriods() {
        val model = Model()
        val result = assessFertility(model, LocalDate.of(2025, 1, 15))
        assertEquals(FertilityCategory.UNCERTAIN, result.category)
        assertEquals("Not enough data", result.title)
    }

    @Test
    fun testAssessFertilityHigherCategory() {
        val model = Model(
            periods = listOf("2025-01-01", "2024-12-04"),
            length = 28,
            days = 5
        )
        // Day 10 of cycle (Jan 1 + 9 days = Jan 10) should fall in days 8..19
        val result = assessFertility(model, LocalDate.of(2025, 1, 10))
        assertEquals(FertilityCategory.HIGHER, result.category)
        assertEquals(10, result.cycleDay)
    }

    @Test
    fun testAssessFertilityOutsideEstimateCategory() {
        val model = Model(
            periods = listOf("2025-01-01", "2024-12-04"),
            length = 28,
            days = 5
        )
        // Day 21 of cycle (Jan 1 + 20 days = Jan 21) is outside days 8..19
        val result = assessFertility(model, LocalDate.of(2025, 1, 21))
        assertEquals(FertilityCategory.OUTSIDE_ESTIMATE, result.category)
        assertEquals(21, result.cycleDay)
    }
}
