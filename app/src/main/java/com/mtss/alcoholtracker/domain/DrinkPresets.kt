package com.mtss.alcoholtracker.domain

data class Preset(val name: String, val abv: Double, val ml: Double, val cost: Double)

/** The POPULAR grid on the log sheet — a convenience layer, never a limit. */
object DrinkPresets {
    val ALL = listOf(
        Preset("Margarita", 13.0, 150.0, 14.0),
        Preset("Mojito", 10.0, 200.0, 13.0),
        Preset("Old Fashioned", 27.0, 90.0, 16.0),
        Preset("Cosmopolitan", 20.0, 120.0, 14.0),
        Preset("Gin & Tonic", 11.0, 210.0, 12.0),
        Preset("Whiskey Sour", 16.0, 150.0, 14.0),
        Preset("Moscow Mule", 11.0, 240.0, 13.0),
        Preset("Piña Colada", 13.0, 180.0, 15.0),
        Preset("Espresso Martini", 23.0, 110.0, 16.0),
        Preset("Aperol Spritz", 11.0, 210.0, 13.0)
    )

    // The onboarding goal and baseline labels used to live here as English
    // literals. They are resources now (ob_goal_*, ob_base_*) and are read by
    // index from OnboardingScreens, so the index a user's answer is stored under
    // is unchanged while the words follow the locale.
}
