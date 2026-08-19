//
// This source file is part of the My Heart Counts Android open-source project
//
// SPDX-FileCopyrightText: 2026 Stanford University and the project authors (see CONTRIBUTORS.md)
//
// SPDX-License-Identifier: MIT

package org.grovealliance.ui.validation.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.grovealliance.ui.testIdentifier
import org.grovealliance.ui.validation.OutlinedValidatedTextField
import org.grovealliance.ui.validation.ReceiveValidation
import org.grovealliance.ui.validation.Validate
import org.grovealliance.ui.validation.ValidatedTextField
import org.grovealliance.ui.validation.ValidationContext
import org.grovealliance.ui.validation.ValidationRule
import org.grovealliance.ui.validation.minimalPassword
import org.grovealliance.ui.validation.nonEmpty

enum class Field {
    INPUT, NON_EMPTY_INPUT
}

enum class FocusValidationRulesTestIdentifier {
    EMAIL_TEXTFIELD, PASSWORD_TEXTFIELD
}

@Composable
fun FocusValidationRules() {
    val input = remember { mutableStateOf("") }
    val nonEmptyInput = remember { mutableStateOf("") }
    val validationContext = remember { mutableStateOf(ValidationContext()) }
    val lastValid = remember { mutableStateOf<Boolean?>(null) }

    ReceiveValidation(validationContext) {
        Column {
            Text("Has Engines: ${if (!validationContext.value.isEmpty) "Yes" else "No"}")
            Text("Input Valid: ${if (validationContext.value.allInputValid) "Yes" else "No"}")
            lastValid.value?.let { lastValid ->
                Text("Last state: ${if (lastValid) "valid" else "invalid"}")
            }
            Button(
                onClick = {
                    lastValid.value = validationContext.value
                        .validateHierarchy()
                }
            ) {
                Text("Validate")
            }

            Validate(nonEmptyInput.value, rules = listOf(ValidationRule.nonEmpty)) {
                ValidatedTextField(
                    value = nonEmptyInput.value,
                    onValueChange = { nonEmptyInput.value = it },
                    modifier = Modifier.testIdentifier(FocusValidationRulesTestIdentifier.EMAIL_TEXTFIELD),
                    label = {
                        Text(Field.NON_EMPTY_INPUT.name)
                    },
                )
            }

            Validate(input.value, rules = listOf(ValidationRule.minimalPassword)) {
                OutlinedValidatedTextField(
                    value = input.value,
                    onValueChange = { input.value = it },
                    modifier = Modifier.testIdentifier(FocusValidationRulesTestIdentifier.PASSWORD_TEXTFIELD),
                    label = {
                        Text(Field.INPUT.name)
                    },
                )
            }
        }
    }
}
