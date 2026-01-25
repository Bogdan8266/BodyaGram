<div align="center">

# ✈️ BODYAGRAM (Telegram Client Fork)

<!-- BADGES -->
![Status](https://img.shields.io/badge/STATUS-OPERATIONAL-green?style=for-the-badge&logo=telegram)
![Platform](https://img.shields.io/badge/PLATFORM-ANDROID-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Base](https://img.shields.io/badge/BASED_ON-TELEGRAM_SOURCE-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white)
![Integration](https://img.shields.io/badge/INTEGRATION-SERVER_UPLINK-red?style=for-the-badge&logo=server)

<h3>The Specialized Communication Tool.</h3>

<p>
This is the tactical client that completes the ecosystem. It connects seamlessly with 
<a href="https://github.com/Bogdan8266/BodyaSync-Compose">BodyaSync-Compose</a>, replaces the fallen 
<a href="https://github.com/Bogdan8266/BodyaSync-Server">BodyaSync-Server</a>, Backend
<a href="https://github.com/Bogdan8266/BodyaSync-Gallery"><s>BodyaSync-Gallery(Flutter)</s></a>, 
and is the frontend for the <b><a href="https://github.com/Bogdan8266/BodyaGram">BodyaGram App</a></b>.
</p>

</div>

---

## 💀 Mission Briefing

**BodyaGram** is a modified fork of the official Telegram Android client. It looks like Telegram, it acts like Telegram, but under the hood, it packs a secret weapon.

We didn't just change the colors. We integrated a **Direct Server Uplink**.
While standard users are stuck uploading files from their phone's limited storage, **BodyaGram** lets you browse your **self-hosted server (BodyaSync)** directly inside the chat interface and send files that aren't even on your device.

**The result:** You command terabytes of data from a device that fits in your pocket. Zero local storage used. Instant transmission via Userbot.

---

## 💥 The Arsenal (Custom Features)

*   **Server-Side Picker:** A custom button right next to the standard attachment menu.
*   **Remote File Access:** Browse your server's `Originals` folder instantly.
*   **Smart Indicators:** Video files are clearly marked.
*   **Auto-Chat Detection:** The app knows which chat you are in and tells the server exactly where to smuggle the file.
*   **Zero-Traffic Upload:** The file goes from Server -> Telegram. Your phone is just the remote detonator.

---

## 📜 Rules of Engagement (Official Telegram Requirements)

We play by the rules here. This is a fork of the official [Telegram App for Android](https://play.google.com/store/apps/details?id=org.telegram.messenger). To use this source code or build your own version, you must follow these directives:

1.  **Get your credentials:** [Obtain your own `api_id`](https://core.telegram.org/api/obtaining_api_id) for your application. Don't use mine.
2.  **Identity:** **Do not** use the name "Telegram" for your app. Make sure your users know it is unofficial. This is **BodyaGram**.
3.  **Branding:** **Do not** use their standard logo (white paper plane in a blue circle). Get your own insignia.
4.  **Security:** Study the [Security Guidelines](https://core.telegram.org/mtproto/security_guidelines). Take care of your users' data like it's your own.
5.  **Open Source:** Remember to publish **your** code too. Comply with the licenses.

**Documentation:**
*   [Telegram API Manuals](https://core.telegram.org/api)
*   [MTProto Protocol Manuals](https://core.telegram.org/mtproto)

---

## 🛠 Assembly Guide (Compilation)

Want to build this weapon yourself? Here is the blueprint.

**Note:** To support [reproducible builds](https://core.telegram.org/reproducible-builds), this repo contains dummy files. You need to replace them with real ammo before deployment.

**Requirements:**
*   Android Studio (Latest Stable)
*   Android NDK (Check `build.gradle` for exact version)
*   Android SDK

### Step-by-Step Protocol:

1.  **Acquire the Target:**
    Download the source code.
    ```bash
    git clone https://github.com/Bogdan8266/BodyaGram.git
    ```

2.  **Secure the Keys:**
    Copy your `release.keystore` into the `TMessagesProj/config` folder.

3.  **Configure Access:**
    Open `gradle.properties` and fill out these fields to access your keystore:
    *   `RELEASE_KEY_PASSWORD`
    *   `RELEASE_KEY_ALIAS`
    *   `RELEASE_STORE_PASSWORD`

4.  **Firebase Setup:**
    *   Go to the [Firebase Console](https://console.firebase.google.com/).
    *   Create two Android apps with IDs: `org.telegram.messenger` and `org.telegram.messenger.beta` (or your custom package name).
    *   Turn on Firebase Messaging.
    *   Download `google-services.json` and drop it into the `TMessagesProj` folder.

5.  **Initialize:**
    Open the project in Android Studio. **Do not import** — just Open.

6.  **Hardcode the Variables:**
    Locate `TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java`.
    Fill out the values (API ID, API Hash). There are comments inside showing where to get the data.

7.  **Compile:**
    Hit "Build". If you followed instructions, you are ready to rock.

---

## 🌍 Localization

We moved all translations to [translations.telegram.org/en/android/](https://translations.telegram.org/en/android/). Use it if you want to speak the local language.

---

<div align="center">

**⚠️ SYSTEM STATUS: CUSTOM BUILD ⚠️**
*Based on DrKLO/Telegram Source Code.*
*Modified for the BodyaSync Ecosystem.*

</div>
