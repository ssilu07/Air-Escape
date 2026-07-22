package com.royals.airescape.data

import com.royals.airescape.R

/**
 * Passive abilities that planes can have.
 */
enum class PlaneAbility(val description: String) {
    NONE("No special ability"),
    STAR_MAGNET("Stars pulled toward you"),
    SHIELD_REGEN("Shield regenerates every 45s"),
    SCORE_BONUS("Permanent +20% score bonus"),
    EXTRA_LIFE("Survive one hit without shield")
}

data class PlaneConfig(
    val id: Int,
    val name: String,
    val colorPrimary: Long,
    val colorSecondary: Long,
    val speedModifier: Float = 1.0f,
    val unlockCost: Int,
    val drawableRes: Int = R.drawable.plane_second,
    /** Degrees to add so the bitmap nose aligns with the movement angle.
     *  +45 = bitmap faces upper-right, +90 = bitmap faces up. */
    val rotationOffset: Float = 90f,
    /** When true the plane has a built-in gun that fires green bullets permanently. */
    val hasGun: Boolean = false,
    /** Passive ability for this plane. */
    val ability: PlaneAbility = PlaneAbility.NONE
) {
    companion object {
        val PLANES = listOf(
            PlaneConfig(0, "Rookie",      0xFF00E676, 0xFF00C853, 1.00f,    0, R.drawable.plane_second, 45f),
            PlaneConfig(1, "Air Force",   0xFFB0BEC5, 0xFF78909C, 1.05f,  100, R.drawable.air_force,    45f),
            PlaneConfig(2, "Aircraft",    0xFF40C4FF, 0xFF0091EA, 1.10f,  200, R.drawable.aircraft,      90f),
            PlaneConfig(3, "Tejas",       0xFF69F0AE, 0xFF00E676, 1.15f,  300, R.drawable.transport,     90f),
            PlaneConfig(4, "Jet",         0xFF7C4DFF, 0xFF6200EA, 1.20f,  400, R.drawable.jet,           90f, hasGun = true),
            PlaneConfig(5, "Rafale",      0xFFFF5252, 0xFFD50000, 1.25f,  500, R.drawable.jet_plane,     90f, hasGun = true),
            // New planes with unique passive abilities
            PlaneConfig(6, "Magnet",      0xFFFFD740, 0xFFFFC400, 1.05f,  600, R.drawable.aircraft,      90f, ability = PlaneAbility.STAR_MAGNET),
            PlaneConfig(7, "Guardian",    0xFF4FC3F7, 0xFF039BE5, 1.00f,  700, R.drawable.air_force,     45f, ability = PlaneAbility.SHIELD_REGEN),
            PlaneConfig(8, "Fortune",     0xFFFF9800, 0xFFE65100, 1.10f,  800, R.drawable.transport,     90f, ability = PlaneAbility.SCORE_BONUS),
            PlaneConfig(9, "Phoenix",     0xFFFF1744, 0xFFD50000, 1.15f, 1000, R.drawable.jet_plane,     90f, hasGun = true, ability = PlaneAbility.EXTRA_LIFE)
        )
    }
}
