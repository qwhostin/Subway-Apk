package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AccessoryType
import com.example.data.model.CharacterAppearance
import com.example.data.model.HairStyle
import com.example.data.model.OutfitStyle
import com.example.data.model.ShoeStyle
import com.example.data.model.TrailEffect
import com.example.game3d.Character3DTurntable
import com.example.ui.RunvantaUiState
import com.example.ui.theme.CyberGold
import com.example.ui.theme.DeepMidnight
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HotCoral
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MintEmerald
import kotlinx.coroutines.launch
import java.util.UUID

private val skinTones = listOf(0xFFDBACL, 0xF1C27DL, 0xE0AC69L, 0xC68642L, 0x8D5524L, 0x5C381AL)
private val swatchColors = listOf(
    0x00E5FFL, 0xFF4D6DL, 0xFFD166L, 0x06D6A0L, 0x7B61FFL, 0xFF6B35L, 0xFFFFFFL, 0x1E293BL
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterStudioScreen(
    state: RunvantaUiState,
    editingCharacter: CharacterAppearance?,
    onLoadPhoto: suspend (android.net.Uri) -> Bitmap?,
    onAcceptPhotoNotice: () -> Unit,
    onUnlockCosmetic: (key: String, cost: Int, label: String, onDone: () -> Unit) -> Unit,
    onSaveCharacter: (CharacterAppearance, Bitmap?, () -> Unit) -> Unit,
    onDeleteCharacter: (CharacterAppearance) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showConsentDialog by remember { mutableStateOf(false) }

    var draft by remember(editingCharacter) {
        mutableStateOf(
            editingCharacter?.copy(
                id = if (editingCharacter.isDefault) "custom_${UUID.randomUUID()}" else editingCharacter.id,
                isDefault = false,
                name = if (editingCharacter.isDefault) "${editingCharacter.name} Custom" else editingCharacter.name
            ) ?: CharacterAppearance(
                id = "custom_${UUID.randomUUID()}",
                name = "My Hero",
                isDefault = false
            )
        )
    }

    var pickedSourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var liveCroppedPreview by remember { mutableStateOf<Bitmap?>(null) }

    // Load existing face texture when editing an existing custom character
    LaunchedEffect(draft.faceImagePath) {
        val path = draft.faceImagePath
        if (pickedSourceBitmap == null && !path.isNullOrBlank()) {
            pickedSourceBitmap = runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
        }
    }

    // Re-bake live preview bitmap whenever face framing or skin tone changes
    LaunchedEffect(pickedSourceBitmap, draft.faceScale, draft.faceOffsetX, draft.faceOffsetY, draft.skinColorRgb) {
        val src = pickedSourceBitmap ?: return@LaunchedEffect
        val size = 192
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        val bgPaint = android.graphics.Paint().apply {
            color = (0xFF000000L or (draft.skinColorRgb and 0xFFFFFFL)).toInt()
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)
        val path = android.graphics.Path().apply {
            addOval(
                android.graphics.RectF(size * 0.08f, size * 0.06f, size * 0.92f, size * 0.94f),
                android.graphics.Path.Direction.CW
            )
        }
        canvas.clipPath(path)
        val baseScale = maxOf(size.toFloat() / src.width, size.toFloat() / src.height)
        val finalScale = baseScale * draft.faceScale
        val dw = src.width * finalScale
        val dh = src.height * finalScale
        val left = (size - dw) / 2f + draft.faceOffsetX * size * 0.35f
        val top = (size - dh) / 2f + draft.faceOffsetY * size * 0.35f
        canvas.drawBitmap(src, null, android.graphics.RectF(left, top, left + dw, top + dh), null)
        liveCroppedPreview = out
    }

    // Zero-permission Android System Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = onLoadPhoto(uri)
                if (bmp != null) {
                    pickedSourceBitmap = bmp
                    draft = draft.copy(faceScale = 1.0f, faceOffsetX = 0f, faceOffsetY = 0f)
                }
            }
        }
    }

    fun launchPhotoPickerWithNotice() {
        if (!state.photoNoticeAccepted) {
            showConsentDialog = true
        } else {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            icon = { Icon(Icons.Filled.PrivacyTip, contentDescription = null, tint = ElectricCyan) },
            title = { Text("Photo Permission & Local Privacy") },
            text = {
                Text(
                    "Play As Anyone lets you map a photo onto your 3D character.\n\n" +
                        "• Confirm you have permission before using a photo of another person.\n" +
                        "• Photos stay 100% on your device, are never uploaded to servers, and are never used for AI training.\n" +
                        "• Temporary files are deleted automatically, and you can delete saved characters anytime.\n" +
                        "• Abusive, harassing, or inappropriate impersonation is prohibited."
                )
            },
            confirmButton = {
                Button(onClick = {
                    onAcceptPhotoNotice()
                    showConsentDialog = false
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Text("I Understand & Agree")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConsentDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        containerColor = DeepMidnight,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("CHARACTER STUDIO", style = MaterialTheme.typography.headlineLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (editingCharacter != null && !editingCharacter.isDefault) {
                        IconButton(
                            onClick = {
                                onDeleteCharacter(editingCharacter)
                                onBack()
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete character", tint = HotCoral)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepMidnight,
                    titleContentColor = ElectricCyan,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Interactive 3D Character Preview Card
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.img_studio_banner),
                            contentDescription = "Studio stage backdrop",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.28f
                        )
                        Character3DTurntable(
                            appearance = draft,
                            liveFaceBitmap = liveCroppedPreview,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = "DRAG TO ROTATE 3D HERO",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                        )
                    }
                }

                // Upload Photo CTA & Face Crop Framing
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("PLAY AS ANYONE FACE MAP", style = MaterialTheme.typography.titleMedium, color = ElectricCyan)
                                Text(
                                    "On-device Photo Picker • Zero server uploads",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = { launchPhotoPickerWithNotice() },
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("upload_photo_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DeepMidnight)
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (pickedSourceBitmap == null) "Upload Photo" else "Replace")
                            }
                        }

                        if (pickedSourceBitmap != null) {
                            val src = pickedSourceBitmap!!
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Interactive Drag-to-Position Face Cropper
                                Box(
                                    modifier = Modifier
                                        .size(132.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(draft.skinColorRgb or 0xFF000000L))
                                        .pointerInput(Unit) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                draft = draft.copy(
                                                    faceOffsetX = (draft.faceOffsetX + dragAmount.x / 180f).coerceIn(-1f, 1f),
                                                    faceOffsetY = (draft.faceOffsetY + dragAmount.y / 180f).coerceIn(-1f, 1f)
                                                )
                                            }
                                        }
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val img = src.asImageBitmap()
                                        val base = maxOf(size.width / img.width, size.height / img.height)
                                        val finalScale = base * draft.faceScale
                                        val dw = (img.width * finalScale).toInt().coerceAtLeast(1)
                                        val dh = (img.height * finalScale).toInt().coerceAtLeast(1)
                                        val ox = ((size.width - dw) / 2f + draft.faceOffsetX * size.width * 0.35f).toInt()
                                        val oy = ((size.height - dh) / 2f + draft.faceOffsetY * size.height * 0.35f).toInt()
                                        drawImage(img, dstOffset = IntOffset(ox, oy), dstSize = IntSize(dw, dh))
                                        drawOval(
                                            color = ElectricCyan,
                                            topLeft = Offset(size.width * 0.08f, size.height * 0.06f),
                                            size = androidx.compose.ui.geometry.Size(size.width * 0.84f, size.height * 0.88f),
                                            style = Stroke(width = 3.dp.toPx())
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Face Zoom (${"%.1f".format(draft.faceScale)}x)", style = MaterialTheme.typography.labelLarge, color = Color.White)
                                    Slider(
                                        value = draft.faceScale,
                                        onValueChange = { draft = draft.copy(faceScale = it) },
                                        valueRange = 0.7f..2.5f
                                    )
                                    Text("Drag face box to center eyes & smile", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(onClick = {
                                        pickedSourceBitmap = null
                                        liveCroppedPreview = null
                                        draft = draft.copy(faceImagePath = null)
                                    }) {
                                        Text("Remove Photo", color = HotCoral)
                                    }
                                }
                            }
                        }
                    }
                }

                // Character Name
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { draft = draft.copy(name = it.take(24)) },
                    label = { Text("Character Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("studio_name_input")
                )

                // Skin Tone Selector
                ColorSwatchRow(
                    title = "Skin Appearance",
                    colors = skinTones,
                    selectedRgb = draft.skinColorRgb,
                    onSelect = { draft = draft.copy(skinColorRgb = it) }
                )

                // Outfit & Accent Colors
                ColorSwatchRow(
                    title = "Outfit Color",
                    colors = swatchColors,
                    selectedRgb = draft.outfitColorRgb,
                    onSelect = { draft = draft.copy(outfitColorRgb = it) }
                )
                ColorSwatchRow(
                    title = "Accent & Glow Color",
                    colors = swatchColors,
                    selectedRgb = draft.accentColorRgb,
                    onSelect = { draft = draft.copy(accentColorRgb = it) }
                )

                // Hairstyle Selector
                UnlockableOptionRow(
                    title = "Hairstyle & Headgear",
                    items = HairStyle.entries,
                    selected = draft.hairStyle,
                    unlockedIds = state.unlockedIds,
                    keyPrefix = "hair",
                    labelOf = { it.label },
                    costOf = { it.unlockCoins },
                    onSelectOrUnlock = { style ->
                        val key = "hair:${style.name}"
                        if (style.unlockCoins == 0 || key in state.unlockedIds) {
                            draft = draft.copy(hairStyle = style)
                        } else {
                            onUnlockCosmetic(key, style.unlockCoins, style.label) {
                                draft = draft.copy(hairStyle = style)
                            }
                        }
                    }
                )

                ColorSwatchRow(
                    title = "Hair Color",
                    colors = swatchColors,
                    selectedRgb = draft.hairColorRgb,
                    onSelect = { draft = draft.copy(hairColorRgb = it) }
                )

                // Outfit Style
                UnlockableOptionRow(
                    title = "Outfit Style",
                    items = OutfitStyle.entries,
                    selected = draft.outfitStyle,
                    unlockedIds = state.unlockedIds,
                    keyPrefix = "outfit",
                    labelOf = { it.label },
                    costOf = { it.unlockCoins },
                    onSelectOrUnlock = { style ->
                        val key = "outfit:${style.name}"
                        if (style.unlockCoins == 0 || key in state.unlockedIds) {
                            draft = draft.copy(outfitStyle = style)
                        } else {
                            onUnlockCosmetic(key, style.unlockCoins, style.label) {
                                draft = draft.copy(outfitStyle = style)
                            }
                        }
                    }
                )

                // Shoes
                UnlockableOptionRow(
                    title = "Footwear",
                    items = ShoeStyle.entries,
                    selected = draft.shoeStyle,
                    unlockedIds = state.unlockedIds,
                    keyPrefix = "shoe",
                    labelOf = { it.label },
                    costOf = { it.unlockCoins },
                    onSelectOrUnlock = { style ->
                        val key = "shoe:${style.name}"
                        if (style.unlockCoins == 0 || key in state.unlockedIds) {
                            draft = draft.copy(shoeStyle = style)
                        } else {
                            onUnlockCosmetic(key, style.unlockCoins, style.label) {
                                draft = draft.copy(shoeStyle = style)
                            }
                        }
                    }
                )

                // Accessories
                UnlockableOptionRow(
                    title = "Accessories",
                    items = AccessoryType.entries,
                    selected = draft.accessory,
                    unlockedIds = state.unlockedIds,
                    keyPrefix = "acc",
                    labelOf = { it.label },
                    costOf = { it.unlockCoins },
                    onSelectOrUnlock = { item ->
                        val key = "acc:${item.name}"
                        if (item.unlockCoins == 0 || key in state.unlockedIds) {
                            draft = draft.copy(accessory = item)
                        } else {
                            onUnlockCosmetic(key, item.unlockCoins, item.label) {
                                draft = draft.copy(accessory = item)
                            }
                        }
                    }
                )

                // Trail Effect
                UnlockableOptionRow(
                    title = "Sprint Trail Effect",
                    items = TrailEffect.entries,
                    selected = draft.trailEffect,
                    unlockedIds = state.unlockedIds,
                    keyPrefix = "trail",
                    labelOf = { it.label },
                    costOf = { it.unlockCoins },
                    onSelectOrUnlock = { item ->
                        val key = "trail:${item.name}"
                        if (item.unlockCoins == 0 || key in state.unlockedIds) {
                            draft = draft.copy(trailEffect = item)
                        } else {
                            onUnlockCosmetic(key, item.unlockCoins, item.label) {
                                draft = draft.copy(trailEffect = item)
                            }
                        }
                    }
                )

                // Save Button
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f).height(54.dp)
                    ) {
                        Text("CANCEL")
                    }
                    Button(
                        onClick = {
                            onSaveCharacter(draft, pickedSourceBitmap) {
                                onBack()
                            }
                        },
                        modifier = Modifier.weight(2f).height(54.dp).testTag("save_character_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MintEmerald, contentColor = DeepMidnight)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("SAVE & EQUIP HERO", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchRow(
    title: String,
    colors: List<Long>,
    selectedRgb: Long,
    onSelect: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelLarge, color = Color.White)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            colors.forEach { rgb ->
                val isSelected = rgb == selectedRgb
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(rgb or 0xFF000000L))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) ElectricCyan else Color.White.copy(alpha = 0.25f),
                            shape = CircleShape
                        )
                        .clickable { onSelect(rgb) }
                )
            }
        }
    }
}

@Composable
private fun <T> UnlockableOptionRow(
    title: String,
    items: List<T>,
    selected: T,
    unlockedIds: Set<String>,
    keyPrefix: String,
    labelOf: (T) -> String,
    costOf: (T) -> Int,
    onSelectOrUnlock: (T) -> Unit
) where T : Enum<T> {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelLarge, color = Color.White)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(items) { item ->
                val cost = costOf(item)
                val unlocked = cost == 0 || "$keyPrefix:${item.name}" in unlockedIds
                val isSelected = item == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectOrUnlock(item) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!unlocked) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "Locked",
                                    tint = CyberGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(if (unlocked) labelOf(item) else "${labelOf(item)} ($cost)")
                        }
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
            }
        }
    }
}
