package com.example.airescape.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vector2(val x: Float = 0f, val y: Float = 0f) {

    operator fun plus(other: Vector2): Vector2 =
        Vector2(x + other.x, y + other.y)

    operator fun minus(other: Vector2): Vector2 =
        Vector2(x - other.x, y - other.y)

    operator fun times(scalar: Float): Vector2 =
        Vector2(x * scalar, y * scalar)

    operator fun div(scalar: Float): Vector2 {
        if (scalar == 0f) return ZERO
        return Vector2(x / scalar, y / scalar)
    }

    operator fun unaryMinus(): Vector2 =
        Vector2(-x, -y)

    fun magnitude(): Float =
        sqrt(x * x + y * y)

    fun normalized(): Vector2 {
        val mag = magnitude()
        return if (mag == 0f) ZERO else this / mag
    }

    /** Returns the angle in radians of this vector relative to the positive X axis. */
    fun angle(): Float =
        atan2(y, x)

    fun dot(other: Vector2): Float =
        x * other.x + y * other.y

    fun distance(other: Vector2): Float =
        (this - other).magnitude()

    fun lerp(target: Vector2, t: Float): Vector2 {
        val clamped = t.coerceIn(0f, 1f)
        return Vector2(
            x + (target.x - x) * clamped,
            y + (target.y - y) * clamped
        )
    }

    companion object {
        val ZERO = Vector2(0f, 0f)
        val UP = Vector2(0f, -1f)
        val DOWN = Vector2(0f, 1f)
        val LEFT = Vector2(-1f, 0f)
        val RIGHT = Vector2(1f, 0f)

        fun fromAngle(radians: Float): Vector2 =
            Vector2(cos(radians), sin(radians))
    }
}
