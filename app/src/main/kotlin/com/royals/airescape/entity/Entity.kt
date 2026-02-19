package com.royals.airescape.entity

import android.graphics.Canvas
import com.royals.airescape.util.Vector2

/**
 * Base class for all game entities.
 * Every entity has a position, velocity, bounding radius, and alive flag.
 */
abstract class Entity(
    var position: Vector2 = Vector2(),
    var velocity: Vector2 = Vector2(),
    var radius: Float = 10f,
    var alive: Boolean = true
) {
    /** Advance the entity state by [dt] seconds. */
    abstract fun update(dt: Float)

    /** Draw the entity onto [canvas]. */
    abstract fun render(canvas: Canvas)
}
