package com.kenza.callsim.initiative

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class WeatherTriggerProvider(private val apiKey: String) {
    private val client = OkHttpClient()

    fun getCurrentWeatherCondition(city: String): String {
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$apiKey"
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string().orEmpty())
                json.getJSONArray("weather").getJSONObject(0).getString("main")
            }
        } catch (e: Exception) {
            Log.e("WeatherProvider", "Fetch failed", e)
            "Unknown"
        }
    }

    fun shouldTriggerCall(city: String): Pair<Boolean, String?> {
        val condition = getCurrentWeatherCondition(city)
        return when (condition) {
            "Rain", "Drizzle", "Thunderstorm", "Snow" -> true to "It's $condition outside"
            else -> false to null
        }
    }
}
