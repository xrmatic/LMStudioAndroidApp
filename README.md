# LM Studio Android App

An Android chat client for connecting to your local [LM Studio](https://lmstudio.ai/) server.

---

## Features

| Feature | Details |
|---|---|
| **Chat interface** | Clean chat-bubble UI with user (right) and assistant (left) messages |
| **Model selector** | Lists all models loaded in your LM Studio instance via the `/v1/models` endpoint |
| **WiFi-only mode** | Optionally block requests over mobile data, preventing accidental external exposure |
| **HTTPS / TLS** | HTTPS is the default; HTTP cleartext requires an explicit opt-in per host |
| **Self-signed cert support** | Optional trust-all mode for servers with self-signed or custom TLS certificates |
| **API key auth** | Store your LM Studio API key; it is persisted with Android Keystore-backed `EncryptedSharedPreferences` |
| **System prompt** | Configurable system prompt sent at the start of every conversation |
| **Temperature** | Adjustable sampling temperature (0.0 – 2.0) via a SeekBar |
| **Connection test** | One-tap test to verify connectivity before saving settings |
| **Verbose logging** | Optional HTTP body logging for debugging (ProGuard-stripped in release builds) |

---

## Requirements

- Android 7.0+ (API 24+)
- LM Studio >= 0.2 running on a machine reachable from the phone (e.g. same WiFi network)
- LM Studio server started with **Enable CORS** enabled (or appropriate network settings)

---

## Build

### Prerequisites

- JDK 17
- Android SDK (API 34 build tools included)

```bash
./gradlew assembleDebug
```

The resulting APK is at `app/build/outputs/apk/debug/app-debug.apk`.

### Run unit tests

```bash
./gradlew test
```

---

## Architecture

```
app/src/main/java/com/xrmatic/lmstudio/
├── MainActivity.kt              # Launcher: routes to Settings or Chat
├── ChatActivity.kt              # Main chat screen
├── SettingsActivity.kt          # Connection & security settings
├── api/
│   ├── LMStudioApiService.kt    # Retrofit interface (OpenAI-compatible API)
│   └── LMStudioClient.kt        # OkHttp / Retrofit builder with security options
├── adapter/
│   └── MessageAdapter.kt        # RecyclerView adapter for chat bubbles
├── model/
│   └── ApiModels.kt             # Data classes for API request/response and UI
├── prefs/
│   └── AppPreferences.kt        # Encrypted + plain SharedPreferences wrapper
├── util/
│   └── NetworkUtils.kt          # WiFi / network state helpers
└── viewmodel/
    └── ChatViewModel.kt         # MVVM ViewModel (Coroutines + LiveData)
```

---

## Connection Setup

1. On first launch you are taken to the **Settings** screen.
2. Enter the server URL, e.g. `http://192.168.1.100:1234` (LM Studio default port is **1234**).
3. If you have API authentication enabled in LM Studio, enter your API key.
4. Toggle **WiFi only** to prevent requests over mobile data.
5. If your server uses a self-signed TLS certificate, toggle **Accept self-signed certificates**.
6. Tap **Test connection** to verify, then **Save & Connect**.

> **Security note:** HTTP (cleartext) is shown with a warning banner. Use HTTPS wherever possible.
> The "Accept self-signed certificates" mode disables hostname verification -- use it only on trusted local networks.

---

## API

LM Studio exposes an OpenAI-compatible REST API. The app uses:

| Endpoint | Purpose |
|---|---|
| `GET /v1/models` | List loaded models |
| `POST /v1/chat/completions` | Send a chat message and receive the assistant reply |

---

## License

MIT
