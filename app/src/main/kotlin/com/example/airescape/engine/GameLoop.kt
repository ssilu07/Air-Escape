package com.example.airescape.engine

/**
 * Fixed-timestep game loop running on its own thread.
 * Targets 60 updates per second with an accumulator pattern.
 */
class GameLoop(private val callback: Callback) : Thread("GameLoop") {

    interface Callback {
        fun update(dt: Float)
        fun render()
    }

    @Volatile
    var running: Boolean = false
        private set

    /** Most recent measured frames-per-second (for debug overlay). */
    @Volatile
    var fps: Int = 0
        private set

    companion object {
        private const val TARGET_UPS = 60
        private const val NS_PER_UPDATE = 1_000_000_000L / TARGET_UPS
    }

    fun startLoop() {
        running = true
        start()
    }

    fun stopLoop() {
        running = false
        try {
            join(2000)
        } catch (_: InterruptedException) {
            // ignored
        }
    }

    override fun run() {
        var previousTime = System.nanoTime()
        var accumulator = 0L
        var frameCount = 0
        var fpsTimer = System.nanoTime()
        val fixedDt = 1.0f / TARGET_UPS

        while (running) {
            val currentTime = System.nanoTime()
            val elapsed = currentTime - previousTime
            previousTime = currentTime
            accumulator += elapsed

            // Prevent spiral of death: cap accumulated time
            if (accumulator > NS_PER_UPDATE * 5) {
                accumulator = NS_PER_UPDATE * 5
            }

            // Fixed-rate updates
            while (accumulator >= NS_PER_UPDATE) {
                callback.update(fixedDt)
                accumulator -= NS_PER_UPDATE
            }

            // Render once per iteration
            callback.render()
            frameCount++

            // Update FPS counter every second
            if (currentTime - fpsTimer >= 1_000_000_000L) {
                fps = frameCount
                frameCount = 0
                fpsTimer = currentTime
            }

            // Yield a tiny bit to avoid burning the CPU when ahead of schedule
            val afterWork = System.nanoTime()
            val sleepNs = NS_PER_UPDATE - (afterWork - currentTime)
            if (sleepNs > 1_000_000) {
                try {
                    sleep(sleepNs / 1_000_000, (sleepNs % 1_000_000).toInt())
                } catch (_: InterruptedException) {
                    // ignored
                }
            }
        }
    }
}
