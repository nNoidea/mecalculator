package com.nnoidea.mecalculator.ui.main

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.nnoidea.mecalculator.data.KeyColorRole
import com.nnoidea.mecalculator.data.KeypadCatalog

data class KeyColors(
    val containerColor: Color,
    val contentColor: Color,
)

@Composable
fun keyColorsForRole(role: KeyColorRole): KeyColors {
    val colorScheme = MaterialTheme.colorScheme
    return when (role) {
        KeyColorRole.NUMBER -> KeyColors(
            containerColor = colorScheme.surfaceContainerHigh,
            contentColor = colorScheme.onSurface,
        )
        KeyColorRole.OPERATOR -> KeyColors(
            containerColor = colorScheme.secondaryContainer,
            contentColor = colorScheme.onSecondaryContainer,
        )
        KeyColorRole.ACTION -> KeyColors(
            containerColor = colorScheme.primary,
            contentColor = colorScheme.onPrimary,
        )
        KeyColorRole.TERTIARY -> KeyColors(
            containerColor = colorScheme.tertiaryContainer,
            contentColor = colorScheme.onTertiaryContainer,
        )
        KeyColorRole.EMPTY -> KeyColors(
            containerColor = colorScheme.surfaceContainerLow,
            contentColor = colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun keyColorsFor(
    keyId: String,
    isFolder: Boolean = false,
): KeyColors = keyColorsForRole(KeypadCatalog.colorRoleFor(keyId, isFolder = isFolder))
