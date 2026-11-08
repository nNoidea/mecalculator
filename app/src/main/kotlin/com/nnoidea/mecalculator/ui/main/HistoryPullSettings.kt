package com.nnoidea.mecalculator.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nnoidea.mecalculator.R

@Composable
internal fun ExpressivePullSettingsButton(
    height: Dp,
    isSnapped: Boolean,
    contentAlpha: Float,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fixedWidth = 185.dp
    val cornerRadius = (height / 2f).coerceAtMost(36.dp)

    Surface(
        modifier = modifier
            .width(fixedWidth)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(onClick = onOpenSettings),
        shape = RoundedCornerShape(cornerRadius),
        color = if (isSnapped) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
        },
        contentColor = if (isSnapped) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        tonalElevation = if (isSnapped) 6.dp else 2.dp,
        shadowElevation = if (isSnapped) 6.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = contentAlpha },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = null,
                modifier = Modifier.size(26.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }
    }
}
