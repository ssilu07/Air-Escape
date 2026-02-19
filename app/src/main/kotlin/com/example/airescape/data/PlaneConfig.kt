package com.example.airescape.data

import com.example.airescape.R

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
    val rotationOffset: Float = 90f
) {
    companion object {
        val PLANES = listOf(
            PlaneConfig(0, "Rookie",    0xFF00E676, 0xFF00C853, 1.00f,   0, R.drawable.plane_second, 45f),
            PlaneConfig(1, "Air Force", 0xFFB0BEC5, 0xFF78909C, 1.05f, 100, R.drawable.air_force,    90f),
            PlaneConfig(2, "Aircraft",  0xFF40C4FF, 0xFF0091EA, 1.10f, 200, R.drawable.aircraft,      90f),
            PlaneConfig(3, "Transport", 0xFF69F0AE, 0xFF00E676, 1.15f, 300, R.drawable.transport,     90f),
            PlaneConfig(4, "Jet",       0xFF7C4DFF, 0xFF6200EA, 1.20f, 400, R.drawable.jet,           90f),
            PlaneConfig(5, "Jet Plane", 0xFFFF5252, 0xFFD50000, 1.25f, 500, R.drawable.jet_plane,     90f)
        )
    }
}
