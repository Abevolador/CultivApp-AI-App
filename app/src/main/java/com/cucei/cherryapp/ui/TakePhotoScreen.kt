package com.cucei.cherryapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakePhotoScreen(
    onTakePhoto: () -> Unit,
    onBack: () -> Unit,
    error: String?
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tomar foto") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = onTakePhoto) {
                    Text("Tomar foto")
                }
                if (error != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
} 