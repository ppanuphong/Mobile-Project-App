package com.petcare.app.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petcare.app.ui.theme.Fraunces
import com.petcare.app.ui.theme.PetCareTheme
import com.petcare.app.viewmodel.AppViewModelProvider
import com.petcare.app.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onRegister: () -> Unit,
    viewModel: LoginViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var showPassword by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.loggedInUser) {
        if (state.loggedInUser != null) onLoggedIn()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        // ส่วนหัวสีเขียวมรกตเข้ม
        Box(
            Modifier
                .fillMaxWidth()
                .background(PetCareTheme.colors.topBar)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 56.dp),
        ) {
            Column {
                Surface(shape = RoundedCornerShape(16.dp), color = PetCareTheme.colors.amber) {
                    Text("🐾", fontSize = 28.sp, modifier = Modifier.padding(10.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "PetCare",
                    fontFamily = Fraunces,
                    fontSize = 40.sp,
                    lineHeight = 44.sp,
                    color = PetCareTheme.colors.onTopBar,
                )
                Text(
                    "บันทึกวัคซีนและนัดหมอของสัตว์เลี้ยง ไม่พลาดทุกนัดสำคัญ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = PetCareTheme.colors.onTopBarMuted,
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-28).dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("เข้าสู่ระบบ", style = MaterialTheme.typography.headlineSmall)

                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text("อีเมล") },
                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                    isError = state.emailError != null,
                    supportingText = state.emailError?.let { { Text(it) } },
                    singleLine = true,
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("รหัสผ่าน") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (showPassword) "ซ่อนรหัสผ่าน" else "แสดงรหัสผ่าน",
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = state.passwordError != null,
                    supportingText = state.passwordError?.let { { Text(it) } },
                    singleLine = true,
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); viewModel.login() }),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.errorMessage != null) {
                    Surface(
                        color = PetCareTheme.colors.rustContainer,
                        contentColor = PetCareTheme.colors.onRustContainer,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ErrorOutline, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text(state.errorMessage.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Button(
                    onClick = { focusManager.clearFocus(); viewModel.login() },
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.size(12.dp))
                        Text("กำลังเข้าสู่ระบบ…")
                    } else {
                        Text("เข้าสู่ระบบ")
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(Modifier.weight(1f))
                    Text(
                        "หรือ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    HorizontalDivider(Modifier.weight(1f))
                }

                OutlinedButton(
                    onClick = { focusManager.clearFocus(); viewModel.loginDemo() },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("ทดลองใช้ด้วยบัญชีเดโม") }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("ยังไม่มีบัญชี?", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onRegister, enabled = !state.isLoading) { Text("สมัครสมาชิก") }
        }
    }
}
