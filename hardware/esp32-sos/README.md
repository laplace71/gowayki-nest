# ESP32 · botón de pánico Wayki Nest

Firmware Arduino para el **botón físico de SOS** que acompaña a la app móvil de Wayki Nest.

Este directorio contiene **únicamente el código fuente** (`codigo_spe32.ino`), copiado de
`/home/jean/spe32/codigo_spe32`. Los binarios y artefactos de compilación (`build/`, `.elf`,
`.bin`, `.map`, `sdkconfig`, etc., ~29 MB) **no se versionan**: se regeneran con el IDE de Arduino.

## Qué hace

Un ESP32 con un pulsador y un buzzer que se comunica con el celular por **BLE**, sin WiFi,
sin tokens y sin depender del backend. Hay dos direcciones de comunicación:

| Dirección | Quién la dispara | Mensaje BLE | Efecto en la app |
| --- | --- | --- | --- |
| ESP32 → celular | 1 clic | `SOS!` | El chofer confirma el SOS desde la pantalla |
| ESP32 → celular | 3 clics en menos de 3 s | `SOS_TRIPLE` | Envía el punto GPS de emergencia y dispara la alerta |
| ESP32 → ESP32 | otro botón en emergencia | `SOS_ALERT` | Suena el buzzer y avisa de una unidad cercana en emergencia |

Triple clic además emite por BLE el UUID de servicio de alarma `0xFFF1` durante **25 s**
(`ALARM_MS`) para que otros ESP32 y la app detecten la emergencia.

## Dónde está en el modelo OSI

La comunicación de este botón **no usa IP**: es BLE punto a punto (el ESP32 con el celular, y
ESP32 con ESP32), así que no hay capas de red/transporte TCP/IP.

| Capa OSI | Qué interviene aquí |
| --- | --- |
| **1 · Física** | Radio BLE 2,4 GHz; señales eléctricas del pulsador (`GPIO14`, `INPUT_PULLUP`) y del buzzer (`GPIO4`). |
| **2 · Enlace de datos** | Link Layer de BLE: advertising, scanning, conexión, direcciones y CRC. Aquí viven los UUID `0xFFF1`, `4fafc201-…` y `beb5483e-…`. |
| **3–4 · Red y transporte** | **No aplican**: no hay WiFi, IP, TCP ni UDP. BLE usa su propia pila (L2CAP) en lugar de TCP/IP. |
| **5–6 · Sesión y presentación** | GATT/ATT de BLE: descubrimiento de servicio, característica `READ | NOTIFY` y notificaciones. |
| **7 · Aplicación** | El protocolo de texto del proyecto: `SOS!`, `SOS_TRIPLE`, `SOS_ALERT`, interpretado por la app. |

En resumen: **capas 1 y 2 en el aire, GATT como protocolo de sesión/presentación y la semántica SOS en la capa de aplicación**.

## Qué más se usó en este proyecto

**En el firmware del botón**

- Arduino framework en C++ y placa ESP32 (toolchain `xtensa-esp32-elf`).
- Librería **NimBLE-Arduino** (NimBLEDevice) para BLE: servidor GATT, cliente/escáner y advertising.
- Periférico **LEDC** para el PWM del buzzer (2700 Hz, 8 bits).
- `INPUT_PULLUP` + máquina de estados con debounce para los clics.
- Tareas internas de FreeRTOS (BLE) y secciones críticas con `portMUX` para compartir banderas.
- Tiempo no bloqueante con `millis()`; sin `delay()` ni esperas activas.
- Filtrado exponencial del RSSI para el ritmo del buzzer.

**En el resto del sistema Wayki Nest**

- App móvil: **Kotlin Multiplatform + Compose Multiplatform**, Ktor para HTTP, MQTT para chat/SOS, navegación con `NavHost`.
- Backend de negocio: **Django REST Framework** sobre PostgreSQL/PostGIS, desplegado en **Docker** en la VPS.
- Backend de catálogo y mapa: **Spring Boot (Java)** leyendo la misma base de datos.
- Panel de administración: **React + TypeScript + Vite + MUI**.
- Editor de automatizaciones: **React Flow** (proyecto `flow`, estilo n8n).
- Videos de presentación: **HyperFrames** (HTML renderizado a video).

## Hardware

| Elemento | Pin | Detalle |
| --- | --- | --- |
| Pulsador | `GPIO14` | `INPUT_PULLUP`; pulsar = `LOW`. Debounce de 30 ms |
| Buzzer | `GPIO4` | PWM 2700 Hz, 8 bits. `DUTY_OFF = 255` (HIGH) = silencio |

Clics:

- **1 clic**: suelta después de menos de 700 ms → `SOS!`.
- **2 clics**: no generan mensaje (se descartan al vencer la ventana de 3 s).
- **3 clics**: el segundo debe llegar antes de 700 ms y el tercero antes de 3 s desde el
  primero → `SOS_TRIPLE`.

## BLE

- Nombre del dispositivo: `WAYKI_` + últimas cifras de la MAC (p. ej. `WAYKI_96E6`).
- Servicio principal: `4fafc201-1fb5-459e-8fcc-c5c9c331914b`.
- Característica `READ | NOTIFY`: `beb5483e-36e1-4688-b7f5-ea07361b26a8`.
- Servicio de alarma anunciado: `0xFFF1`.

Estos valores coinciden con los constantes de la app en
`shared/src/commonMain/kotlin/com/gowayki/nesh/app/features/sos/domain/model/SosModels.kt`
(`SERVICE_UUID`, `CHAR_UUID`, `DEVICE_NAME_PREFIX`, `PAYLOAD_TRIPLE`, `PAYLOAD_REMOTE`).
Si cambias un UUID aquí, cámbialo también allí.

## Alerta remota

Cada ESP32 escanea continuamente otros dispositivos que anuncien `0xFFF1` (excluyéndose a sí
mismo). Al detectar uno:

- envía `SOS_ALERT` al celular si hay conexión;
- activa el buzzer con un ritmo según el **RSSI** (fuerza de señal, **no distancia exacta**):
  `-60 dBm` → 100 ms, `-75 dBm` → 300 ms, más débil → 700 ms;
- termina a los 25 s o cuando deja de recibir la señal por más de 4 s.

El `loop()` no usa `delay`, `vTaskDelay` ni bucles de espera: usa `millis()` y banderas
protegidas con `portMUX` porque los callbacks de BLE escriben desde otra tarea.

## Compilar y grabar

1. Arduino IDE con el paquete de placas **esp32** instalado.
2. Librería **NimBLEDevice** (h2zero/NimBLE-Arduino).
3. Abrir `codigo_spe32.ino`, seleccionar la placa ESP32 y el puerto.
4. Compilar y subir. Monitor serie a **115200 baudios** para ver el nombre del dispositivo,
   las pulsaciones y si el celular estaba conectado al notificar.

Nota: si el celular no está conectado por BLE, la notificación se imprime en el monitor pero
**no se envía** (`Sin celular: ... (no enviada)`).

## Relación con el resto del sistema

El botón solo genera el evento. La lógica de SOS (confirmación, ubicación, turno, chat/MQTT y
grupos de compartir) vive en la app, en
`shared/src/commonMain/kotlin/com/gowayki/nesh/app/features/sos/`.
