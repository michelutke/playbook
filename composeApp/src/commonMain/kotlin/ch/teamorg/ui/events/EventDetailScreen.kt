package ch.teamorg.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.teamorg.domain.CheckInEntry
import ch.teamorg.domain.EventWithTeams
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Alarm
import ch.teamorg.ui.attendance.AttendanceRsvpButtons
import ch.teamorg.ui.attendance.BegrundungSheet
import ch.teamorg.ui.attendance.CoachOverrideSheet
import ch.teamorg.ui.attendance.MemberResponseList
import ch.teamorg.ui.attendance.ResponseDeadlineLabel
import ch.teamorg.ui.inbox.ReminderPickerSheet
import ch.teamorg.ui.inbox.formatLeadTime
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Design tokens from V1 Pencil
private val BgPrimary = Color(0xFF090912)
private val CardBg = Color(0xFF1C1C2E)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextLight = Color(0xFFE0E0FF)
private val TextMuted = Color(0xFF9090B0)
private val TextName = Color(0xFFF0F0FF)
private val AccentBlue = Color(0xFF4F8EF7)
private val DividerColor = Color(0xFF2A2A40)
private val ColorConfirmed = Color(0xFF22C55E)
private val ColorMaybe = Color(0xFFFACC15)
private val ColorDeclined = Color(0xFFEF4444)
private val ColorError = Color(0xFFEF4444)
private val BtnGoingBg = Color(0xFF065F46)
private val BtnInactiveBg = Color(0xFF1F2937)
private val BtnMaybeBg = Color(0xFF3D3400)
private val BtnDeclinedBg = Color(0xFF450A0A)
private val InactiveText = Color(0xFF6B7280)

private fun formatDay(instant: Instant): String {
    val l = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val d = l.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val m = l.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "$d, ${l.dayOfMonth} $m ${l.year}"
}

private fun formatTime(instant: Instant): String {
    val l = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${l.hour.toString().padStart(2, '0')}:${l.minute.toString().padStart(2, '0')}"
}

private fun formatTimeLine(start: Instant, end: Instant, meetup: Instant?): String {
    val base = "${formatTime(start)} – ${formatTime(end)}"
    return if (meetup != null) "$base  ·  Meet ${formatTime(meetup)}" else base
}

@Composable
fun EventDetailScreen(
    viewModel: EventDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onCancel: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showCancelScopeSheet by remember { mutableStateOf(false) }
    var showUncancelScopeSheet by remember { mutableStateOf(false) }
    var showBegrundung by remember { mutableStateOf(false) }
    var begrundungStatus by remember { mutableStateOf("unsure") }
    var showOverrideReason by remember { mutableStateOf(false) }
    var overrideTarget by remember { mutableStateOf<CheckInEntry?>(null) }
    var overrideStatus by remember { mutableStateOf("") }
    var showReminderSheet by remember { mutableStateOf(false) }

    val isCancelled = state.event?.event?.status == "cancelled"
    val isSeries = state.event?.event?.seriesId != null

    Column(modifier = Modifier.fillMaxSize().background(BgPrimary).windowInsetsPadding(WindowInsets.statusBars)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button (circle)
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(CardBg)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("←", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            // Title
            Text(
                text = state.event?.event?.title ?: "Event",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                textAlign = TextAlign.Center
            )

            // Edit / more
            if (state.isCoach) {
                Box {
                    Text(
                        "✏",
                        fontSize = 16.sp,
                        modifier = Modifier.clickable { showMenu = true }
                    )
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        if (!isCancelled) {
                            DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onEdit() })
                            DropdownMenuItem(text = { Text("Duplicate") }, onClick = { showMenu = false; onDuplicate() })
                            DropdownMenuItem(
                                text = { Text("Cancel event", color = ColorError) },
                                onClick = {
                                    showMenu = false
                                    if (isSeries) showCancelScopeSheet = true
                                    else viewModel.cancelEvent("this_only")
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Restore event", color = ColorConfirmed) },
                                onClick = {
                                    showMenu = false
                                    if (isSeries) showUncancelScopeSheet = true
                                    else viewModel.uncancelEvent("this_only")
                                }
                            )
                            DropdownMenuItem(text = { Text("Duplicate") }, onClick = { showMenu = false; onDuplicate() })
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(20.dp))
            }
        }

        // Cancel scope sheet
        if (showCancelScopeSheet) {
            RecurringScopeSheet(
                mode = "cancel",
                onContinue = { scope ->
                    viewModel.cancelEvent(scope)
                    showCancelScopeSheet = false
                },
                onDismiss = { showCancelScopeSheet = false }
            )
        }

        // Uncancel scope sheet
        if (showUncancelScopeSheet) {
            RecurringScopeSheet(
                mode = "uncancel",
                onContinue = { scope ->
                    viewModel.uncancelEvent(scope)
                    showUncancelScopeSheet = false
                },
                onDismiss = { showUncancelScopeSheet = false }
            )
        }

        // Divider
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error ?: "Error", color = ColorError)
            }
            state.event != null -> EventDetailBody(
                ewt = state.event!!,
                myResponse = state.myResponse,
                confirmedCount = state.confirmedCount,
                maybeCount = state.maybeCount,
                declinedCount = state.declinedCount,
                responseDeadline = state.responseDeadline,
                deadlinePassed = state.deadlinePassed,
                checkInEntries = state.checkInEntries,
                isCoach = state.isCoach,
                reminderLeadMinutes = state.reminderLeadMinutes,
                onReminderTap = { showReminderSheet = true },
                onRsvpSelect = { status ->
                    if (status == "unsure" || status == "declined") {
                        begrundungStatus = status
                        showBegrundung = true
                    } else {
                        viewModel.submitResponse(status, null)
                    }
                },
                onOverrideTap = { entry, status ->
                    if (status == "absent" || status == "excused") {
                        overrideTarget = entry
                        overrideStatus = status
                        showOverrideReason = true
                    } else {
                        viewModel.submitOverride(entry.userId, status, null)
                    }
                }
            )
        }

        BegrundungSheet(
            visible = showBegrundung,
            mode = begrundungStatus,
            onDismiss = { showBegrundung = false },
            onConfirm = { reason ->
                showBegrundung = false
                viewModel.submitResponse(begrundungStatus, reason.ifBlank { null })
            }
        )

        BegrundungSheet(
            visible = showOverrideReason,
            mode = overrideStatus,
            onDismiss = { showOverrideReason = false; overrideTarget = null },
            onConfirm = { reason ->
                val target = overrideTarget
                if (target != null) {
                    viewModel.submitOverride(target.userId, overrideStatus, reason.ifBlank { null })
                }
                showOverrideReason = false
                overrideTarget = null
            }
        )

        val eventId = state.event?.event?.id
        if (showReminderSheet && eventId != null) {
            ReminderPickerSheet(
                currentLeadMinutes = state.reminderLeadMinutes,
                onConfirm = { minutes ->
                    viewModel.setReminderOverride(eventId, minutes)
                    showReminderSheet = false
                },
                onRemove = {
                    viewModel.setReminderOverride(eventId, null)
                    showReminderSheet = false
                },
                onDismiss = { showReminderSheet = false }
            )
        }
    }
}

@Composable
private fun EventDetailBody(
    ewt: EventWithTeams,
    myResponse: String?,
    confirmedCount: Int,
    maybeCount: Int,
    declinedCount: Int,
    responseDeadline: Instant?,
    deadlinePassed: Boolean,
    checkInEntries: List<CheckInEntry>,
    isCoach: Boolean,
    reminderLeadMinutes: Int?,
    onReminderTap: () -> Unit,
    onRsvpSelect: (String) -> Unit,
    onOverrideTap: (CheckInEntry, String) -> Unit
) {
    val event = ewt.event
    val isCancelled = event.status == "cancelled"
    val startLocal = event.startAt.toLocalDateTime(TimeZone.currentSystemDefault())
    val endLocal = event.endAt.toLocalDateTime(TimeZone.currentSystemDefault())
    val isMultiDay = startLocal.date != endLocal.date

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Cancelled banner
        if (isCancelled) {
            Box(
                modifier = Modifier.fillMaxWidth().background(ColorError.copy(alpha = 0.15f))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text("This event has been cancelled", color = ColorError, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Meta section
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Date
            MetaRow(icon = "📅", text = if (isMultiDay) "${formatDay(event.startAt)} – ${formatDay(event.endAt)}" else formatDay(event.startAt))
            // Time
            MetaRow(icon = "🕐", text = formatTimeLine(event.startAt, event.endAt, event.meetupAt))
            // Location
            val loc = event.location
            if (loc != null) {
                MetaRow(icon = "📍", text = loc)
            }
            // Team
            if (ewt.matchedTeams.isNotEmpty()) {
                MetaRow(icon = "👥", text = ewt.matchedTeams.joinToString(", ") { it.name })
            }
        }

        // Divider
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))

        // Reminder row
        val reminderText = when {
            reminderLeadMinutes == null -> "Global default (2 h)"
            reminderLeadMinutes == -1 -> "No reminder"
            else -> formatLeadTime(reminderLeadMinutes)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onReminderTap() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Alarm, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text("Reminder", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Spacer(Modifier.weight(1f))
            Text(reminderText, style = MaterialTheme.typography.labelLarge, color = TextMuted)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
        }

        // Divider
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))

        // RSVP buttons
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
            AttendanceRsvpButtons(
                currentResponse = myResponse,
                confirmedCount = confirmedCount,
                maybeCount = maybeCount,
                declinedCount = declinedCount,
                deadlinePassed = deadlinePassed,
                compact = false,
                onSelect = onRsvpSelect
            )
            if (responseDeadline != null) {
                Spacer(modifier = Modifier.height(8.dp))
                ResponseDeadlineLabel(deadline = responseDeadline)
            }
        }

        // Divider
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))

        // Member response list
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            MemberResponseList(
                entries = checkInEntries,
                isCoach = isCoach,
                onOverrideTap = onOverrideTap
            )
        }
    }
}

@Composable
private fun MetaRow(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(icon, fontSize = 14.sp)
        Text(text, color = TextLight, fontSize = 14.sp)
    }
}

@Composable
private fun RsvpButton(
    symbol: String,
    label: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .height(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(symbol, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(label, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SectionHeader(label: String, count: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$label  ·  $count",
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}
