package com.evchargedreminder.data.bundled

data class BundledVehicle(
    val year: Int,
    val make: String,
    val model: String,
    val trim: String? = null,
    val isHybrid: Boolean = false,
    val batteryCapacityKwh: Double
)

object BundledEvData {

    val vehicles: List<BundledVehicle> = listOf(
        BundledVehicle(2024, "Tesla", "Model 3", "Standard Range", false, 57.5),
        BundledVehicle(2024, "Tesla", "Model 3", "Long Range", false, 75.0),
        BundledVehicle(2024, "Tesla", "Model Y", "Long Range", false, 75.0),
        BundledVehicle(2024, "Tesla", "Model S", null, false, 100.0),
        BundledVehicle(2023, "Chevrolet", "Bolt EV", null, false, 65.0),
        BundledVehicle(2024, "Chevrolet", "Equinox EV", null, false, 85.0),
        BundledVehicle(2024, "Ford", "Mustang Mach-E", "Standard", false, 72.0),
        BundledVehicle(2024, "Ford", "F-150 Lightning", null, false, 98.0),
        BundledVehicle(2024, "Hyundai", "Ioniq 5", "Long Range", false, 77.4),
        BundledVehicle(2024, "Hyundai", "Ioniq 6", "Long Range", false, 77.4),
        BundledVehicle(2024, "Kia", "EV6", "Long Range", false, 77.4),
        BundledVehicle(2024, "Nissan", "Leaf", null, false, 40.0),
        BundledVehicle(2024, "Nissan", "Ariya", null, false, 87.0),
        BundledVehicle(2024, "Rivian", "R1T", "Large Pack", false, 135.0),
        BundledVehicle(2024, "Rivian", "R1S", "Large Pack", false, 135.0),
        BundledVehicle(2024, "BMW", "iX", "xDrive50", false, 76.6),
        BundledVehicle(2024, "Mercedes", "EQS", "450+", false, 108.4),
        BundledVehicle(2024, "Volkswagen", "ID.4", "Pro S", false, 82.0),
        BundledVehicle(2024, "Polestar", "2", "Long Range", false, 78.0),
        BundledVehicle(2024, "Toyota", "bZ4X", null, false, 71.4),
        BundledVehicle(2024, "Toyota", "Prius Prime", null, true, 13.6),
        BundledVehicle(2019, "Chevrolet", "Volt", null, true, 18.4),
        BundledVehicle(2021, "BMW", "i3 REx", null, true, 42.2),
        BundledVehicle(2024, "Hyundai", "Tucson PHEV", null, true, 13.8),
        BundledVehicle(2024, "Jeep", "Wrangler 4xe", null, true, 17.3)
    )

    fun findByMakeAndModel(make: String, model: String): List<BundledVehicle> =
        vehicles.filter {
            it.make.equals(make, ignoreCase = true) &&
                it.model.equals(model, ignoreCase = true)
        }

    fun findByMake(make: String): List<BundledVehicle> =
        vehicles.filter { it.make.equals(make, ignoreCase = true) }

    fun getAllMakes(): List<String> =
        vehicles.map { it.make }.distinct().sorted()

    fun getModelsForMake(make: String): List<String> =
        vehicles.filter { it.make.equals(make, ignoreCase = true) }
            .map { it.model }
            .distinct()
            .sorted()
}
