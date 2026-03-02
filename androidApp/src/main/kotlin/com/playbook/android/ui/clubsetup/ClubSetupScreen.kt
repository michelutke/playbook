package com.playbook.android.ui.clubsetup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel

private val SPORT_TYPES = listOf(
    "Football", "Basketball", "Volleyball", "Tennis", "Swimming",
    "Athletics", "Cycling", "Rugby", "Hockey", "Handball",
    "Baseball", "Cricket", "Golf", "Martial Arts", "Other",
)

@Composable
fun ClubSetupScreen(
    onClubCreated: (clubId: String) -> Unit,
    viewModel: ClubSetupViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ClubSetupEvent.ClubCreated -> onClubCreated(event.clubId)
            }
        }
    }
    ClubSetupContent(state = state, onAction = viewModel::submitAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClubSetupContent(
    state: ClubSetupScreenState,
    onAction: (ClubSetupAction) -> Unit,
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) onAction(ClubSetupAction.LogoSelected(uri))
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Set up your club") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (state.logoUri != null) {
                    AsyncImage(
                        model = state.logoUri,
                        contentDescription = "Club logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.AddPhotoAlternate,
                        contentDescription = "Add logo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = "Add club logo",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = state.name,
                onValueChange = { onAction(ClubSetupAction.NameChanged(it)) },
                label = { Text("Club name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            ExposedDropdownMenuBox(
                expanded = state.sportTypeDropdownExpanded,
                onExpandedChange = { onAction(ClubSetupAction.SportTypeDropdownToggled) },
            ) {
                OutlinedTextField(
                    value = state.sportType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sport type") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = state.sportTypeDropdownExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = state.sportTypeDropdownExpanded,
                    onDismissRequest = { onAction(ClubSetupAction.SportTypeDropdownToggled) },
                ) {
                    SPORT_TYPES.forEach { sport ->
                        DropdownMenuItem(
                            text = { Text(sport) },
                            onClick = { onAction(ClubSetupAction.SportTypeChanged(sport)) },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.location,
                onValueChange = { onAction(ClubSetupAction.LocationChanged(it)) },
                label = { Text("City / Location (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            if (state.error != null) {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = { onAction(ClubSetupAction.Submit) },
                enabled = state.isFormValid && !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Create Club")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
