package com.juanpablo.permissionauditor

object PermissionWeights {
    val weights: Map<String, Int> = mapOf(
        "READ_SMS" to 9,
        "READ_CONTACTS" to 8,
        "ACCESS_FINE_LOCATION" to 8,
        "CAMERA" to 6,
        "RECORD_AUDIO" to 7,
        "READ_CALL_LOG" to 8,
        "BLUETOOTH_CONNECT" to 4,
        "BLUETOOTH_SCAN" to 5
    )
    fun weightFor(shortName: String): Int = weights[shortName] ?: 3
}

data class PermisoDetalle(val nombre: String, val peso: Int)
data class ComboBonus(val descripcion: String, val puntos: Int)
data class RiskResult(val score: Int, val permisos: List<PermisoDetalle>, val combos: List<ComboBonus>)

object RiskScorer {

    fun calculate(dangerousPermissionShortNames: List<String>): Int {
        return calculateDetailed(dangerousPermissionShortNames).score
    }

    fun calculateDetailed(dangerousPermissionShortNames: List<String>): RiskResult {
        val permisos = dangerousPermissionShortNames.map {
            PermisoDetalle(it, PermissionWeights.weightFor(it))
        }
        val scoreBase = permisos.sumOf { it.peso }

        val combos = mutableListOf<ComboBonus>()

        if (dangerousPermissionShortNames.contains("READ_SMS") &&
            dangerousPermissionShortNames.contains("READ_CONTACTS")
        ) {
            combos.add(ComboBonus("SMS + Contactos: podría leer tus mensajes y filtrar tu agenda", 15))
        }

        if (dangerousPermissionShortNames.contains("CAMERA") &&
            dangerousPermissionShortNames.contains("RECORD_AUDIO")
        ) {
            combos.add(ComboBonus("Cámara + Micrófono: podría grabar audio y video sin que lo notes", 15))
        }

        if (dangerousPermissionShortNames.contains("ACCESS_FINE_LOCATION") &&
            dangerousPermissionShortNames.contains("BLUETOOTH_SCAN")
        ) {
            combos.add(ComboBonus("Ubicación + Bluetooth: podría rastrear tu posición con más precisión", 10))
        }

        val scoreTotal = (scoreBase + combos.sumOf { it.puntos }).coerceIn(0, 100)

        return RiskResult(scoreTotal, permisos, combos)
    }
}