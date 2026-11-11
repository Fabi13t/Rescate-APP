# Rescate · App Android

App nativa en **Kotlin + XML Views + ViewBinding + Material 3**, basada en el Figma y la arquitectura del proyecto
"Rescate de Alimentos y Comercio Justo".

## Cómo abrirlo en Android Studio

1. Clona el repositorio:
   ```
   git clone https://github.com/Fabi13t/Rescate-APP.git
   ```
   O en GitHub: **Code › Download ZIP** y descomprímelo.
2. Android Studio → **File › Open** → elige la carpeta `Rescate-APP` (la que tiene `settings.gradle.kts`).
3. Espera a que termine **Gradle Sync** (la primera vez descarga dependencias, necesita internet).
4. Elige un emulador (API 24 o superior; recomendado API 34/35) y pulsa ▶ **Run**.

> Requisitos: Android Studio Ladybug (2024.2) o más reciente, JDK 17 (viene incluido en Android Studio).
> Si Android Studio sugiere actualizar el *Android Gradle Plugin*, puedes aceptar.
> Si aparece "SDK location not found", Android Studio crea `local.properties` solo al abrir el proyecto;
> ese archivo no se sube al repositorio.

## Qué incluye el proyecto

```
Rescate-APP/
├── app/        ← App Android (Kotlin + XML + ViewBinding + Material 3)
└── backend/    ← API Node.js + Express + PostgreSQL (23 tablas)  → ver backend/README.md
```

- **Base de datos en la nube:** `backend/db/schema.sql` (21 tablas del modelo + `reclamos` + `calificaciones`)
  y la guía paso a paso para Neon + Render en `backend/README.md`.
- **Libro de reclamaciones:** el vecino registra un *reclamo* (producto) o *queja* (atención) desde
  Perfil › Libro de reclamaciones o desde una reserva en Mis reservas; el comercio lo responde en
  Perfil › Reclamos recibidos (plazo de 15 días hábiles).
- **Calificaciones:** después de recoger, el vecino califica con 1–5 estrellas en Mis reservas › Calificar.
  El promedio se ve en el detalle del pack (toca la estrella para ver opiniones) y el comercio responde en
  Perfil › Calificaciones.
- **Imágenes:** `app/src/main/res/drawable-nodpi/` (WebP optimizado, 25 imágenes, ~2 MB).

## Cómo probarla (modo demo)

La app trae `DEMO_MODE = true` (en `app/build.gradle.kts`), así funciona **sin backend** con datos de ejemplo.

| Rol | Cómo entrar |
| --- | --- |
| Vecino | Bienvenida › Comenzar / Ya tengo cuenta › cualquier correo válido + contraseña de 6+ caracteres |
| Comercio | "¿Tienes un comercio? Ingresa aquí" › mismo criterio |
| Error de login | Contraseña `error` (muestra la alerta animada) |
| Código de recuperación | Cualquier código de 6 dígitos |

**Flujo completo para la demo:** Inicio › un pack › Reservar › elegir cantidad › temporizador 03:00 ›
Pagar con Yape › escribir un número de operación (ej. `12345678`) › "Pago en revisión" › el comercio lo
aprueba (en demo se aprueba solo en ~4 s) › **Comprobante con QR**.

Luego cierra sesión, entra como **Comercio** y verás ese pago en *Pagos*, y el pedido en *Pedidos*,
donde puedes **escanear el QR** o ingresar el código para marcarlo como entregado.

## Conectar la API real (Node.js + Express)

Sigue `backend/README.md` (base de datos en Neon + API en Render o en tu PC). Luego:

1. En `app/build.gradle.kts` cambia `DEMO_MODE` a `false`.
2. `API_BASE_URL`: `http://10.0.2.2:3000/api/v1/` (API en tu PC) o `https://TU-APP.onrender.com/api/v1/` (nube).
3. Las rutas esperadas están en `data/remote/ApiService.kt`. El token JWT se agrega solo
   (interceptor en `ApiClient.kt`) y los errores 401 · 409 · 410 · 422 se convierten en mensajes de pantalla.

## Pruebas

| Tipo | Dónde | Cómo ejecutarlas |
| --- | --- | --- |
| Unitarias (JUnit, 16) | `app/src/test` | Clic derecho en `com.rescate.app (test)` › Run, o `./gradlew test` |
| Interfaz (Espresso, 2) | `app/src/androidTest` | Con el emulador encendido: clic derecho en `LoginFlowTest` › Run |
| API (node:test, 10) | `backend/test/api.test.js` | `cd backend` y `npm test` (necesita PostgreSQL y el `.env`) |

> Para Espresso, desactiva las animaciones del emulador: Ajustes › Sistema › Opciones para desarrolladores ›
> Escala de animación de ventana, de transición y de duración de animador en **Desactivada**.

## Seguridad de credenciales

- Las claves (`DATABASE_URL`, `JWT_SECRET`) van en `backend/.env` o en Render, nunca en el código.
- En el repositorio solo está `backend/.env.example` con valores de ejemplo: cópialo como `.env` y complétalo.
- El `.gitignore` excluye `.env`, `*.jks`, `*.keystore`, `local.properties`, `build/` y `node_modules/`.

## Ramas y commits

| Rama | Uso |
| --- | --- |
| `main` | Versiones estables y entregas |
| `develop` | Integración de los cambios |
| `feature/<tema>` | Una función nueva; entra a `develop` con Pull Request |

Los commits siguen el formato `tipo(módulo): descripción`, por ejemplo `feat(reservas): retención de 3 min`,
`fix(login): mensaje con correo vacío` o `docs: pasos para clonar y abrir el proyecto`.
Tipos: `feat`, `fix`, `test`, `docs`, `refactor`, `chore`.

## Arquitectura (igual al diagrama)

```
ui/ (Fragments + ViewBinding)  →  ViewModel (corrutinas, LiveData: carga · éxito · error)
   →  repository/ (elige red o caché)  →  remote/ (Retrofit + OkHttp + Gson, JWT)
                                       →  local/  (SQLite: favoritos, caché del feed, distritos
                                                   SharedPreferences: sesión JWT y preferencias)
```

```
com.rescate.app
├── data/model          Modelos (Pack, Store, Reservation, Payment, Donation…)
├── data/remote         ApiService (Retrofit), ApiClient (interceptor JWT, errores)
├── data/local          SessionManager (SharedPreferences), RescateDbHelper (SQLite)
├── data/repository     Auth, Pack, Reservation, Donation, Merchant, Feedback (reclamos y calificaciones)
├── data/demo           Datos de demostración
├── ui/auth             Splash, bienvenida, onboarding, login, recuperar, registro (4 pasos)
├── ui/home             Inicio (feed), búsqueda + filtros, notificaciones
├── ui/detail           Detalle del pack
├── ui/map              Mapa OpenStreetMap
├── ui/reserve          Cantidad, retención 03:00, pago Yape, comprobante QR
├── ui/reservations     Mis reservas
├── ui/impact           Mi impacto + gráfico animado
├── ui/profile          Perfil, editar, ajustes (5 canales), ayuda
├── ui/donation         Donar alimentos, mis donaciones
├── ui/merchant         Modo comercio: inicio, pedidos (QR), nuevo pack, pagos, perfil
├── ui/feedback         Libro de reclamaciones, calificaciones y opiniones
└── util                Animaciones, notificaciones, ubicación, QR
```

## Librerías

| Librería | Uso |
| --- | --- |
| Material Components 3 | Botones, chips, tarjetas, sliders, bottom sheets, time picker |
| Navigation Component | Navegación entre pantallas con transiciones |
| Lifecycle (ViewModel + LiveData) + Coroutines | Estados de pantalla y tareas en segundo plano |
| Retrofit + OkHttp + Gson | API REST con JWT |
| SQLite (SQLiteOpenHelper) | Favoritos, caché del feed y distritos |
| osmdroid | Mapa OpenStreetMap gratuito, sin API key |
| ZXing Android Embedded | Generar el QR del comprobante y escanearlo |
| Coil | Carga de fotos de packs (galería/cámara) |
| Facebook Shimmer | Esqueleto de carga animado |
| Play Services Location | Ubicación (FusedLocationProvider) |
| Core SplashScreen | Splash nativo |

## Animaciones (sutiles)

- Splash nativo y logo con rebote + textos escalonados en la bienvenida.
- Onboarding con transición de páginas y puntos que se estiran.
- Transiciones deslizantes entre pantallas y fundido entre pestañas.
- Listas que entran escalonadas, *shimmer* mientras carga, tarjetas que se encogen al tocarlas.
- Errores de formulario con sacudida; botones que se convierten en indicador de carga.
- Corazón de favorito con rebote; contador de cantidad con "pop".
- Temporizador circular que cambia de verde → ámbar → rojo y late los últimos 30 s.
- Check animado (AnimatedVectorDrawable) en pantallas de éxito.
- Números de impacto que cuentan desde 0 y gráfico de barras que crece.
- Tarjeta del mapa que sube al elegir un comercio.

## Notificaciones push (FCM)

Los 5 canales ya están creados (`NotificationHelper`). Para recibir push reales:
agrega Firebase al proyecto (`google-services.json` + plugin `com.google.gms.google-services`
+ `firebase-messaging`) y en tu `FirebaseMessagingService` llama a `NotificationHelper.show(...)`.
