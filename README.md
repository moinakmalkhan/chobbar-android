# Chobbar — Android app

Kotlin + Jetpack Compose app for **chobbar.shop** (activewear & streetwear, made in Sialkot). Talks to `chobbar-backend` (`/api/v1/` + WebSockets).

- English, Urdu and Arabic with full right-to-left layouts. First launch picks the language from the visitor's country (via the API's IP lookup); change it anytime in Account or Android's per-app language settings.
- Home, shop, filters, search with debounced suggestions, product pages with colour/size pickers, bag, guest or account checkout (COD and bank transfer with receipt upload), order tracking, returns, reviews, wishlist, addresses, notifications and live support chat.
- Email and Google (Gmail) sign-in via Credential Manager.
- Small and fast: no DI or annotation processing, no extra icon pack, R8 + resource shrinking, only en/ur/ar resources, reduced animations and image memory on low-RAM phones.
- On-device data: Keystore-encrypted session tokens, guest bag token, guest wishlist, recently viewed, recent searches, last checkout contact and theme (DataStore), plus a 10 MB HTTP cache and an 80 MB image cache.

## Build
Open the project in Android Studio (JDK 17+), or:
```bash
./gradlew assembleDebug        # debug talks to http://10.0.2.2:8000 (the API on your machine, from the emulator)
./gradlew bundleRelease        # release uses https://api.chobbar.shop
```
Optional `local.properties` keys: `API_URL`, `WS_URL`, `GOOGLE_WEB_CLIENT_ID` (the backend's web OAuth client id), `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.

Strings are generated from the website dictionaries so copy stays identical:
```bash
python3 tools/gen_strings.py ../chobbar-frontend/src/i18n/dictionaries
```
