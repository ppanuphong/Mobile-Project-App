package com.petcare.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.petcare.app.ui.theme.PetCareTheme

/** แถบบนสุดสีเขียวมรกตเข้ม */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetCareTopBar(
    title: String,
    subtitle: String? = null,
    onNavigateUp: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = PetCareTheme.colors
    TopAppBar(
        title = {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.onTopBarMuted, maxLines = 1)
                }
            }
        },
        navigationIcon = {
            if (onNavigateUp != null) {
                IconButton(onClick = onNavigateUp) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ย้อนกลับ")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = c.topBar,
            titleContentColor = c.onTopBar,
            navigationIconContentColor = c.onTopBar,
            actionIconContentColor = c.onTopBar,
        ),
    )
}

/**
 * Scaffold มาตรฐานของทุกหน้าจอ
 * @param isTabScreen หน้าที่มี Bottom Navigation อยู่แล้ว ไม่ต้องเว้นที่ให้แถบนำทางของระบบอีก
 */
@Composable
fun PetCareScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onNavigateUp: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState? = null,
    isTabScreen: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = { PetCareTopBar(title, subtitle, onNavigateUp, actions) },
        floatingActionButton = floatingActionButton,
        snackbarHost = { if (snackbarHostState != null) SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = if (isTabScreen) WindowInsets(0) else WindowInsets.navigationBars.union(WindowInsets.ime),
        content = content,
    )
}
