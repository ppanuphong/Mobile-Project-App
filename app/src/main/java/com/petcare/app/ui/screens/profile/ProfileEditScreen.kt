package com.petcare.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petcare.app.ui.components.CoverImage
import com.petcare.app.ui.components.ImagePickerLauncher
import com.petcare.app.ui.components.LoadingBox
import com.petcare.app.ui.components.PetCareScaffold
import com.petcare.app.ui.components.ProfileAvatar
import com.petcare.app.ui.components.rememberImagePicker
import com.petcare.app.ui.theme.PetCareTheme
import com.petcare.app.viewmodel.AppViewModelProvider
import com.petcare.app.viewmodel.ProfileEditViewModel

@Composable
fun ProfileEditScreen(
    onDone: () -> Unit,
    viewModel: ProfileEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val photoPicker = rememberImagePicker(viewModel::onPhotoPicked)
    val coverPicker = rememberImagePicker(viewModel::onCoverPicked)

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    PetCareScaffold(
        title = "แก้ไขโปรไฟล์",
        onNavigateUp = onDone,
        snackbarHostState = snackbar,
        actions = {
            TextButton(onClick = viewModel::save, enabled = !state.isSaving && !state.isLoading) {
                Text("บันทึก", color = PetCareTheme.colors.onTopBar)
            }
        },
    ) { padding ->
        if (state.isLoading) {
            LoadingBox(Modifier.padding(padding))
            return@PetCareScaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // รูปโปรไฟล์
            Text("รูปโปรไฟล์", style = MaterialTheme.typography.titleLarge)
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(contentAlignment = Alignment.Center) {
                    ProfileAvatar(state.name, state.photo, size = 128.dp, borderWidth = 0.dp)
                    if (state.processingPhoto) ProcessingOverlay(Modifier.size(128.dp).clip(CircleShape))
                }
            }
            PhotoButtons(photoPicker, hasPhoto = state.photo.isNotBlank(), enabled = !state.processingPhoto, onRemove = viewModel::onRemovePhoto)

            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            // รูปหน้าปก
            Text("รูปหน้าปก", style = MaterialTheme.typography.titleLarge)
            Box(Modifier.clip(RoundedCornerShape(12.dp))) {
                CoverImage(state.coverPhoto, height = 150.dp)
                if (state.processingCover) ProcessingOverlay(Modifier.matchParentSize())
            }
            PhotoButtons(coverPicker, hasPhoto = state.coverPhoto.isNotBlank(), enabled = !state.processingCover, onRemove = viewModel::onRemoveCover)

            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            // ข้อมูลส่วนตัว
            Text("ข้อมูลส่วนตัว", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("ชื่อ-นามสกุล *") },
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.bio,
                onValueChange = viewModel::onBioChange,
                label = { Text("แนะนำตัว") },
                placeholder = { Text("เช่น ทาสแมว 2 ตัว รักการถ่ายรูปน้อง ๆ") },
                supportingText = { Text("${state.bio.length}/${ProfileEditViewModel.BIO_MAX}") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.phone,
                onValueChange = viewModel::onPhoneChange,
                label = { Text("เบอร์โทรศัพท์") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.location,
                onValueChange = viewModel::onLocationChange,
                label = { Text("อาศัยอยู่ที่") },
                placeholder = { Text("เช่น กรุงเทพมหานคร") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.email,
                onValueChange = {},
                label = { Text("อีเมล") },
                supportingText = { Text("ใช้อีเมลนี้เข้าสู่ระบบ จึงแก้ไขไม่ได้") },
                enabled = false,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("บันทึกโปรไฟล์")
                }
            }
        }
    }
}

@Composable
private fun PhotoButtons(picker: ImagePickerLauncher, hasPhoto: Boolean, enabled: Boolean, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        TextButton(onClick = picker.pickFromGallery, enabled = enabled) {
            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("เลือกรูป")
        }
        TextButton(onClick = picker.takePhoto, enabled = enabled) {
            Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("ถ่ายรูป")
        }
        if (hasPhoto) {
            TextButton(onClick = onRemove, enabled = enabled) {
                Text("ลบรูป", color = PetCareTheme.colors.rust)
            }
        }
    }
}

@Composable
private fun ProcessingOverlay(modifier: Modifier) {
    Box(modifier.background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
    }
}
