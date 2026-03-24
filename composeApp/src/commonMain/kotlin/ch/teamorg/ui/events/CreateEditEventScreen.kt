package ch.teamorg.ui.events

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

// V1 color tokens
private val BgPrimary = Color(0xFF0A0A0F)
private val CardBg = Color(0xFF1C1C2E)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextLight = Color(0xFFE0E0FF)
private val TextMuted = Color(0xFF9090B0)
private val TextPlaceholder = Color(0xFF6B7280)
private val AccentBlue = Color(0xFF4F8EF7)
private val SelectedTileBg = Color(0xFF1A2744)
private val DividerColor = Color(0xFF2A2A40)
private val ToggleBg = Color(0xFF374151)
private val SectionLabel = Color(0xFF6B7280)
private val BottomSheetBg = Color(0xFF13131F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditEventScreen(
    viewModel: CreateEditEventViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showRecurringSheet by remember { mutableStateOf(false) }
    var showScopeSheet by remember { mutableStateOf(false) }
    var showSubgroupSheet by remember { mutableStateOf(false) }

    // Date/time picker dialog states
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showMeetupTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is FormEvent.SaveSuccess -> onSaved()
                is FormEvent.CancelSuccess -> onSaved()
            }
        }
    }

    // Auto-open recurring sheet when recurring is enabled without a pattern
    LaunchedEffect(state.recurringEnabled, state.recurringPattern) {
        if (state.recurringEnabled && state.recurringPattern == null) {
            showRecurringSheet = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // -- HEADER --
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CardBg)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Centered title
            Text(
                text = if (state.isEditMode) "Edit Event" else "New Event",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            // Save text (create mode only)
            if (!state.isEditMode) {
                Text(
                    text = "Save",
                    color = AccentBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        if (state.isEditMode && state.isSeriesEvent) {
                            showScopeSheet = true
                        } else {
                            viewModel.save()
                        }
                    }
                )
            } else {
                Spacer(modifier = Modifier.width(36.dp))
            }
        }

        // -- DIVIDER --
        HorizontalDivider(color = DividerColor, thickness = 1.dp)

        // -- ERROR BANNER --
        if (state.saveError != null) {
            Text(
                text = state.saveError!!,
                color = Color(0xFFEF4444),
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2D1B1B))
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        // -- SCROLLABLE BODY --
        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ===== EVENT TYPE =====
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                V1SectionLabel("EVENT TYPE")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EventTypeTile(
                        label = "Training",
                        emoji = "\uD83C\uDFCB\uFE0F",
                        selected = state.type == "training",
                        onClick = { viewModel.setType("training") },
                        modifier = Modifier.weight(1f)
                    )
                    EventTypeTile(
                        label = "Match",
                        emoji = "\uD83C\uDFC6",
                        selected = state.type == "match",
                        onClick = { viewModel.setType("match") },
                        modifier = Modifier.weight(1f)
                    )
                    EventTypeTile(
                        label = "Other",
                        emoji = "\u2022\u2022\u2022",
                        selected = state.type == "other",
                        onClick = { viewModel.setType("other") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ===== DATE & TIME =====
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                V1SectionLabel("DATE & TIME")

                // Date row: start date → end date
                val isMultiDay = state.startDate != state.endDate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    V1InputRow(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("📅", fontSize = 14.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            formatDate(state.startDate),
                            color = TextPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text("→", color = TextMuted, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterVertically))
                    V1InputRow(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text(
                            if (isMultiDay) formatDate(state.endDate) else "Same day",
                            color = if (isMultiDay) TextPrimary else TextMuted,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text("›", color = TextMuted, fontSize = 16.sp)
                    }
                }

                // Times row: meetup chip | start → end
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Meetup chip
                    Box(
                        modifier = Modifier
                            .width(88.dp)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (state.meetupEnabled) SelectedTileBg else DividerColor)
                            .clickable {
                                if (state.meetupEnabled) {
                                    showMeetupTimePicker = true
                                } else {
                                    viewModel.toggleMeetup(true)
                                    showMeetupTimePicker = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (state.meetupEnabled) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        formatTime(state.meetupTime),
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "✕",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        modifier = Modifier.clickable { viewModel.toggleMeetup(false) }
                                    )
                                }
                                Text(
                                    "Meetup",
                                    color = AccentBlue,
                                    fontSize = 10.sp
                                )
                            } else {
                                Text(
                                    "Meetup",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Text("|", color = DividerColor, fontSize = 20.sp)

                    // Start → End times
                    V1InputRow(
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text(
                            formatTime(state.startTime),
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text("→", color = TextMuted, fontSize = 14.sp)
                        Text(
                            formatTime(state.endTime),
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showEndTimePicker = true },
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (state.endTimeError != null) {
                    Text(
                        state.endTimeError!!,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp
                    )
                }
            }

            // ===== DETAILS =====
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                V1SectionLabel("DETAILS")

                // Title input
                V1TextInput(
                    value = state.title,
                    onValueChange = viewModel::setTitle,
                    placeholder = "Event title",
                    leadingEmoji = "\u270F\uFE0F"
                )
                if (state.titleError != null) {
                    Text(
                        state.titleError!!,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp
                    )
                }

                // Location input
                V1TextInput(
                    value = state.location,
                    onValueChange = viewModel::setLocation,
                    placeholder = "Venue or address",
                    leadingEmoji = "\uD83D\uDCCD"
                )

                // Description input
                V1TextInput(
                    value = state.description,
                    onValueChange = viewModel::setDescription,
                    placeholder = "Description (optional)",
                    leadingEmoji = "\uD83D\uDCC4",
                    singleLine = false,
                    minLines = 2
                )
            }

            // ===== AUDIENCE =====
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                V1SectionLabel("AUDIENCE")

                // Team row
                V1InputRow(
                    onClick = { /* team selection handled by toggling below */ },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Team", color = TextPrimary, fontSize = 15.sp)
                    Spacer(Modifier.weight(1f))
                    if (state.availableTeams.isEmpty()) {
                        Text("No teams", color = TextMuted, fontSize = 14.sp)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.availableTeams.forEach { team ->
                                val isSelected = team.id in state.selectedTeamIds
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) SelectedTileBg else CardBg)
                                        .then(
                                            if (isSelected) Modifier.border(
                                                1.dp,
                                                AccentBlue,
                                                RoundedCornerShape(8.dp)
                                            ) else Modifier
                                        )
                                        .clickable { viewModel.toggleTeam(team.id) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(AccentBlue)
                                            )
                                        }
                                        Text(
                                            team.name,
                                            color = if (isSelected) TextPrimary else TextMuted,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (state.teamError != null) {
                    Text(
                        state.teamError!!,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp
                    )
                }

                // Sub-groups row
                V1InputRow(
                    onClick = { showSubgroupSheet = true },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Sub-groups", color = TextPrimary, fontSize = 15.sp)
                    Spacer(Modifier.weight(1f))
                    val subgroupLabel = if (state.selectedSubgroupIds.isEmpty()) {
                        "All Players"
                    } else {
                        val names = state.availableSubgroups
                            .filter { it.id in state.selectedSubgroupIds }
                            .joinToString(", ") { it.name }
                        names.ifEmpty { "All Players" }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(subgroupLabel, color = TextMuted, fontSize = 13.sp)
                    }
                }
            }

            // ===== OPTIONS =====
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                V1SectionLabel("OPTIONS")

                // Recurring card — expandable
                val isSeriesLocked = state.isEditMode && state.isSeriesEvent
                val recurringPattern = state.recurringPattern
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBg)
                        .animateContentSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("\uD83D\uDD01", fontSize = 16.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Recurring",
                            color = if (isSeriesLocked) TextMuted else TextPrimary,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSeriesLocked) {
                            Text("Series", color = AccentBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        } else {
                            Switch(
                                checked = state.recurringEnabled,
                                onCheckedChange = viewModel::setRecurringEnabled,
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = AccentBlue,
                                    uncheckedTrackColor = ToggleBg,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }
                    if (!isSeriesLocked && state.recurringEnabled && recurringPattern != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            buildRecurringSummary(recurringPattern),
                            color = AccentBlue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF162340))
                                .clickable { showRecurringSheet = true }
                                .padding(vertical = 8.dp)
                        )
                    }
                }

                // Auto-scroll to bottom when min attendees expands
                LaunchedEffect(state.minAttendeesEnabled) {
                    if (state.minAttendeesEnabled) {
                        kotlinx.coroutines.delay(150) // wait for animateContentSize to start
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                }

                // Min attendees card — expandable
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBg)
                        .animateContentSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("👥", fontSize = 16.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Min. Attendees",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = state.minAttendeesEnabled,
                            onCheckedChange = viewModel::toggleMinAttendees,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = AccentBlue,
                                uncheckedTrackColor = ToggleBg,
                                uncheckedThumbColor = TextMuted,
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }
                    if (state.minAttendeesEnabled) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF162340))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Minimum", color = TextMuted, fontSize = 14.sp)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    "−",
                                    color = if (state.minAttendees > 1) AccentBlue else TextMuted,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        if (state.minAttendees > 1) viewModel.setMinAttendees(state.minAttendees - 1)
                                    }
                                )
                                Text(
                                    state.minAttendees.toString(),
                                    color = TextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "+",
                                    color = AccentBlue,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        viewModel.setMinAttendees(state.minAttendees + 1)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // -- CTA BUTTON --
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp)
        ) {
            Button(
                onClick = {
                    if (state.isEditMode && state.isSeriesEvent) {
                        showScopeSheet = true
                    } else {
                        viewModel.save()
                    }
                },
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue,
                    disabledContainerColor = AccentBlue.copy(alpha = 0.5f)
                )
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        if (state.isEditMode) "Save Changes" else "Create Event",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Date pickers
    if (showStartDatePicker) {
        EventDatePickerDialog(
            initialDate = state.startDate,
            onDateSelected = { date ->
                viewModel.setStartDate(date)
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        EventDatePickerDialog(
            initialDate = state.endDate,
            onDateSelected = { date ->
                viewModel.setEndDate(date)
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }

    // Time pickers
    if (showStartTimePicker) {
        EventTimePickerDialog(
            initialTime = state.startTime,
            onTimeSelected = { time ->
                viewModel.setStartTime(time)
                showStartTimePicker = false
            },
            onDismiss = { showStartTimePicker = false }
        )
    }
    if (showEndTimePicker) {
        EventTimePickerDialog(
            initialTime = state.endTime,
            onTimeSelected = { time ->
                viewModel.setEndTime(time)
                showEndTimePicker = false
            },
            onDismiss = { showEndTimePicker = false }
        )
    }
    if (showMeetupTimePicker) {
        EventTimePickerDialog(
            initialTime = state.meetupTime,
            onTimeSelected = { time ->
                viewModel.setMeetupTime(time)
                showMeetupTimePicker = false
            },
            onDismiss = { showMeetupTimePicker = false }
        )
    }

    // Recurring pattern bottom sheet
    if (showRecurringSheet) {
        RecurringPatternSheet(
            initialPattern = state.recurringPattern ?: RecurringPatternState(),
            onDone = { pattern ->
                viewModel.setRecurringPattern(pattern)
                showRecurringSheet = false
            },
            onDismiss = {
                if (state.recurringPattern == null) {
                    viewModel.setRecurringEnabled(false)
                }
                showRecurringSheet = false
            }
        )
    }

    // Scope sheet for edit of recurring events
    if (showScopeSheet) {
        RecurringScopeSheet(
            mode = "edit",
            onContinue = { scope ->
                showScopeSheet = false
                viewModel.save(scope)
            },
            onDismiss = { showScopeSheet = false }
        )
    }

    // Sub-groups bottom sheet
    if (showSubgroupSheet) {
        SubgroupsSheet(
            subgroups = state.availableSubgroups,
            selectedIds = state.selectedSubgroupIds,
            onToggle = viewModel::toggleSubgroup,
            onDismiss = { showSubgroupSheet = false }
        )
    }
}

// ─── Reusable V1 components ───

@Composable
private fun V1SectionLabel(text: String) {
    Text(
        text = text,
        color = SectionLabel,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun EventTypeTile(
    label: String,
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) SelectedTileBg else CardBg
    val borderMod = if (selected) {
        Modifier.border(2.dp, AccentBlue, RoundedCornerShape(14.dp))
    } else Modifier
    val iconColor = if (selected) AccentBlue else TextPlaceholder
    val textColor = if (selected) TextPrimary else TextMuted

    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .then(borderMod)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(emoji, fontSize = 22.sp, color = iconColor)
            Text(label, fontSize = 13.sp, color = textColor, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun V1InputRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun V1TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingEmoji: String,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(leadingEmoji, fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        // Use BasicTextField wrapped in a Box for full control
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = minLines,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = TextPrimary,
                fontSize = 15.sp
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(AccentBlue),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            color = TextPlaceholder,
                            fontSize = 15.sp
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

private fun formatDate(date: LocalDate): String {
    val dayOfWeek = date.dayOfWeek.name.take(3).lowercase()
        .replaceFirstChar { it.uppercase() }
    val month = date.month.name.take(3).lowercase()
        .replaceFirstChar { it.uppercase() }
    return "$dayOfWeek, ${date.dayOfMonth} $month ${date.year}"
}

private fun formatTime(time: LocalTime): String {
    return "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
}

private fun buildRecurringSummary(pattern: RecurringPatternState): String {
    val typePart = when (pattern.patternType) {
        "daily" -> "Repeats daily"
        "weekly" -> {
            if (pattern.weekdays.isEmpty()) {
                "Repeats weekly"
            } else {
                val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                val days = pattern.weekdays.sorted().joinToString(", ") { dayNames[it] }
                "Repeats every $days"
            }
        }
        else -> "Repeats every ${pattern.intervalDays} days"
    }
    val endPart = if (pattern.hasEndDate && pattern.endDate != null) {
        " until ${formatDate(pattern.endDate)}"
    } else ""
    return "$typePart$endPart"
}

// ─── Date/Time Picker Dialogs (kept from existing code) ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = run {
            val epochDays = initialDate.toEpochDays()
            epochDays * 24L * 60 * 60 * 1000
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = datePickerState.selectedDateMillis
                if (millis != null) {
                    val epochDays = (millis / (24L * 60 * 60 * 1000)).toInt()
                    onDateSelected(LocalDate.fromEpochDays(epochDays))
                } else {
                    onDismiss()
                }
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventTimePickerDialog(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select time") },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(onClick = {
                onTimeSelected(LocalTime(timePickerState.hour, timePickerState.minute))
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ─── Sub-groups bottom sheet ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubgroupsSheet(
    subgroups: List<ch.teamorg.domain.SubGroup>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BottomSheetBg,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DividerColor)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Select Sub-groups",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CardBg)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = TextPrimary, fontSize = 14.sp)
                }
            }

            Text(
                "Select who this event is for",
                color = SectionLabel,
                fontSize = 13.sp
            )

            // "All Players" option
            val allSelected = selectedIds.isEmpty()
            SubgroupListItem(
                label = "All Players",
                count = subgroups.sumOf { it.memberCount },
                selected = allSelected,
                onClick = {
                    // Deselect all sub-groups to mean "all players"
                    selectedIds.forEach { id -> onToggle(id) }
                }
            )

            // Individual sub-groups
            subgroups.forEach { subgroup ->
                SubgroupListItem(
                    label = subgroup.name,
                    count = subgroup.memberCount,
                    selected = subgroup.id in selectedIds,
                    onClick = { onToggle(subgroup.id) }
                )
            }

            // Apply button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text(
                    "Apply",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SubgroupListItem(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) SelectedTileBg else CardBg
    val borderMod = if (selected) {
        Modifier.border(1.dp, AccentBlue, RoundedCornerShape(12.dp))
    } else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .then(borderMod)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextPrimary, fontSize = 15.sp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(count.toString(), color = TextMuted, fontSize = 13.sp)
            if (selected) {
                Text("✓", color = AccentBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
