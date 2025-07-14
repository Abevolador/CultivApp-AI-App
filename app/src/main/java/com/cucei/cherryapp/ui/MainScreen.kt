package com.cucei.cherryapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MainScreen(onNavigate: (Screen) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Cherry", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(32.dp))
        Button(onClick = { onNavigate(Screen.LocalJson) }, modifier = Modifier.fillMaxWidth()) {
            Text("Abrir archivo JSON local")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onNavigate(Screen.NetworkJson) }, modifier = Modifier.fillMaxWidth()) {
            Text("Cargar datos desde la red")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onNavigate(Screen.TakePhoto) }, modifier = Modifier.fillMaxWidth()) {
            Text("Tomar foto")
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onNavigate(Screen.Gallery) }, modifier = Modifier.fillMaxWidth()) {
            Text("Ver galería de fotos")
        }
    }
}

sealed class Screen {
    object Main : Screen()
    object LocalJson : Screen()
    object NetworkJson : Screen()
    object TakePhoto : Screen()
    object Gallery : Screen()
} 