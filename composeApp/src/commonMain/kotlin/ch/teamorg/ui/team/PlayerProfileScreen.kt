package ch.teamorg.ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import ch.teamorg.domain.TeamMember
import ch.teamorg.ui.util.rememberImagePickerLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileScreen(
    teamId: String,
    userId: String,
    viewModel: PlayerProfileViewModel,
    onBack: () -> Unit,
    onLeftTeam: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showJerseyDialog by remember { mutableStateOf(false) }
    var showPositionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.leftTeam) {
        if (state.leftTeam) onLeftTeam()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.member?.displayName ?: "Player Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.member == null -> Text(
                    "Player not found",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> {
                    val member = state.member!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Avatar
                        val pickImage = rememberImagePickerLauncher { bytes, ext ->
                            viewModel.uploadAvatar(teamId, userId, bytes, ext)
                        }
                        Box(
                            modifier = Modifier.size(96.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                    .then(if (state.isOwnProfile) Modifier.clickable { pickImage() } else Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                if (member.avatarUrl != null) {
                                    AsyncImage(
                                        model = member.avatarUrl,
                                        contentDescription = member.displayName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            if (state.isOwnProfile) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CameraAlt,
                                        contentDescription = "Upload avatar",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }

                        Text(
                            text = member.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        // Role badge
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = member.role.replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Info card
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Jersey number
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Jersey Number", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = member.jerseyNumber?.let { "#$it" } ?: "Not set",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    if (state.isCoachOrManager) {
                                        IconButton(onClick = { showJerseyDialog = true }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit jersey")
                                        }
                                    }
                                }

                                HorizontalDivider()

                                // Position
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Position", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = member.position ?: "Not set",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    if (state.isCoachOrManager) {
                                        IconButton(onClick = { showPositionDialog = true }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit position")
                                        }
                                    }
                                }
                            }
                        }

                        // Error
                        state.error?.let { error ->
                            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Leave team button — only for own profile as player
                        if (state.isOwnProfile && member.role == "player") {
                            Button(
                                onClick = { showLeaveDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Leave Team")
                            }
                        }
                    }
                }
            }
        }
    }

    // Leave confirmation dialog
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Team") },
            text = { Text("Are you sure you want to leave this team? You will need a new invitation to rejoin.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveDialog = false
                        viewModel.leaveTeam(teamId)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit jersey dialog
    if (showJerseyDialog) {
        TextFieldDialog(
            title = "Edit Jersey Number",
            initialValue = state.member?.jerseyNumber?.toString() ?: "",
            placeholder = "Jersey number",
            keyboardType = KeyboardType.Number,
            onConfirm = { text ->
                showJerseyDialog = false
                viewModel.updateJerseyNumber(teamId, userId, text.toIntOrNull())
            },
            onDismiss = { showJerseyDialog = false }
        )
    }

    // Edit position dialog
    if (showPositionDialog) {
        TextFieldDialog(
            title = "Edit Position",
            initialValue = state.member?.position ?: "",
            placeholder = "Position (e.g. Forward)",
            onConfirm = { text ->
                showPositionDialog = false
                viewModel.updatePosition(teamId, userId, text.ifBlank { null })
            },
            onDismiss = { showPositionDialog = false }
        )
    }
}

@Composable
private fun TextFieldDialog(
    title: String,
    initialValue: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
