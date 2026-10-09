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

* `POST /api/sos/alerts`: registra una pulsación autenticada del dispositivo y la
  vincula a la última posición móvil válida con el mismo `deviceId`.
* `POST /api/sos/locations`: recibe la posición del teléfono con el secreto
  `SOS_MOBILE_LOCATION_TOKEN`. Solo acepta la LAN, lecturas de hasta 15 segundos y
  precisión de hasta 100 m.
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
2. Para iniciar el backend oculto en segundo plano al iniciar sesión en Windows,
   ejecuta una vez desde PowerShell en la raíz del repositorio:

   ```powershell
   .\scripts\wayki-sos-backend.ps1 -Action Install
   ```

   El script lee el ID y el token de dispositivo del `secrets.h` local, solicita el
   token móvil (el mismo que se pondrá en la app), compila el backend y registra una
   tarea de inicio de sesión. Las credenciales quedan cifradas con DPAPI para el usuario
   actual, fuera del repositorio. No hace falta mantener
   `.\gradlew.bat :sos-server:run` abierto.

   Para administrarlo:

   ```powershell
   .\scripts\wayki-sos-backend.ps1 -Action Status
   .\scripts\wayki-sos-backend.ps1 -Action Start
   .\scripts\wayki-sos-backend.ps1 -Action Stop
   .\scripts\wayki-sos-backend.ps1 -Action Uninstall
   ```

   `Uninstall` quita la tarea y las credenciales protegidas. Si el puerto 8080 está
   ocupado por otro proceso, el script lo informa y no lo detiene.
3. Conecta el teléfono/ESP32 a la misma red Wi-Fi y ejecuta
   `.\gradlew.bat :androidApp:assembleDebug`. El tráfico HTTP local está permitido solo
   en el build `debug`; el build `release` exige HTTPS.
4. Abre la pantalla SOS, escribe el mismo token móvil ingresado durante `Install` en el
   campo del token y pulsa **Compartir GPS mientras la app esté abierta**. Android solicitará
   permiso; permite ubicación precisa y deja abierta la pantalla. Al pulsar GPIO14, la
   ESP32 envía su ID y el servidor usa exclusivamente la última lectura GPS fresca del
   teléfono para crear la alerta. Si no hay ubicación fresca, el servidor devuelve `409`
   y no crea una emergencia; la placa conserva ese evento para reintentar. No se aceptan
   coordenadas simuladas ni se genera una alerta desde un botón virtual.

Los logs quedan en `%LOCALAPPDATA%\Waykiu\SosBackend\server.out.log` y
`server.err.log`. Windows Firewall puede requerir permitir TCP 8080 solo en el perfil
Privado para que el móvil y la placa alcancen el servidor; no abras el puerto a redes
públicas.

### Cargar el firmware de prueba ESP32

1. Copia `hardware/esp32_sos_button/secrets.example.h` como
   `hardware/esp32_sos_button/secrets.h`; rellena localmente Wi-Fi, IPv4 de la PC,
   identificador y el token de dispositivo de `SOS_DEVICE_TOKENS`. El archivo de
   ejemplo contiene solo marcadores y `secrets.h` está excluido de Git.
2. Instala el paquete de placas **esp32 by Espressif Systems** en Arduino IDE. Abre
   `hardware/esp32_sos_button/esp32_sos_button.ino`, selecciona **ESP32 Dev Module** y
   el puerto USB de la ESP32 DevKitC V4. El sketch usa `WiFi`, `HTTPClient` y
   `Preferences` incluidos en ese paquete; no necesita librerías Arduino adicionales.
3. Carga el programa y abre Serial Monitor a 115200 baudios. El botón entre GPIO14 y
   GND genera el evento después de sincronizar UTC. La placa no incluye coordenadas:
   el servidor solo crea la alerta si recibe el GPS reciente del teléfono.
   Reintentos conservan el ID; la placa filtra rebotes durante 40 ms.
   La ESP32 DevKitC V4 requiere Wi-Fi de 2.4 GHz; si el router separa las bandas,
   configura el SSID/clave de 2.4 GHz. La red de la placa y la PC servidor deben
   tener conectividad entre sí; una PC conectada a 5 GHz puede compartir la LAN con
   la ESP32 en 2.4 GHz si el router permite esa comunicación.
4. Espera el mensaje `Wi-Fi conectado`; el monitor indica el canal y la IPv4 recibida
   por la placa. Si muestra reintentos cada 20 segundos, revisa SSID/clave, la banda
   de 2.4 GHz y que la URL `SOS_API_URL` apunte a la IPv4 actual de la PC. Al pulsar,
   comprueba el mensaje de alerta aceptada o el código HTTP de rechazo.

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

Ese ejemplo de coordenadas es ficticio. No hay GPS en la placa: la fuente de ubicación
real de este flujo es el teléfono Android, mientras la pantalla SOS permanece abierta,
el permiso está concedido, el móvil está conectado a la Wi-Fi local y comparte una
lectura fresca. iOS todavía no tiene implementado el lector GPS nativo.

## Iconos (SVG → XML)

1. Coloca el `.svg` en `shared/src/commonMain/iconsSvg/`
2. Corre `./gradlew :shared:convertSvgIcons` (también corre solo al build)
3. Usa el recurso: `painterResource(Res.drawable.nombre_archivo)`

Salida: `shared/src/commonMain/composeResources/drawable/*.xml`  
Requisito: Node.js (`npx svg2vectordrawable`)
