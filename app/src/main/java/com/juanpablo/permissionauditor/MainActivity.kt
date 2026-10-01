package com.juanpablo.permissionauditor

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.juanpablo.permissionauditor.ui.theme.PermissionAuditorTheme
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getDatabase(this)
        val dao = db.appDao()

        setContent {
            val haptic = LocalHapticFeedback.current
            var apps by remember { mutableStateOf<List<AppConIcono>>(emptyList()) }
            var escaneando by remember { mutableStateOf(false) }
            var appSeleccionada by remember { mutableStateOf<AppInfo?>(null) }
            val scope = rememberCoroutineScope()
            var busquedaActiva by remember { mutableStateOf(false) }
            var textoBusqueda by remember { mutableStateOf("") }

            suspend fun escanearYGuardar() {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                escaneando = true
                val appsInstaladas = packageManager.getInstalledPackages(0)


                val listaDeApps = mutableListOf<AppInfo>()
                var contadorFiltradas = 0

                for (app in appsInstaladas) {
                    val esAppDeSistema = (app.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
                    if (esAppDeSistema) {
                        contadorFiltradas++
                        continue
                    }

                    val nombrePaquete = app.packageName
                    val score = calcularScoreDeApp(packageManager, nombrePaquete)
                    val nombreVisible = try {
                        val appInfoDelSistema = packageManager.getApplicationInfo(nombrePaquete, 0)
                        packageManager.getApplicationLabel(appInfoDelSistema).toString()
                    } catch (e: PackageManager.NameNotFoundException) {
                        nombrePaquete
                    }
                    listaDeApps.add(AppInfo(nombrePaquete,nombreVisible, score))
                }


                dao.borrarTodas()
                dao.insertarTodas(listaDeApps)
                val listaOrdenada = dao.obtenerTodasOrdenadas()

                apps = listaOrdenada.map { appInfo ->
                    val icono = try {
                        packageManager.getApplicationIcon(appInfo.packageName)
                    } catch (e: PackageManager.NameNotFoundException) {
                        null
                    }
                    AppConIcono(appInfo, icono)

                }

                escaneando = false
            }

            LaunchedEffect(Unit) {
                val listaGuardada = dao.obtenerTodasOrdenadas()
                apps = listaGuardada.map{ appInfo ->
                    val icono = try {
                        packageManager.getApplicationIcon(appInfo.packageName)
                    } catch (e: PackageManager.NameNotFoundException) {
                        null
                    }
                    AppConIcono(appInfo, icono)
                }
                if (apps.isEmpty()) {
                    escanearYGuardar()
                }
            }

            val appsFiltradas = if (textoBusqueda.isBlank()) {
                apps
            } else {
                apps.filter { it.info.appName.contains(textoBusqueda, ignoreCase = true) }
            }
            PermissionAuditorTheme {
                if (appSeleccionada != null) {
                    val permisos = obtenerPermisosPeligrosos(
                        packageManager,
                        appSeleccionada!!.packageName
                    )
                    DetalleAppScreen(
                        app = appSeleccionada!!,
                        permisosPeligrosos = permisos,
                        onVolver = { appSeleccionada = null }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            TopAppBar(
                                title = {
                                    if (busquedaActiva) {
                                        TextField(
                                            value = textoBusqueda,
                                            onValueChange = {textoBusqueda = it},
                                            placeholder = { Text("Buscar app...")},
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        Text("Permission Auditor")
                                    }
                                },
                                actions = {
                                    if (busquedaActiva) {
                                        IconButton(onClick = {
                                            busquedaActiva = false
                                            textoBusqueda = ""
                                        }) {
                                            Icon(Icons.Filled.Close, contentDescription = "Cerrar Busqueda")
                                        }
                                    } else {
                                        IconButton(onClick = {busquedaActiva = true}) {
                                            Icon(Icons.Filled.Search, contentDescription = "Buscar")
                                        }
                                    }
                                }
                            )
                        },
                        floatingActionButton = {
                            FloatingActionButton(onClick = {
                                scope.launch { escanearYGuardar() }
                            }) {
                                Text("↻")
                            }
                        }
                    ) { innerPadding ->
                        if (escaneando) {
                            LazyColumn(
                                modifier = Modifier.padding(innerPadding),
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(6) {
                                    ShimmerAppCard()
                                }
                            }
                        } else {
                            SimpleList(
                                apps = appsFiltradas,
                                modifier = Modifier.padding(innerPadding),
                                onAppClick = { appSeleccionada = it }
                            )
                        }
                    }
                }
            }
        }
    }
}

fun obtenerPermisosPeligrosos(packageManager: PackageManager, packageName: String): List<String> {
    val permisosPeligrosos = mutableListOf<String>()
    try {
        val packageInfo =
            packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        val permisos = packageInfo.requestedPermissions

        if (permisos != null) {
            for (permiso in permisos) {
                try {
                    val permInfo = packageManager.getPermissionInfo(permiso, 0)
                    val esDangerous =
                        (permInfo.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE) ==
                                PermissionInfo.PROTECTION_DANGEROUS

                    if (esDangerous) {
                        val nombreCorto = permiso.substringAfterLast(".")
                        permisosPeligrosos.add(nombreCorto)
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    // permiso no encontrado, lo ignoramos
                }
            }
        }
    } catch (e: PackageManager.NameNotFoundException) {
        // paquete no encontrado, devolvemos lista vacía
    }
    return permisosPeligrosos
}

fun calcularScoreDeApp(packageManager: PackageManager, packageName: String): Int {
    val permisosPeligrosos = obtenerPermisosPeligrosos(packageManager, packageName)
    return RiskScorer.calculate(permisosPeligrosos)
}

@Entity(tableName = "apps")
data class AppInfo(
    @PrimaryKey val packageName: String,
    val appName: String,
    val score: Int
)
data class AppConIcono(
    val info: AppInfo,
    val icono: Drawable?
)
fun Drawable.toImageBitmap(): ImageBitmap {
    val bitmap = createBitmap(intrinsicWidth.coerceAtLeast(1), intrinsicHeight.coerceAtLeast(1))
    val canvas = android.graphics.Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap.asImageBitmap()
}

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition("shimmer")
    val translateanim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslate"
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f),
        ),
        start = Offset(translateanim - 500f, 0f),
        end = Offset(translateanim, 0f)
    )
    background(brush)
}

@Composable
fun ShimmerAppCard(){
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(50))
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier
                .size(width = 48.dp, height = 28.dp)
                .clip(RoundedCornerShape(50))
                .shimmerEffect()
            )
        }
    }
}
@Composable
fun SimpleList(apps: List<AppConIcono>, modifier: Modifier = Modifier, onAppClick: (AppInfo) -> Unit) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(apps) { appConIcono ->
            val app = appConIcono.info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAppClick(app) },
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (appConIcono.icono != null) {
                        Image(
                            bitmap = remember(app.packageName) { appConIcono.icono.toImageBitmap() },
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = app.appName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ScoreChip(score = app.score)
                }
            }
        }
    }
}
    @Composable
    fun ScoreChip(score: Int) {
        val color = when {
            score <= 30 -> Color(0xFF2E7D32)
            score <= 60 -> Color(0xFFF9A825)
                        else -> Color(0xFFC62828)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(color)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ){
                        Text(text = "$score", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }

    @Preview(showBackground = true)
    @Composable
fun GreetingPreview() {
    PermissionAuditorTheme {
        SimpleList(
            apps = listOf(AppConIcono(AppInfo("com.package.app", "App de ejemplo", 42), null)),
            onAppClick = {}
        )
    }
}
