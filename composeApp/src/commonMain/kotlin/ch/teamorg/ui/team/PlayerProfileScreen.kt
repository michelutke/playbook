package ch.teamorg.ui.team

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.teamorg.domain.AbwesenheitRule
import ch.teamorg.domain.CreateAbwesenheitRequest
import ch.teamorg.ui.attendance.AbsenceCard
import ch.teamorg.ui.attendance.AddAbsenceSheet
import ch.teamorg.ui.attendance.AttendanceStatsBar
import ch.teamorg.ui.util.rememberImagePickerLauncher
import coil3.compose.AsyncImage

private val ScreenBg = Color(0xFF090912)
private val CardBg = Color(0xFF1C1C2E)
private val TextPrimary = Color(0xFFF0F0FF)
private val TextMuted = Color(0xFF9090B0)
private val AccentBlue = Color(0xFF4F8EF7)
private val AccentOrange = Color(0xFFF97316)
private val DestructiveRed = Color(0xFFEF4444)
private val DividerColor = Color(0xFF2A2A40)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileScreen(
    teamId: String,
    userId: String,
    viewModel: PlayerProfileViewModel,
    onBack: () -> Unit,
    onLeftTeam: () -> Unit,
    isNavProfile: Boolean = false  // true = bottom nav profile tab, false = member detail
) {
    val state by viewModel.state.collectAsState()
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showJerseyDialog by remember { mutableStateOf(false) }
    var showPositionDialog by remember { mutableStateOf(false) }
    var showAddAbsenceSheet by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<AbwesenheitRule?>(null) }
    var deleteTargetRule by remember { mutableStateOf<AbwesenheitRule?>(null) }

    LaunchedEffect(state.leftTeam) {
        if (state.leftTeam) onLeftTeam()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Header bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!isNavProfile) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                }
                Text(
                    text = "Profile",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentBlue)
                    }
                }
                state.member == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Player not found", color = TextMuted)
                    }
                }
                else -> {
                    val member = state.member!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Hero card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF13131F)),
                            border = BorderStroke(1.dp, DividerColor)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Avatar 80dp
                                    val pickImage = rememberImagePickerLauncher { bytes, ext ->
                                        viewModel.uploadAvatar(teamId, userId, bytes, ext)
                                    }
                                    Box(
                                        modifier = Modifier.size(80.dp),
                                        contentAlignment = Alignment.BottomEnd
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(CircleShape)
                                                .background(CardBg)
                                                .then(
                                                    if (state.isOwnProfile) Modifier.clickable { pickImage() } else Modifier
                                                ),
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
                                                    modifier = Modifier.size(40.dp),
                                                    tint = TextMuted
                                                )
                                            }
                                        }
                                        if (state.isOwnProfile) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(AccentBlue),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.CameraAlt,
                                                    contentDescription = "Upload avatar",
                                                    modifier = Modifier.size(12.dp),
                                                    tint = Color.White
                                                )
                                            }
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = member.displayName,
                                            color = TextPrimary,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        // Role chip
                                        Surface(
                                            color = AccentBlue.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = member.role.replaceFirstChar { it.uppercase() },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                color = AccentBlue,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        // Jersey / position
                                        val info = listOfNotNull(
                                            member.jerseyNumber?.let { "#$it" },
                                            member.position
                                        ).joinToString(" · ")
                                        if (info.isNotEmpty()) {
                                            Text(
                                                text = info,
                                                color = TextMuted,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }

                                AttendanceStatsBar(presencePct = state.presencePct)
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // My Absences section header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MY ABSENCES",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "View All",
                                color = AccentBlue,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Absence list or empty state
                        val displayedRules = state.absenceRules
                        if (displayedRules.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No absences",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Add an absence rule to automatically decline events.",
                                    color = TextMuted,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                displayedRules.forEach { rule ->
                                    AbsenceCard(
                                        rule = rule,
                                        onClick = {
                                            editingRule = rule
                                            showAddAbsenceSheet = true
                                        },
                                        onLongPress = { deleteTargetRule = rule }
                                    )
                                }
                            }
                        }

                        // Backfill snackbar hint
                        if (state.backfillStatus == "pending") {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                color = CardBg,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Applying absence rule to matching events...",
                                    modifier = Modifier.padding(12.dp),
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        state.error?.let { error ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = error,
                                modifier = Modifier.padding(horizontal = 20.dp),
                                color = DestructiveRed,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // Coach-editable fields
                        if (state.isCoachOrManager) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBg)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Jersey Number", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                            Text(
                                                text = member.jerseyNumber?.let { "#$it" } ?: "Not set",
                                                color = TextPrimary,
                                                fontSize = 16.sp
                                            )
                                        }
                                        TextButton(onClick = { showJerseyDialog = true }) {
                                            Text("Edit", color = AccentBlue, fontSize = 14.sp)
                                        }
                                    }

                                    HorizontalDivider(color = DividerColor)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Position", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                            Text(
                                                text = member.position ?: "Not set",
                                                color = TextPrimary,
                                                fontSize = 16.sp
                                            )
                                        }
                                        TextButton(onClick = { showPositionDialog = true }) {
                                            Text("Edit", color = AccentBlue, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        if (state.isOwnProfile && member.role == "player") {
                            Button(
                                onClick = { showLeaveDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed)
                            ) {
                                Text("Leave Team", color = Color.White)
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        // Bottom padding for FAB
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }

        // FAB — Add absence (own profile or coach viewing member)
        val canManageAbsences = state.isOwnProfile || state.isCoachOrManager
        if (canManageAbsences) {
            FloatingActionButton(
                onClick = {
                    editingRule = null
                    showAddAbsenceSheet = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = if (isNavProfile) 80.dp else 24.dp)
                    .size(56.dp),
                containerColor = AccentBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add absence")
            }
        }
    }

    // Add/Edit absence sheet
    AddAbsenceSheet(
        visible = showAddAbsenceSheet,
        editingRule = editingRule,
        onDismiss = {
            showAddAbsenceSheet = false
            editingRule = null
        },
        onSave = { request ->
            showAddAbsenceSheet = false
            val editing = editingRule
            if (editing != null) {
                viewModel.updateAbsence(
                    editing.id,
                    ch.teamorg.domain.UpdateAbwesenheitRequest(
                        presetType = request.presetType,
                        label = request.label,
                        bodyPart = request.bodyPart,
                        ruleType = request.ruleType,
                        weekdays = request.weekdays,
                        startDate = request.startDate,
                        endDate = request.endDate
                    )
                )
            } else {
                viewModel.createAbsence(request)
            }
            editingRule = null
        }
    )

    // Delete confirm dialog
    deleteTargetRule?.let { rule ->
        AlertDialog(
            onDismissRequest = { deleteTargetRule = null },
            title = {
                Text(
                    text = "Delete absence rule?",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "This rule will be removed and will no longer auto-decline matching events.",
                    color = TextMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAbsence(rule.id)
                        deleteTargetRule = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = DestructiveRed)
                ) {
                    Text("Delete Rule")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTargetRule = null }) {
                    Text("Keep Rule", color = AccentBlue)
                }
            },
            containerColor = CardBg
        )
    }

    // Leave confirmation dialog
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Team", color = TextPrimary) },
            text = { Text("Are you sure you want to leave this team? You will need a new invitation to rejoin.", color = TextMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveDialog = false
                        viewModel.leaveTeam(teamId)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = DestructiveRed)
                ) { Text("Leave") }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("Cancel") }
            },
            containerColor = CardBg
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
