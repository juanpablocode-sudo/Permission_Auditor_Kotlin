# 🔐 PermissionAuditor

**PermissionAuditor** es una aplicación Android desarrollada con **Kotlin y Jetpack Compose** que permite analizar las aplicaciones instaladas en un dispositivo, consultar los permisos que solicitan y obtener una evaluación de riesgo basada en dichos permisos.

El proyecto está orientado al aprendizaje práctico de:

* Android Security
* Mobile Security
* Privacidad
* Análisis de permisos
* Ciberdefensa
* Desarrollo Android con Kotlin

---

## 🛡️ Security Focus

Las aplicaciones Android pueden solicitar diferentes permisos para acceder a recursos y funciones del dispositivo.

PermissionAuditor permite visualizar estos permisos y realizar una evaluación basada en los permisos considerados sensibles o peligrosos.

El flujo principal de análisis es:

```text
Aplicación instalada
        │
        ▼
Permisos detectados
        │
        ▼
Clasificación de permisos
        │
        ├── Permisos normales
        │
        └── Permisos sensibles
                 │
                 ▼
             Risk Score
                 │
                 ▼
          Evaluación visual
```

El objetivo es transformar información técnica relacionada con Android en información más fácil de interpretar.

---

# 🚀 Características

## 📱 Aplicaciones instaladas

PermissionAuditor permite consultar las aplicaciones instaladas en el dispositivo y seleccionar una aplicación para analizarla.

## 🔎 Búsqueda de aplicaciones

Incluye una función de búsqueda para encontrar rápidamente una aplicación instalada.

Esto permite analizar aplicaciones específicas sin tener que recorrer manualmente toda la lista.

## 🔐 Análisis de permisos

La aplicación analiza los permisos asociados a las aplicaciones.

Los permisos son clasificados para facilitar su interpretación.

### 🟢 Permisos normales

Permisos que representan un nivel de acceso menor según la clasificación utilizada por la aplicación.

### 🔴 Permisos sensibles

Permisos que pueden proporcionar acceso a información o recursos relevantes para la privacidad y seguridad del usuario.

---

# 📊 Risk Score

PermissionAuditor incorpora un sistema de evaluación de riesgo implementado en:

```text
RiskScorer.kt
```

El sistema utiliza un modelo heurístico basado en pesos asignados a determinados permisos.

Además, algunas combinaciones de permisos generan puntos adicionales.

> El Risk Score es una evaluación basada en permisos y no representa por sí solo una confirmación de que una aplicación sea maliciosa.

---

## ⚖️ Pesos de permisos

Actualmente el sistema utiliza los siguientes valores:

| Permiso                   | Peso |
| ------------------------- | ---: |
| `READ_SMS`                |    9 |
| `READ_CONTACTS`           |    8 |
| `ACCESS_FINE_LOCATION`    |    8 |
| `READ_CALL_LOG`           |    8 |
| `RECORD_AUDIO`            |    7 |
| `CAMERA`                  |    6 |
| `BLUETOOTH_SCAN`          |    5 |
| `BLUETOOTH_CONNECT`       |    4 |
| Otros permisos analizados |    3 |

Si un permiso no está definido explícitamente en la tabla de pesos, el sistema utiliza un valor predeterminado de **3 puntos**.

---

# 🧮 ¿Cómo se calcula el Risk Score?

Primero se calcula un score base sumando el peso de cada permiso detectado.

```text
Score base = suma de los pesos de los permisos
```

Después se comprueban determinadas combinaciones de permisos.

```text
Score total =
    Score base
    +
    Bonificaciones por combinaciones
```

Finalmente, el resultado se limita a un máximo de **100 puntos**.

```text
Risk Score = 0 ... 100
```

---

# 🔗 Combinaciones de permisos

Actualmente el sistema contempla tres combinaciones especiales.

## 📱 SMS + Contactos

Cuando una aplicación posee:

```text
READ_SMS
+
READ_CONTACTS
```

se agregan:

```text
+15 puntos
```

Descripción utilizada por el sistema:

> SMS + Contactos: podría leer tus mensajes y filtrar tu agenda.

---

## 📷 Cámara + Micrófono

Cuando una aplicación posee:

```text
CAMERA
+
RECORD_AUDIO
```

se agregan:

```text
+15 puntos
```

Esta combinación representa el acceso conjunto a cámara y audio.

---

## 📍 Ubicación + Bluetooth

Cuando una aplicación posee:

```text
ACCESS_FINE_LOCATION
+
BLUETOOTH_SCAN
```

se agregan:

```text
+10 puntos
```

Esta combinación representa una capacidad potencialmente mayor para obtener información relacionada con la ubicación y dispositivos cercanos.

---

# 🧪 Ejemplo de cálculo

Supongamos que una aplicación solicita:

```text
READ_SMS
READ_CONTACTS
CAMERA
```

El cálculo sería:

```text
READ_SMS       = 9
READ_CONTACTS  = 8
CAMERA         = 6
-------------------
Score base     = 23
```

La aplicación también tiene la combinación:

```text
READ_SMS + READ_CONTACTS
```

Por lo tanto:

```text
Bonus = +15
```

Resultado:

```text
23 + 15 = 38
```

### Risk Score

```text
38 / 100
```

---

# 🧠 Interpretación del Risk Score

El Risk Score es una **evaluación heurística**, no una detección de malware.

Un score elevado significa que la aplicación solicita una combinación de permisos que el modelo considera más sensible.

No significa automáticamente que la aplicación sea maliciosa.

```text
Score elevado
      ≠
Malware confirmado
```

El resultado debe interpretarse teniendo en cuenta la función y el contexto de cada aplicación.

---

# 🧩 Componentes del sistema de riesgo

El sistema de evaluación está dividido en diferentes componentes.

## `PermissionWeights`

Mantiene los pesos asignados a los permisos.

```kotlin
PermissionWeights
```

Permite centralizar los valores utilizados por el algoritmo.

---

## `PermisoDetalle`

Representa un permiso analizado junto con su peso.

```kotlin
data class PermisoDetalle(
    val nombre: String,
    val peso: Int
)
```

---

## `ComboBonus`

Representa una bonificación generada por una combinación específica de permisos.

```kotlin
data class ComboBonus(
    val descripcion: String,
    val puntos: Int
)
```

---

## `RiskResult`

Agrupa el resultado completo del análisis:

```kotlin
data class RiskResult(
    val score: Int,
    val permisos: List<PermisoDetalle>,
    val combos: List<ComboBonus>
)
```

El resultado contiene:

* Score final.
* Permisos analizados.
* Bonificaciones aplicadas.

---

## `RiskScorer`

Contiene la lógica principal del cálculo.

Dispone de:

```kotlin
calculate()
```

para obtener el score.

Y:

```kotlin
calculateDetailed()
```

para obtener información detallada del análisis.

---

# 🏗️ Estructura del proyecto

La estructura principal del proyecto es:

```text
com.juanpablo.permissionauditor
│
├── MainActivity.kt
│
├── AppDao.kt
├── AppDatabase.kt
├── DetalleAppScreen.kt
├── RiskScorer.kt
│
└── ui
    └── theme
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

---

# 📄 Principales archivos

### `MainActivity.kt`

Es el punto de entrada principal de la aplicación Android.

Inicializa la aplicación y configura la interfaz desarrollada con Jetpack Compose.

### `DetalleAppScreen.kt`

Contiene la pantalla de detalle de una aplicación seleccionada.

Permite visualizar información relacionada con sus permisos y su evaluación de riesgo.

### `RiskScorer.kt`

Contiene la lógica utilizada para calcular el Risk Score.

Analiza los permisos, asigna pesos y aplica bonificaciones cuando se detectan determinadas combinaciones.

### `AppDao.kt`

Contiene las operaciones de acceso a datos utilizadas por la aplicación.

### `AppDatabase.kt`

Define la configuración de la base de datos local utilizada por la aplicación.

### `ui/theme/`

Contiene la configuración visual de Jetpack Compose:

```text
Color.kt
Theme.kt
Type.kt
```

---

# 🛠️ Tecnologías utilizadas

| Tecnología      | Uso                     |
| --------------- | ----------------------- |
| Kotlin          | Lenguaje principal      |
| Android         | Plataforma              |
| Jetpack Compose | Interfaz de usuario     |
| Material 3      | Componentes visuales    |
| Android SDK     | APIs del sistema        |
| Room            | Persistencia de datos   |
| Gradle          | Sistema de construcción |
| Android Studio  | Entorno de desarrollo   |

---

# 🔐 Mobile Security

PermissionAuditor permite trabajar de forma práctica con conceptos relacionados con:

* Android Permissions
* Dangerous Permissions
* Normal Permissions
* Package Manager
* Installed Applications
* Application Metadata
* Risk Assessment
* Privacy
* Mobile Security
* Defensive Security

El proyecto está planteado desde una perspectiva **educativa y defensiva**.

---

# 🧠 ¿Qué problema intenta resolver?

Los usuarios pueden tener muchas aplicaciones instaladas y no siempre resulta sencillo comprender qué permisos solicitan.

PermissionAuditor intenta simplificar este análisis:

```text
¿Qué aplicaciones tengo?
          ↓
¿Qué permisos solicitan?
          ↓
¿Cuáles son sensibles?
          ↓
¿Qué Risk Score obtiene el análisis?
```

La aplicación permite consultar esta información desde una única interfaz.

---

# 🔒 Privacidad

PermissionAuditor está diseñado como una herramienta de análisis local.

El propósito del proyecto es consultar información disponible en el dispositivo y analizar los permisos asociados a las aplicaciones.

La aplicación no pretende modificar los permisos de otras aplicaciones ni acceder de manera no autorizada a sus datos.

---

# 🎯 Objetivo del proyecto

PermissionAuditor forma parte de mi aprendizaje en:

**Android + Kotlin + Mobile Cybersecurity**

El proyecto busca combinar desarrollo de software con conceptos de **ciberseguridad móvil y ciberdefensa**.

A través de este proyecto se practican conceptos de desarrollo Android y análisis de información relacionada con aplicaciones y permisos.

---

# 🔮 Roadmap

## Implementado

* [x] Listado de aplicaciones
* [x] Búsqueda de aplicaciones
* [x] Consulta de permisos
* [x] Clasificación de permisos
* [x] Identificación de permisos sensibles
* [x] Risk Score
* [x] Análisis de combinaciones de permisos
* [x] Pantalla de detalle
* [x] Interfaz con Jetpack Compose
* [x] Persistencia de datos

## Próximas mejoras

* [ ] Explicación individual de cada permiso
* [ ] Mostrar permisos concedidos frente a permisos declarados
* [ ] Mejorar el modelo de Risk Score
* [ ] Incorporar más combinaciones de permisos
* [ ] Filtros por categoría
* [ ] Ordenar aplicaciones por Risk Score
* [ ] Historial de análisis
* [ ] Exportación de informes
* [ ] Tests automatizados
* [ ] Mejoras de accesibilidad
* [ ] Información adicional sobre seguridad

---

# 🧪 Aprendizaje

PermissionAuditor es un proyecto práctico para estudiar:

```text
Kotlin
   ↓
Android
   ↓
Jetpack Compose
   ↓
Android APIs
   ↓
Permisos
   ↓
Análisis de riesgo
   ↓
Mobile Security
```

El proyecto continuará evolucionando a medida que se incorporen nuevos conocimientos sobre Android y ciberseguridad móvil.

---

# 🤖 Desarrollo asistido por IA

PermissionAuditor fue desarrollado con asistencia de herramientas de **inteligencia artificial** utilizadas como apoyo durante el proceso de aprendizaje y desarrollo.

La IA se utilizó para explorar soluciones, analizar problemas, revisar implementaciones y acelerar determinadas etapas del desarrollo.

La integración, configuración, pruebas y evolución del proyecto forman parte del proceso de desarrollo del autor.

---

# 👨‍💻 Autor

## Juan Pablo Code

Proyecto personal enfocado en:

**Android · Kotlin · Mobile Security · Ciberdefensa**

Este proyecto forma parte de mi proceso de aprendizaje y construcción de un portfolio orientado al desarrollo y la seguridad de aplicaciones móviles.

---

# 📄 Licencia

Proyecto desarrollado con fines educativos y de aprendizaje.

Consultar el archivo `LICENSE` del repositorio para conocer las condiciones específicas de uso.
