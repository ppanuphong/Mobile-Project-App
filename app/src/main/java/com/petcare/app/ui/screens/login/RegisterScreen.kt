package com.petcare.app.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petcare.app.ui.components.PetCareScaffold
import com.petcare.app.viewmodel.AppViewModelProvider
import com.petcare.app.viewmodel.RegisterViewModel
import com.petcare.app.viewmodel.RegisterViewModel.Companion.FIELD_CONFIRM
import com.petcare.app.viewmodel.RegisterViewModel.Companion.FIELD_EMAIL
import com.petcare.app.viewmodel.RegisterViewModel.Companion.FIELD_NAME
import com.petcare.app.viewmodel.RegisterViewModel.Companion.FIELD_PASSWORD
import com.petcare.app.viewmodel.RegisterViewModel.Companion.FIELD_PHONE

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: RegisterViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.registeredUser) {
        if (state.registeredUser != null) onRegistered()
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    PetCareScaffold(title = "สมัครสมาชิก", onNavigateUp = onNavigateUp, snackbarHostState = snackbar) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "สร้างบัญชีเพื่อบันทึกข้อมูลสัตว์เลี้ยงไว้บนคลาวด์ ใช้ได้ทุกเครื่อง",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            val errors = state.errors
            RegisterField(state.name, viewModel::onNameChange, "ชื่อ-นามสกุล", Icons.Outlined.Person, errors[FIELD_NAME])
            RegisterField(
                state.phone, viewModel::onPhoneChange, "เบอร์โทรศัพท์ (ไม่บังคับ)", Icons.Outlined.Phone,
                errors[FIELD_PHONE], KeyboardType.Phone,
            )
            RegisterField(
                state.email, viewModel::onEmailChange, "อีเมล", Icons.Outlined.Email,
                errors[FIELD_EMAIL], KeyboardType.Email,
            )
            RegisterField(
                state.password, viewModel::onPasswordChange, "รหัสผ่าน (อย่างน้อย 6 ตัว)", Icons.Outlined.Lock,
                errors[FIELD_PASSWORD], KeyboardType.Password, isPassword = true,
            )
            RegisterField(
                state.confirmPassword, viewModel::onConfirmChange, "ยืนยันรหัสผ่าน", Icons.Outlined.Lock,
                errors[FIELD_CONFIRM], KeyboardType.Password, isPassword = true, imeAction = ImeAction.Done,
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = viewModel::register,
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("สมัครสมาชิก")
                }
            }
        }
    }
}

@Composable
private fun RegisterField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        modifier = Modifier.fillMaxWidth(),
    )
}
