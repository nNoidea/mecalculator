package com.nnoidea.mecalculator.data

import org.junit.Assert.assertEquals
import org.junit.Test

class KeypadCatalogTest {

    @Test
    fun colorRoleFor_numbers_returnsNumberRole() {
        listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ".").forEach { keyId ->
            assertEquals("Key $keyId should be NUMBER", KeyColorRole.NUMBER, KeypadCatalog.colorRoleFor(keyId))
        }
    }

    @Test
    fun colorRoleFor_operators_returnsOperatorRole() {
        listOf("+", "-", "−", "×", "·", "÷", "xʸ", "(", ")", "%").forEach { keyId ->
            assertEquals("Key $keyId should be OPERATOR", KeyColorRole.OPERATOR, KeypadCatalog.colorRoleFor(keyId))
        }
    }

    @Test
    fun colorRoleFor_scientificAndConstants_returnsOperatorRole() {
        listOf("√", "sin", "cos", "tan", "log", "ln", "π", "e", "asin", "acos", "atan", "abs", "fact").forEach { keyId ->
            assertEquals("Key $keyId should be OPERATOR", KeyColorRole.OPERATOR, KeypadCatalog.colorRoleFor(keyId))
        }
    }

    @Test
    fun colorRoleFor_arrows_returnsOperatorRole() {
        listOf("▲", "▼", "◀", "▶").forEach { keyId ->
            assertEquals("Key $keyId should be OPERATOR", KeyColorRole.OPERATOR, KeypadCatalog.colorRoleFor(keyId))
        }
    }

    @Test
    fun colorRoleFor_action_returnsActionRole() {
        listOf("=", "OK").forEach { keyId ->
            assertEquals("Key $keyId should be ACTION", KeyColorRole.ACTION, KeypadCatalog.colorRoleFor(keyId))
        }
    }

    @Test
    fun colorRoleFor_tertiary_returnsTertiaryRole() {
        assertEquals("Backspace should be TERTIARY", KeyColorRole.TERTIARY, KeypadCatalog.colorRoleFor("⌫"))
        assertEquals("Close icon should be TERTIARY", KeyColorRole.TERTIARY, KeypadCatalog.colorRoleFor("✕"))
        assertEquals("Folder key ID should be TERTIARY", KeyColorRole.TERTIARY, KeypadCatalog.colorRoleFor(KeypadCatalog.FOLDER_KEY_ID))
        assertEquals("isFolder=true should be TERTIARY", KeyColorRole.TERTIARY, KeypadCatalog.colorRoleFor("anything", isFolder = true))
    }

    @Test
    fun colorRoleFor_empty_returnsEmptyRole() {
        assertEquals("Empty string should be EMPTY", KeyColorRole.EMPTY, KeypadCatalog.colorRoleFor(""))
    }

    @Test
    fun allKeysInCatalog_matchDeclaredColorRole() {
        KeypadCatalog.allKeys.forEach { keyDef ->
            val expected = KeypadCatalog.colorRoleFor(keyDef.id)
            assertEquals("KeyDefinition(${keyDef.id}).colorRole must match colorRoleFor", expected, keyDef.colorRole)
        }
    }
}
