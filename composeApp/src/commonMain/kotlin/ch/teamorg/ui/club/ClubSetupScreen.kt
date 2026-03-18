package ch.teamorg.ui.club

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import ch.teamorg.ui.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubSetupScreen(
    viewModel: ClubSetupViewModel,
    onBack: () -> Unit,
    onClubCreated: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    // Using a simple event handler for navigation
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            if (event is ClubSetupEvent.ClubCreated) {
                // In a real app, might wait for logo upload or just proceed
                onClubCreated(event.club.id)
            }
        }
    }

    Scaffold(
        modifier = Modifier.testTagsAsResourceId(),
        topBar = {
            TopAppBar(
                title = { Text("Set Up Your Club") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Logo Selection (simplified - assuming an image picker integration)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (state.logoUrl != null) {
                    AsyncImage(
                        model = state.logoUrl,
                        contentDescription = "Club Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (state.isLogoUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                } else {
                    IconButton(
                        onClick = { /* Launch Image Picker */ },
                        modifier = Modifier.testTag("btn_add_logo")
                    ) {
                        Icon(
                            Icons.Default.AddAPhoto,
                            contentDescription = "Add Logo",
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                "Club Details",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.align(Alignment.Start)
            )

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Club Name") },
                modifier = Modifier.fillMaxWidth().testTag("tf_club_name"),
                isError = state.error != null && state.name.isBlank(),
                singleLine = true
            )

            OutlinedTextField(
                value = state.sportType,
                onValueChange = viewModel::onSportTypeChange,
                label = { Text("Sport Type") },
                modifier = Modifier.fillMaxWidth().testTag("tf_sport_type"),
                singleLine = true
            )

            OutlinedTextField(
                value = state.location,
                onValueChange = viewModel::onLocationChange,
                label = { Text("Location (Optional)") },
                modifier = Modifier.fillMaxWidth().testTag("tf_location"),
                singleLine = true
            )

            if (state.error != null) {
                Text(
                    state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = viewModel::createClub,
                modifier = Modifier.fillMaxWidth().testTag("btn_create_club"),
                enabled = !state.isLoading && state.name.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create Club")
                }
            }
        }
    }
}
