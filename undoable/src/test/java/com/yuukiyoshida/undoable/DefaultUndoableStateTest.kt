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

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DefaultUndoableStateTest {

    @Test
    fun `creates with correct initial state`() {
        val state = DefaultUndoableState("initial")

        assertEquals("initial", state.value)
        assertFalse("Should not be able to undo initially", state.canUndo)
        assertFalse("Should not be able to redo initially", state.canRedo)
        assertEquals("History should be empty initially", 0, state.getHistorySize())
        assertEquals("Future should be empty initially", 0, state.getFutureSize())
    }

    @Test
    fun `updates value and maintains history`() {
        val state = DefaultUndoableState("A")

        state.value = "B"
        assertEquals("B", state.value)
        assertTrue("Should be able to undo after change", state.canUndo)
        assertFalse("Should not be able to redo", state.canRedo)
        assertEquals("History should contain one item", 1, state.getHistorySize())

        state.value = "C"
        assertEquals("C", state.value)
        assertEquals("History should contain two items", 2, state.getHistorySize())
    }

    @Test
    fun `undo restores previous values correctly`() {
        val state = DefaultUndoableState("A")
        state.value = "B"
        state.value = "C"

        state.undoSync()
        assertEquals("Should restore to B", "B", state.value)
        assertTrue("Should still be able to undo", state.canUndo)
        assertTrue("Should be able to redo", state.canRedo)

        state.undoSync()
        assertEquals("Should restore to A", "A", state.value)
        assertFalse("Should not be able to undo further", state.canUndo)
        assertTrue("Should be able to redo", state.canRedo)
    }

    @Test
    fun `redo restores future values correctly`() {
        val state = DefaultUndoableState("A")
        state.value = "B"
        state.value = "C"
        state.undoSync()
        state.undoSync()

        state.redoSync()
        assertEquals("Should restore to B", "B", state.value)
        assertTrue("Should be able to undo", state.canUndo)
        assertTrue("Should be able to redo", state.canRedo)

        state.redoSync()
        assertEquals("Should restore to C", "C", state.value)
        assertTrue("Should be able to undo", state.canUndo)
        assertFalse("Should not be able to redo further", state.canRedo)
    }

    @Test
    fun `respects maximum history size`() {
        val state = DefaultUndoableState(0, maxHistory = 3)

        // Add more values than maxHistory
        for (i in 1..5) {
            state.value = i
        }

        assertEquals("Current value should be 5", 5, state.value)
        assertEquals("History should be limited to maxHistory", 3, state.getHistorySize())

        // Verify oldest values were removed
        state.undoSync() // 5 -> 4
        assertEquals(4, state.value)
        state.undoSync() // 4 -> 3
        assertEquals(3, state.value)
        state.undoSync() // 3 -> 2
        assertEquals(2, state.value)

        // Should not be able to undo further (value 1 and 0 were discarded)
        assertFalse("Should not be able to undo beyond history limit", state.canUndo)
    }

    @Test
    fun `ignores duplicate consecutive values`() {
        val state = DefaultUndoableState("A")

        state.value = "A" // Same value
        assertFalse("Should not add duplicate to history", state.canUndo)
        assertEquals("History should remain empty", 0, state.getHistorySize())

        state.value = "B"
        assertTrue("Should add different value to history", state.canUndo)

        state.value = "B" // Same value again
        assertEquals("History should not grow for duplicate", 1, state.getHistorySize())
    }

    @Test
    fun `new value clears redo history`() {
        val state = DefaultUndoableState("A")
        state.value = "B"
        state.value = "C"
        state.undoSync() // Now at B, can redo to C

        assertTrue("Should be able to redo", state.canRedo)

        state.value = "D" // New value should clear redo
        assertFalse("Redo should be cleared", state.canRedo)

        state.undoSync()
        assertEquals("Should undo to B, not C", "B", state.value)
    }

    @Test
    fun `clear removes all history`() {
        val state = DefaultUndoableState("A")
        state.value = "B"
        state.value = "C"
        state.undoSync() // Create both history and future

        assertTrue("Should have undo history", state.canUndo)
        assertTrue("Should have redo history", state.canRedo)
        assertEquals("Current value should be B", "B", state.value)

        state.clear()

        assertFalse("Undo should be cleared", state.canUndo)
        assertFalse("Redo should be cleared", state.canRedo)
        assertEquals("Current value should be preserved", "B", state.value)
        assertEquals("History should be empty", 0, state.getHistorySize())
        assertEquals("Future should be empty", 0, state.getFutureSize())
    }

    @Test
    fun `custom equality check works correctly`() {
        val state = DefaultUndoableState(
            "Hello",
            equalityCheck = { a, b -> a.lowercase() == b.lowercase() }
        )

        state.value = "HELLO" // Same when case-insensitive
        assertFalse("Should not add case variant to history", state.canUndo)
        assertEquals("Value should not change", "Hello", state.value)

        state.value = "World"
        assertTrue("Should add genuinely different value", state.canUndo)
        assertEquals("Value should change", "World", state.value)
    }

    @Test
    fun `async operations work correctly`() = runTest {
        val state = DefaultUndoableState("A")

        state.setValue("B")
        assertEquals("Async setValue should work", "B", state.value)

        state.undo()
        assertEquals("Async undo should work", "A", state.value)

        state.redo()
        assertEquals("Async redo should work", "B", state.value)
    }

    @Test
    fun `interface contract is properly implemented`() {
        val state: UndoableState<String> = DefaultUndoableState("Test")

        // Verify all interface methods work
        state.value = "Updated"
        assertTrue("Interface: canUndo should work", state.canUndo)

        state.undoSync()
        assertEquals("Interface: undoSync should work", "Test", state.value)
        assertTrue("Interface: canRedo should work", state.canRedo)

        state.redoSync()
        assertEquals("Interface: redoSync should work", "Updated", state.value)

        state.clear()
        assertFalse("Interface: clear should work", state.canUndo)
        assertFalse("Interface: clear should work", state.canRedo)
    }

    @Test
    fun `compose state integration provides reactive updates`() {
        val state = DefaultUndoableState("Initial")

        // Access Compose states
        val valueState = state.state
        val canUndoState = state.canUndoState
        val canRedoState = state.canRedoState

        assertEquals("Initial value state", "Initial", valueState.value)
        assertFalse("Initial undo state", canUndoState.value)
        assertFalse("Initial redo state", canRedoState.value)

        state.value = "Updated"
        assertEquals("Updated value state", "Updated", valueState.value)
        assertTrue("Updated undo state", canUndoState.value)
        assertFalse("Updated redo state", canRedoState.value)

        state.undoSync()
        assertEquals("Undone value state", "Initial", valueState.value)
        assertFalse("Undone undo state", canUndoState.value)
        assertTrue("Undone redo state", canRedoState.value)
    }

    @Test
    fun `toString provides useful debugging information`() {
        val state = DefaultUndoableState("Test")
        state.value = "Updated"
        state.undoSync()

        val stringRepresentation = state.toString()
        assertTrue("Should contain current value", stringRepresentation.contains("Test"))
        assertTrue("Should contain history info", stringRepresentation.contains("historySize"))
        assertTrue("Should contain future info", stringRepresentation.contains("futureSize"))
    }

    @Test
    fun `validates constructor parameters`() {
        assertThrows("Should reject negative maxHistory", IllegalArgumentException::class.java) {
            DefaultUndoableState("test", maxHistory = -1)
        }

        assertThrows("Should reject zero maxHistory", IllegalArgumentException::class.java) {
            DefaultUndoableState("test", maxHistory = 0)
        }

        // Should accept positive maxHistory
        val state = DefaultUndoableState("test", maxHistory = 1)
        assertNotNull("Should create with maxHistory = 1", state)
    }

    @Test
    fun `operations on empty history are safe`() {
        val state = DefaultUndoableState("Test")

        // These should not throw exceptions
        state.undoSync() // No history
        assertEquals("Value should remain unchanged", "Test", state.value)

        state.redoSync() // No future
        assertEquals("Value should remain unchanged", "Test", state.value)

        state.clear() // Nothing to clear
        assertEquals("Value should remain unchanged", "Test", state.value)
    }
}