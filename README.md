# Gowayki Nesh

## SOS en red local (desarrollo)

La pantalla SOS reutiliza el mapa OSM de la app. Consulta el servicio cada 3 segundos,
muestra solo alertas `ACTIVE` y cierra la zona del mapa al cerrar una alerta. Cada aura
se proyecta como un radio geográfico de 250 m (no píxeles); la ubicación de prueba se
marca explícitamente como ficticia. El mapa necesita Internet para descargar los tiles
de OpenStreetMap.

El repositorio no contenía backend ni firmware ESP32. Se añadió `:sos-server`, un
servicio de desarrollo en Kotlin/JVM que expone:

* `POST /api/sos/alerts`: registra la alerta; requiere `Authorization: Bearer <token>`
  individual del dispositivo.
* `GET /api/sos/alerts?status=ACTIVE`: entrega las alertas activas solo a clientes de
  redes privadas.
* `GET /api/sos/alerts?status=CLOSED`: consulta las alertas cerradas del proceso actual.
* `DELETE /api/sos/alerts/{id}`: cierra la alerta desde la red privada; el registro
  permanece con estado `CLOSED` mientras el proceso siga ejecutándose.

El servicio usa polling HTTP como alternativa sencilla a WebSocket. Los eventos se
deduplican por `(deviceId, eventId)`, por lo que se puede reintentar el mismo POST sin
crear otra alerta. Además, el servidor rechaza con HTTP 429 una segunda alerta del mismo
dispositivo durante 2 segundos, como protección adicional contra rebotes del botón. El
firmware también aplica debounce local de 40 ms y conserva el mismo `eventId` al
reintentar. El almacenamiento es en memoria y se pierde al reiniciar el proceso;
este proyecto no tenía un historial SOS persistente.

### Configurar y ejecutar

1. Copia `.env.example` a `.env`, conserva tus valores existentes y rellena
   `SOS_API_BASE_URL` con `http://<IPv4-de-esta-PC>:8080`. Obtén la IPv4 con `ipconfig`.
   Usa la IPv4 real de la PC, no `localhost`, para que el teléfono y el ESP32 puedan
   alcanzar el servidor. No pongas tokens en `.env` de la app.
2. En PowerShell, crea un token aleatorio de al menos 32 caracteres por placa y arranca
   el servicio. No compartas ni subas los tokens:

   ```powershell
   $bytes = New-Object byte[] 32
   [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
   $token = [Convert]::ToBase64String($bytes)
   $env:SOS_DEVICE_TOKENS = "ESP32-AQP-017=$token"
   .\gradlew.bat :sos-server:run
   ```

   Para varios dispositivos, asigna a `SOS_DEVICE_TOKENS` pares
   `DEVICE_ID=token` separados por comas. El servidor falla al iniciar si no se
   configuran.
3. Conecta el teléfono/ESP32 a la misma red Wi-Fi y ejecuta
   `.\gradlew.bat :androidApp:assembleDebug`. El tráfico HTTP local está permitido solo
   en el build `debug`; el build `release` exige HTTPS.
4. Abre la pantalla SOS. Si `SOS_API_BASE_URL` está vacío, el botón **Simular alerta
   SOS** crea una alerta ficticia en las coordenadas de muestra existentes de Lima.
   Si la URL está configurada, la pantalla consulta el backend y muestra errores de
   conexión/reintenta en el siguiente ciclo.

### Cargar el firmware de prueba ESP32

1. Copia `hardware/esp32_sos_button/secrets.example.h` como
   `hardware/esp32_sos_button/secrets.h`; rellena localmente Wi-Fi, IPv4 de la PC,
   identificador y el mismo token que configuraste en `SOS_DEVICE_TOKENS`.
2. Instala el paquete de placas **esp32 by Espressif Systems** en Arduino IDE. Abre
   `hardware/esp32_sos_button/esp32_sos_button.ino`, selecciona **ESP32 Dev Module** y
   el puerto USB de la ESP32 DevKitC V4. El sketch usa `WiFi`, `HTTPClient` y
   `Preferences` incluidos en ese paquete; no necesita librerías Arduino adicionales.
3. Carga el programa y abre Serial Monitor a 115200 baudios. El pulsador entre GPIO14
   y GND genera una alerta únicamente después de sincronizar la hora UTC. La app la
   etiqueta como simulada. Reintentos de red conservan el mismo ID; la placa también
   filtra rebotes durante 40 ms.

Ejemplo de cuerpo que el ESP32 debe enviar (la fecha debe ser UTC ISO-8601 y el
`eventId` debe mantenerse igual en todos los reintentos de esa pulsación):

```json
{
  "eventId": "evt-01J9XQ3D4G8K7M2P",
  "deviceId": "ESP32-AQP-017",
  "latitude": -12.0464,
  "longitude": -77.0422,
  "occurredAt": "2026-10-09T05:30:00Z",
  "simulated": true
}
```

Ese ejemplo usa coordenadas de muestra ya definidas en la app; no es una emergencia ni
la ubicación del hardware. El sketch de
`hardware/esp32_sos_button/esp32_sos_button.ino` está limitado a modo de prueba, usa el
botón GPIO 14 conectado entre el pin y GND, y siempre envía `simulated: true`. La placa
informada es ESP32 DevKitC V4. No hay módulo GPS conectado ni otra fuente de ubicación,
así que el sketch no reporta ubicaciones reales y el mapa identifica esas alertas como
simuladas. Para ubicar emergencias reales falta agregar un GPS y seleccionar su modelo,
cableado UART y velocidad antes de habilitar coordenadas reales.

## Iconos (SVG → XML)

1. Coloca el `.svg` en `shared/src/commonMain/iconsSvg/`
2. Corre `./gradlew :shared:convertSvgIcons` (también corre solo al build)
3. Usa el recurso: `painterResource(Res.drawable.nombre_archivo)`

Salida: `shared/src/commonMain/composeResources/drawable/*.xml`  
Requisito: Node.js (`npx svg2vectordrawable`)
