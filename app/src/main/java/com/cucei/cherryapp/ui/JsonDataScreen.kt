package com.cucei.cherryapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api

// Modelo de datos
data class Registro(val temperatura: String, val humedad: String, val luminosidad: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonDataScreen(
    registros: List<Registro>?,
    error: String?,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Datos JSON") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                error != null -> {
                    Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                }
                registros == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn {
                        itemsIndexed(registros) { idx, reg ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Registro #${idx + 1}", style = MaterialTheme.typography.titleMedium)
                                    Text("Temperatura: ${reg.temperatura}")
                                    Text("Humedad: ${reg.humedad}")
                                    Text("Luminosidad: ${reg.luminosidad}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 