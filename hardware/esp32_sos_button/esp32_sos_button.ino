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
  if (WiFi.status() == WL_CONNECTED || millis() - lastWifiAttemptMs < RETRY_INTERVAL_MS) {
    return;
  }
  lastWifiAttemptMs = millis();
  Serial.println("Wi-Fi desconectado; reintentando conexión.");
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
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
    Serial.println("No se pudo abrir la URL SOS; se conservará el evento para reintentar.");
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
  preferences.begin("wayki-sos", false);
  nextEventSequence = preferences.getUInt("sequence", 0);
  pendingAlert = preferences.getBool("pending", false);
  if (pendingAlert) {
    pendingEventId = preferences.getString("eventId", "");
    pendingOccurredAt = preferences.getString("occurredAt", "");
  }

  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  configTime(0, 0, "pool.ntp.org", "time.google.com");
  Serial.println("ESP32 SOS listo en TEST_MODE; GPIO14 se activa al conectarlo a GND.");
}

void loop() {
  reconnectWifi();
  readButton();
  sendPendingAlert();
  delay(5);
}
