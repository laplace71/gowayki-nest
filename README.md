# Gowayki Nesh

## SOS en red local (desarrollo)

La pantalla SOS reutiliza el mapa OSM de la app. Consulta el servicio cada 3 segundos,
muestra solo alertas `ACTIVE` y cierra la zona del mapa al cerrar una alerta. Cada aura
se proyecta como un radio geográfico de 250 m (no píxeles). En el flujo del botón físico,
la posición se obtiene del GPS del teléfono, no de coordenadas de prueba. El mapa necesita
Internet para descargar los tiles de OpenStreetMap.

Los mosaicos empiezan a descargarse al calcular el primer encuadre, priorizando los más
cercanos al centro del mapa; las descargas y la decodificación ocurren fuera del hilo de
interfaz. Se conservan en memoria durante la vida de la pantalla, los errores no bloquean
reintentos y la acción **Reintentar** vuelve a solicitar las zonas que faltaron. Sin
conexión a Internet, OpenStreetMap no puede entregar calles nuevas.

El repositorio no contenía backend ni firmware ESP32. Se añadió `:sos-server`, un
servicio de desarrollo en Kotlin/JVM que expone:

* `POST /api/sos/alerts`: registra una pulsación del botón físico (reenviada por el
  teléfono vía Bluetooth) y la vincula a la última posición móvil válida con el mismo
  `deviceId`. Solo acepta peticiones desde la red privada; no usa tokens.
* `POST /api/sos/locations`: recibe la posición del teléfono. Solo acepta la LAN,
  lecturas de hasta 15 segundos y precisión de hasta 100 m. No usa tokens.
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

La ESP32 ya no usa Wi-Fi: se comunica con el teléfono por **Bluetooth Low Energy**.
La placa actúa como periférico BLE (servicio `6f1a0001-…`), notifica cada pulsación al
teléfono y espera su confirmación. La app Android se conecta al botón desde la pantalla
SOS, recibe el evento y lo reenvía al backend con `POST /api/sos/alerts` sin tokens;
el resto del flujo (GPS del teléfono, deduplicación, mapa) no cambia.

### Configurar y ejecutar

1. Copia `.env.example` a `.env`, conserva tus valores existentes y rellena
   `SOS_API_BASE_URL` con `http://<IPv4-de-esta-PC>:8080`. Obtén la IPv4 con `ipconfig`.
   Usa la IPv4 real de la PC, no `localhost`, para que el teléfono y el ESP32 puedan
   alcanzar el servidor. No pongas tokens en `.env` de la app.
2. Para iniciar el backend oculto en segundo plano al iniciar sesión en Windows,
   ejecuta una vez desde PowerShell en la raíz del repositorio:

   ```powershell
   .\scripts\wayki-sos-backend.ps1 -Action Install
   ```

   El script lee el ID de dispositivo del `secrets.h` local, compila el backend y
   registra una tarea de inicio de sesión. No hace falta mantener
   `.\gradlew.bat :sos-server:run` abierto.

   Para administrarlo:

   ```powershell
   .\scripts\wayki-sos-backend.ps1 -Action Status
   .\scripts\wayki-sos-backend.ps1 -Action Start
   .\scripts\wayki-sos-backend.ps1 -Action Stop
   .\scripts\wayki-sos-backend.ps1 -Action Uninstall
   ```

   `Uninstall` quita la tarea y la configuración local. Si el puerto 8080 está
   ocupado por otro proceso, el script lo informa y no lo detiene.
3. Conecta el teléfono a la misma red Wi-Fi que la PC (la ESP32 ya no usa Wi-Fi:
   se enlaza al teléfono por Bluetooth) y ejecuta
   `.\gradlew.bat :androidApp:assembleDebug`. El tráfico HTTP local está permitido solo
   en el build `debug`; el build `release` exige HTTPS.
4. Abre la pantalla SOS y pulsa **Compartir GPS mientras la app esté abierta**.
   Android solicitará permiso; permite
   ubicación precisa y deja abierta la pantalla. Que la app muestre «Servidor en línea»
   solo confirma que puede consultar el backend; para aceptar una alerta física, el
   backend también requiere el GPS reciente compartido por el teléfono. Al pulsar GPIO14, la
   ESP32 notifica su ID por Bluetooth al teléfono, la app lo reenvía al servidor y este
   usa exclusivamente la última lectura GPS fresca del teléfono para crear la alerta.
   Si no hay ubicación fresca, el servidor devuelve `409` y no crea una emergencia; la
   placa conserva ese evento para reintentar. No se aceptan coordenadas simuladas ni se
   genera una alerta desde un botón virtual.

Los logs quedan en `%LOCALAPPDATA%\Waykiu\SosBackend\server.out.log` y
`server.err.log`. Windows Firewall puede requerir una regla entrante para TCP 8080.
Si el hotspot aparece como red Pública, limita la regla a la IPv4 Wi-Fi actual de la
PC, la interfaz Wi-Fi y la subred local; actualiza la dirección de la regla si cambia
la IPv4. No permitas TCP 8080 desde cualquier dirección en perfiles públicos.

### Cargar el firmware de prueba ESP32 (Bluetooth)

1. Copia `hardware/esp32_sos_button/secrets.example.h` como
   `hardware/esp32_sos_button/secrets.h`; rellena localmente el identificador
   (`SOS_DEVICE_ID`), que también es el nombre Bluetooth de la placa. El archivo de
   ejemplo contiene solo marcadores y `secrets.h` está excluido de Git. Con BLE la
   placa no necesita SSID, clave Wi-Fi, IPv4 de la PC ni tokens.
2. Instala el paquete de placas **esp32 by Espressif Systems** en Arduino IDE. Abre
   `hardware/esp32_sos_button/esp32_sos_button.ino`, selecciona **ESP32 Dev Module** y
   el puerto USB de la ESP32 DevKitC V4. El sketch usa la librería BLE incluida en ese
   paquete (`BLEDevice.h`); no necesita librerías Arduino adicionales.
3. Carga el programa y abre Serial Monitor a 115200 baudios. El botón entre GPIO14 y
   GND genera un pitido corto y crea el evento. El buzzer va conectado a GPIO25 y GND;
   permanece en nivel inactivo y recibe un tono PWM breve de bajo ciclo de trabajo al
   presionar. La placa no incluye coordenadas: el servidor solo crea la alerta si
   recibe el GPS reciente del teléfono. Reintentos conservan el ID; la placa filtra
   rebotes durante 40 ms y reintenta la notificación BLE cada 5 segundos hasta que el
   teléfono confirme la entrega.
4. En la pantalla SOS de la app, pulsa **Vincular botón por Bluetooth** en la tarjeta
   «Botón físico … (Bluetooth)». Android pedirá permisos de Bluetooth; acéptalos y
   mantén la placa cerca. Al presionar el interruptor (GPIO14), el monitor serie
   muestra la notificación y la confirmación del teléfono; la app reenvía la alerta al
   backend. Tras cambiar `secrets.h`, recompila y vuelve a cargar el firmware: editar
   el archivo no actualiza la placa por sí solo.

Ejemplo de cuerpo que el teléfono reenvía al backend tras recibir el evento BLE (la
fecha debe ser UTC ISO-8601 y el `eventId` se mantiene igual en todos los reintentos
de esa pulsación):

```json
{
  "eventId": "evt-01J9XQ3D4G8K7M2P",
  "deviceId": "ESP32-AQP-017",
  "occurredAt": "2026-10-09T05:30:00Z"
}
```

No hay GPS en la placa: la fuente de ubicación real de este flujo es el teléfono
Android, mientras la pantalla SOS permanece abierta, el permiso está concedido y el
móvil comparte una lectura fresca con el backend. iOS todavía no tiene implementados
el lector GPS nativo ni el cliente BLE del botón.

## Iconos (SVG → XML)

1. Coloca el `.svg` en `shared/src/commonMain/iconsSvg/`
2. Corre `./gradlew :shared:convertSvgIcons` (también corre solo al build)
3. Usa el recurso: `painterResource(Res.drawable.nombre_archivo)`

Salida: `shared/src/commonMain/composeResources/drawable/*.xml`  
Requisito: Node.js (`npx svg2vectordrawable`)
