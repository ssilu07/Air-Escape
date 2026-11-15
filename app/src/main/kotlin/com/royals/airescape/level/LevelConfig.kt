package com.royals.airescape.level

import kotlin.math.max

/**
 * Configuration and metadata for a single game level.
 * Defines the target distance, theme, difficulty scaling, and hurdles.
 */
data class LevelConfig(
    val levelNumber: Int,
    val title: String,
    val subtitle: String,
    val targetDistance: Float, // in world units (e.g. 6000f = 600 meters)
    val corridorWidth: Float = 2200f,
    val themeId: Int = 0,
    val missileSpawnInterval: Float = 2.0f,
    val maxMissiles: Int = 3,
    val airMinesCount: Int = 0,
    val laserGatesCount: Int = 0,
    val hasStorms: Boolean = false,
    val hasBoss: Boolean = false,
    val targetStars: Int = 10,
    val rewardCoins: Int = 50
) {
    val distanceMeters: Int get() = (targetDistance / 10f).toInt()
}

/**
 * Manages level generation and configuration up to 500 levels.
 * Generates handcrafted milestone experiences for early levels and
 * smooth procedural scaling across 50 sectors up to Level 500.
 */
object LevelManager {

    const val TOTAL_LEVELS = 500

    private val SECTOR_NAMES = arrayOf(
        "Coastal Dawn",      // 1-10
        "Crimson Ridge",     // 11-20
        "Neon Skyway",       // 21-30
        "Abyssal Trench",    // 31-40
        "Iron Citadel",      // 41-50
        "Vortex Canyon",     // 51-60
        "Solar Flare",       // 61-70
        "Frostfang Peak",    // 71-80
        "Plasma Nebula",     // 81-90
        "Titan Fortress",    // 91-100
        "Obsidian Valley",   // 101-110
        "Thunder Horizon",   // 111-120
        "Quantum Rift",      // 121-130
        "Mirage Dunes",      // 131-140
        "Overlord Bastion",  // 141-150
        "Celestial Orbit",   // 151-160
        "Cyber Spire",       // 161-170
        "Magma Core",        // 171-180
        "Aurora Borealis",   // 181-190
        "Dreadnought Fleet", // 191-200
        "Phantom Void",      // 201-210
        "Supernova Sector",  // 211-220
        "Aegis Perimeter",   // 221-230
        "Glacial Abyss",     // 231-240
        "Apocalypse Prime",  // 241-250
        "Dark Matter Reach", // 251-260
        "Electrostorm Axis", // 261-270
        "Hyperion Bastion",  // 271-280
        "Inferno Rift",      // 281-290
        "Colossus Keep",     // 291-300
        "Zero Gravity Zone", // 301-310
        "Starlight Passage", // 311-320
        "Ironclad Redoubt",  // 321-330
        "Plasma Vortex",     // 331-340
        "Gargantua Gate",    // 341-350
        "Eclipse Corridor",  // 351-360
        "Tempest Citadel",   // 361-370
        "Valkyrie Haven",    // 371-380
        "Singularity Point", // 381-390
        "Omega Horizon",     // 391-400
        "Cosmic Abyss",      // 401-410
        "Hyperstorm Core",   // 411-420
        "Titan Graveyard",   // 421-430
        "Starfall Expanse",  // 431-440
        "Vanguard Prime",    // 441-450
        "Chronos Rift",      // 451-460
        "Void Sovereign",    // 461-470
        "Galactic Bulwark",  // 471-480
        "Infinity Expanse",  // 481-490
        "Ultimate Apex"      // 491-500
    )

    fun getLevel(levelNumber: Int): LevelConfig {
        val lvl = levelNumber.coerceIn(1, TOTAL_LEVELS)

        // Handcrafted custom thematic milestones for the first 5 levels
        return when (lvl) {
            1 -> LevelConfig(
                levelNumber = 1,
                title = "Level 1: First Flight",
                subtitle = "Fly 400m to the Aircraft Carrier",
                targetDistance = 4000f,
                themeId = 0, // Teal Sky
                missileSpawnInterval = 2.8f,
                maxMissiles = 2,
                airMinesCount = 0,
                laserGatesCount = 0,
                hasStorms = false,
                hasBoss = false,
                targetStars = 8,
                rewardCoins = 30
            )
            2 -> LevelConfig(
                levelNumber = 2,
                title = "Level 2: Minefield Run",
                subtitle = "Dodge floating magnetic mines",
                targetDistance = 5500f,
                themeId = 4, // Ocean Blue
                missileSpawnInterval = 2.4f,
                maxMissiles = 3,
                airMinesCount = 6,
                laserGatesCount = 0,
                hasStorms = false,
                hasBoss = false,
                targetStars = 10,
                rewardCoins = 40
            )
            3 -> LevelConfig(
                levelNumber = 3,
                title = "Level 3: Laser Grid",
                subtitle = "Slip through high-voltage laser gates",
                targetDistance = 7000f,
                themeId = 1, // Sunset
                missileSpawnInterval = 2.2f,
                maxMissiles = 3,
                airMinesCount = 4,
                laserGatesCount = 4,
                hasStorms = false,
                hasBoss = false,
                targetStars = 12,
                rewardCoins = 50
            )
            4 -> LevelConfig(
                levelNumber = 4,
                title = "Level 4: Thunder Canyon",
                subtitle = "Watch out for lightning strikes & swarms",
                targetDistance = 8500f,
                themeId = 2, // Night
                missileSpawnInterval = 2.0f,
                maxMissiles = 4,
                airMinesCount = 8,
                laserGatesCount = 5,
                hasStorms = true,
                hasBoss = false,
                targetStars = 15,
                rewardCoins = 60
            )
            5 -> LevelConfig(
                levelNumber = 5,
                title = "Level 5: Titan Intercept",
                subtitle = "Boss battle at the landing extraction point!",
                targetDistance = 10000f,
                themeId = 3, // Cosmic Space
                missileSpawnInterval = 1.8f,
                maxMissiles = 4,
                airMinesCount = 8,
                laserGatesCount = 6,
                hasStorms = true,
                hasBoss = true,
                targetStars = 20,
                rewardCoins = 150
            )
            else -> {
                // Procedural scaling across 500 levels
                val isBossLevel = (lvl % 10 == 0 || lvl == 5)
                val isMajorBoss = (lvl % 50 == 0)

                val sectorIndex = ((lvl - 1) / 10).coerceIn(0, SECTOR_NAMES.size - 1)
                val sectorName = SECTOR_NAMES[sectorIndex]
                val stageInSector = ((lvl - 1) % 10) + 1

                // Distance scales from 400m at level 1 to ~12,000m at level 500
                val distanceMeters = (400 + (lvl - 1) * 23.5f).toInt()
                val targetDist = distanceMeters * 10f

                // Themes cycle across sectors (0=Teal, 4=Ocean, 1=Sunset, 2=Night, 5=Neon, 3=Cosmic)
                val themes = intArrayOf(0, 4, 1, 2, 5, 3)
                val themeId = themes[sectorIndex % themes.size]

                // Missile spawn interval accelerates from 2.8s down to 0.85s
                val interval = (2.8f - (lvl - 1) * 0.0039f).coerceIn(0.85f, 2.8f)

                // Max simultaneous missiles scales from 2 to 7
                val maxM = (2 + (lvl / 80)).coerceIn(2, 7)

                // Air mines scale from 4 up to 100
                val mines = (4 + (lvl * 0.20f)).toInt().coerceIn(4, 100)

                // Laser gates scale from 3 up to 50
                val lasers = (3 + (lvl * 0.10f)).toInt().coerceIn(3, 50)

                // Storm hazards on designated sectors or boss levels
                val storm = lvl >= 4 && (lvl % 3 == 0 || isBossLevel || lvl % 5 == 0)

                val title = when {
                    isMajorBoss -> "Level $lvl: ★ APEX TITAN: $sectorName ★"
                    isBossLevel -> "Level $lvl: Titan Nemesis - $sectorName"
                    else -> "Level $lvl: $sectorName - Stage $stageInSector"
                }

                val subtitle = when {
                    isMajorBoss -> "DEFEAT APEX BOSS & REACH CARRIER (${distanceMeters}m)"
                    isBossLevel -> "Destroy the Sector Titan & reach the carrier (${distanceMeters}m)"
                    else -> "Reach the extraction carrier at ${distanceMeters}m"
                }

                LevelConfig(
                    levelNumber = lvl,
                    title = title,
                    subtitle = subtitle,
                    targetDistance = targetDist,
                    corridorWidth = (2200f - (lvl * 1.0f)).coerceAtLeast(1700f),
                    themeId = themeId,
                    missileSpawnInterval = interval,
                    maxMissiles = maxM,
                    airMinesCount = mines,
                    laserGatesCount = lasers,
                    hasStorms = storm,
                    hasBoss = isBossLevel,
                    targetStars = (8 + (lvl * 0.15f)).toInt().coerceIn(8, 75),
                    rewardCoins = if (isMajorBoss) 300 + lvl * 3 else if (isBossLevel) 150 + lvl * 2 else 40 + (lvl * 0.8f).toInt()
                )
            }
        }
    }
}
