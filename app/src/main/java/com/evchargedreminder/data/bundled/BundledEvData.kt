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
        // Tesla
        BundledVehicle(2024, "Tesla", "Model 3", "Standard Range", false, 57.5),
        BundledVehicle(2024, "Tesla", "Model 3", "Long Range", false, 75.0),
        BundledVehicle(2024, "Tesla", "Model Y", "Long Range", false, 75.0),
        BundledVehicle(2024, "Tesla", "Model S", null, false, 100.0),
        BundledVehicle(2024, "Tesla", "Model X", null, false, 100.0),
        // Chevrolet
        BundledVehicle(2023, "Chevrolet", "Bolt EV", null, false, 65.0),
        BundledVehicle(2024, "Chevrolet", "Equinox EV", null, false, 85.0),
        BundledVehicle(2024, "Chevrolet", "Blazer EV", null, false, 102.0),
        // Ford
        BundledVehicle(2024, "Ford", "Mustang Mach-E", "Standard", false, 72.0),
        BundledVehicle(2024, "Ford", "Mustang Mach-E", "Extended Range", false, 91.0),
        BundledVehicle(2024, "Ford", "F-150 Lightning", null, false, 98.0),
        // Hyundai
        BundledVehicle(2024, "Hyundai", "Ioniq 5", "Standard Range", false, 58.0),
        BundledVehicle(2024, "Hyundai", "Ioniq 5", "Long Range", false, 77.4),
        BundledVehicle(2024, "Hyundai", "Ioniq 6", "Long Range", false, 77.4),
        // Kia
        BundledVehicle(2024, "Kia", "EV6", "Standard Range", false, 58.0),
        BundledVehicle(2024, "Kia", "EV6", "Long Range", false, 77.4),
        BundledVehicle(2024, "Kia", "EV9", "Long Range", false, 99.8),
        BundledVehicle(2024, "Kia", "Niro EV", null, false, 64.8),
        // Nissan
        BundledVehicle(2024, "Nissan", "Leaf", null, false, 40.0),
        BundledVehicle(2024, "Nissan", "Ariya", null, false, 87.0),
        // Rivian
        BundledVehicle(2024, "Rivian", "R1T", "Large Pack", false, 135.0),
        BundledVehicle(2024, "Rivian", "R1S", "Large Pack", false, 135.0),
        // BMW
        BundledVehicle(2024, "BMW", "iX", "xDrive50", false, 76.6),
        BundledVehicle(2024, "BMW", "i4", "eDrive40", false, 83.9),
        // Mercedes
        BundledVehicle(2024, "Mercedes", "EQS", "450+", false, 108.4),
        BundledVehicle(2024, "Mercedes", "EQB", null, false, 66.5),
        // Volkswagen
        BundledVehicle(2024, "Volkswagen", "ID.4", "Pro S", false, 82.0),
        BundledVehicle(2024, "Volkswagen", "ID.Buzz", null, false, 82.0),
        // Audi
        BundledVehicle(2024, "Audi", "Q8 e-tron", null, false, 106.0),
        BundledVehicle(2024, "Audi", "Q4 e-tron", null, false, 77.0),
        // Porsche
        BundledVehicle(2024, "Porsche", "Taycan", "Performance Battery Plus", false, 93.4),
        // Polestar
        BundledVehicle(2024, "Polestar", "2", "Long Range", false, 78.0),
        // Volvo
        BundledVehicle(2024, "Volvo", "XC40 Recharge", null, false, 78.0),
        BundledVehicle(2024, "Volvo", "C40 Recharge", null, false, 78.0),
        // Cadillac
        BundledVehicle(2024, "Cadillac", "Lyriq", null, false, 102.0),
        // Genesis
        BundledVehicle(2024, "Genesis", "GV60", null, false, 77.4),
        BundledVehicle(2024, "Genesis", "Electrified GV70", null, false, 77.4),
        // Honda / Acura
        BundledVehicle(2024, "Honda", "Prologue", null, false, 85.0),
        BundledVehicle(2024, "Acura", "ZDX", null, false, 102.0),
        // Toyota / Lexus / Subaru
        BundledVehicle(2024, "Toyota", "bZ4X", null, false, 71.4),
        BundledVehicle(2024, "Subaru", "Solterra", null, false, 71.4),
        BundledVehicle(2024, "Lexus", "RZ 450e", null, false, 71.4),
        // Lucid
        BundledVehicle(2024, "Lucid", "Air", "Grand Touring", false, 112.0),
        // Mini
        BundledVehicle(2024, "Mini", "Cooper SE", null, false, 28.6),
        // PHEVs
        BundledVehicle(2024, "Toyota", "Prius Prime", null, true, 13.6),
        BundledVehicle(2024, "Toyota", "RAV4 Prime", null, true, 18.1),
        BundledVehicle(2019, "Chevrolet", "Volt", null, true, 18.4),
        BundledVehicle(2021, "BMW", "i3 REx", null, true, 42.2),
        BundledVehicle(2024, "Hyundai", "Tucson PHEV", null, true, 13.8),
        BundledVehicle(2024, "Jeep", "Wrangler 4xe", null, true, 17.3),
        BundledVehicle(2024, "Chrysler", "Pacifica Hybrid", null, true, 16.0),
        BundledVehicle(2024, "Mitsubishi", "Outlander PHEV", null, true, 20.0),
        BundledVehicle(2024, "Kia", "Sportage PHEV", null, true, 13.8),
        BundledVehicle(2024, "Ford", "Escape PHEV", null, true, 14.4)
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

    fun getTrimsForMakeAndModel(make: String, model: String): List<String> =
        findByMakeAndModel(make, model)
            .mapNotNull { it.trim }
            .distinct()
            .sorted()
}
