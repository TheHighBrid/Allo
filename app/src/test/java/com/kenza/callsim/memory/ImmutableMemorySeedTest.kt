package com.kenza.callsim.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImmutableMemorySeedTest {
    @Test
    fun `seed includes corrected age and language facts`() {
        val seeded = ImmutableMemorySeed.apply(MemorySnapshot())

        assertTrue(seeded.profiles.kenzaProfile.contains("26 and turns 27 in September"))
        assertTrue(seeded.profiles.kenzaProfile.contains("fluent in English and French"))
        assertTrue(seeded.profiles.kenzaProfile.contains("limited conversational Moroccan Darija"))
    }

    @Test
    fun `applying seed is idempotent and preserves custom profiles`() {
        val custom = MemorySnapshot(
            profiles = PersonalityProfiles(kenzaProfile = "Custom user note."),
        )

        val once = ImmutableMemorySeed.apply(custom)
        val twice = ImmutableMemorySeed.apply(once)

        assertEquals(once.items, twice.items)
        assertEquals(once.profiles, twice.profiles)
        assertTrue(twice.profiles.kenzaProfile.startsWith("Custom user note."))
    }

    @Test
    fun `locked memories replace stale copies after deletion`() {
        val seeded = ImmutableMemorySeed.apply(MemorySnapshot())
        val withoutAge = seeded.copy(items = seeded.items.filterNot { it.id == "core_kenza_identity" })

        val restored = ImmutableMemorySeed.apply(withoutAge)

        assertTrue(restored.items.any { it.id == "core_kenza_identity" && it.pinned })
    }
}
