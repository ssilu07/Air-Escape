package com.example.airescape.data

import com.example.airescape.R

data class PlaneConfig(
    val id: Int,
    val name: String,
    val colorPrimary: Long,
    val colorSecondary: Long,
    val speedModifier: Float = 1.0f,
    val unlockCost: Int,
    val drawableRes: Int = R.drawable.plane_second
) {
    companion object {
        val PLANES = listOf(
            PlaneConfig(0, "Rookie",    0xFF00E676, 0xFF00C853, 1.00f,   0, R.drawable.plane_second),
            PlaneConfig(1, "Air Force", 0xFFB0BEC5, 0xFF78909C, 1.05f,  50, R.drawable.air_force),
            PlaneConfig(2, "Aircraft",  0xFF40C4FF, 0xFF0091EA, 1.10f, 100, R.drawable.aircraft),
            PlaneConfig(3, "Transport", 0xFF69F0AE, 0xFF00E676, 1.15f, 175, R.drawable.transport),
            PlaneConfig(4, "Jet",       0xFF7C4DFF, 0xFF6200EA, 1.20f, 250, R.drawable.jet),
            PlaneConfig(5, "Jet Plane", 0xFFFF5252, 0xFFD50000, 1.25f, 350, R.drawable.jet_plane)
        )
    }
}
