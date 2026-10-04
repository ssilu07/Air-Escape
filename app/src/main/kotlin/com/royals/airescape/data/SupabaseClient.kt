package com.royals.airescape.data

import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors

/**
 * Lightweight, direct Supabase REST client for AirEscape.
 * Handles player wallet persistence, synchronization, and withdrawal submissions
 * using standard Android networking (zero extra dependencies).
 */
object SupabaseClient {

    private const val TAG = "SupabaseClient"
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    var isSyncing: Boolean = false
        private set

    var lastSyncSuccess: Boolean = true
        private set

    private fun getIsoTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    /**
     * Upserts player wallet data to Supabase table `player_wallets`.
     * If the player already exists, updates coins, total_stars, upi_id, and last_active.
     */
    fun syncWallet(
        playerId: String,
        coins: Int,
        totalStars: Int,
        upiId: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (playerId.isBlank()) return
        isSyncing = true

        executor.execute {
            var success = false
            var conn: HttpURLConnection? = null
            try {
                val url = URL("${Constants.SUPABASE_URL}/rest/v1/player_wallets")
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 10000
                    doOutput = true
                    setRequestProperty("apikey", Constants.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer ${Constants.SUPABASE_ANON_KEY}")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Prefer", "resolution=merge-duplicates")
                }

                val payload = JSONObject().apply {
                    put("player_id", playerId)
                    put("coins", coins)
                    put("total_stars", totalStars)
                    put("upi_id", upiId)
                    put("last_active", getIsoTimestamp())
                }

                OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                val code = conn.responseCode
                success = code in 200..299
                if (success) {
                    Log.d(TAG, "Wallet successfully synced for player $playerId ($coins coins)")
                } else {
                    val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    Log.w(TAG, "Wallet sync failed (HTTP $code): $err")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing wallet to Supabase", e)
                success = false
            } finally {
                conn?.disconnect()
                lastSyncSuccess = success
                isSyncing = false
                mainHandler.post {
                    onComplete?.invoke(success)
                }
            }
        }
    }

    /**
     * Fetches current wallet status for [playerId] from Supabase.
     */
    fun fetchWallet(
        playerId: String,
        onResult: (remoteCoins: Int?, remoteStars: Int?, remoteUpi: String?) -> Unit
    ) {
        if (playerId.isBlank()) {
            mainHandler.post { onResult(null, null, null) }
            return
        }

        executor.execute {
            var remoteCoins: Int? = null
            var remoteStars: Int? = null
            var remoteUpi: String? = null
            var conn: HttpURLConnection? = null

            try {
                val query = "player_id=eq.${URLEncoder.encode(playerId, "UTF-8")}&select=*"
                val url = URL("${Constants.SUPABASE_URL}/rest/v1/player_wallets?$query")
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("apikey", Constants.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer ${Constants.SUPABASE_ANON_KEY}")
                    setRequestProperty("Accept", "application/json")
                }

                if (conn.responseCode in 200..299) {
                    val resp = conn.inputStream.bufferedReader().use { it.readText() }
                    val array = JSONArray(resp)
                    if (array.length() > 0) {
                        val obj = array.getJSONObject(0)
                        remoteCoins = obj.optInt("coins")
                        remoteStars = obj.optInt("total_stars")
                        remoteUpi = obj.optString("upi_id")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching wallet from Supabase", e)
            } finally {
                conn?.disconnect()
                mainHandler.post {
                    onResult(remoteCoins, remoteStars, remoteUpi)
                }
            }
        }
    }

    /**
     * Submits a withdrawal request record into Supabase `withdrawals` table.
     */
    fun submitWithdrawal(
        req: GameData.WithdrawalRequest,
        playerId: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        executor.execute {
            var success = false
            var message: String? = null
            var conn: HttpURLConnection? = null

            try {
                val url = URL("${Constants.SUPABASE_URL}/rest/v1/withdrawals")
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 12000
                    readTimeout = 12000
                    doOutput = true
                    setRequestProperty("apikey", Constants.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer ${Constants.SUPABASE_ANON_KEY}")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Prefer", "return=representation")
                }

                val payload = JSONObject().apply {
                    put("id", req.id)
                    put("player_id", playerId)
                    put("upi_id", req.upiId)
                    put("coins", req.coins)
                    put("rupee_amount", req.rupeeAmount.toDouble())
                    put("status", req.status)
                }

                OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                val code = conn.responseCode
                success = code in 200..299
                if (success) {
                    Log.d(TAG, "Withdrawal ${req.id} submitted to Supabase successfully")
                } else {
                    val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    message = "Server response: $code"
                    Log.w(TAG, "Withdrawal submission failed ($code): $err")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting withdrawal to Supabase", e)
                message = e.localizedMessage
                success = false
            } finally {
                conn?.disconnect()
                mainHandler.post {
                    onComplete?.invoke(success, message)
                }
            }
        }
    }

    /**
     * Fetches all withdrawal transactions for [playerId] from Supabase to update live statuses.
     */
    fun fetchWithdrawals(
        playerId: String,
        onResult: (List<GameData.WithdrawalRequest>?) -> Unit
    ) {
        if (playerId.isBlank()) {
            mainHandler.post { onResult(null) }
            return
        }

        executor.execute {
            val list = mutableListOf<GameData.WithdrawalRequest>()
            var conn: HttpURLConnection? = null
            var success = false

            try {
                val query = "player_id=eq.${URLEncoder.encode(playerId, "UTF-8")}&order=created_at.desc&limit=25"
                val url = URL("${Constants.SUPABASE_URL}/rest/v1/withdrawals?$query")
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("apikey", Constants.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer ${Constants.SUPABASE_ANON_KEY}")
                    setRequestProperty("Accept", "application/json")
                }

                if (conn.responseCode in 200..299) {
                    val resp = conn.inputStream.bufferedReader().use { it.readText() }
                    val array = JSONArray(resp)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val id = obj.optString("id")
                        val upi = obj.optString("upi_id")
                        val coins = obj.optInt("coins")
                        val rupee = obj.optDouble("rupee_amount", 0.0).toFloat()
                        val status = obj.optString("status", "PENDING")
                        // Format or timestamp
                        list.add(
                            GameData.WithdrawalRequest(
                                id = id,
                                upiId = upi,
                                coins = coins,
                                rupeeAmount = rupee,
                                timestamp = System.currentTimeMillis(),
                                status = status
                            )
                        )
                    }
                    success = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching withdrawals from Supabase", e)
            } finally {
                conn?.disconnect()
                mainHandler.post {
                    onResult(if (success) list else null)
                }
            }
        }
    }
}
