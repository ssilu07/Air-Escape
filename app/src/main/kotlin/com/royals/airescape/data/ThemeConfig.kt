package com.royals.airescape.data

data class ThemeConfig(
    val id: Int,
    val name: String,
    val skyColor: Long,
    val cloudColor: Long,
    val cloudAlpha: Int,
    val unlockCost: Int
) {
    companion object {
        val THEMES: List<ThemeConfig> = listOf(
            ThemeConfig(0, "Teal Sky", 0xFF80DEEA, 0xFFFFFFFF, 35, 0),
            ThemeConfig(1, "Sunset", 0xFFFF8A65, 0xFFFFE082, 40, 200),
            ThemeConfig(2, "Night", 0xFF1A237E, 0xFF9FA8DA, 25, 300),
            ThemeConfig(3, "Space", 0xFF0D020D, 0xFFB388FF, 15, 500),
            ThemeConfig(4, "Ocean", 0xFF006064, 0xFF80E8EA, 30, 400),
            ThemeConfig(5, "Sakura", 0xFFF8BBD0, 0xFFFFFFFF, 40, 350)
        )
    }
}
