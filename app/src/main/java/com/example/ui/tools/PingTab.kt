package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun PingTab() {
    var host by remember { mutableStateOf("github.com") }
    var portStr by remember { mutableStateOf("443") }
    var isPinging by remember { mutableStateOf(false) }
    var pingResultMsg by remember { mutableStateOf("") }
    var latencyMs by remember { mutableStateOf<Long?>(null) }
    val pingHistory = remember { mutableStateListOf<Long>() }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Prueba de Conectividad (Ping CI/CD)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Verifica la disponibilidad y latencia de red de tus servidores de integración continua, GitHub Actions, Jenkins, o APIs de Google Play.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host o Dirección IP") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = portStr,
                    onValueChange = { portStr = it },
                    label = { Text("Puerto") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val port = portStr.toIntOrNull() ?: 443
                        isPinging = true
                        pingResultMsg = ""
                        latencyMs = null

                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            val startTime = System.nanoTime()
                            var success = false
                            try {
                                java.net.Socket().use { socket ->
                                    socket.connect(java.net.InetSocketAddress(host, port), 4000)
                                    success = true
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            val endTime = System.nanoTime()
                            val duration = (endTime - startTime) / 1_000_000

                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                isPinging = false
                                if (success) {
                                    latencyMs = duration
                                    pingResultMsg = "Conexión exitosa a $host:$port"
                                    pingHistory.add(0, duration)
                                    if (pingHistory.size > 5) {
                                        pingHistory.removeAt(5)
                                    }
                                } else {
                                    latencyMs = null
                                    pingResultMsg = "No se pudo conectar a $host en el puerto $port. Servidor inaccesible o fuera de línea."
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isPinging && host.isNotEmpty()
                ) {
                    if (isPinging) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.NetworkCheck, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Realizar Ping de Conexión")
                    }
                }
            }
        }

        if (pingResultMsg.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (latencyMs != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = pingResultMsg,
                        color = if (latencyMs != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    latencyMs?.let { latency ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = if (latency < 100) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Latencia (RTT): $latency ms",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (latency < 100) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }

        if (pingHistory.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Historial Reciente (Latencia RTT)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    pingHistory.forEachIndexed { index, ms ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ping #${pingHistory.size - index}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (ms < 100) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                             ) {
                                Text(
                                    text = "$ms ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ms < 100) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
