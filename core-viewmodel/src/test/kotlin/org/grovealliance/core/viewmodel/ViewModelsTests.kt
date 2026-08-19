//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.core.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.google.common.truth.Truth.assertThat
import org.grovealliance.core.GroveError
import org.grovealliance.core.requireDependency
import org.grovealliance.core.viewmodel.internal.ViewModelFactories
import org.grovealliance.testing.core.testGroveApplication
import org.junit.Test

class ViewModelsTests {

    @Test
    fun `it should register a single viewModel factory`() {
        // given
        testGroveApplication {
            viewModel { CounterViewModel() }
        }

        // when
        val factories = requireDependency<ViewModelFactories>()
        val instance = factories.create(CounterViewModel::class, SavedStateHandle())

        // then
        assertThat(instance).isInstanceOf(CounterViewModel::class.java)
    }

    @Test
    fun `it should accumulate factories across multiple viewModel calls`() {
        // given
        testGroveApplication {
            viewModel { CounterViewModel() }
            viewModel { MessageViewModel() }
        }

        // when
        val factories = requireDependency<ViewModelFactories>()
        val counter = factories.create(CounterViewModel::class, SavedStateHandle())
        val message = factories.create(MessageViewModel::class, SavedStateHandle())

        // then
        assertThat(counter).isInstanceOf(CounterViewModel::class.java)
        assertThat(message).isInstanceOf(MessageViewModel::class.java)
    }

    @Test
    fun `it should override a factory when the same ViewModel type is registered twice`() {
        // given
        testGroveApplication {
            viewModel { MessageViewModel(text = "first") }
            viewModel { MessageViewModel(text = "second") }
        }

        // when
        val factories = requireDependency<ViewModelFactories>()
        val instance = factories.create(MessageViewModel::class, SavedStateHandle())

        // then
        assertThat(instance.text).isEqualTo("second")
    }

    @Test
    fun `it should resolve a module dependency inside a viewModel factory`() {
        // given
        val repo = UserRepository(name = "Alice")
        testGroveApplication {
            singleton { repo }
            viewModel { ProfileViewModel(repository = dependency()) }
        }

        // when
        val factories = requireDependency<ViewModelFactories>()
        val vm = factories.create(ProfileViewModel::class, SavedStateHandle())

        // then
        assertThat(vm.repository).isEqualTo(repo)
    }

    @Test
    fun `it should resolve an optional dependency inside a viewModel factory`() {
        // given
        testGroveApplication {
            viewModel { ProfileViewModel(repository = optionalDependency()) }
        }

        // when
        val factories = requireDependency<ViewModelFactories>()
        val vm = factories.create(ProfileViewModel::class, SavedStateHandle())

        // then
        assertThat(vm.repository).isNull()
    }

    @Test
    fun `it should expose the savedStateHandle inside a viewModel factory`() {
        // given
        val handle = SavedStateHandle(mapOf("id" to "42"))
        testGroveApplication {
            viewModel { DetailViewModel(handle = savedStateHandle()) }
        }

        // when
        val factories = requireDependency<ViewModelFactories>()
        val vm = factories.create(DetailViewModel::class, handle)

        // then
        assertThat(vm.handle.get<String>("id")).isEqualTo("42")
    }

    @Test
    fun `it should throw a grove error when creating an unregistered viewModel`() {
        // given
        testGroveApplication {
            viewModel { CounterViewModel() }
        }

        // when
        val factories = requireDependency<ViewModelFactories>()
        val error = runCatching {
            factories.create(MessageViewModel::class, SavedStateHandle())
        }.exceptionOrNull() as? GroveError

        // then
        assertThat(error).isNotNull()
        assertThat(error?.message).contains("MessageViewModel")
    }
}

private class CounterViewModel : ViewModel()

private class MessageViewModel(val text: String = "") : ViewModel()

private data class UserRepository(val name: String)

private class ProfileViewModel(val repository: UserRepository?) : ViewModel()

private class DetailViewModel(val handle: SavedStateHandle) : ViewModel()
