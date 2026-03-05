package com.evchargedreminder.domain.model

enum class ChargerType(
    val label: String,
    val powerKw: Double,
    val isAc: Boolean
) {
    STANDARD_HOUSEHOLD_OUTLET("Standard Household Outlet (15A)", 1.4, true),
    OUTLET_120V_20A("120V / 20A Outlet", 1.9, true),
    OUTLET_240V_20A("240V / 20A Outlet", 3.8, true),
    OUTLET_240V_30A("240V / 30A Outlet", 5.8, true),
    OUTLET_240V_50A("240V / 50A Outlet", 9.6, true),
    LEVEL2_EVSE_32A("Level 2 EVSE — J1772 (32A)", 7.7, true),
    LEVEL2_EVSE_48A("Level 2 EVSE — J1772 (48A)", 11.5, true),
    LEVEL2_EVSE_80A("Level 2 EVSE — J1772 (80A)", 19.2, true),
    DC_FAST_50KW("DC Fast — CCS1 (50 kW)", 50.0, false),
    DC_FAST_150KW("DC Fast — CCS1 (150 kW)", 150.0, false),
    DC_FAST_350KW("DC Fast — CCS1 (350 kW)", 350.0, false),
    NACS_LEVEL2("NACS Level 2 (48A)", 11.5, true),
    NACS_SUPERCHARGER_250KW("NACS Supercharger (250 kW)", 250.0, false),
    CUSTOM_AC("Custom (AC)", 0.0, true),
    CUSTOM_DC("Custom (DC)", 0.0, false);
}
