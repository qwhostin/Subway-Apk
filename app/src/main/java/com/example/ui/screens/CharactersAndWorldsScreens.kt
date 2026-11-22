package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CharacterAppearance
import com.example.data.model.EnvironmentTheme
import com.example.data.model.EnvironmentsCatalog
import com.example.data.model.formatCoinDisplay
import com.example.game3d.Character3DTurntable
import com.example.ui.RunvantaUiState
import com.example.ui.theme.CyberGold
import com.example.ui.theme.DeepMidnight
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HotCoral
import com.example.ui.theme.MidnightCard
import com.example.ui.theme.MintEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharactersScreen(
    state: RunvantaUiState,
    onSelectOrUnlock: (CharacterAppearance) -> Unit,
    onEditInStudio: (CharacterAppearance?) -> Unit,
    onRenameCharacter: (CharacterAppearance, String) -> Unit,
    onDeleteCustom: (CharacterAppearance) -> Unit,
    onBack: () -> Unit
) {
    var previewed by remember(state.selectedCharacter) { mutableStateOf(state.selectedCharacter) }
    var renameTarget by remember { mutableStateOf<CharacterAppearance?>(null) }
    var renameInput by remember { mutableStateOf("") }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Character") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it.take(24) },
                    label = { Text("Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    renameTarget?.let { onRenameCharacter(it, renameInput) }
                    renameTarget = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        containerColor = DeepMidnight,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("CHARACTERS", style = MaterialTheme.typography.headlineLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Surface(color = MidnightCard, shape = RoundedCornerShape(50)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = CyberGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(formatCoinDisplay(state.coins), style = MaterialTheme.typography.labelLarge, color = CyberGold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                // Live 3D Turntable for currently inspected character
                Card(
                    modifier = Modifier.fillMaxWidth().height(210.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Character3DTurntable(
                            appearance = previewed,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            color = DeepMidnight.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                        ) {
                            Text(
                                text = previewed.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = ElectricCyan,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = { onEditInStudio(null) },
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("create_custom_char_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = DeepMidnight)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("CREATE NEW PHOTO HERO IN STUDIO", style = MaterialTheme.typography.labelLarge)
                }

                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(state.characters, key = { it.id }) { char ->
                        val isUnlocked = !char.isDefault || char.unlockCoins == 0 || "char:${char.id}" in state.unlockedIds
                        val isSelected = state.selectedCharacter.id == char.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { previewed = char },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MidnightCard),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, ElectricCyan) else null
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(char.outfitColorRgb or 0xFF000000L))
                                        .border(2.dp, Color(char.accentColorRgb or 0xFF000000L), CircleShape)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = char.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (!char.isDefault) "Custom Studio Hero" else "${char.outfitStyle.label} • ${char.hairStyle.label}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (!char.isDefault) {
                                    IconButton(onClick = {
                                        renameTarget = char
                                        renameInput = char.name
                                    }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Rename", tint = ElectricCyan)
                                    }
                                    IconButton(onClick = { onDeleteCustom(char) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = HotCoral)
                                    }
                                }

                                Button(
                                    onClick = {
                                        previewed = char
                                        onSelectOrUnlock(char)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when {
                                            isSelected -> MintEmerald
                                            isUnlocked -> ElectricCyan
                                            else -> CyberGold
                                        },
                                        contentColor = DeepMidnight
                                    ),
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    if (!isUnlocked) {
                                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("${char.unlockCoins}")
                                    } else if (isSelected) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("ACTIVE")
                                    } else {
                                        Text("SELECT")
                                    }
                                }
                            }
                            if (!char.isDefault || isUnlocked) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp)
                                        .padding(bottom = 10.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(onClick = { onEditInStudio(char) }) {
                                        Text(if (char.isDefault) "Customize Copy in Studio" else "Edit in Studio")
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentsScreen(
    state: RunvantaUiState,
    onSelectOrUnlockEnvironment: (EnvironmentTheme) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = DeepMidnight,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("ENVIRONMENTS", style = MaterialTheme.typography.headlineLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Surface(color = MidnightCard, shape = RoundedCornerShape(50)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = CyberGold, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(formatCoinDisplay(state.coins), style = MaterialTheme.typography.labelLarge, color = CyberGold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepMidnight,
                    titleContentColor = MintEmerald,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(EnvironmentsCatalog.all, key = { it.id }) { env ->
                val isUnlocked = env.unlockCoins == 0 || "env:${env.id}" in state.unlockedIds
                val isSelected = state.selectedEnvironment.id == env.id

                Card(
                    modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MidnightCard),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, ElectricCyan) else null
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(env.skyTopRgb or 0xFF000000L),
                                            Color(env.skyBottomRgb or 0xFF000000L),
                                            Color(env.railAccentRgb or 0xFF000000L)
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = env.name.uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(env.tagline, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                Text(
                                    "Tempo ${env.baseTempoBpm} BPM • Unique 3D obstacles & lighting",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Button(
                                onClick = { onSelectOrUnlockEnvironment(env) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        isSelected -> MintEmerald
                                        isUnlocked -> ElectricCyan
                                        else -> CyberGold
                                    },
                                    contentColor = DeepMidnight
                                ),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                if (!isUnlocked) {
                                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("${env.unlockCoins}")
                                } else if (isSelected) {
                                    Text("SELECTED")
                                } else {
                                    Text("SELECT")
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
