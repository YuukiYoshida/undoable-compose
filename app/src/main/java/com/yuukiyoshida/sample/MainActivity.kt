package com.yuukiyoshida.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yuukiyoshida.undoable.rememberUndoableState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UndoSampleApp()
        }
    }
}

@Composable
fun UndoSampleApp() {
    val undoState = rememberUndoableState("")
    val currentValue by undoState.state
    val canUndo by undoState.canUndoState
    val canRedo by undoState.canRedoState

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = currentValue,
                    onValueChange = { newValue -> undoState.value = newValue },
                    label = { Text(stringResource(R.string.input)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "Current value: '$currentValue'")
                Text(
                    text = "History: ${undoState.getHistorySize()}, Future: ${undoState.getFutureSize()}",
                    style = MaterialTheme.typography.bodySmall
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { undoState.undoSync() },
                        enabled = canUndo
                    ) {
                        Text(stringResource(R.string.undo))
                    }

                    Button(
                        onClick = { undoState.redoSync() },
                        enabled = canRedo
                    ) {
                        Text(stringResource(R.string.redo))
                    }

                    Button(
                        onClick = { undoState.clear() }
                    ) {
                        Text(stringResource(R.string.clear))
                    }
                }
            }
        }
    }
}