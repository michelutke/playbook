package com.playbook.ui.attendance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.playbook.domain.AttendanceResponseStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BegrundungSheet(
    status: AttendanceResponseStatus,
    onSubmit: (reason: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var text by remember { mutableStateOf("") }
    val isUnsure = status == AttendanceResponseStatus.UNSURE
    val title = if (isUnsure) "Why are you unsure?" else "Reason for declining"
    val submitEnabled = if (isUnsure) text.isNotBlank() else true

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (isUnsure) "Reason (required)" else "Reason (optional)") },
                isError = isUnsure && text.isBlank(),
                supportingText = if (isUnsure && text.isBlank()) {
                    { Text("*Required") }
                } else null,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSubmit(text.ifBlank { null }) },
                enabled = submitEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Submit")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
