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

import androidx.compose.runtime.State

/**
 * Represents an undoable state management system for Jetpack Compose.
 *
 * This interface provides a complete state management solution with history tracking,
 * optimized for Compose's declarative UI model with reactive state updates.
 *
 * The state automatically tracks changes and maintains both undo and redo history,
 * while providing Compose-observable states that trigger recomposition when values change.
 *
 * @param T The type of value being managed
 *
 * @sample
 * ```
 * @Composable
 * fun MyScreen() {
 *     val undoableState = rememberUndoableState("")
 *     val currentValue by undoableState.state
 *     val canUndo by undoableState.canUndoState
 *     val canRedo by undoableState.canRedoState
 *
 *     TextField(
 *         value = currentValue,
 *         onValueChange = { undoableState.value = it }
 *     )
 *
 *     Row {
 *         Button(
 *             onClick = { undoableState.undoSync() },
 *             enabled = canUndo
 *         ) {
 *             Text("Undo")
 *         }
 *
 *         Button(
 *             onClick = { undoableState.redoSync() },
 *             enabled = canRedo
 *         ) {
 *             Text("Redo")
 *         }
 *     }
 * }
 * ```
 */
interface UndoableState<T> {

    /**
     * Current value with direct property access.
     *
     * Setting this property will automatically add the previous value to history
     * and trigger recomposition of any observing Composables.
     */
    var value: T

    /**
     * Compose-observable state for the current value.
     *
     * Use this with the `by` delegate in Compose functions to automatically
     * trigger recomposition when the value changes.
     *
     * @sample
     * ```
     * val currentValue by undoableState.state
     * ```
     */
    val state: State<T>

    /**
     * Compose-observable state for undo availability.
     *
     * Updates automatically when history changes. Use this to control
     * the enabled state of undo buttons or other UI elements.
     *
     * @sample
     * ```
     * val canUndo by undoableState.canUndoState
     * Button(enabled = canUndo, onClick = { undoableState.undoSync() })
     * ```
     */
    val canUndoState: State<Boolean>

    /**
     * Compose-observable state for redo availability.
     *
     * Updates automatically when future history changes. Use this to control
     * the enabled state of redo buttons or other UI elements.
     *
     * @sample
     * ```
     * val canRedo by undoableState.canRedoState
     * Button(enabled = canRedo, onClick = { undoableState.redoSync() })
     * ```
     */
    val canRedoState: State<Boolean>

    /**
     * Whether undo operation is available.
     *
     * Returns `true` if there are previous states in the history that can be restored.
     * This is a direct property access alternative to [canUndoState] for non-Compose usage.
     */
    val canUndo: Boolean

    /**
     * Whether redo operation is available.
     *
     * Returns `true` if there are future states that can be restored after an undo operation.
     * This is a direct property access alternative to [canRedoState] for non-Compose usage.
     */
    val canRedo: Boolean

    /**
     * Set a new value (synchronous version for UI thread).
     *
     * This method is safe to call from the main/UI thread and will immediately
     * update the state and add the previous value to history if they differ.
     *
     * @param newValue The new value to set
     */
    fun setValueSync(newValue: T)

    /**
     * Set a new value (asynchronous version for use in coroutines).
     *
     * This method provides thread-safe access when called from coroutines
     * or background threads. Use [setValueSync] for UI thread operations.
     *
     * @param newValue The new value to set
     */
    suspend fun setValue(newValue: T)

    /**
     * Undo to the previous value (synchronous version).
     *
     * Restores the most recent value from history and moves the current value
     * to the redo history. Safe to call from the UI thread.
     *
     * This operation has no effect if [canUndo] is `false`.
     */
    fun undoSync()

    /**
     * Undo to the previous value (asynchronous version).
     *
     * Thread-safe version of [undoSync] for use in coroutines or background threads.
     *
     * This operation has no effect if [canUndo] is `false`.
     */
    suspend fun undo()

    /**
     * Redo to the next value (synchronous version).
     *
     * Restores the most recent value from redo history and moves the current value
     * back to the main history. Safe to call from the UI thread.
     *
     * This operation has no effect if [canRedo] is `false`.
     */
    fun redoSync()

    /**
     * Redo to the next value (asynchronous version).
     *
     * Thread-safe version of [redoSync] for use in coroutines or background threads.
     *
     * This operation has no effect if [canRedo] is `false`.
     */
    suspend fun redo()

    /**
     * Clear all history and future states.
     *
     * Removes all undo and redo history while preserving the current value.
     * After calling this method, both [canUndo] and [canRedo] will be `false`.
     */
    fun clear()

    /**
     * Get the current size of the undo history stack.
     *
     * @return Number of states that can be undone
     */
    fun getHistorySize(): Int

    /**
     * Get the current size of the redo history stack.
     *
     * @return Number of states that can be redone
     */
    fun getFutureSize(): Int
}