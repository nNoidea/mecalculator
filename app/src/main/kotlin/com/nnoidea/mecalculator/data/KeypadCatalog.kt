package com.nnoidea.mecalculator.data

enum class KeyCategory(val title: String) {
    NUMBERS("Numbers"),
    OPERATORS("Operators"),
    SCIENTIFIC("Scientific"),
    NAVIGATION("Navigation"),
}

enum class KeyColorRole {
    NUMBER,
    OPERATOR,
    ACTION,
    TERTIARY,
    EMPTY,
}

data class KeyDefinition(
    val id: String,
    val label: String,
    val category: KeyCategory,
) {
    val colorRole: KeyColorRole
        get() = KeypadCatalog.colorRoleFor(id)
}

sealed interface KeypadSlot {
    data class Key(val keyId: String) : KeypadSlot
    data class Folder(
        val title: String,
        val keyIds: List<String>, // 24 slots (4x6 grid); "" = empty slot
    ) : KeypadSlot
}

object KeypadCatalog {
    const val FOLDER_KEY_ID = "__FOLDER__"

    val allKeys: List<KeyDefinition> = listOf(
        // Numbers (1 2 3 4 5 6 7 8 9 0 .)
        KeyDefinition("1", "1", KeyCategory.NUMBERS),
        KeyDefinition("2", "2", KeyCategory.NUMBERS),
        KeyDefinition("3", "3", KeyCategory.NUMBERS),
        KeyDefinition("4", "4", KeyCategory.NUMBERS),
        KeyDefinition("5", "5", KeyCategory.NUMBERS),
        KeyDefinition("6", "6", KeyCategory.NUMBERS),
        KeyDefinition("7", "7", KeyCategory.NUMBERS),
        KeyDefinition("8", "8", KeyCategory.NUMBERS),
        KeyDefinition("9", "9", KeyCategory.NUMBERS),
        KeyDefinition("0", "0", KeyCategory.NUMBERS),
        KeyDefinition(".", ".", KeyCategory.NUMBERS),
        // Operators
        KeyDefinition("+", "+", KeyCategory.OPERATORS),
        KeyDefinition("−", "−", KeyCategory.OPERATORS),
        KeyDefinition("×", "×", KeyCategory.OPERATORS),
        KeyDefinition("÷", "÷", KeyCategory.OPERATORS),
        KeyDefinition("xʸ", "xʸ", KeyCategory.OPERATORS),
        KeyDefinition("(", "(", KeyCategory.OPERATORS),
        KeyDefinition(")", ")", KeyCategory.OPERATORS),
        KeyDefinition("=", "=", KeyCategory.OPERATORS),
        KeyDefinition("%", "%", KeyCategory.OPERATORS),
        // Scientific
        KeyDefinition("√", "√", KeyCategory.SCIENTIFIC),
        KeyDefinition("sin", "sin", KeyCategory.SCIENTIFIC),
        KeyDefinition("cos", "cos", KeyCategory.SCIENTIFIC),
        KeyDefinition("tan", "tan", KeyCategory.SCIENTIFIC),
        KeyDefinition("log", "log", KeyCategory.SCIENTIFIC),
        KeyDefinition("ln", "ln", KeyCategory.SCIENTIFIC),
        KeyDefinition("π", "π", KeyCategory.SCIENTIFIC),
        KeyDefinition("e", "e", KeyCategory.SCIENTIFIC),
        KeyDefinition("asin", "asin", KeyCategory.SCIENTIFIC),
        KeyDefinition("acos", "acos", KeyCategory.SCIENTIFIC),
        KeyDefinition("atan", "atan", KeyCategory.SCIENTIFIC),
        KeyDefinition("abs", "abs", KeyCategory.SCIENTIFIC),
        KeyDefinition("fact", "n!", KeyCategory.SCIENTIFIC),
        // Navigation, Folder & Empty
        KeyDefinition("⌫", "⌫", KeyCategory.NAVIGATION),
        KeyDefinition("▲", "▲", KeyCategory.NAVIGATION),
        KeyDefinition("▼", "▼", KeyCategory.NAVIGATION),
        KeyDefinition("◀", "◀", KeyCategory.NAVIGATION),
        KeyDefinition("▶", "▶", KeyCategory.NAVIGATION),
        KeyDefinition(FOLDER_KEY_ID, "Folder", KeyCategory.NAVIGATION),
        KeyDefinition("", "", KeyCategory.NAVIGATION),
    )

    fun labelFor(keyId: String): String =
        if (keyId.isEmpty()) "" else (allKeys.find { it.id == keyId }?.label ?: keyId)

    fun colorRoleFor(keyId: String, isFolder: Boolean = false): KeyColorRole = when {
        isFolder || keyId == FOLDER_KEY_ID || keyId == "⌫" || keyId == "✕" -> KeyColorRole.TERTIARY
        keyId == "=" || keyId == "OK" -> KeyColorRole.ACTION
        keyId.isEmpty() -> KeyColorRole.EMPTY
        keyId.length == 1 && (keyId[0].isDigit() || keyId[0] == '.') -> KeyColorRole.NUMBER
        else -> KeyColorRole.OPERATOR
    }

    val defaultSlots: List<KeypadSlot> = listOf(
        // Row 1
        KeypadSlot.Key("("),
        KeypadSlot.Key(")"),
        KeypadSlot.Key("xʸ"),
        KeypadSlot.Key("÷"),
        // Row 2
        KeypadSlot.Key("7"),
        KeypadSlot.Key("8"),
        KeypadSlot.Key("9"),
        KeypadSlot.Key("×"),
        // Row 3
        KeypadSlot.Key("4"),
        KeypadSlot.Key("5"),
        KeypadSlot.Key("6"),
        KeypadSlot.Key("−"),
        // Row 4
        KeypadSlot.Key("1"),
        KeypadSlot.Key("2"),
        KeypadSlot.Key("3"),
        KeypadSlot.Key("+"),
        // Row 5
        KeypadSlot.Key("0"),
        KeypadSlot.Key("."),
        KeypadSlot.Key("▲"),
        KeypadSlot.Key("="),
        // Row 6
        KeypadSlot.Key("⌫"),
        KeypadSlot.Key("◀"),
        KeypadSlot.Key("▼"),
        KeypadSlot.Key("▶"),
    )

    // Every folder starts completely EMPTY
    val defaultFolderKeys: List<String> = List(24) { "" }
}
