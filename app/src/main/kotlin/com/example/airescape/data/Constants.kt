package com.example.airescape.data

object Constants {

    // Player
    const val PLAYER_SPEED = 400f            // px/s
    const val PLAYER_RADIUS = 15f
    const val PLAYER_TURN_RATE = 8.0f       // rad/s (steering speed)

    // Missiles
    const val MISSILE_INITIAL_SPEED = 350f   // px/s
    const val MISSILE_MAX_SPEED = 650f       // px/s
    const val MISSILE_TURN_RATE = 1.6f       // rad/s (base — scales down with speed)
    const val MISSILE_RADIUS = 12f
    const val MISSILE_SPEED_RAMP_TIME = 15f  // seconds to reach max speed

    // Spawning
    const val MISSILE_SPAWN_INITIAL = 3.0f   // seconds between spawns (initial)
    const val MISSILE_SPAWN_MIN = 0.5f       // minimum seconds between spawns
    const val MISSILE_SPAWN_DECREASE_RATE = 0.05f // decrease per spawn event

    // Stars
    const val STAR_SPAWN_INTERVAL = 2.0f     // seconds
    const val STAR_RADIUS = 22f

    // Power-ups
    const val POWERUP_SPAWN_INTERVAL = 15.0f // seconds
    const val POWERUP_RADIUS = 18f

    // Shield
    const val SHIELD_DURATION = 5.0f         // seconds

    // Speed boost
    const val SPEED_BOOST_DURATION = 4.0f    // seconds
    const val SPEED_BOOST_MULTIPLIER = 1.8f

    // Slow motion (slows all missiles)
    const val SLOW_MOTION_DURATION = 5.0f    // seconds
    const val SLOW_MOTION_MULTIPLIER = 0.3f  // missiles at 30% speed

    // Missile jammer (freezes all missiles)
    const val MISSILE_JAMMER_DURATION = 3.0f // seconds

    // Double score
    const val DOUBLE_SCORE_DURATION = 8.0f   // seconds

    // Hard mode
    const val HARD_MODE_SPAWN_MULTIPLIER = 1.6f     // missiles spawn 60% faster
    const val HARD_MODE_MISSILE_SPEED_MULT = 1.3f   // missiles move 30% faster
    const val HARD_MODE_TURN_RATE_MULT = 1.2f       // missiles steer 20% better

    // Particles / visual effects
    const val TRAIL_PARTICLE_LIFETIME = 0.3f // seconds
    const val EXPLOSION_PARTICLE_COUNT = 20
    const val EXPLOSION_LIFETIME = 0.6f      // seconds

    // Colors (ARGB as Long so they can be used with .toInt())
    const val BACKGROUND_COLOR = 0xFF1A1A2E  // dark navy
    const val HUD_COLOR = 0xFFFFFFFF         // white
    const val STAR_COLOR = 0xFFFFD700        // gold
    const val SHIELD_COLOR = 0xFF4FC3F7      // light blue
    const val SPEED_BOOST_COLOR = 0xFFFF9800 // orange
    const val SLOW_MOTION_COLOR = 0xFF7C4DFF // purple
    const val MISSILE_JAMMER_COLOR = 0xFFE040FB // magenta/pink
    const val DOUBLE_SCORE_COLOR = 0xFFFFD740 // amber/gold
    const val MISSILE_COLOR = 0xFFFF1744     // red
    const val PLAYER_DEFAULT_COLOR = 0xFF00E676 // green
}
