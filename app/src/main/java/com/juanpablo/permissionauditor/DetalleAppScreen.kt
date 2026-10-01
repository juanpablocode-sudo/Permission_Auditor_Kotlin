package com.juanpablo.permissionauditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DetalleAppScreen(app: AppInfo, permisosPeligrosos: List<String>, onVolver: () -> Unit) {
    val resultado = RiskScorer.calculateDetailed(permisosPeligrosos)

    Column(modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .padding(16.dp)) {
        Button(onClick = onVolver) {
            Text("← Volver")
        }

        Text(
            text = app.packageName,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Score: ${resultado.score}",
            style = MaterialTheme.typography.titleMedium
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(text = "Permisos peligrosos", style = MaterialTheme.typography.titleSmall)

        LazyColumn(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(resultado.permisos) { permiso ->
                Text(text = "${permiso.nombre} — ${permiso.peso} pts")
            }

            if (resultado.combos.isNotEmpty()) {
                item {
                    Text(
                        text = "Combinaciones riesgosas",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                items(resultado.combos) { combo ->
                    Text(text = "⚠ ${combo.descripcion} (+${combo.puntos} pts)")
                }
            }
        }
    }
}