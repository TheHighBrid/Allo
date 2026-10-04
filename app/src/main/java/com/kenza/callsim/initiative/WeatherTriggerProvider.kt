package com.kenza.callsim.initiative

import android.util.Log
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

/**
 * Weather-based "care call" trigger using OpenWeatherMap current conditions: rain, drizzle,
 * thunderstorms or snow in the user's city can give Kenza a reason to call.
 *
 * Not yet wired into [InitiativeScheduler]; nothing calls it today. It is safe to wire later:
 * it makes no network request when the API key or city is blank, and the blocking fetch must
 * run off the main thread.
 */
class WeatherTriggerProvider(
    private val apiKey: String,
    private val client: OkHttpClient = OkHttpClient(),
    private val baseUrl: HttpUrl = DEFAULT_BASE_URL.toHttpUrl(),
) {

    /** True only when both an API key and a city are present. */
    fun isConfigured(city: String): Boolean = apiKey.isNotBlank() && city.isNotBlank()

    /** Returns OpenWeatherMap's `weather[0].main` (e.g. "Rain"), or null if unavailable. */
    fun getCurrentWeatherCondition(city: String): String? {
        if (!isConfigured(city)) return null
        val url = baseUrl.newBuilder()
            .addQueryParameter("q", city.trim())
            .addQueryParameter("appid", apiKey.trim())
            .build()
        return try {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) return null
                parseCondition(response.body.string())
            }
        } catch (e: IOException) {
            // The URL carries the API key, so only the exception type is logged.
            Log.w(TAG, "Weather fetch failed: ${e.javaClass.simpleName}")
            null
        }
    }

    fun shouldTriggerCall(city: String): Pair<Boolean, String?> {
        val reason = getCurrentWeatherCondition(city)?.let(::triggerReasonFor)
        return (reason != null) to reason
    }

    companion object {
        private const val TAG = "WeatherProvider"
        const val DEFAULT_BASE_URL = "https://api.openweathermap.org/data/2.5/weather"

        private val CARE_CONDITIONS = setOf("Rain", "Drizzle", "Thunderstorm", "Snow")

        fun parseCondition(body: String): String? = runCatching {
            JSONObject(body).getJSONArray("weather").getJSONObject(0).getString("main")
        }.getOrNull()?.takeIf { it.isNotBlank() }

        fun triggerReasonFor(condition: String): String? =
            if (condition in CARE_CONDITIONS) "It's $condition outside" else null
    }
}
