package com.sri.nfcdemo

import org.junit.Assert.* // ktlint-disable no-wildcard-imports
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class NfcUtilTest {

    @Test
    fun `test NdefMessage`() {
        val expectedText = "hello word"
        val text = createNdefMessage(expectedText)

        assertNotNull(text)
        assertEquals(1, text.records.size)
        assertNotNull(text.records[0])
    }
}
