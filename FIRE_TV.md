# Stremio per Amazon Fire TV Stick Lite (e Android TV)

Questa versione del progetto aggiunge il supporto completo a TV e telecomando (D-pad) senza
rimuovere nulla: su telefoni e tablet l'app si comporta esattamente come prima, perché tutta la
logica TV si attiva solo quando l'app rileva un dispositivo TV (Fire TV, Android/Google TV o
qualunque dispositivo senza touchscreen).

## Cosa è stato modificato

| File | Modifica |
|---|---|
| `app/src/main/AndroidManifest.xml` | `android.software.leanback`, `android.hardware.touchscreen` e `android.hardware.faketouch` dichiarati `required="false"`; categoria `android.intent.category.LEANBACK_LAUNCHER` nella MainActivity; `android:banner` per la home di Fire TV. |
| `app/src/main/res/drawable/tv_banner.xml` | Nuovo banner 160×90 dp (320×180 px) per il launcher TV. |
| `presentation/tv/TvSupport.kt` (nuovo) | Rilevamento TV (`TvDevice`), `LocalIsTvDevice`, `TvFocusIndication` (evidenziazione del focus per tutti i `clickable`), modificatori `tvFocusRing`, `tvFocusScale`, `tvFocusTrap`, `notFocusable`. |
| `presentation/tv/TvNavigationRail.kt` (nuovo) | Barra di navigazione laterale (Cerca, Home, Discover, Library, Settings) usata su TV al posto della bottom bar. |
| `MainActivity.kt` | Su TV: testo scalato del 15% per la visione a distanza (anche in dialog e popup) e flag TV fornito a Compose. |
| `StremioMobileApp.kt` | Indicazione di focus TV globale; tasto Indietro: dalle sezioni secondarie torna a Home, su Home serve una doppia pressione per uscire; focus intrappolato nella sovrapposizione in primo piano (ricerca, dettaglio, addon, stream); ripristino del focus sull'elemento di partenza alla chiusura; rail laterale; griglie a 6 colonne; dettaglio/addon limitati a 820 dp di larghezza. |
| `PlayerScreen.kt` | Telecomando nel player: a controlli nascosti ←/→ = −/+ 10 s (durata configurabile nelle impostazioni), OK = play/pausa, ↑/↓/Menu = mostra i controlli; tasti media (Play/Pausa, Avanti veloce, Indietro veloce) sempre attivi; Indietro chiude prima i controlli e poi il player; auto-hide a 5 s su TV; il focus non esce mai dal player. |
| `player/PlayerControls.kt` | Anello di focus su tutti i pulsanti, gli slider e le voci di menu; focus iniziale sul pulsante play/pausa (o su Indietro durante il buffering). |
| `ThemedComponents.kt` | Anello di focus su Button/IconButton/TextButton/Switch/Slider; su TV vengono usati Switch e Slider Material (pilotabili con il D-pad) al posto delle varianti "Liquid Glass" solo touch. Le impostazioni restano identiche. |
| `PosterComponents.kt`, `FeaturedHero.kt` | Poster ingranditi dell'8% quando hanno il focus; il carosello in evidenza non scorre da solo mentre ha il focus. |
| `DetailSheet.kt`, `SettingsPanel.kt`, `AuthFlow.kt` | Anello di focus sugli elementi che non usano l'indicazione standard. |
| `UpdateRepository.kt`, `app/build.gradle.kts` | Repository degli aggiornamenti in-app configurabile (`-PUPDATE_REPOSITORY=utente/repo`), così una build Fire TV non propone di installare la versione upstream per telefoni. |
| `.github/workflows/firetv-apk.yml` (nuovo) | Workflow GitHub Actions che compila l'APK per Fire TV con lo streaming server nativo incluso. |

### Comandi del telecomando

| Dove | Tasto | Azione |
|---|---|---|
| Ovunque | ↑ ↓ ← → | Sposta il focus (riquadro bianco ben visibile) |
| Ovunque | OK (centrale) | Apre/attiva l'elemento selezionato |
| Board | ← dal bordo sinistro | Entra nella barra laterale |
| Board, sezione ≠ Home | Indietro | Torna a Home |
| Home | Indietro ×2 (entro 2 s) | Esce dall'app |
| Pannelli (dettaglio, stream, ricerca) | Indietro | Chiude il pannello, il focus torna dov'era |
| Player, controlli nascosti | ← / → | −/+ 10 s |
| Player, controlli nascosti | OK | Play/pausa e mostra i controlli |
| Player, controlli nascosti | ↑ / ↓ / Menu | Mostra i controlli |
| Player, controlli visibili | Indietro | Nasconde i controlli (seconda pressione: esce) |
| Player, sulla barra di avanzamento | ← / → | Scorre dell'1% della durata |
| Player | ⏯ ⏪ ⏩ | Play/pausa, −10 s, +10 s |

## Come ottenere l'APK

Il Fire TV Stick Lite usa Fire OS 7 (Android 9) a 32 bit: serve l'APK **armeabi-v7a**.

### Opzione A (consigliata): GitHub Actions, nessun SDK da installare

Lo streaming server per i torrent è una libreria Rust che va compilata con NDK, Rust e vcpkg.
Il workflow incluso fa tutto da solo.

1. Metti il progetto su un tuo repository GitHub (anche privato), in uno dei due modi:
   - **Dallo zip** (contiene già tutti i sorgenti, submodule inclusi):
     ```bash
     cd stremio-android
     git init -b main
     git add -A
     git commit -m "Stremio Android con supporto Fire TV"
     git remote add origin https://github.com/TUO_UTENTE/stremio-firetv.git
     git push -u origin main
     ```
   - **Da un fork** di `pinasu/stremio-android` applicando la patch inclusa nello zip:
     ```bash
     git clone --recurse-submodules https://github.com/TUO_UTENTE/stremio-android.git
     cd stremio-android
     git apply /percorso/FIRE_TV_CHANGES.patch
     git add -A && git commit -m "Fire TV / Android TV support" && git push
     ```
   Il workflow deve trovarsi sul branch predefinito per comparire nella scheda Actions.
2. (Facoltativo ma consigliato) crea una chiave di firma fissa, così i futuri aggiornamenti si
   installano sopra la versione precedente senza perdere i dati:
   ```bash
   keytool -genkeypair -v -keystore firetv.jks -alias firetv -keyalg RSA -keysize 2048 -validity 10000
   base64 -w0 firetv.jks > firetv.jks.b64   # su macOS: base64 -i firetv.jks -o firetv.jks.b64
   ```
   In Settings → Secrets and variables → Actions aggiungi: `ANDROID_RELEASE_KEYSTORE_BASE64`
   (contenuto di `firetv.jks.b64`), `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` (`firetv`),
   `ANDROID_KEY_PASSWORD`. Senza secret il workflow firma con una chiave temporanea.
3. Scheda **Actions** → **Fire TV APK** → **Run workflow**. Dopo 20–40 minuti (la prima volta;
   poi la cache accelera) trovi gli APK negli artifact dell'esecuzione e in **Releases**.
4. Scarica `StremioMobile-vX.Y.Z-armeabi-v7a-release.apk`.

### Opzione B: build locale con Android Studio

Requisiti: JDK 21, Android Studio aggiornato con Android SDK 37 e NDK `29.0.13846066`.

```bash
cd stremio-android
./gradlew :mpv-android-lib:assembleRelease :app:assembleRelease      # Windows: gradlew.bat ...
```

Senza chiave di firma Gradle produce APK *unsigned*, che Fire TV non installa. Firma con le
variabili d'ambiente lette da `app/build.gradle.kts`:

```bash
export ANDROID_KEYSTORE_FILE=/percorso/firetv.jks
export ANDROID_KEYSTORE_PASSWORD=...
export ANDROID_KEY_ALIAS=firetv
export ANDROID_KEY_PASSWORD=...
./gradlew :mpv-android-lib:assembleRelease :app:assembleRelease
```

L'APK è in `app/build/outputs/apk/release/` (file con `armeabi-v7a` nel nome).

In alternativa, per una prova veloce: `./gradlew :app:assembleDebug` (firma di debug automatica,
ma include LeakCanary ed è più pesante sul Fire Stick Lite).

Nota: in una build locale lo streaming server Rust (`libstream_server.so`) non viene compilato
automaticamente. L'app funziona lo stesso (stream HTTP e debrid), ma la riproduzione dei torrent
richiede la libreria: compilala con `./gradlew compileNativeLibs` (servono Rust, `cargo-ndk` e
vcpkg) oppure usa l'opzione A.

## Installazione sul Fire TV Stick Lite (sideload)

1. Sul Fire TV: **Impostazioni → La mia Fire TV → Informazioni**, premi 7 volte sul nome del
   dispositivo per attivare le **Opzioni sviluppatore**.
2. **Impostazioni → La mia Fire TV → Opzioni sviluppatore**: attiva **Debug ADB** e consenti
   le **app da fonti sconosciute** (per l'app che userai per installare, es. Downloader).
3. Installa l'APK in uno dei due modi:
   - **ADB dal computer** (stessa rete Wi-Fi; l'IP è in Impostazioni → La mia Fire TV →
     Informazioni → Rete):
     ```bash
     adb connect 192.168.1.50:5555
     adb install -r StremioMobile-vX.Y.Z-armeabi-v7a-release.apk
     ```
     Al primo collegamento conferma sulla TV la richiesta di debug.
   - **App Downloader** (dall'Appstore Amazon): inserisci il link diretto all'APK (ad es. quello
     della Release su GitHub, se il repository è pubblico) e conferma l'installazione.
4. L'app compare in **App e canali** con il banner Stremio; puoi spostarla nella riga principale.

Se esiste già una versione firmata con un'altra chiave: `adb uninstall com.stremio.mobile` prima
di installare.

## Note per il Fire TV Stick Lite

- Lascia lo stile **Classic** (Impostazioni → Interface): gli effetti "Liquid Glass" su Fire OS 7
  (Android 9) ricadono comunque su una versione semplificata, ma Classic è il più leggero per
  1 GB di RAM.
- Il player predefinito ExoPlayer usa la decodifica hardware ed è la scelta migliore; MPV resta
  disponibile nelle impostazioni del player.
- Login con Facebook e autenticazione Trakt aprono il browser: su Fire TV serve un browser
  installato (Silk). Il login con email e password funziona direttamente.
- Il campo di ricerca apre la tastiera a schermo di Fire TV; ↑/↓ escono dal campo.
