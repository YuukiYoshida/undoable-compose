# Undoable Compose

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.yuukiyoshida/undoable-compose.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.yuukiyoshida/undoable-compose)

A powerful and easy-to-use undo/redo state management library for Jetpack Compose.

## Features

* ✅ **Jetpack Compose Native** - Built specifically for Compose with reactive state updates
* ✅ **Thread-Safe** - Safe to use from any thread with async/sync API variants
* ✅ **Memory Efficient** - Automatic history size limiting prevents memory leaks
* ✅ **Customizable** - Pluggable equality checking for complex data types
* ✅ **Performance Optimized** - Uses ArrayDeque for O(1) operations
* ✅ **Well Tested** - Comprehensive test suite with 100% coverage

## Installation

To use this library via **Maven Central**, add the following to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.yuukiyoshida:undoable-compose:1.0.0")
}
```

## Quick Start

### Basic Usage

```kotlin
@Composable
fun TextEditor() {
    val textState = rememberUndoableState("")
    val currentText by textState.state
    val canUndo by textState.canUndoState
    val canRedo by textState.canRedoState

    Column {
        TextField(
            value = currentText,
            onValueChange = { textState.value = it }
        )

        Row {
            Button(
                onClick = { textState.undoSync() },
                enabled = canUndo
            ) {
                Text("Undo")
            }

            Button(
                onClick = { textState.redoSync() },
                enabled = canRedo
            ) {
                Text("Redo")
            }
        }
    }
}
```

### Advanced Usage

```kotlin
@Composable
fun AdvancedExample() {
    // Custom equality for case-insensitive text
    val textState = rememberUndoableState(
        initialValue = "Hello",
        equalityCheck = { old, new -> old.equals(new, ignoreCase = true) }
    )

    // UI omitted for brevity
}
```

## License

```
Copyright 2025 Yuki Yoshida

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```