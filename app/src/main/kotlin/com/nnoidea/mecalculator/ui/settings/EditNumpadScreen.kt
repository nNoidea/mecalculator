package com.nnoidea.mecalculator.ui.settings

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nnoidea.mecalculator.data.AppSettingsRepository
import com.nnoidea.mecalculator.data.KeyCategory
import com.nnoidea.mecalculator.data.KeypadCatalog
import com.nnoidea.mecalculator.data.KeypadSlot
import com.nnoidea.mecalculator.ui.main.keyColorsFor
import com.nnoidea.mecalculator.ui.util.HapticHelper

private val BackIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "AutoMirrored.Filled.ArrowBack",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = true,
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(20f, 11f)
            horizontalLineTo(7.83f)
            lineToRelative(5.59f, -5.59f)
            lineTo(12f, 4f)
            lineToRelative(-8f, 8f)
            lineToRelative(8f, 8f)
            lineToRelative(1.41f, -1.41f)
            lineTo(7.83f, 13f)
            horizontalLineTo(20f)
            verticalLineToRelative(-2f)
            close()
        }
    }.build()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditNumpadScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(Unit) {
        AppSettingsRepository.init(context)
    }
    val settings by AppSettingsRepository.settings.collectAsState()
    val slots = settings.keypadSlots

    // null = editing main 4x6 numpad; 0..23 = editing the 4x6 grid inside that Folder slot
    var editingFolderSlotIndex by remember { mutableStateOf<Int?>(null) }
    var selectedSlotIndex by remember { mutableIntStateOf(0) }

    val activeFolder = editingFolderSlotIndex?.let { slots.getOrNull(it) as? KeypadSlot.Folder }
    if (editingFolderSlotIndex != null && activeFolder == null) {
        editingFolderSlotIndex = null
    }

    BackHandler(enabled = editingFolderSlotIndex != null) {
        selectedSlotIndex = editingFolderSlotIndex ?: 0
        editingFolderSlotIndex = null
    }

    fun firstNeighborSlot(folderIdx: Int): Int {
        val r = folderIdx / 4
        val c = folderIdx % 4
        return when {
            c > 0 -> r * 4 + (c - 1)
            c < 3 -> r * 4 + (c + 1)
            r > 0 -> (r - 1) * 4 + c
            else -> (r + 1).coerceAtMost(5) * 4 + c
        }
    }

    fun assignKeyToSelectedSlot(keyId: String) {
        val folderIdx = editingFolderSlotIndex
        if (keyId == KeypadCatalog.FOLDER_KEY_ID) {
            if (folderIdx != null) return // Never allow folders inside folders
            val targetIdx = selectedSlotIndex
            val existing = slots.getOrNull(targetIdx) as? KeypadSlot.Folder
            if (existing == null) {
                val mutSlots = slots.toMutableList()
                mutSlots[targetIdx] = KeypadSlot.Folder(
                    title = "ƒ(x)",
                    keyIds = KeypadCatalog.defaultFolderKeys,
                )
                AppSettingsRepository.setKeypadSlots(context, mutSlots)
            }
            editingFolderSlotIndex = targetIdx
            selectedSlotIndex = firstNeighborSlot(targetIdx)
            return
        }

        if (folderIdx != null) {
            if (selectedSlotIndex == folderIdx) {
                // Replacing the folder button itself with a regular key exits folder mode
                val mutSlots = slots.toMutableList()
                mutSlots[folderIdx] = KeypadSlot.Key(keyId)
                AppSettingsRepository.setKeypadSlots(context, mutSlots)
                editingFolderSlotIndex = null
                selectedSlotIndex = folderIdx
            } else {
                val folder = slots.getOrNull(folderIdx) as? KeypadSlot.Folder ?: return
                val mutKeys = folder.keyIds.toMutableList()
                mutKeys[selectedSlotIndex] = keyId
                val mutSlots = slots.toMutableList()
                mutSlots[folderIdx] = folder.copy(keyIds = mutKeys)
                AppSettingsRepository.setKeypadSlots(context, mutSlots)
            }
        } else {
            val mutSlots = slots.toMutableList()
            mutSlots[selectedSlotIndex] = KeypadSlot.Key(keyId)
            AppSettingsRepository.setKeypadSlots(context, mutSlots)
        }
    }

    fun updateFolderTitle(newTitle: String) {
        val folderIdx = editingFolderSlotIndex ?: return
        val folder = slots.getOrNull(folderIdx) as? KeypadSlot.Folder ?: return
        val mutSlots = slots.toMutableList()
        mutSlots[folderIdx] = folder.copy(title = newTitle.take(8))
        AppSettingsRepository.setKeypadSlots(context, mutSlots)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.systemBars
                    .union(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                title = {
                    Text(
                        text = if (activeFolder != null) {
                            "Folder: ${activeFolder.title}"
                        } else {
                            "Edit Numpad"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    Surface(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                HapticHelper.performClick(view)
                                if (editingFolderSlotIndex != null) {
                                    selectedSlotIndex = editingFolderSlotIndex!!
                                    editingFolderSlotIndex = null
                                } else {
                                    onBack()
                                }
                            },
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = BackIcon,
                                contentDescription = "Back",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                HapticHelper.performHeavyClick(view)
                                val folderIdx = editingFolderSlotIndex
                                if (folderIdx != null) {
                                    val folder = slots.getOrNull(folderIdx) as? KeypadSlot.Folder
                                    if (folder != null) {
                                        val mutSlots = slots.toMutableList()
                                        mutSlots[folderIdx] = folder.copy(
                                            keyIds = KeypadCatalog.defaultFolderKeys,
                                        )
                                        AppSettingsRepository.setKeypadSlots(context, mutSlots)
                                    }
                                } else {
                                    AppSettingsRepository.resetKeypadSlots(context)
                                }
                            },
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            text = "Reset",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val cutoutStart = innerPadding.calculateStartPadding(layoutDirection)
        val cutoutEnd = innerPadding.calculateEndPadding(layoutDirection)
        val topPadding = innerPadding.calculateTopPadding()
        val bottomPadding = innerPadding.calculateBottomPadding()

        val gridPane: @Composable (Modifier) -> Unit = { paneModifier ->
            Column(
                modifier = paneModifier,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (row in 0 until 6) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        for (col in 0 until 4) {
                            val idx = row * 4 + col
                            val isSelected = selectedSlotIndex == idx
                            val isFolderOriginSlot = editingFolderSlotIndex == idx
                            val isFolderOnMain = activeFolder == null && slots.getOrNull(idx) is KeypadSlot.Folder

                            val slotKeyId = if (activeFolder != null) {
                                if (isFolderOriginSlot) "✕" else activeFolder.keyIds.getOrElse(idx) { "" }
                            } else {
                                when (val s = slots.getOrElse(idx) { KeypadSlot.Key("") }) {
                                    is KeypadSlot.Key -> s.keyId
                                    is KeypadSlot.Folder -> KeypadCatalog.FOLDER_KEY_ID
                                }
                            }

                            val isFolder = isFolderOriginSlot || isFolderOnMain || slotKeyId == KeypadCatalog.FOLDER_KEY_ID
                            val isEmptySlot = slotKeyId.isEmpty()

                            val slotDisplay = if (activeFolder != null) {
                                if (isFolderOriginSlot) {
                                    "✕"
                                } else {
                                    val kid = activeFolder.keyIds.getOrElse(idx) { "" }
                                    KeypadCatalog.labelFor(kid)
                                }
                            } else {
                                when (val s = slots.getOrElse(idx) { KeypadSlot.Key("") }) {
                                    is KeypadSlot.Key -> KeypadCatalog.labelFor(s.keyId)
                                    is KeypadSlot.Folder -> s.title
                                }
                            }

                            val baseColors = keyColorsFor(slotKeyId, isFolder = isFolder)

                            val bg by animateColorAsState(
                                targetValue = if (isEmptySlot) {
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
                                } else {
                                    if (isSelected) {
                                        lerp(baseColors.containerColor, baseColors.contentColor, 0.18f)
                                    } else {
                                        baseColors.containerColor
                                    }
                                },
                                label = "editSlotBg",
                            )
                            val borderCol by animateColorAsState(
                                targetValue = if (isSelected) {
                                    if (baseColors.containerColor == MaterialTheme.colorScheme.primary) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                } else if (isEmptySlot) {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                } else {
                                    Color.Transparent
                                },
                                label = "editSlotBorder",
                            )
                            val borderWidth by animateDpAsState(
                                targetValue = if (isSelected) 3.dp else if (isEmptySlot) 1.dp else 0.dp,
                                label = "editSlotBorderWidth",
                            )
                            val cornerPercent by animateIntAsState(
                                targetValue = if (isSelected) 24 else 50,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "editSlotCornerPercent",
                            )
                            val shape = RoundedCornerShape(percent = cornerPercent)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(shape)
                                    .background(bg)
                                    .border(
                                        width = borderWidth,
                                        color = borderCol,
                                        shape = shape,
                                    )
                                    .clickable {
                                        HapticHelper.performClick(view)
                                        if (activeFolder == null && slots.getOrNull(idx) is KeypadSlot.Folder) {
                                            editingFolderSlotIndex = idx
                                            selectedSlotIndex = firstNeighborSlot(idx)
                                        } else {
                                            selectedSlotIndex = idx
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = slotDisplay,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isFolderOnMain) 16.sp else 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isEmptySlot && isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        baseColors.contentColor
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        val pickerPane: @Composable (Modifier) -> Unit = { paneModifier ->
            Column(
                modifier = paneModifier
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (activeFolder != null) {
                    OutlinedTextField(
                        value = activeFolder.title,
                        onValueChange = { updateFolderTitle(it) },
                        label = { Text("Folder Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                KeyCategory.entries.forEach { category ->
                    val categoryKeys = remember(category, editingFolderSlotIndex) {
                        KeypadCatalog.allKeys.filter { keyDef ->
                            keyDef.category == category &&
                                (editingFolderSlotIndex == null || keyDef.id != KeypadCatalog.FOLDER_KEY_ID)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = category.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                categoryKeys.forEach { keyDef ->
                                    val isFolderChip = keyDef.id == KeypadCatalog.FOLDER_KEY_ID
                                    val isEmptyChip = keyDef.id.isEmpty()
                                    val chipColors = keyColorsFor(keyDef.id, isFolder = isFolderChip)
                                    val chipLabel = if (isEmptyChip) "Empty" else keyDef.label

                                    Box(
                                        modifier = Modifier
                                            .widthIn(min = 48.dp)
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(chipColors.containerColor)
                                            .then(
                                                if (isEmptyChip) {
                                                    Modifier.border(
                                                        width = 1.dp,
                                                        color = MaterialTheme.colorScheme.outlineVariant,
                                                        shape = RoundedCornerShape(50),
                                                    )
                                                } else {
                                                    Modifier
                                                },
                                            )
                                            .clickable {
                                                HapticHelper.performClick(view)
                                                assignKeyToSelectedSlot(keyDef.id)
                                            }
                                            .padding(horizontal = 14.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = chipLabel,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = chipColors.contentColor,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp + bottomPadding))
            }
        }

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = cutoutStart + 12.dp,
                        end = cutoutEnd + 12.dp,
                        top = topPadding + 6.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                gridPane(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(bottom = 6.dp + bottomPadding),
                )
                pickerPane(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = cutoutStart + 12.dp,
                        end = cutoutEnd + 12.dp,
                        top = topPadding + 6.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                gridPane(
                    Modifier
                        .fillMaxWidth()
                        .weight(0.95f),
                )
                pickerPane(
                    Modifier
                        .fillMaxWidth()
                        .weight(1.05f),
                )
            }
        }
    }
}
