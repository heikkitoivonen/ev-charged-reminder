package com.evchargedreminder.data.bundled

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BundledEvDataTest {

    @Test
    fun `all vehicles have positive battery capacity`() {
        BundledEvData.vehicles.forEach { vehicle ->
            assertTrue(
                "${vehicle.make} ${vehicle.model} has non-positive capacity: ${vehicle.batteryCapacityKwh}",
                vehicle.batteryCapacityKwh > 0
            )
        }
    }

    @Test
    fun `all vehicles have non-blank make and model`() {
        BundledEvData.vehicles.forEach { vehicle ->
            assertTrue("Vehicle has blank make", vehicle.make.isNotBlank())
            assertTrue("Vehicle has blank model", vehicle.model.isNotBlank())
        }
    }

    @Test
    fun `all vehicles have valid year`() {
        BundledEvData.vehicles.forEach { vehicle ->
            assertTrue(
                "${vehicle.make} ${vehicle.model} has invalid year: ${vehicle.year}",
                vehicle.year >= 2015
            )
        }
    }

    @Test
    fun `hybrid vehicles are marked as hybrid`() {
        val expectedHybrids = listOf("Prius Prime", "Volt", "i3 REx", "Tucson PHEV", "Wrangler 4xe")
        expectedHybrids.forEach { model ->
            val found = BundledEvData.vehicles.find { it.model == model }
            assertTrue("$model should be in bundled data", found != null)
            assertTrue("$model should be marked as hybrid", found!!.isHybrid)
        }
    }

    @Test
    fun `non-hybrid vehicles are not marked as hybrid`() {
        val nonHybrids = BundledEvData.vehicles.filter { !it.isHybrid }
        assertTrue("Should have non-hybrid vehicles", nonHybrids.isNotEmpty())
        nonHybrids.forEach { vehicle ->
            assertTrue(
                "${vehicle.make} ${vehicle.model} should not be hybrid",
                !vehicle.isHybrid
            )
        }
    }

    @Test
    fun `findByMakeAndModel returns correct results`() {
        val teslas = BundledEvData.findByMakeAndModel("Tesla", "Model 3")
        assertEquals(2, teslas.size)
        assertTrue(teslas.all { it.make == "Tesla" && it.model == "Model 3" })
    }

    @Test
    fun `findByMakeAndModel is case insensitive`() {
        val teslas = BundledEvData.findByMakeAndModel("tesla", "model 3")
        assertEquals(2, teslas.size)
    }

    @Test
    fun `findByMakeAndModel returns empty for unknown vehicle`() {
        val result = BundledEvData.findByMakeAndModel("Unknown", "Car")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findByMake returns all vehicles for a make`() {
        val hyundais = BundledEvData.findByMake("Hyundai")
        assertEquals(3, hyundais.size)
    }

    @Test
    fun `getAllMakes returns distinct sorted makes`() {
        val makes = BundledEvData.getAllMakes()
        assertTrue(makes.isNotEmpty())
        assertEquals(makes.sorted(), makes)
        assertEquals(makes.distinct(), makes)
    }

    @Test
    fun `getModelsForMake returns models for Tesla`() {
        val models = BundledEvData.getModelsForMake("Tesla")
        assertTrue(models.contains("Model 3"))
        assertTrue(models.contains("Model Y"))
        assertTrue(models.contains("Model S"))
    }

    @Test
    fun `vehicle count matches expected`() {
        assertEquals(25, BundledEvData.vehicles.size)
    }
}
