# Rescate · Backend (Node.js + Express + PostgreSQL)

API REST `/api/v1` que usa la app Android. Base de datos **PostgreSQL** con las **21 tablas** del modelo
+ **2 nuevas**: `reclamos` (Libro de Reclamaciones) y `calificaciones` (puntuación de 1 a 5 estrellas).

```
backend/
├── db/schema.sql        ← crea las 23 tablas, trigger del promedio de estrellas, índices
├── db/seed.sql          ← datos de prueba (mismos comercios y packs del modo demo)
├── scripts/init-db.js   ← ejecuta schema.sql + seed.sql en la base de DATABASE_URL
├── src/index.js         ← servidor Express
├── src/db.js            ← conexión a PostgreSQL (pg)
├── src/auth.js          ← JWT + tabla sesiones
├── src/jobs.js          ← libera reservas vencidas (3 min) y cierra packs
├── src/mappers.js       ← convierte filas SQL al JSON que espera la app
├── src/notify.js        ← tabla notificaciones (+ Firebase opcional)
└── src/routes/          ← auth, packs, reservations, merchant, impact, donations,
                           notifications, complaints (reclamos), ratings (calificaciones)
```

**Usuarios de prueba** (contraseña `123456`): `vecino@rescate.pe` · `comercio@rescate.pe` (Panadería Don Pedro)

---

## 1. Base de datos en la nube con Neon (gratis)

1. Entra a **https://neon.tech** y crea una cuenta (puedes usar tu cuenta de Google).
2. **Create project** → nombre `rescate` → región *AWS US East* (la más cercana a Perú con plan gratis) → PostgreSQL 16.
3. En **Dashboard › Connection string** copia la cadena. Se ve así:
   `postgresql://usuario:clave@ep-xxxx.us-east-2.aws.neon.tech/neondb?sslmode=require`
4. Crea las tablas. Tienes dos formas:
   - **Desde el navegador (sin instalar nada):** en Neon abre **SQL Editor**, pega todo el contenido de
     `db/schema.sql` y pulsa *Run*. Luego pega `db/seed.sql` y pulsa *Run*.
   - **Desde tu PC:** sigue el paso 2 y ejecuta `npm run db:init`.
5. Verifica en **Tables**: deben aparecer 23 tablas.

> Alternativa: **Supabase** (https://supabase.com) también da PostgreSQL gratis; usa su *SQL Editor* igual.

## 2. Ejecutar la API en tu PC

Requisitos: **Node.js 18 o superior** (https://nodejs.org).

```bash
cd backend
npm install
cp .env.example .env          # en Windows: copy .env.example .env
```

Edita `.env`:

```
DATABASE_URL=postgresql://usuario:clave@ep-xxxx.us-east-2.aws.neon.tech/neondb?sslmode=require
DATABASE_SSL=true
JWT_SECRET=una-frase-larga-y-secreta
PORT=3000
PUBLIC_URL=http://10.0.2.2:3000
```

```bash
npm run db:init    # crea las 23 tablas y carga los datos de prueba (¡borra lo que haya!)
npm run dev        # inicia la API en http://localhost:3000/api/v1
```

Prueba en el navegador: **http://localhost:3000/api/v1/health** → `{"ok":true,"db":"conectada","tablas":23}`

## 3. Subir la API a internet con Render (gratis)

1. Sube el proyecto a **GitHub** (la carpeta `backend` dentro del repositorio).
2. En **https://render.com** → *New* → **Web Service** → conecta tu repositorio.
3. Configura:
   - **Root Directory:** `backend`
   - **Build Command:** `npm install`
   - **Start Command:** `npm start`
   - **Environment variables:** `DATABASE_URL` (la de Neon), `DATABASE_SSL=true`,
     `JWT_SECRET` (una frase larga), `PUBLIC_URL` (la URL que te dé Render, ej. `https://rescate-api.onrender.com`)
4. *Create Web Service*. Cuando termine, abre `https://TU-APP.onrender.com/api/v1/health`.

> En el plan gratis, Render "duerme" la API tras 15 min sin uso; la primera petición tarda ~50 s en despertar.
> Las fotos subidas se guardan en disco temporal: para producción conviene Cloudinary o S3.

## 4. Conectar la app Android

En `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://TU-APP.onrender.com/api/v1/\"")   // o http://10.0.2.2:3000/api/v1/ en local
buildConfigField("boolean", "DEMO_MODE", "false")
```

*Sync* y ▶ Run. Entra con `vecino@rescate.pe` / `123456`.

---

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/auth/login` · `/auth/register` | Sesión JWT (tabla `sesiones`) |
| POST | `/auth/recover` · `/auth/reset` | Código de 6 dígitos (`codigos_verificacion`) |
| GET/PUT | `/auth/me` | Perfil |
| GET | `/packs?lat=&lon=&km=&category=&q=` | Feed por distancia y categoría |
| GET | `/packs/:id` · `/stores` · `/stores/:id/ratings` | Detalle, comercios del mapa, opiniones |
| POST | `/packs/:id/favorite` · `/packs/:id/alert` | Favoritos y alertas de stock |
| POST | `/reservations` | **Retiene stock 3 min** (409 si se agotó) |
| POST | `/reservations/:id/yape` | Reporta operación Yape (410 si venció) |
| PATCH | `/reservations/:id/cancel` | Cancela y devuelve stock |
| GET | `/reservations/me` · `/reservations/:id` | Mis reservas |
| GET | `/impact/me?period=resumen\|semanal\|mensual\|total` | Impacto (guarda `reportes_impacto`) |
| POST/GET | `/donations` · `/donations/me` · PATCH `/donations/:id/withdraw` | Donaciones (máx. 7 días si es perecible) |
| GET/PATCH | `/notifications/me` · `/notifications/read-all` · `/notifications/preferences` | Avisos |
| POST | `/notifications/support` | Ticket de soporte |
| POST/GET | `/merchant/packs` | Publicar (multipart con foto) y listar packs |
| GET | `/merchant/orders` · `/merchant/payments` | Pedidos y pagos del comercio |
| PATCH | `/merchant/payments/:id/approve` · `/reject` | Validar Yape |
| PATCH | `/merchant/reservations/:code/pickup` | Recojo (QR) |
| PUT | `/merchant/yape` | Número de cobro |
| POST/GET | `/complaints` · `/complaints/me` | **Libro de reclamaciones** (vecino) |
| GET/PATCH | `/merchant/complaints` · `/merchant/complaints/:id/respond` | Reclamos recibidos y respuesta |
| POST | `/ratings` | **Calificar** (solo reservas recogidas, 1 vez) |
| GET/PATCH | `/merchant/ratings` · `/merchant/ratings/:id/reply` | Calificaciones recibidas y respuesta |

Errores: siempre `{"message": "..."}` con 401 (sesión), 403 (permiso), 404, 409 (conflicto/stock), 410 (reserva vencida), 422 (datos).
