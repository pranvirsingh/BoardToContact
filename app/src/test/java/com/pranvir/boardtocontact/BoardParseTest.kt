package com.pranvir.boardtocontact

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardParseTest {

    @Test fun phone_plainTenDigit() {
        assertEquals(listOf("98765 43210"), BoardParse.extractPhones("Call 9876543210"))
    }

    @Test fun phone_withSpacesAndPrefix() {
        assertEquals(
            listOf("+91 98765 43210"),
            BoardParse.extractPhones("Mob: +91 98765 43210"),
        )
    }

    @Test fun phone_zeroPrefix() {
        assertEquals(listOf("98765 43210"), BoardParse.extractPhones("09876543210"))
    }

    @Test fun phone_ignoresShortAndLandline() {
        assertTrue(BoardParse.extractPhones("Ph: 12345").isEmpty())
        assertTrue(BoardParse.extractPhones("Tel: 080 4123 4567").isEmpty())
    }

    @Test fun phone_dedupesRepeats() {
        assertEquals(
            listOf("98765 43210"),
            BoardParse.extractPhones("98765 43210 or 98765-43210"),
        )
    }

    @Test fun phone_twoNumbers() {
        assertEquals(
            listOf("98765 43210", "91234 56780"),
            BoardParse.extractPhones("9876543210, 9123456780"),
        )
    }

    @Test fun name_firstAlphaLine() {
        val lines = listOf("SHARMA GENERAL STORE", "Main Road, Nashik", "98765 43210")
        assertEquals("SHARMA GENERAL STORE", BoardParse.guessName(lines))
    }

    @Test fun name_skipsPhoneOnlyLines() {
        val lines = listOf("98765 43210", "GUPTA SWEETS")
        assertEquals("GUPTA SWEETS", BoardParse.guessName(lines))
    }

    @Test fun name_emptyWhenNothing() {
        assertEquals("", BoardParse.guessName(listOf("", "  ")))
    }

    @Test fun address_picksRoadAndPin() {
        val lines = listOf(
            "SHARMA GENERAL STORE",
            "12 Main Road, Nashik 422001",
            "98765 43210",
        )
        assertEquals("12 Main Road, Nashik 422001", BoardParse.extractAddress(lines))
    }

    @Test fun address_skipsNameAndPhone() {
        val lines = listOf("SHARMA GENERAL STORE", "98765 43210")
        assertEquals("", BoardParse.extractAddress(lines))
    }

    @Test fun parse_fullBoard() {
        val lines = listOf(
            "GUPTA SWEETS",
            "Near City Chowk, MG Road",
            "Mob +91 91234 56780",
        )
        val result = BoardParse.parse(lines.joinToString("\n"), lines)
        assertEquals("GUPTA SWEETS", result.name)
        assertEquals(listOf("+91 91234 56780"), result.phones)
        assertEquals("Near City Chowk, MG Road", result.address)
    }
}
