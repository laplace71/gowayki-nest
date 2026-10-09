#include <HTTPClient.h>
#include <Preferences.h>
#include <WiFi.h>
#include <time.h>

#include "secrets.h"

constexpr uint8_t SOS_BUTTON_PIN = 14;
constexpr bool TEST_MODE = true;
constexpr double TEST_LATITUDE = -12.0464;
constexpr double TEST_LONGITUDE = -77.0422;
constexpr uint32_t BUTTON_DEBOUNCE_MS = 40;
constexpr uint32_t RETRY_INTERVAL_MS = 5000;
constexpr uint32_t WIFI_CONNECT_TIMEOUT_MS = 20000;

Preferences preferences;
String pendingEventId;
String pendingOccurredAt;
bool pendingAlert = false;
bool lastRawButton = HIGH;
bool stableButton = HIGH;
uint32_t lastButtonChangeMs = 0;
uint32_t lastSendAttemptMs = 0;
uint32_t lastWifiAttemptMs = 0;
uint32_t nextEventSequence = 0;
bool wifiConnecting = false;
bool wifiWasConnected = false;

String utcTimestamp() {
  const time_t now = time(nullptr);
  if (now < 1735689600) {
    return "";
  }

  struct tm utc;
  gmtime_r(&now, &utc);
  char formatted[25];
  strftime(formatted, sizeof(formatted), "%Y-%m-%dT%H:%M:%SZ", &utc);
  return String(formatted);
}

void savePendingAlert() {
  preferences.putBool("pending", pendingAlert);
  preferences.putString("eventId", pendingEventId);
  preferences.putString("occurredAt", pendingOccurredAt);
}

void clearPendingAlert() {
  pendingAlert = false;
  pendingEventId = "";
  pendingOccurredAt = "";
  savePendingAlert();
}

void createTestAlert() {
  if (!TEST_MODE) {
    Serial.println("SOS bloqueado: este sketch no incluye un receptor GPS.");
    return;
  }
  if (pendingAlert) {
    Serial.println("Hay una alerta pendiente de entrega; se conserva su ID para reintentar.");
    return;
  }

  pendingOccurredAt = utcTimestamp();
  if (pendingOccurredAt.length() == 0) {
    Serial.println("No se creó la alerta: el reloj UTC todavía no está sincronizado por NTP.");
    return;
  }

  ++nextEventSequence;
  preferences.putUInt("sequence", nextEventSequence);
  char macText[17];
  snprintf(macText, sizeof(macText), "%llx",
           static_cast<unsigned long long>(ESP.getEfuseMac()));
  const String mac(macText);
  pendingEventId = "evt-" + mac + "-" + String(nextEventSequence);
  pendingAlert = true;
  savePendingAlert();
  Serial.println("Prueba SOS creada; coordenadas ficticias, no usar como ubicación real.");
}

void readButton() {
  const bool raw = digitalRead(SOS_BUTTON_PIN);
  const uint32_t now = millis();
  if (raw != lastRawButton) {
    lastRawButton = raw;
    lastButtonChangeMs = now;
  }
  if (now - lastButtonChangeMs < BUTTON_DEBOUNCE_MS || stableButton == raw) {
    return;
  }
  stableButton = raw;
  if (stableButton == LOW) {
    createTestAlert();
  }
}

void reconnectWifi() {
  const uint32_t now = millis();
  if (WiFi.status() == WL_CONNECTED) {
    if (!wifiWasConnected) {
      Serial.printf("Wi-Fi conectado; canal=%u; IPv4=%s\n", WiFi.channel(),
                    WiFi.localIP().toString().c_str());
    }
    wifiConnecting = false;
    wifiWasConnected = true;
    return;
  }

  if (wifiWasConnected) {
    Serial.println("Wi-Fi desconectado; se iniciará una nueva conexión.");
    wifiWasConnected = false;
    wifiConnecting = false;
    lastWifiAttemptMs = now - RETRY_INTERVAL_MS;
  }

  if (wifiConnecting) {
    if (now - lastWifiAttemptMs < WIFI_CONNECT_TIMEOUT_MS) {
      return;
    }
    Serial.println("Wi-Fi no conectó en 20 s; se reiniciará el intento.");
    WiFi.disconnect(false, false);
    wifiConnecting = false;
    lastWifiAttemptMs = now;
    return;
  }

  if (now - lastWifiAttemptMs < RETRY_INTERVAL_MS) {
    return;
  }
  Serial.println("Intentando Wi-Fi; la ESP32 requiere una red de 2.4 GHz.");
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  lastWifiAttemptMs = now;
  wifiConnecting = true;
}

void sendPendingAlert() {
  if (!pendingAlert || WiFi.status() != WL_CONNECTED ||
      millis() - lastSendAttemptMs < RETRY_INTERVAL_MS) {
    return;
  }
  lastSendAttemptMs = millis();

  HTTPClient http;
  http.setTimeout(5000);
  if (!http.begin(SOS_API_URL)) {
    Serial.println("No se pudo abrir la URL SOS; revisa la IPv4 del servidor.");
    return;
  }

  const String body =
      "{\"eventId\":\"" + pendingEventId +
      "\",\"deviceId\":\"" + String(SOS_DEVICE_ID) +
      "\",\"latitude\":" + String(TEST_LATITUDE, 7) +
      ",\"longitude\":" + String(TEST_LONGITUDE, 7) +
      ",\"occurredAt\":\"" + pendingOccurredAt +
      "\",\"simulated\":true}";
  http.addHeader("Content-Type", "application/json");
  http.addHeader("Authorization", "Bearer " + String(SOS_DEVICE_TOKEN));
  const int responseCode = http.POST(body);
  http.end();

  if (responseCode == 200 || responseCode == 201) {
    Serial.println("Alerta de prueba aceptada por el servidor.");
    clearPendingAlert();
  } else if (responseCode == 429) {
    Serial.println("Alerta repetida suprimida por el servidor (debounce).");
    clearPendingAlert();
  } else if (responseCode >= 400 && responseCode < 500 &&
             responseCode != 408 && responseCode != 409) {
    Serial.printf("El servidor rechazó la alerta (HTTP %d); se descarta el evento.\n", responseCode);
    clearPendingAlert();
  } else {
    Serial.printf("Error de red/servidor (HTTP %d); se reintentará con el mismo eventId.\n",
                  responseCode);
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(SOS_BUTTON_PIN, INPUT_PULLUP);
  WiFi.onEvent([](WiFiEvent_t event, WiFiEventInfo_t info) {
    if (event == ARDUINO_EVENT_WIFI_STA_DISCONNECTED) {
      Serial.printf("Wi-Fi desconectado; motivo del router=%u.\n",
                    info.wifi_sta_disconnected.reason);
    }
  });
  preferences.begin("wayki-sos", false);
  nextEventSequence = preferences.getUInt("sequence", 0);
  pendingAlert = preferences.getBool("pending", false);
  if (pendingAlert) {
    pendingEventId = preferences.getString("eventId", "");
    pendingOccurredAt = preferences.getString("occurredAt", "");
  }

  WiFi.mode(WIFI_STA);
  lastWifiAttemptMs = millis() - RETRY_INTERVAL_MS;
  configTime(0, 0, "pool.ntp.org", "time.google.com");
  Serial.println("ESP32 SOS lista; TEST_MODE usa coordenadas ficticias solo si el móvil no aporta GPS.");
  Serial.println("Botón físico: GPIO14 a GND. Conecta la placa a Wi-Fi 2.4 GHz.");
}

void loop() {
  reconnectWifi();
  readButton();
  sendPendingAlert();
  delay(5);
}
