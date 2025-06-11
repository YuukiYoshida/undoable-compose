/*
 * Copyright 2025 Yuuki Yoshida
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yuukiyoshida.undoable

import androidx.compose.runtime.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Default thread-safe implementation of [UndoableState] with performance optimizations.
 *
 * This implementation provides:
 * - **Thread Safety**: All operations are protected with a [Mutex] for safe concurrent access
 * - **Performance**: Uses [ArrayDeque] for O(1) history operations
 * - **Memory Efficiency**: Automatic history size limiting to prevent memory leaks
 * - **Compose Integration**: Reactive states that automatically trigger recomposition
 * - **Customizable Equality**: Pluggable equality checking for complex data types
 *
 * @param initialValue The initial value of the state
 * @param maxHistory Maximum number of history entries to keep (default: 50).
 *                   When exceeded, oldest entries are automatically removed.
 * @param equalityCheck Function to check equality between values (default: `==`).
 *                      Used to prevent duplicate consecutive entries in history.
 *
 * @sample
 * ```
 * // Basic usage
 * val textState = DefaultUndoableState("Hello")
 * textState.value = "World"
 * textState.undoSync() // Back to "Hello"
 *
 * // With custom equality for case-insensitive text
 * val caseInsensitiveState = DefaultUndoableState(
 *     initialValue = "Hello",
 *     equalityCheck = { a, b -> a.lowercase() == b.lowercase() }
 * )
 *
 * // With limited history
 * val limitedState = DefaultUndoableState(
 *     initialValue = 0,
 *     maxHistory = 10
 * )
 * ```
 */
@Stable
class DefaultUndoableState<T>(
    initialValue: T,
    private val maxHistory: Int = 50,
    private val equalityCheck: (T, T) -> Boolean = { a, b -> a == b }
) : UndoableState<T> {

    init {
        require(maxHistory > 0) { "maxHistory must be positive, was $maxHistory" }
    }

    private val mutex = Mutex()

    private val _state = mutableStateOf(initialValue)
    private val _canUndo = mutableStateOf(false)
    private val _canRedo = mutableStateOf(false)

    override val state: State<T> = _state
    override val canUndoState: State<Boolean> = _canUndo
    override val canRedoState: State<Boolean> = _canRedo

    override var value: T
        get() = _state.value
        set(newValue) = setValueSync(newValue)

    private val history = ArrayDeque<T>(maxHistory)
    private val future = ArrayDeque<T>()

    override val canUndo: Boolean get() = _canUndo.value
    override val canRedo: Boolean get() = _canRedo.value

    private fun updateStates() {
        _canUndo.value = history.isNotEmpty()
        _canRedo.value = future.isNotEmpty()
    }

    override fun setValueSync(newValue: T) {
        if (!equalityCheck(newValue, _state.value)) {
            // Maintain history size limit with efficient removal
            if (history.size >= maxHistory) {
                history.removeFirst()
            }

            history.addLast(_state.value)
            _state.value = newValue
            future.clear() // New value invalidates redo history
            updateStates()
        }
    }

    override suspend fun setValue(newValue: T) {
        mutex.withLock {
            setValueSync(newValue)
        }
    }

    override fun undoSync() {
        if (history.isNotEmpty()) {
            future.addFirst(_state.value)
            _state.value = history.removeLast()
            updateStates()
        }
    }

    override suspend fun undo() {
        mutex.withLock {
            undoSync()
        }
    }

    override fun redoSync() {
        if (future.isNotEmpty()) {
            history.addLast(_state.value)
            _state.value = future.removeFirst()
            updateStates()
        }
    }

    override suspend fun redo() {
        mutex.withLock {
            redoSync()
        }
    }

    override fun clear() {
        history.clear()
        future.clear()
        updateStates()
    }

    override fun getHistorySize(): Int = history.size
    override fun getFutureSize(): Int = future.size

    override fun toString(): String =
        "DefaultUndoableState(value=$value, historySize=${getHistorySize()}, futureSize=${getFutureSize()})"
}

/**
 * Remember an [UndoableState] across recompositions.
 *
 * Creates and remembers an undoable state that survives recomposition and configuration changes.
 * The state automatically triggers recomposition when values change, enabling reactive UI updates.
 *
 * @param initialValue The initial value of the state
 * @param maxHistory Maximum number of history entries to keep (default: 50).
 *                   When exceeded, oldest entries are automatically removed to prevent memory leaks.
 * @param equalityCheck Function to check equality between values (default: `==`).
 *                      Custom equality functions are useful for complex data types or
 *                      case-insensitive comparisons.
 * @return A remembered [UndoableState] instance
 *
 * @sample
 * ```
 * @Composable
 * fun TextEditorScreen() {
 *     val textState = rememberUndoableState("")
 *     val currentText by textState.state
 *     val canUndo by textState.canUndoState
 *     val canRedo by textState.canRedoState
 *
 *     Column {
 *         TextField(
 *             value = currentText,
 *             onValueChange = { textState.value = it },
 *             label = { Text("Enter text") }
 *         )
 *
 *         Row {
 *             Button(
 *                 onClick = { textState.undoSync() },
 *                 enabled = canUndo
 *             ) {
 *                 Text("Undo")
 *             }
 *
 *             Button(
 *                 onClick = { textState.redoSync() },
 *                 enabled = canRedo
 *             ) {
 *                 Text("Redo")
 *             }
 *
 *             Button(
 *                 onClick = { textState.clear() }
 *             ) {
 *                 Text("Clear History")
 *             }
 *         }
 *
 *         Text(
 *             text = "History: ${textState.getHistorySize()}, " +
 *                    "Future: ${textState.getFutureSize()}",
 *             style = MaterialTheme.typography.bodySmall
 *         )
 *     }
 * }
 *
 * @Composable
 * fun CaseInsensitiveTextEditor() {
 *     val textState = rememberUndoableState(
 *         initialValue = "",
 *         equalityCheck = { a, b -> a.trim().lowercase() == b.trim().lowercase() }
 *     )
 *     // Implementation...
 * }
 *
 * @Composable
 * fun LimitedHistoryCounter() {
 *     val counterState = rememberUndoableState(
 *         initialValue = 0,
 *         maxHistory = 5 // Only keep last 5 states
 *     )
 *     // Implementation...
 * }
 * ```
 */
@Composable
fun <T> rememberUndoableState(
    initialValue: T,
    maxHistory: Int = 50,
    equalityCheck: (T, T) -> Boolean = { a, b -> a == b }
): UndoableState<T> {
    return remember(initialValue, maxHistory) {
        DefaultUndoableState(
            initialValue = initialValue,
            maxHistory = maxHistory,
            equalityCheck = equalityCheck
        )
    }
}