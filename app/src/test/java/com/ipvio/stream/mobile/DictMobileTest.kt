package com.ipvio.stream.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every phone/tablet text has all 9 languages of DictMobile plus Dutch. */
class DictMobileTest {
    private fun rows(block: String) = block.lineSequence().filter { it.contains('¦') }.map { it.split('¦') }.toList()

    @Test fun allLanguagesPresent() {
        val main = rows(DictMobile)
        assertTrue(main.isNotEmpty())
        for (r in main) assertEquals("columns: " + r[0], 9, r.count { it.isNotBlank() })
        val nl = rows(DictNlMobile).associate { it[0] to it[1] }
        for (r in main) assertTrue("Dutch missing: " + r[0], !nl[r[0]].isNullOrBlank())
    }
}
