package com.example.airescape.util

object CollisionUtil {

    /**
     * Returns true if two circles overlap.
     */
    fun circleCircle(pos1: Vector2, r1: Float, pos2: Vector2, r2: Float): Boolean {
        val distSq = (pos1.x - pos2.x) * (pos1.x - pos2.x) +
                      (pos1.y - pos2.y) * (pos1.y - pos2.y)
        val radiiSum = r1 + r2
        return distSq <= radiiSum * radiiSum
    }

    /**
     * Returns true if a point lies inside (or on the edge of) a circle.
     */
    fun pointInCircle(point: Vector2, center: Vector2, radius: Float): Boolean {
        val distSq = (point.x - center.x) * (point.x - center.x) +
                      (point.y - center.y) * (point.y - center.y)
        return distSq <= radius * radius
    }
}
