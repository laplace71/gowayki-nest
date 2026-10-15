// Usa la librería BLE incluida en el paquete de placas ESP32 (BLEDevice.h);
// no requiere instalar librerías Arduino adicionales.
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <Preferences.h>

#include "secrets.h"

// Servicio BLE propio del botón SOS. El teléfono se conecta, se suscribe a la
// característica de alerta y reenvía cada evento al backend por HTTP.
#define SOS_SERVICE_UUID "6f1a0001-7c3b-4f2e-9a5d-2f8c1e0b9a01"
#define SOS_ALERT_CHAR_UUID "6f1a0002-7c3b-4f2e-9a5d-2f8c1e0b9a01"
#define SOS_ACK_CHAR_UUID "6f1a0003-7c3b-4f2e-9a5d-2f8c1e0b9a01"

constexpr uint8_t SOS_BUTTON_PIN = 14;
constexpr uint8_t BUZZER_PIN = 25;
constexpr uint32_t BUZZER_FREQUENCY_HZ = 2000;
constexpr uint8_t BUZZER_PWM_RESOLUTION = 8;
constexpr uint32_t BUZZER_OFF_DUTY = 255;
constexpr uint32_t BUZZER_BEEP_DUTY = 230;
constexpr uint32_t BUZZER_BEEP_MS = 100;
constexpr uint32_t BUTTON_DEBOUNCE_MS = 40;
constexpr uint32_t RETRY_INTERVAL_MS = 5000;

Preferences preferences;
BLECharacteristic* alertCharacteristic = nullptr;
String pendingEventId;
String pendingOccurredAt;
bool pendingAlert = false;
bool lastRawButton = HIGH;
bool stableButton = HIGH;
uint32_t lastButtonChangeMs = 0;
uint32_t lastSendAttemptMs = 0;
uint32_t lastDiagnosticMs = 0;
uint32_t nextEventSequence = 0;
bool bleClientConnected = false;

String utcTimestamp() {
  // Sin Wi-Fi no hay NTP: el teléfono reemplaza la fecha al reenviar la alerta.
  // Se envía un marcador fijo para que el servidor valide el formato ISO-8601.
  return "1970-01-01T00:00:00Z";
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

void createAlertEvent() {
  if (pendingAlert) {
    Serial.println("Hay una alerta pendiente de entrega; se conserva su ID para reintentar.");
    return;
  }

  pendingOccurredAt = utcTimestamp();
  ++nextEventSequence;
  preferences.putUInt("sequence", nextEventSequence);
  char macText[17];
  snprintf(macText, sizeof(macText), "%llx",
           static_cast<unsigned long long>(ESP.getEfuseMac()));
  const String mac(macText);
  pendingEventId = "evt-" + mac + "-" + String(nextEventSequence);
  pendingAlert = true;
  savePendingAlert();
  Serial.println("Evento SOS físico creado; el teléfono lo reenviará al servidor.");
}

void soundButtonFeedback() {
  ledcWrite(BUZZER_PIN, BUZZER_BEEP_DUTY);
  delay(BUZZER_BEEP_MS);
  ledcWrite(BUZZER_PIN, BUZZER_OFF_DUTY);
}

void readButton() {
  const bool raw = digitalRead(SOS_BUTTON_PIN);
  const uint32_t now = millis();
  if (raw != lastRawButton) {
    lastRawButton = raw;
    lastButtonChangeMs = now;
    Serial.printf("GPIO14 cambió a %s.\n", raw == LOW ? "LOW" : "HIGH");
  }
  if (now - lastButtonChangeMs < BUTTON_DEBOUNCE_MS || stableButton == raw) {
    return;
  }
  stableButton = raw;
  if (stableButton == LOW) {
    soundButtonFeedback();
    createAlertEvent();
  } else {
    Serial.println("Botón GPIO14 liberado.");
  }
}

class ServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* server) override {
    bleClientConnected = true;
    Serial.println("Teléfono conectado por BLE.");
  }

  void onDisconnect(BLEServer* server) override {
    bleClientConnected = false;
    Serial.println("Teléfono desconectado; se reanuda el advertising.");
    server->startAdvertising();
  }
};

class AckCallbacks : public BLECharacteristicCallbacks {
  // El teléfono escribe "ok" tras registrar la alerta en el servidor.
  void onWrite(BLECharacteristic* characteristic) override {
    const String value = characteristic->getValue().c_str();
    if (value == "ok" && pendingAlert) {
      Serial.println("El teléfono confirmó la entrega de la alerta.");
      clearPendingAlert();
    }
  }
};

void sendPendingAlert() {
  if (!pendingAlert || !bleClientConnected || alertCharacteristic == nullptr ||
      millis() - lastSendAttemptMs < RETRY_INTERVAL_MS) {
    return;
  }
  lastSendAttemptMs = millis();

  // BLE notifica ~20 bytes por paquete; se envía solo el número de evento y el
  // teléfono reconstruye el JSON completo con el deviceId y la fecha actual.
  const String payload = "SOS:" + String(nextEventSequence);
  alertCharacteristic->setValue(payload.c_str());
  alertCharacteristic->notify();
  Serial.println("Alerta notificada por BLE; esperando confirmación del teléfono.");
}

void setup() {
  Serial.begin(115200);
  pinMode(SOS_BUTTON_PIN, INPUT_PULLUP);
  if (!ledcAttach(BUZZER_PIN, BUZZER_FREQUENCY_HZ, BUZZER_PWM_RESOLUTION)) {
    Serial.println("No se pudo iniciar PWM para el buzzer en GPIO25.");
  } else {
    ledcWrite(BUZZER_PIN, BUZZER_OFF_DUTY);
  }

  preferences.begin("wayki-sos", false);
  nextEventSequence = preferences.getUInt("sequence", 0);
  pendingAlert = preferences.getBool("pending", false);
  if (pendingAlert) {
    pendingEventId = preferences.getString("eventId", "");
    pendingOccurredAt = preferences.getString("occurredAt", "");
  }

  BLEDevice::init(SOS_DEVICE_ID);
  BLEServer* server = BLEDevice::createServer();
  server->setCallbacks(new ServerCallbacks());

  BLEService* service = server->createService(SOS_SERVICE_UUID);
  alertCharacteristic = service->createCharacteristic(
      SOS_ALERT_CHAR_UUID,
      BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY);
  alertCharacteristic->addDescriptor(new BLE2902());
  BLECharacteristic* ackCharacteristic = service->createCharacteristic(
      SOS_ACK_CHAR_UUID, BLECharacteristic::PROPERTY_WRITE);
  ackCharacteristic->setCallbacks(new AckCallbacks());
  service->start();

  BLEAdvertising* advertising = BLEDevice::getAdvertising();
  advertising->addServiceUUID(SOS_SERVICE_UUID);
  advertising->setScanResponse(true);
  advertising->start();

  Serial.println("=== FIRMWARE BLE v4 (payload corto) ===");
  Serial.println("ESP32 SOS lista; las alertas se envían por BLE al teléfono.");
  Serial.println("Botón físico: GPIO14 a GND. Empareja el teléfono desde la app.");
  Serial.printf("Estado inicial del botón GPIO14: %s (valor crudo=%d).\n",
                digitalRead(SOS_BUTTON_PIN) == LOW ? "LOW (presionado)" : "HIGH (libre)",
                digitalRead(SOS_BUTTON_PIN));
}

void loop() {
  readButton();
  sendPendingAlert();
  if (millis() - lastDiagnosticMs >= 1000) {
    lastDiagnosticMs = millis();
    Serial.printf("Diagnóstico: GPIO14 crudo=%d; BLE=%s; alerta pendiente=%s.\n",
                  digitalRead(SOS_BUTTON_PIN),
                  bleClientConnected ? "conectado" : "anunciando",
                  pendingAlert ? "sí" : "no");
  }
  delay(5);
}
