package com.royals.airescape.data

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
    const val MISSILE_SPAWN_INITIAL = 3.5f   // seconds between spawns (initial)
    const val MISSILE_SPAWN_MIN = 1.2f       // minimum seconds between spawns (normal)
    const val MISSILE_SPAWN_DECREASE_RATE = 0.05f // decrease per spawn event
    const val MAX_MISSILES_NORMAL = 8        // max missiles on screen (normal)
    const val MAX_MISSILES_HARD = 14         // max missiles on screen (hard)

    // Stars
    const val STAR_SPAWN_INTERVAL = 2.0f     // seconds
    const val STAR_RADIUS = 22f

    // Power-ups
    const val POWERUP_SPAWN_INTERVAL = 15.0f // seconds
    const val POWERUP_RADIUS = 18f

    // Shield – no timer; lasts until a missile hit deactivates it

    // Speed boost
    const val SPEED_BOOST_DURATION = 15.0f   // seconds
    const val SPEED_BOOST_MULTIPLIER = 1.8f

    // Slow motion (slows all missiles)
    const val SLOW_MOTION_DURATION = 15.0f   // seconds
    const val SLOW_MOTION_MULTIPLIER = 0.3f  // missiles at 30% speed

    // Missile jammer (freezes all missiles)
    const val MISSILE_JAMMER_DURATION = 15.0f // seconds

    // Double score
    const val DOUBLE_SCORE_DURATION = 15.0f  // seconds

    // Bullet shoot (player fires green bullets at missiles)
    const val BULLET_SHOOT_DURATION = 15.0f  // seconds
    const val PLAYER_BULLET_SPEED = 900f     // px/s
    const val PLAYER_BULLET_FIRE_RATE = 0.3f // seconds between shots
    const val PLAYER_BULLET_RADIUS = 10f

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
    const val BULLET_SHOOT_COLOR = 0xFF00E676 // green
    const val MISSILE_COLOR = 0xFFFF1744     // red (fallback triangle color)

    // Straight missile — fast, no homing
    const val STRAIGHT_MISSILE_SPEED = 550f            // px/s (faster than homing)

    // Bouncing missile — reflects off viewport edges
    const val BOUNCING_MISSILE_SPEED = 400f            // px/s
    const val BOUNCING_MISSILE_MAX_BOUNCES = 4         // despawn after N bounces

    // Cluster missile — splits into 3 mini missiles on death
    const val CLUSTER_MINI_COUNT = 3
    const val CLUSTER_MINI_SPEED = 500f                // px/s
    const val CLUSTER_MINI_RADIUS = 8f
    const val CLUSTER_MINI_LIFETIME = 3f               // seconds before despawn

    // Stealth missile — blinks invisible periodically
    const val STEALTH_VISIBLE_TIME = 1.2f              // seconds visible
    const val STEALTH_INVISIBLE_TIME = 0.8f            // seconds invisible
    // Combo / Streak system
    const val STAR_STREAK_THRESHOLD = 5          // consecutive stars for bonus
    const val STAR_STREAK_MULTIPLIER = 1.5f      // score multiplier at streak
    const val MISSILE_DESTROY_STREAK_FOR_SHIELD = 3  // destroys without getting hit → free shield

    // Boss fight
    const val BOSS_SPAWN_INTERVAL_MIN = 60f      // earliest boss spawn (seconds)
    const val BOSS_SPAWN_INTERVAL_MAX = 90f      // latest boss spawn (seconds)
    const val BOSS_HP = 5                        // hits to destroy boss
    const val BOSS_RADIUS = 36f                  // 3x normal missile
    const val BOSS_SPEED = 200f                  // slow but menacing
    const val BOSS_TURN_RATE = 1.0f              // slower turning
    const val BOSS_SCORE_REWARD = 200            // score for killing boss
    const val BOSS_COLOR = 0xFFD50000            // dark red

    // Daily challenges
    const val DAILY_CHALLENGE_COUNT = 3          // challenges per day

    // Screen shake
    const val SHAKE_DURATION = 0.3f              // seconds
    const val SHAKE_INTENSITY = 8f               // max pixel offset
    const val SHAKE_INTENSITY_STRONG = 15f       // for death / boss hit

    // Power-up expiry warning
    const val POWERUP_WARN_TIME = 3f             // seconds before expiry to start flashing

    // Sparkle burst (star collect)
    const val SPARKLE_PARTICLE_COUNT = 12
    const val SPARKLE_LIFETIME = 0.4f            // seconds

    // Speed boost streaks
    const val BOOST_STREAK_COUNT = 8             // number of motion blur lines
    const val BOOST_STREAK_LIFETIME = 0.15f      // seconds

    // Shield shatter
    const val SHIELD_SHATTER_COUNT = 16          // fragments
    const val SHIELD_SHATTER_LIFETIME = 0.5f     // seconds

    const val PLAYER_DEFAULT_COLOR = 0xFF00E676 // green

    // Plane abilities
    const val STAR_MAGNET_RADIUS = 200f         // px — stars within this range are pulled
    const val STAR_MAGNET_FORCE = 300f           // px/s attraction speed
    const val SHIELD_REGEN_INTERVAL = 45f        // seconds between auto-shield
    const val SCORE_BONUS_MULTIPLIER = 1.2f      // 20% permanent score bonus

    // Earning & Coins system (1 Star = 1 Coin)
    const val COINS_PER_STAR = 1                 // Each star collected gives 1 coin
    const val COINS_PER_RUPEE = 100              // 100 coins = 1 Rupee (₹1.00)
    const val MIN_WITHDRAW_COINS = 500           // Minimum 500 coins required to withdraw (₹5.00)
    const val REWARD_AD_COINS = 20               // Extra coins for watching rewarded ad
    const val COIN_COLOR = 0xFFFFD700            // Gold coin color

    // Supabase Cloud Configuration
    const val SUPABASE_URL = "https://ugnhfkhsjfkzepsnjiyk.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVnbmhma2hzamZremVwc25qaXlrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODU0OTUxNzksImV4cCI6MjEwMTA3MTE3OX0.H9TAR_WGfvzgUTsnxht-n-n7ywxlfQHoMkk65L7oWdo"
}

