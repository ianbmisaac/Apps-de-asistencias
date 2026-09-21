# Apps de Asistencia — Caso 3: Profesor / Alumno con Firebase Realtime Database

Proyecto académico de dos aplicaciones Android independientes que gestionan asistencia en
tiempo real usando **Firebase Realtime Database**:

| App | Carpeta | Paquete | Rol |
|-----|---------|---------|-----|
| **Asistencia Profesor** | `Profesor/` | `com.example.profesor` | Crea la sesión, comparte el código, ve los asistentes en vivo y cierra la sesión |
| **Asistencia Alumno** | `Alumnos/` | `com.example.alumnos` | Ingresa el código, registra su identificación y confirma su asistencia |

---

## Requisitos

- [Android Studio](https://developer.android.com/studio) (proyectos probados con el JBR de Android Studio).
- JDK 11+ (incluido en Android Studio).
- Proyecto de Firebase con **Realtime Database** habilitado.

Tecnologías y versiones principales:

| Componente | Versión |
|------------|---------|
| Android Gradle Plugin | `9.0.0-beta05` (ver nota abajo) |
| Gradle | 9.1.0 |
| compileSdk / targetSdk | 36 |
| minSdk | 29 |
| Google Services | 4.4.4 |
| Firebase (`firebase-database`) | 21.0.0 |
| Material Components | 1.x |

> **Nota sobre la versión de AGP:** se usa `9.0.0-beta05` porque coincide con el Android
> Studio utilizado para el desarrollo. Si el proyecto no sincroniza en otra máquina, baja el
> AGP a una versión estable (p. ej. `8.7.3`) en `gradle/libs.versions.toml` y sincroniza de nuevo.

---

## Estructura del repositorio

```
Apps-de-asistencias/
├── Profesor/                  # App del profesor (código + google-services.json)
│   ├── app/src/main/java/com/example/profesor/
│   │   ├── MainActivity.java              # P-PF01 Panel del profesor
│   │   ├── NuevaSesionActivity.java       # P-PF02 Crear sesión
│   │   ├── SesionActivaActivity.java      # P-PF03 Sesión activa (listado en vivo)
│   │   ├── AlumnosAdapter.java            # Adaptador del listado en vivo
│   │   ├── AlumnoAsistente.java           # Modelo de un alumno registrado
│   │   └── data/SesionRepository.java     # Capa de acceso a Firebase (RNF-03)
│   └── database.rules.json                # Reglas de la Realtime Database
└── Alumnos/                   # App del alumno (código + google-services.json)
    ├── app/src/main/java/com/example/alumnos/
    │   ├── MainActivity.java              # P-AL01 Ingreso del código
    │   ├── RegistroActivity.java          # P-AL02 Formulario de registro
    │   ├── ConfirmacionActivity.java      # P-AL03 Confirmación con hora
    │   └── data/SesionRepository.java     # Capa de acceso a Firebase (RNF-03)
    └── database.rules.json                # Reglas de la Realtime Database (idénticas)
```

---

## Configuración del proyecto de Firebase

1. Crea un proyecto en [Firebase Console](https://console.firebase.google.com/) (en este
   trabajo se usó `app-asistencia-41e6a`).
2. Agrega la **Realtime Database** (proyecto `app-asistencia-41e6a-default-rtdb`).
3. El archivo `google-services.json` de cada app ya está incluido (uno por paquete):
   - `Profesor/app/google-services.json` → paquete `com.example.profesor`
   - `Alumnos/app/google-services.json` → paquete `com.example.alumnos`

> Si creas otro proyecto de Firebase, reemplaza estos archivos descargándolos desde la
> consola (Configuración del proyecto → Tus apps → `google-services.json`).

### Reglas de la Realtime Database

Publica en **Realtime Database → Reglas** el contenido de `database.rules.json`:

```json
{
  "rules": {
    "sesiones": {
      ".indexOn": ["codigo"],
      "$idSesion": {
        ".read": true,
        ".write": true,
        "alumnos": {
          "$idAlumno": {
            ".write": "!data.exists() || !newData.exists()"
          }
        }
      }
    },
    ".read": true,
    ".write": true
  }
}
```

La regla de `alumnos/$idAlumno` garantiza que:
- un alumno solo se pueda registrar una vez por sesión (bloquea duplicados), y
- un registro existente solo se pueda **eliminar** (para corregir un ID), nunca sobrescribir.

---

## Estructura de datos

```
sesiones/{idSesion}/
├── activa: true|false
├── codigo: "3898"                       (4 dígitos, único)
├── curso: "Programación Android"
├── creadoEn: "2026-09-20T23:52:15Z"     (fecha de creación)
└── alumnos/{idAlumno}/
    ├── nombre: "Maria Lopez"
    └── horaRegistro: "2026-09-20T20:43:51"
```

La hora de registro se guarda como la hora local del dispositivo del alumno y se muestra
idéntica en ambas apps (sin conversión de zona horaria).

---

## Cómo compilar y ejecutar

1. Abre **`Profesor/`** y **`Alumnos/`** como dos proyectos separados en Android Studio.
2. Espera que Gradle sincronice (debe haber conexión a internet la primera vez).
3. Ejecuta la app con un emulador o dispositivo **con los servicios de Google / Google Play**.
4. Flujo de prueba:
   1. En **Profesor**: crea una sesión → aparece el código de 4 dígitos.
   2. En **Alumno**: ingresa el mismo código + tu identificación → se registra.
   3. En **Profesor**: el alumno aparece al instante con sonido y animación.

> Ambos dispositivos deben estar conectados a internet y apuntar al mismo proyecto de Firebase.

---

## Funcionalidades implementadas

### App Profesor
- Crear sesión con código único de 4 dígitos (RF-03, RF-04, RF-05).
- Listado de asistentes **en tiempo real** (RF-07, RF-08, RF-16).
- Cerrar sesión y volver al panel (RF-09); bloquea nuevos registros.
- **Extras:** botón de copiar código, alerta sonora + animación al llegar un alumno,
  orden por hora de llegada, fecha en ítems de días anteriores, hora de inicio de la sesión,
  eliminar un alumno mal registrado (toque largo), scroll automático.

### App Alumno
- Buscar sesión por código (RF-12) y registrar asistencia (RF-13).
- Validaciones con mensajes de error en español (ERR-01 a ERR-06).
- Confirmación con "hoy, 20:53" y **feedback háptico y sonoro**.
- **Extras:** barra/estado de carga durante el registro, animación de entrada.

---

## Seguridad

- **No subir a este repositorio** el archivo de *service account* de Firebase
  (`*-firebase-adminsdk-*.json`). Es una credencial de administrador y debe quedar local
  (por ejemplo, en la carpeta `Descargas`). Los `.gitignore` del proyecto ya excluyen
  `build/`, `.gradle/`, `.idea/` y `local.properties`.
- Se recomienda mantener el repositorio **privado**, ya que `google-services.json` expone
  la configuración del proyecto de Firebase.