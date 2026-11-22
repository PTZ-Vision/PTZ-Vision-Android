# Latenza RTSP in PTZ-Vision: cosa non andava e cosa funziona ora

Questo documento spiega, in italiano e con qualche paragone, perché l'anteprima
RTSP era lenta o fragile, cosa è stato cambiato (issue #4) e come misurare la
latenza con lo stream orologio del Raspberry.

---

## 0. Il quadro generale: il viaggio di un fotogramma

Ogni immagine che vedi sul telefono fa questo percorso:

```
 Telecamera            Rete                 Telefono
┌──────────┐   RTP    ┌─────┐   TCP   ┌───────────┐  ┌──────────┐  ┌────────┐  ┌──────────┐
│ sensore  │─encoder─▶│ LAN │───────▶│ RTSP/RTP  │─▶│ decoder  │─▶│ Surface│─▶│ schermo  │
│          │  H.264   │Wi-Fi│        │ parsing   │  │ hardware │  │        │  │          │
└──────────┘          └─────┘        └───────────┘  └──────────┘  └────────┘  └──────────┘
```

La latenza totale è la **somma** di tutti i tratti. Il trucco per avere bassa
latenza non è "andare più veloce", ma **non far mai aspettare un fotogramma in
coda** da nessuna parte.

> **Metafora: il ristorante.**
> Un piatto (fotogramma) passa da cucina → passe → cameriere → tavolo.
> Se ogni passaggio "aspetta di averne tre prima di portarli", il cliente
> mangia freddo. Un buon ristorante a bassa latenza porta ogni piatto appena
> pronto, anche se uno alla volta.

Prima c'erano diversi punti in cui i piatti si accumulavano o cadevano per
terra. Vediamoli.

---

## 1. Cosa c'era di sbagliato prima

### 1.1 Il decoder riceveva i "pezzi" invece dei "fotogrammi"

Uno stream H.264 è fatto di **NAL unit** (unità). Un fotogramma (detto
*access unit*, AU) può essere composto da più NAL: per esempio SPS + PPS + più
*slice* dell'immagine.

Il vecchio codice passava al decoder **ogni singola NAL** come se fosse un
buffer indipendente:

```kotlin
// PRIMA (RtspNalExtractor)
val nalu = rtpBuffer.push(payload) ?: continue
onNalUnit(START_CODE + nalu, rtpBuffer.lastPts)   // un pezzo alla volta
```

Problema: il decoder non sa quando il fotogramma è **finito**. Per sicurezza
aspetta di vedere l'inizio del fotogramma *successivo* → **almeno 1 frame di
ritardo** (33 ms a 30 fps), spesso di più.

> **Paragone:** è come dettare una frase parola per parola senza mai dire
> "punto". Chi scrive aspetta la parola successiva per capire se la frase è
> finita.

**Ora** l'RTP ha un bit apposta, il *marker bit*, che dice "questo è l'ultimo
pacchetto del fotogramma". Lo usiamo per consegnare **un fotogramma intero per
volta**:

```kotlin
// ORA (AccessUnitAssembler)
if (auHasData && timestamp != auTimestamp) flush()  // nuovo frame → chiudi il precedente
...
if (marker) flush()                                  // "punto": frame completo, consegnalo subito
```

### 1.2 Ricostruzione dei frammenti byte per byte

I fotogrammi grandi (soprattutto i keyframe, IDR) non stanno in un pacchetto e
vengono spezzati (FU-A). Il vecchio codice li rimetteva insieme così:

```kotlin
// PRIMA
private val fuBuffer = mutableListOf<ByteArray>()
...
fuBuffer.flatten().toByteArray()
private fun List<ByteArray>.flatten(): List<Byte> = flatMap { it.toList() }
```

`it.toList()` trasforma ogni byte in un **oggetto `Byte`** (boxing). Un keyframe
da 200 KB diventa 200.000 oggetti allocati e poi ricopiati in un array. Lento e
pieno di garbage collection, proprio sui frame più importanti.

> **Paragone:** per traslocare un libro, lo smonti pagina per pagina, metti
> ogni pagina in una busta separata, e poi rileghi tutto. Funziona, ma…

**Ora** si scrive direttamente in un unico buffer riutilizzato, e lo si passa
al decoder **senza copiarlo**:

```kotlin
// ORA
au.write(p, offset + 2, end - offset - 2)              // append diretto
listener.onAccessUnit(au.array(), au.size(), auKey)     // nessuna copia
```

### 1.3 STAP-A e header RTP letti male

- **STAP-A** (più NAL piccole in un pacchetto, es. SPS+PPS) veniva copiato in
  blocco con dentro i campi "lunghezza" da 2 byte: il decoder riceveva
  spazzatura in mezzo ai dati.
- L'**header RTP** era dato per scontato a 12 byte fissi. In realtà può avere
  CSRC, estensioni e padding. Con certe telecamere i dati partivano dal byte
  sbagliato.

```kotlin
// ORA: header calcolato davvero
var offset = 12 + csrcCount * 4
if (extension) offset += 4 + extLen * 4
if (padding)  end -= p[len - 1].toInt() and 0xFF
```

### 1.4 Una coda che buttava via dati

```kotlin
// PRIMA (ZeroCopyMediaCodecDecoder)
if (!inputQueue.offer(NalUnit(data, pts))) {
    inputQueue.poll()          // coda piena → butta il più vecchio
    inputQueue.offer(...)
}
...
val idx = c.dequeueInputBuffer(TIMEOUT)
if (idx < 0) continue          // nessun buffer libero → NAL persa in silenzio
```

In H.264 i fotogrammi P dipendono dai precedenti. **Perderne uno corrompe
l'immagine fino al keyframe successivo** (i classici "blocchi verdi" o le
scie). E la coda in sé è un punto dove i frame aspettano.

> **Paragone:** un nastro trasportatore che, quando è pieno, butta giù la
> scatola più vecchia. Peccato che ogni scatola contenga le istruzioni per
> montare la successiva.

**Ora** non c'è nessuna coda: il thread di rete passa il fotogramma
direttamente al decoder. Se per qualche motivo un frame va perso, invece di
decodificare immagini rotte si **aspetta il prossimo keyframe**:

```kotlin
if (index < 0) {           // decoder bloccato
    droppedInput++
    waitingForKeyFrame = true   // riparti pulito dal prossimo IDR
    return
}
```

### 1.5 Il percorso OpenGL: un passaggio in più

Il vecchio "ZeroCopy" faceva così:

```
decoder → SurfaceTexture → (callback) → thread GL → updateTexImage()
        → shader che disegna un quad → eglSwapBuffers → SurfaceFlinger → schermo
```

Era davvero zero-copy (nessuna copia in CPU), ma aggiungeva **un passaggio di
composizione**: il thread GL deve svegliarsi, disegnare e consegnare a sua
volta un buffer al compositore. Di solito costa un vsync (~16 ms) e impedisce
al sistema di usare l'**overlay hardware**.

**Ora** il decoder scrive direttamente nella `Surface` di una `SurfaceView`:

```
decoder → SurfaceView → SurfaceFlinger (spesso overlay HW) → schermo
```

```kotlin
c.configure(format, surfaceView.holder.surface, null, 0)
...
c.releaseOutputBuffer(index, true)   // il frame va dritto al compositore
```

> **Paragone:** prima il cameriere portava il piatto a un secondo cameriere,
> che lo impiattava di nuovo e poi lo portava al tavolo. Ora lo porta lo
> chef direttamente.

Inoltre ora il video mantiene le **proporzioni corrette** (prima veniva stirato
sull'area disponibile).

### 1.6 Decoder "generico" senza modalità a bassa latenza

Molti decoder hardware trattengono alcuni fotogrammi in un buffer interno (il
*DPB*) per gestire eventuali B-frame, anche quando lo stream non ne ha. Il
vecchio codice impostava solo `KEY_LOW_LATENCY` sul primo decoder che capitava.

**Ora** `LowLatencyH264Decoder`:

1. sceglie un decoder **hardware** che dichiara `FEATURE_LowLatency`
   (sul Realme GT: `c2.qti.avc.decoder.low_latency`);
2. imposta le chiavi standard e quelle **dei produttori** (le stesse che usa
   Moonlight, il client di game streaming):

```kotlin
setInteger(MediaFormat.KEY_LOW_LATENCY, 1)
setInteger(MediaFormat.KEY_PRIORITY, 0)                     // realtime
setInteger(MediaFormat.KEY_OPERATING_RATE, Short.MAX_VALUE) // clock al massimo
// Qualcomm
setInteger("vendor.qti-ext-dec-picture-order.enable", 1)
setInteger("vendor.qti-ext-dec-low-latency.enable", 1)
// + Exynos, Amlogic, HiSilicon
```

Se un dispositivo rifiuta le chiavi vendor, si riprova senza (niente crash).

### 1.7 Il fallback: due player sulla stessa superficie

`RtspPreview` creava una `RtspSurfaceView` della libreria, e se la libreria
falliva ci attaccava sopra **anche ExoPlayer**:

```kotlin
// PRIMA
rtspAdapter.stop()
streamManager.start(url, sv)   // sv è una RtspSurfaceView, già gestita dalla libreria
```

Due proprietari per la stessa superficie = conflitti. E ExoPlayer, anche
"tarato", ha comunque un buffer di centinaia di millisecondi.

**Ora** il fallback usa **solo** la libreria `rtsp-client-android` (che è già
a bassa latenza), ed ExoPlayer è stato rimosso dall'anteprima.

### 1.8 Riconnessioni a cascata

Nel vecchio `RtspClientAdapter`, `start()` chiamava `stop()`. Lo stop generava
un evento "disconnected" che arrivava **dopo**, quando la nuova sessione era già
partita → la nuova sessione si riavviava → nuovo "disconnected" → …

**Ora** ogni sessione ha un numero di generazione; gli eventi delle sessioni
vecchie vengono ignorati:

```kotlin
val gen = ++generation
...
override fun onRtspStatusDisconnected() {
    if (gen != generation) return    // evento di una sessione morta: ignoralo
    scheduleReconnect(...)
}
```

> **Paragone:** cambi numero di telefono, ma rispondi ancora alle chiamate
> sul vecchio numero e ogni volta richiami tutti dal nuovo. Ora le chiamate
> sul numero vecchio vengono semplicemente ignorate.

### 1.9 Il Wi-Fi del telefono "dormiva"

Questo non era un bug del codice, ma è stato misurato:

```
ping Pi → router (cavo):     avg 0.2 ms, max 0.4 ms
ping telefono → router (Wi-Fi): avg 10–26 ms, max 140–280 ms
```

Con il *power save* il telefono spegne la radio tra un beacon e l'altro, e
l'access point tiene i pacchetti in attesa. Risultato: fotogrammi che arrivano
a raffiche.

**Ora**, mentre l'anteprima è visibile, l'app prende un
`WifiLock` in modalità `WIFI_MODE_FULL_LOW_LATENCY` (Android 10+), che chiede
al sistema di disattivare il power save. Serve il permesso `WAKE_LOCK`
(concesso automaticamente).

> Nota: il lock aiuta, ma non fa miracoli su una rete congestionata. Nei test
> il telefono era passato da 5 GHz a 2.4 GHz: per l'uso reale conviene restare
> sui **5 GHz**.

### 1.10 Problemi Compose

- `AndroidView(factory = { view })` viene creato **una sola volta**: se
  cambiava telecamera, restava visibile la vista vecchia. Ora è avvolto in
  `key(view)`.
- Gli effetti erano legati all'intero oggetto `Cam`, che contiene anche
  pan/tilt/zoom: ogni aggiornamento del DB avrebbe **riavviato lo stream**.
  Ora la chiave è solo l'URL RTSP.

### 1.11 Il build non compilava

AGP 9 non genera più `BuildConfig` di default, ma `RtspPreview` lo usava.
Aggiunto in `app/build.gradle.kts`:

```kotlin
buildFeatures {
    compose = true
    buildConfig = true
}
```

---

## 2. Come funziona ora

### Architettura

```
SelectedCam
 ├─ LowLatencyWifiLockEffect()             Wi-Fi senza power save
 ├─ ZeroCopyVideoPreview (Compose)  ◀── percorso principale
 │    ├─ ZeroCopyVideoView (FrameLayout + SurfaceView)
 │    │    ├─ RtspNalExtractor        RTSP/TCP → fotogrammi H.264 completi
 │         └─ LowLatencyH264Decoder   MediaCodec HW → Surface (statistiche in logcat)
 │
 └─ RtspPreview (fallback)          ◀── solo se lo ZeroCopy non può farcela
      └─ RtspClientAdapter → rtsp-client-android
```

| File | Ruolo |
|---|---|
| `zerocopy/RtspNalExtractor.kt` | Client RTSP minimale: OPTIONS/DESCRIBE/SETUP/PLAY su TCP, parsing RTP corretto, assemblaggio fotogrammi, keep-alive |
| `zerocopy/LowLatencyH264Decoder.kt` | Scelta decoder HW low-latency, chiavi vendor, rendering diretto, statistiche |
| `zerocopy/ZeroCopyVideoView.kt` | Ciclo di vita surface/sessione, aspect ratio, riconnessione |
| `zerocopy/ZeroCopyVideoPreview.kt` | Integrazione Compose (spinner di connessione/riconnessione) |
| `LowLatencyWifiLock.kt` | Wi-Fi lock a bassa latenza |
| `RtspClientAdapter.kt`, `screen/RtspPreview.kt` | Fallback basato su libreria |

Rimossi: `ZeroCopyRenderer.kt`, `ZeroCopyGLSurfaceView.kt`,
`ZeroCopyMediaCodecDecoder.kt`, `RtspStreamManager.kt`.

### Quando si passa al fallback?

Una regola importante: **gli errori di rete non sono un motivo per cambiare
player**. Se la telecamera è spenta, il percorso ZeroCopy ritenta all'infinito
con back-off breve (300 ms → 600 ms → … → max 3 s).

Si passa alla libreria solo se:

- lo stream non è H.264 (es. H.265);
- il decoder non si riesce a creare;
- per 3 volte di fila il PLAY va a buon fine ma **non arriva mai un'immagine**
  (sintomo di incompatibilità, non di rete).

```kotlin
// Solo le sessioni arrivate a PLAY ma senza video contano come "incompatibilità"
if (connected && !everRendered) failedSessionsWithoutVideo++
if (failedSessionsWithoutVideo >= MAX_FAILED_SESSIONS_WITHOUT_VIDEO) fail(...)
```

> **Paragone:** se il negozio è chiuso, ripassi più tardi; non cambi città.
> Cambi negozio solo se ti dicono "questo prodotto non lo trattiamo".

---

## 3. Misurare la latenza

### Lo stream orologio sul Raspberry

Sul Pi (`raspisax`, 192.168.1.11) gira il servizio `latency-clock`:
mediamtx + ffmpeg, che pubblica `rtsp://192.168.1.11:8554/2` (lo stesso path
che usa l'app). File in `../raspi-latency-clock/`.

```bash
sudo systemctl status latency-clock
sudo systemctl restart latency-clock
# parametri (risoluzione, fps, GOP, bitrate):
sudo nano /opt/latency-clock/clock.env
```

Ogni fotogramma riporta l'**ora reale in cui è stato generato**. Il trucco è
dare a ogni frame come timestamp l'orologio di sistema nel momento in cui passa
nel filtro:

```
settb=1/1000000,setpts=RTCTIME,          ← PTS = ora reale in µs
drawtext=...text='%{eif\:mod(floor((t+3600)/3600)\,24)\:d\:2}\:...'
settb=1/30,setpts=N                      ← timestamp normali per l'encoder
```

(Una prima versione usava l'ora di avvio di ffmpeg + il tempo trascorso: era
sbagliata, perché includeva il tempo di avvio dell'encoder e "anticipava"
l'orologio di qualche centinaio di ms.)

Encoder: `libx264 -preset ultrafast -tune zerolatency -profile baseline -bf 0
-g 15`, cioè niente B-frame, niente lookahead e un keyframe ogni 0.5 s.

### Il problema degli orologi

Per confrontare "ora sul video" e "ora sul telefono", i due orologi devono
essere d'accordo. Misurato:

```
telefono − Raspberry = −294 ms
```

Android corregge l'orologio solo quando si discosta parecchio, quindi qualche
centinaio di ms di errore è normale. Senza correzione la latenza sarebbe
risultata **negativa**.

Durante lo sviluppo l'app aveva un **overlay di debug** che mostrava l'ora
corretta via SNTP (`time.android.com`) sopra il video. Le misure qui sotto sono
state fatte così. L'overlay è stato poi **rimosso** dall'app.

Per misurare di nuovo senza overlay:

- **Metodo consigliato:** filma con un secondo telefono (meglio a 120/240 fps)
  sia lo schermo del Realme sia un orologio di riferimento preciso (es.
  time.is su un PC sincronizzato). La differenza tra i due orologi nel video
  è la latenza "glass-to-glass", pannello compreso.
- Le statistiche del decoder (fps, ms di decodifica, frame scartati) restano
  in logcat, a livello verbose:

```bash
adb logcat -s ZeroCopyVideoView:V
# V ZeroCopyVideoView: stats: Stats(decoderName=c2.qti.avc.decoder.low_latency,
#   lowLatency=true, width=1280, height=720, fps=29.73, decodeLatencyMs=7.33, droppedInput=0)
```

### Come leggere una misura

Con l'overlay la latenza si leggeva come **ora NTP del telefono − ora grande
sul video**. Esempio reale (10 campioni):

| NTP telefono | Ora sul video | Differenza |
|---|---|---|
| 19.569 | 19.553 | 16 ms |
| 20.931 | 20.914 | 17 ms |
| 22.243 | 22.220 | 23 ms |
| 24.884 | 24.883 | 1 ms |
| 28.772 | 28.646 | **126 ms** (picco Wi-Fi) |
| 30.084 | 30.084 | 0 ms |

**Come interpretarli:**

- A regime la latenza è **sotto la risoluzione della misura**, cioè circa
  0–25 ms. L'incertezza NTP è ±21 ms, per questo alcuni valori escono
  leggermente negativi.
- I picchi isolati vengono dal Wi-Fi. Il ritardo **non si accumula**: dopo un
  picco la pipeline torna subito a ~0, segno che non ci sono buffer che si
  riempiono.
- Lo screenshot cattura l'immagine composta, **non il pannello**: aggiungi
  ~10–20 ms di scansione del display.
- Con una telecamera PTZ vera va aggiunta la latenza dell'**encoder della
  telecamera** (spesso 50–150 ms). Conviene impostare un GOP corto (≤ 0.5–1 s)
  e disattivare i B-frame dall'interfaccia web della camera.

---

## 4. Compilare e installare senza Android Studio

Toolchain attuale: **JDK 25** (Corretto 25.0.5), Gradle 9.9.2, AGP 9.5.2,
Kotlin 2.4.21, compileSdk/targetSdk 37 (37.2), **minSdk 29 (Android 10)**,
platform-tools/ADB 37.0.3. `jvmToolchain(25)` nel build fa sì che, se il JDK 25
manca, Gradle lo scarichi da solo (plugin foojay).

```bash
# da PTZ-Vision-Android/
export JAVA_HOME="$HOME/.jdks/corretto-25.0.5"
./gradlew assembleDebug

# aggiornare SDK / ADB (cmdline-tools installati in Sdk/cmdline-tools/latest)
# da PowerShell, per via del ';' nei nomi dei pacchetti:
#   sdkmanager.bat "platform-tools" "platforms;android-37.2" "build-tools;37.0.0"

ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
"$ADB" install -r app/build/outputs/apk/debug/app-debug.apk

# log utili
"$ADB" logcat -s RtspNalExtractor LowLatencyDecoder ZeroCopyVideoView:V RtspClientAdapter LowLatencyWifiLock

# unit test (JVM, senza dispositivo)
./gradlew testDebugUnitTest
# report HTML: app/build/reports/tests/testDebugUnitTest/index.html
```

Log attesi con lo stream del Pi:

```
D RtspNalExtractor: Connecting to 192.168.1.11:8554
D LowLatencyDecoder: Decoder c2.qti.avc.decoder.low_latency started (lowLatency=true, vendorKeys=true)
D RtspNalExtractor: PLAY ok, session=... timeout=60s pt=96
D LowLatencyDecoder: Output format: 1280x720
```

---

## 5. Unit test

I test girano sulla JVM del PC (non serve il telefono): 39 test, ~0.2 s.

| File di test | Cosa verifica |
|---|---|
| `zerocopy/AccessUnitAssemblerTest` | RTP → fotogrammi H.264: marker bit, cambio timestamp, FU-A, STAP-A, AUD, perdita pacchetti, attesa keyframe, header RTP con CSRC/estensioni/padding |
| `zerocopy/SdpParserTest` | Parsing SDP (payload type, control, SPS/PPS da `sprop-parameter-sets`, tracce audio, H.265 non supportato) e risoluzione URL di controllo |
| `zerocopy/RtspNalExtractorSessionTest` | Sessione completa contro un **finto server RTSP** su localhost: handshake, header `Transport`/`Session`/`Authorization`, dati interleaved con RTCP e risposte RTSP in mezzo, errori 404, H.265 → fallback, connessione rifiutata |
| `controller/ViscaCommandsTest` | Byte esatti dei comandi VISCA (pan/tilt, zoom, focus, scene, tracking) e validazione dei parametri |
| `RtspUrlTest` | URL RTSP della camera, che non deve cambiare quando cambiano pan/tilt/zoom |

Per rendere testabile il codice senza Android:

- `android.util.Base64` è stato sostituito da `java.util.Base64`, disponibile
  da Android 8, quindi va bene con minSdk 29.
- `parseSdp`, `resolveControl` e `AccessUnitAssembler` sono `internal`, quindi
  visibili ai test.
- `unitTests.isReturnDefaultValues = true` fa sì che `android.util.Log` non
  lanci eccezioni sulla JVM.

> **Prova che i test servono:** disattivando la riga `if (marker) flush()`
> (il cuore della correzione della latenza) falliscono 11 test su 39.

Non sono coperti da unit test il decoder (`MediaCodec`) e le View: richiedono
un dispositivo, quindi andrebbero testati con test strumentati (`androidTest`).

---

## 6. Limiti noti e prossimi passi

- **Autenticazione:** il percorso ZeroCopy supporta solo Basic auth. Il modello
  `Cam` oggi non ha credenziali; con Digest auth interviene il fallback.
- **H.265:** gestito solo dal fallback.
- **Riscrittura SPS** (forzare `max_dec_frame_buffering=0` nei decoder senza
  modalità low-latency): la fa la libreria nel fallback, non ancora il
  percorso ZeroCopy. Sui Qualcomm non serve.
- **Misura più rigorosa:** filmare con un altro telefono a 240 fps sia il
  monitor del Pi sia lo schermo del Realme. Così si include anche il pannello e
  si elimina il problema della sincronizzazione degli orologi.
