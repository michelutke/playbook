package com.playbook.ui.clubedit

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.playbook.di.kmpViewModel
import org.koin.core.parameter.parametersOf

private val SPORT_TYPES = listOf(
    "Football", "Basketball", "Volleyball", "Tennis", "Swimming",
    "Athletics", "Cycling", "Rugby", "Hockey", "Handball",
    "Baseball", "Cricket", "Golf", "Martial Arts", "Other",
)

@Composable
fun ClubEditScreen(
    clubId: String,
    onSaved: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ClubEditViewModel = kmpViewModel(key = clubId) { parametersOf(clubId) },
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ClubEditEvent.Saved -> onSaved()
            }
        }
    }
    ClubEditContent(
        state = state,
        onAction = viewModel::submitAction,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClubEditContent(
    state: ClubEditScreenState,
    onAction: (ClubEditAction) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Club") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            else -> Column(
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
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.logoUrl != null) {
                        AsyncImage(
                            model = state.logoUrl,
                            contentDescription = "Club logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.AddPhotoAlternate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                OutlinedTextField(
                    value = state.name,
                    onValueChange = { onAction(ClubEditAction.NameChanged(it)) },
                    label = { Text("Club name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                ExposedDropdownMenuBox(
                    expanded = state.sportTypeDropdownExpanded,
                    onExpandedChange = { onAction(ClubEditAction.SportTypeDropdownToggled) },
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
                        onDismissRequest = { onAction(ClubEditAction.SportTypeDropdownToggled) },
                    ) {
                        SPORT_TYPES.forEach { sport ->
                            DropdownMenuItem(
                                text = { Text(sport) },
                                onClick = { onAction(ClubEditAction.SportTypeChanged(sport)) },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state.location,
                    onValueChange = { onAction(ClubEditAction.LocationChanged(it)) },
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
                    onClick = { onAction(ClubEditAction.Save) },
                    enabled = state.isFormValid && !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Save")
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
