# Chobbar — Android (Kotlin, Jetpack Compose)

Repo `chobbar-android`. API: `chobbar-backend`. Web: `chobbar-frontend` (shares the copy deck).

## Layout
```
app/src/main/java/shop/chobbar/app/
  ChobbarApp.kt / MainActivity.kt   app container, Coil loader (low-RAM aware), splash, first-launch language detection, deep links
  di/AppContainer.kt                hand-written DI (no Hilt/KSP: smaller APK, faster start)
  data/model/Models.kt              DTOs mirroring the Django serializers (kotlinx.serialization)
  data/api/                         Retrofit interface, OkHttp client (JWT refresh before expiry, X-Language/X-Cart-Token/X-Session-Key), error envelope
  data/local/Stores.kt              SessionStore (Keystore AES-GCM tokens), LocalStore (device data), LanguageStore
  data/repo/Repositories.kt         Auth, Cart, Wishlist (server or device), Realtime (notifications + chat WebSockets)
  i18n/AppLocale.kt                 per-app locale (LocaleManager on 13+, context wrap below)
  ui/theme, ui/components           rose/stone theme, logo, product card, price, stepper, icons drawn from path data
  ui/nav                            Root (tabs + NavHost), UiMessages (snackbars), DeepLinks, rememberResource (rotation-safe loader)
  ui/screens                        Home, Catalog (shop/list/filters/search), Product, CartCheckout, Auth, Account (orders, wishlist, addresses, notifications, settings, track, FAQ, pages, chat)
tools/gen_strings.py                strings.xml (en/ur/ar) generated from the web dictionaries + tools/app_strings.json
```

## Conventions
- Every user-facing string comes from `R.string.*`; add new ones to `tools/app_strings.json` (all three languages) and regenerate.
- RTL: use start/end paddings and `AutoMirrored` icons; mirror directional artwork with `graphicsLayer { scaleX = -1f }`.
- Debounce input before network calls (search 300 ms, price filter 500 ms, city quote 400 ms, bag quantity 350 ms).
- Respect `LocalLowEnd`: shorter/no animations, no shimmer, RGB_565 images.
- Only use icons from material-icons-core; draw others in `AppIcons`.
- Commit messages: plain, descriptive, no tool attribution lines.
