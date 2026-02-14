package com.example.airescape.data

data class PlaneConfig(
    val id: Int,
    val name: String,
    val colorPrimary: Long,
    val colorSecondary: Long,
    val speedModifier: Float = 1.0f,
    val unlockCost: Int
) {
    companion object {
        val PLANES = listOf(
            PlaneConfig(0, "Rookie", 0xFF00E676, 0xFF00C853, 1.0f, 0),        // Green - free
            PlaneConfig(1, "Blaze", 0xFFFF5252, 0xFFD50000, 1.05f, 50),       // Red
            PlaneConfig(2, "Frost", 0xFF40C4FF, 0xFF0091EA, 1.0f, 75),        // Blue
            PlaneConfig(3, "Shadow", 0xFF7C4DFF, 0xFF6200EA, 1.1f, 100),      // Purple
            PlaneConfig(4, "Solar", 0xFFFFD740, 0xFFFFC400, 0.95f, 125),      // Gold - slightly slower but looks cool
            PlaneConfig(5, "Phantom", 0xFFB0BEC5, 0xFF78909C, 1.15f, 200),    // Gray/silver - fast
            PlaneConfig(6, "Viper", 0xFF69F0AE, 0xFF00E676, 1.1f, 175),      // Neon green
            PlaneConfig(7, "Inferno", 0xFFFF6E40, 0xFFFF3D00, 1.2f, 300),    // Orange - fastest
            PlaneConfig(8, "Stealth", 0xFF37474F, 0xFF263238, 1.1f, 250),    // Dark - cool looking
            PlaneConfig(9, "Prism", 0xFFE040FB, 0xFFAA00FF, 1.05f, 350)     // Pink/magenta - premium
        )
    }
}
