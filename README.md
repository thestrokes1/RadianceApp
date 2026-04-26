# Radiance — Fashion App

App Android de indumentaria e-commerce, desarrollada como proyecto portfolio. Permite explorar, buscar y comprar ropa con una experiencia de usuario moderna y elegante.

**Stack:** Kotlin · Jetpack Compose · Firebase · MVVM

> ⚠️ **Nota:** La app utiliza Firebase en plan gratuito (Spark). No está publicada en Play Store — se distribuye como APK firmado desde GitHub Releases.

---

## Download

**[⬇ Descargar APK](https://github.com/thestrokes1/RadianceApp/releases/latest)**  
Requisito mínimo: Android 8.0 (API 26)

**Instalación manual:**
1. Descargá el archivo `.apk` desde Releases
2. En tu Android: Ajustes → Seguridad → Activar *"Fuentes desconocidas"*
3. Abrí el `.apk` descargado e instalá

---

## Pantallas

| Pantalla | Descripción |
|---------|------------|
| **Splash** | Animación de entrada con logo de marca |
| **Onboarding** | 3 slides presentando las funcionalidades (con skip) |
| **Login / Registro** | Email + contraseña · Google Sign-In |
| **Home** | Grid de productos con filtros por categoría y sort por precio/rating |
| **Detalle de producto** | Galería de imágenes · selección de talle y color · carrito / wishlist |
| **Búsqueda** | Búsqueda en tiempo real por nombre, marca y categoría |
| **Carrito** | Items con quantity selector · resumen de precios · checkout |
| **Wishlist** | Grilla de productos guardados |
| **Checkout** | Flujo 3 pasos: dirección → resumen → confirmación |
| **Perfil** | Estadísticas · modo oscuro · idioma (ES/EN) · logout |

---

## Features

- **Autenticación completa** — login, registro y Google Sign-In via Firebase Auth
- **Catálogo dinámico** — productos cargados desde Firestore con fallback a datos locales
- **Filtros y sort** — por categoría (Mujer, Hombre, Niños, Accesorios, Sale) y por precio/rating/novedad
- **Rango de precio** — slider $0–$200
- **Carrito persistente** — quantity adjustments, eliminar ítems, calcular total con descuentos
- **Wishlist** — agregar/quitar con un tap
- **Checkout real** — guarda órdenes en Firestore con ID único
- **Modo oscuro** — toggle en perfil, persistido con DataStore
- **Bilingüe** — español e inglés (strings.xml), persistido con DataStore
- **Shimmer loading** — skeleton UI mientras cargan los productos
- **Admin panel** — CRUD de productos en Firestore (acceso: tap 5 veces en versión)

---

## Tech Stack

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Kotlin 100% |
| UI | Jetpack Compose + Material Design 3 |
| Arquitectura | MVVM (ViewModel + StateFlow) |
| Navegación | Jetpack Navigation Compose |
| Backend | Firebase Firestore (southamerica-east1) |
| Autenticación | Firebase Auth + Google Identity |
| Imágenes | Coil Compose |
| Skeleton | Compose Shimmer |
| Preferencias | DataStore Preferences |
| Build | Gradle Kotlin DSL |
| Min SDK | API 26 (Android 8.0) |
| Target SDK | API 35 |

---

## Arquitectura

```
com.cristianv.radianceapp/
├── model/              # Product, User, Order, CartItem (data classes)
├── data/               # FirebaseRepository, AuthRepository, SampleData (fallback)
├── viewmodel/          # HomeViewModel, CartViewModel, WishlistViewModel, AuthViewModel
├── ui/
│   ├── screens/        # Una pantalla por archivo
│   │   ├── SplashScreen.kt
│   │   ├── OnboardingScreen.kt
│   │   ├── AuthScreen.kt
│   │   ├── HomeScreen.kt
│   │   ├── ProductDetailScreen.kt
│   │   ├── CartScreen.kt
│   │   ├── WishlistScreen.kt
│   │   ├── SearchScreen.kt
│   │   ├── CheckoutScreen.kt
│   │   ├── OtherScreens.kt      (ProfileScreen)
│   │   └── AdminScreen.kt
│   ├── components/     # ProductCard, ShimmerCard
│   └── theme/          # Color.kt, Type.kt, Theme.kt
└── MainActivity.kt     # NavHost + ViewModels compartidos
```

**Decisiones de arquitectura clave:**
- Los ViewModels se instancian en `MainActivity` y se pasan como parámetros — evita el bug de carrito vacío al navegar entre pantallas
- `FirebaseRepository` intenta Firestore primero; si falla usa `SampleData` como fallback — nunca pantalla vacía
- Todo el estado es `StateFlow` — sin `LiveData`

---

## Paleta de diseño

| Color | Hex | Uso |
|-------|-----|-----|
| Negro | `#0A0A0A` | Background principal |
| Dorado | `#C9A84C` | Accent, botones, badges |
| Gris oscuro | `#1A1A1A` | Cards, superficies |
| Blanco | `#FFFFFF` | Texto principal |

---

## Build local

```bash
# Requiere Android Studio + SDK API 26+
git clone https://github.com/thestrokes1/RadianceApp.git

# Abrí el proyecto en Android Studio
# Sync Gradle → Run en emulador o dispositivo físico

# Build release APK (requiere keystore)
./gradlew assembleRelease
adb install app/release/app-release.apk
```

> La app requiere un `google-services.json` válido en `/app` para conectar con Firebase. El archivo no está incluido en el repositorio por seguridad.

---

Desarrollado por **Cristian Vázquez** — proyecto portfolio Android.  
[github.com/thestrokes1](https://github.com/thestrokes1)
