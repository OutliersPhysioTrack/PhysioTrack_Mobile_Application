package org.example.physiotrack.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.physiotrack.model.RegistrationDraft
import org.example.physiotrack.ui.components.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingProfileScreen(
    onBack: () -> Unit,
    onContinue: (RegistrationDraft) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("Select a condition") }
    var conditionExpanded by remember { mutableStateOf(false) }

    val conditions = listOf(
        "Post-surgery rehabilitation",
        "Chronic pain management",
        "Stroke recovery",
        "Sports injury",
        "Arthritis",
        "General mobility improvement",
        "Other",
    )

    val canContinue =
        name.isNotBlank() &&
                age.isNotBlank() &&
                height.isNotBlank() &&
                weight.isNotBlank() &&
                phone.isNotBlank() &&
                condition != "Select a condition"

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val nameFocus = remember { FocusRequester() }
    val ageFocus = remember { FocusRequester() }
    val heightFocus = remember { FocusRequester() }
    val weightFocus = remember { FocusRequester() }
    val phoneFocus = remember { FocusRequester() }

    val scrollState = rememberScrollState()
    val nameBring = remember { BringIntoViewRequester() }
    val ageBring = remember { BringIntoViewRequester() }
    val heightBring = remember { BringIntoViewRequester() }
    val weightBring = remember { BringIntoViewRequester() }
    val phoneBring = remember { BringIntoViewRequester() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Your Profile", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },

        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp)
                    .navigationBarsPadding()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "This information helps us provide appropriate exercise recommendations and safety monitoring tailored to your needs.",
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(Modifier.height(12.dp))

                PrimaryButton(
                    text = "Continue",
                    enabled = canContinue,
                    onClick = {
                        val draft = RegistrationDraft(
                            age = age.toIntOrNull() ?: 0,
                            heightCm = height.toIntOrNull() ?: 0,
                            weightKg = weight.toDoubleOrNull() ?: 0.0,
                            primaryCondition = condition,
                            phone = phone,
                            name = name.trim(),
                        )
                        onContinue(draft)
                    },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 180.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            Text(
                "Help us personalize your experience",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("Enter your name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(nameFocus)
                    .bringIntoViewRequester(nameBring)
                    .onFocusEvent { fs -> if (fs.isFocused) scope.launch { nameBring.bringIntoView() } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { ageFocus.requestFocus() }
                ),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = age,
                onValueChange = { age = it.filter(Char::isDigit).take(3) },
                label = { Text("Age") },
                placeholder = { Text("Enter your age") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(ageFocus)
                    .bringIntoViewRequester(ageBring)
                    .onFocusEvent { fs -> if (fs.isFocused) scope.launch { ageBring.bringIntoView() } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { heightFocus.requestFocus() }
                ),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = height,
                onValueChange = { height = it.filter(Char::isDigit).take(3) },
                label = { Text("Height (cm)") },
                placeholder = { Text("Enter your height") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(heightFocus)
                    .bringIntoViewRequester(heightBring)
                    .onFocusEvent { fs -> if (fs.isFocused) scope.launch { heightBring.bringIntoView() } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { weightFocus.requestFocus() }
                ),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it.filter(Char::isDigit).take(3) },
                label = { Text("Weight (kg)") },
                placeholder = { Text("Enter your weight") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(weightFocus)
                    .bringIntoViewRequester(weightBring)
                    .onFocusEvent { fs -> if (fs.isFocused) scope.launch { weightBring.bringIntoView() } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { phoneFocus.requestFocus() }
                ),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter(Char::isDigit).take(20) },
                label = { Text("Phone") },
                placeholder = { Text("812xxxxxxx") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(phoneFocus)
                    .bringIntoViewRequester(phoneBring)
                    .onFocusEvent { fs -> if (fs.isFocused) scope.launch { phoneBring.bringIntoView() } },
                singleLine = true,
                visualTransformation = IdPhonePrefixVisualTransformation("+62 "),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
            )

            Spacer(Modifier.height(14.dp))
            Text("Primary Condition", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = conditionExpanded,
                onExpandedChange = { conditionExpanded = !conditionExpanded },
            ) {
                OutlinedTextField(
                    value = condition,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    singleLine = true,
                )

                DropdownMenu(
                    expanded = conditionExpanded,
                    onDismissRequest = { conditionExpanded = false },
                ) {
                    conditions.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                condition = item
                                conditionExpanded = false
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}


private class IdPhonePrefixVisualTransformation(
    private val prefix: String
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val transformed = AnnotatedString(prefix + text.text)
        val prefixLen = prefix.length

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset + prefixLen
            override fun transformedToOriginal(offset: Int): Int = (offset - prefixLen).coerceAtLeast(0)
        }

        return TransformedText(transformed, mapping)
    }
}
