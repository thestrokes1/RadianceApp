# CLAUDE.md — RadianceApp

> Guía de contexto para Claude Code. Léela completa antes de tocar cualquier archivo.

---

## 1. Proyecto

**RadianceApp** — app Android de indumentaria, portfolio-grade.  
Stack: Kotlin + Jetpack Compose + Firebase (Auth + Firestore).  
Objetivo: APK firmado + demo video en GitHub. **No se publica en Play Store.**

---

## 2. Entorno de desarrollo

| Variable | Valor |
|---|---|
| OS | Windows 11 |
| IDE | Android Studio (instalado en `D:\Android`) |
| SDK | `D:\SDK` |
| Proyecto | `D:\Aprojects\RadianceApp` |
| Package | `com.cristianv.radianceapp` |
| Min SDK | API 26 |
| Target SDK | API 34 (Pixel 7 emulator) |
| JDK | 17 |
| Build scripts | Kotlin DSL (`.kts`) |
| Keystore | `D:\RadianceKeystore\radiance.jks` (alias: `radiance`, país: AR) |

---

## 3. Arquitectura

**Patrón:** MVVM estricto.

```
com.cristianv.radianceapp/
├── model/              # Data classes (Product, User, CartItem…)
├── data/               # FirebaseRepository, AuthRepository, SampleData
├── viewmodel/          # CartViewModel, WishlistViewModel, HomeViewModel, AuthViewModel
├── ui/
│   ├── screens/        # Una pantalla por archivo
│   ├── components/     # Composables reutilizables
│   └── theme/          # Color, Typography, Theme
└── MainActivity.kt     # NavHost + instanciación de ViewModels compartidos
```

---

## 4. Reglas críticas de arquitectura

### 4.1 ViewModels — instanciación SIEMPRE en MainActivity
```kotlin
// ✅ CORRECTO — en MainActivity, pasar como parámetro a cada screen
val cartViewModel: CartViewModel = viewModel()
val wishlistViewModel: WishlistViewModel = viewModel()
```
**Nunca** uses `viewModel()` dentro de un Composable de pantalla individual.  
Razón: estado aislado por pantalla → bug de carrito vacío / wishlist vacío.

### 4.2 Firestore — tipos de campo estrictos
| Dato | Tipo Firestore |
|---|---|
| price, rating | `double` |
| reviewCount, stock | `int64` |
| isAvailable, isFeatured | `boolean` |
| id, name, category… | `string` |

Mismatch de tipos → lectura silenciosa falla, lista vacía sin excepción.

### 4.3 Navegación
- Librería: **Jetpack Navigation Compose**
- Transiciones: slide horizontal entre pantallas principales, fade en Detail/Search
- Bottom nav: **oculta** en `ProductDetailScreen` y `SearchScreen`
- Los IDs de producto se pasan como argumento de ruta; la lookup se hace **siempre** sobre la lista cargada desde Firebase, nunca sobre `SampleData`

### 4.4 Fallback de datos
`FirebaseRepository` intenta Firestore primero. Si falla o la colección está vacía → `SampleData` como fallback local. Nunca mostrar pantalla vacía.

### 4.5 Firebase
- Región Firestore: `southamerica-east1`
- Auth: Email/Password
- En signup, el `User` data class se sincroniza a la colección `users` en Firestore
- `google-services.json` va en `app/` — nunca en la raíz del proyecto

---

## 5. Pantallas implementadas

| Screen | Estado |
|---|---|
| SplashScreen | ✅ completa |
| AuthScreen (login + signup) | ✅ completa |
| HomeScreen | ✅ completa |
| ProductDetailScreen | ✅ completa |
| CartScreen | ✅ completa |
| WishlistScreen | ✅ completa |
| SearchScreen | ✅ completa |
| ProfileScreen | ✅ completa |
| AdminScreen | 🔲 pendiente |

---

## 6. Próximas tareas conocidas

1. **AdminScreen** — CRUD de productos en Firestore sin usar Firebase Console
2. **GitHub release** — APK firmado + README + demo video

---

## 7. Bugs resueltos (no regresar)

| Bug | Causa raíz | Fix |
|---|---|---|
| "Product not found" al navegar al detalle | Lookup sobre `SampleData` en vez de lista Firebase | Lookup siempre sobre `homeViewModel.products` |
| Auto-login al abrir la app sin credenciales | `init` block de `AuthViewModel` hacía auto-login | Eliminado; ahora siempre arranca en AuthScreen |
| Carrito vacío después de agregar ítems | `CartViewModel` instanciado por pantalla | Instancia única en `MainActivity` |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Firma debug vs release en conflicto | Desinstalar APK debug antes de instalar release |
| Artefactos en splash (esquinas del ícono) | Color de background del tema splash no coincidía | Fijar `#0A0A0A` en `splash_theme.xml` |

---

## 8. Convenciones de código

- Kotlin idiomático: `data class`, `sealed class` para estados UI, `StateFlow` en ViewModels
- Composables: nombrados en PascalCase, un archivo por pantalla
- Strings de UI: en `strings.xml` (no hardcodeados)
- Colores / tipografía: siempre desde el `Theme`, nunca valores mágicos inline
- No uses `LiveData`; todo es `StateFlow` / `collectAsState()`

---

## 9. Comandos útiles

```bash
# Build release APK firmado
./gradlew assembleRelease

# Instalar en emulador/dispositivo
adb install app/release/app-release.apk

# Ver logs del proceso de la app
adb logcat -s "RadianceApp"

# Limpiar build
./gradlew clean
```

---

## 10. Lo que Claude NO debe hacer

- ❌ No agregar dependencias nuevas sin preguntar primero
- ❌ No usar `LiveData` — el proyecto es 100% `StateFlow`
- ❌ No hacer lookup de productos en `SampleData` para navegación
- ❌ No instanciar ViewModels dentro de Composables de pantalla
- ❌ No modificar `google-services.json` ni `local.properties`
- ❌ No commitear secretos ni el keystore