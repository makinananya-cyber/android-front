package com.hisaabwise.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Mirrors the iOS TypedAmount behaviour. */
class TypedAmountTest {
    @Test fun wholeNumbers() {
        assertEquals(50000L, TypedAmount.minor("500", 2))
        assertEquals(500L, TypedAmount.minor("500", 0))
        assertEquals(500000L, TypedAmount.minor("500", 3))
    }

    @Test fun decimalSeparatorIsTheLastDotOrComma() {
        assertEquals(1250L, TypedAmount.minor("12.5", 2))
        assertEquals(1250L, TypedAmount.minor("12,50", 2))
        assertEquals(1234L, TypedAmount.minor("12.34", 2))
    }

    @Test fun groupedThousands() {
        assertEquals(125000000L, TypedAmount.minor("1,250,000", 2))
        assertEquals(125000000L, TypedAmount.minor("1.250.000", 2))
        assertNull(TypedAmount.minor("12,3456", 2))
        assertNull(TypedAmount.minor("1234,567", 2))
    }

    @Test fun otherScriptsDigits() {
        assertEquals(50000L, TypedAmount.minor("٥٠٠", 2)) // Arabic-Indic
        assertEquals(50000L, TypedAmount.minor("५००", 2)) // Devanagari
    }

    @Test fun zeroEmptyAndOverflowAreNull() {
        assertNull(TypedAmount.minor("", 2))
        assertNull(TypedAmount.minor("0", 2))
        assertNull(TypedAmount.minor("abc", 2))
        assertNull(TypedAmount.minor("99999999999999999999", 2))
    }

    @Test fun majorFormatsWithoutAFormatter() {
        assertEquals("1250", TypedAmount.major(125000, 2))
        assertEquals("12.50", TypedAmount.major(1250, 2))
        assertEquals("12.05", TypedAmount.major(1205, 2))
        assertEquals("500", TypedAmount.major(500, 0))
    }
}

class PasswordStrengthTest {
    @Test fun scores() {
        assertEquals(PasswordStrength.None, PasswordStrength.of(""))
        assertEquals(PasswordStrength.Weak, PasswordStrength.of("abc"))
        assertEquals(PasswordStrength.Weak, PasswordStrength.of("abcdefgh"))
        assertEquals(PasswordStrength.Fair, PasswordStrength.of("abcdefghijkl"))
        assertEquals(PasswordStrength.Good, PasswordStrength.of("Abcdefghijkl"))
        assertEquals(PasswordStrength.Strong, PasswordStrength.of("Abcdefghij1!"))
    }
}
