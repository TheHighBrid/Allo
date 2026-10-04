package com.kenza.callsim.initiative

import okhttp3.OkHttpClient
import okhttp3.Interceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextTriggerProvidersTest {

    private val city = "Ottawa"

    @Test
    fun weatherParsesOpenWeatherMapCondition() {
        assertEquals(
            "Rain",
            WeatherTriggerProvider.parseCondition("""{"weather":[{"id":500,"main":"Rain"}]}"""),
        )
        assertNull(WeatherTriggerProvider.parseCondition("""{"cod":401,"message":"Invalid API key"}"""))
        assertNull(WeatherTriggerProvider.parseCondition("not json"))
    }

    @Test
    fun onlyCareConditionsTriggerACall() {
        assertEquals("It's Snow outside", WeatherTriggerProvider.triggerReasonFor("Snow"))
        assertEquals("It's Thunderstorm outside", WeatherTriggerProvider.triggerReasonFor("Thunderstorm"))
        assertNull(WeatherTriggerProvider.triggerReasonFor("Clear"))
        assertNull(WeatherTriggerProvider.triggerReasonFor("Clouds"))
    }

    @Test
    fun missingKeyOrCityNeverTouchesTheNetwork() {
        var requests = 0
        val countingClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain -> requests++; chain.proceed(chain.request()) })
            .build()

        val noKey = WeatherTriggerProvider(apiKey = " ", client = countingClient)
        assertFalse(noKey.isConfigured(city))
        assertNull(noKey.getCurrentWeatherCondition(city))
        assertEquals(false to null, noKey.shouldTriggerCall(city))

        val noCity = WeatherTriggerProvider(apiKey = "key", client = countingClient)
        assertFalse(noCity.isConfigured(""))
        assertNull(noCity.getCurrentWeatherCondition(""))

        assertEquals(0, requests)
    }

    @Test
    fun calendarReasonHandlesMissingTitle() {
        assertEquals(true to "Your event 'Dentist' just ended", CalendarTriggerProvider.reasonFor(" Dentist "))
        assertEquals(true to "Your event just ended", CalendarTriggerProvider.reasonFor(null))
        assertTrue(CalendarTriggerProvider.WINDOW_MS == 15 * 60 * 1000L)
    }
}
