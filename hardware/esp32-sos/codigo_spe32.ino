// ============================================================
// WAYKI - Sketch principal
// ============================================================

// ===== LIBRERÍAS =====
// ============================================================
#include <Arduino.h>
#include <NimBLEDevice.h>
#include <esp_arduino_version.h>
// ============================================================

// ===== CONFIGURACIÓN: PINES Y PARÁMETROS DEL BUZZER =====
// ============================================================
constexpr uint8_t BUTTON_PIN = 14, BUZZER_PIN = 4;
// Señal original: PWM de 2700 Hz, 8 bits; HIGH constante = silencio.
constexpr uint32_t BUZZ_FREQ = 2700;
constexpr uint8_t BUZZ_DUTY = 128, DUTY_OFF = 255;
constexpr uint8_t BUZZER_CHANNEL = 0;
// ============================================================

// ===== 3. FUNCIÓN ACTUADORA DEL BUZZER =====
void setBuzzer(bool on) {
#if ESP_ARDUINO_VERSION_MAJOR >= 3
  ledcWrite(BUZZER_PIN, on ? BUZZ_DUTY : DUTY_OFF);
#else
  ledcWrite(BUZZER_CHANNEL, on ? BUZZ_DUTY : DUTY_OFF);
#endif
}

// ===== 4. CONFIGURACIÓN: TIEMPOS Y UUIDs BLE =====
constexpr uint32_t ALARM_MS = 25000, DEBOUNCE_MS = 30;
constexpr uint32_t SINGLE_MS = 700, CLICK_WINDOW_MS = 3000;
constexpr char SERVICE_UUID[] = "4fafc201-1fb5-459e-8fcc-c5c9c331914b";
constexpr char CHAR_UUID[] = "beb5483e-36e1-4688-b7f5-ea07361b26a8";
constexpr uint16_t ALERT_UUID = 0xFFF1;

// ===== OBJETOS GLOBALES BLE =====
// ============================================================
NimBLECharacteristic* characteristic = nullptr;
NimBLEAdvertising* advertising = nullptr;
NimBLEScan* scanner = nullptr;
std::string ownAddress;
portMUX_TYPE mux = portMUX_INITIALIZER_UNLOCKED;

// Solo los callbacks escriben estas señales; loop las consume bajo exclusión.
bool remotePending = false, restartAdvertising = false;
uint32_t remoteSeenAt = 0;
int remoteRssi = -100;
// ============================================================

// ===== CALLBACKS BLE =====
// ============================================================
class ServerCallbacks : public NimBLEServerCallbacks {
  void onConnect(NimBLEServer*, NimBLEConnInfo&) override {
    portENTER_CRITICAL(&mux);
    restartAdvertising = true;
    portEXIT_CRITICAL(&mux);
  }
  void onDisconnect(NimBLEServer*, NimBLEConnInfo&, int) override {
    portENTER_CRITICAL(&mux);
    restartAdvertising = true;
    portEXIT_CRITICAL(&mux);
  }
} serverCallbacks;

class ScanCallbacks : public NimBLEScanCallbacks {
  void onResult(const NimBLEAdvertisedDevice* device) override {
    if (!device->isAdvertisingService(NimBLEUUID(ALERT_UUID)) ||
        device->getAddress().toString() == ownAddress) return;
    const uint32_t now = millis();
    const int rssi = device->getRSSI();
    portENTER_CRITICAL(&mux);
    remoteSeenAt = now;
    remoteRssi = rssi;
    remotePending = true;
    portEXIT_CRITICAL(&mux);
  }
} scanCallbacks;
// ============================================================

// ===== 9. ESTADO LOCAL DE loop() =====
bool announcing = false, alarming = false, beepOn = false;
uint32_t announceAt = 0, alarmAt = 0, beepAt = 0;
uint32_t lastSeen = 0, scanRetryAt = 0;
int filteredRssi = -100;
uint8_t clickCount = 0;
uint32_t firstClickAt = 0, rawChangedAt = 0;
bool rawButton = HIGH, stableButton = HIGH;

// ===== FUNCIÓN ENVÍO DE EVENTO =====
// ============================================================
void notifyPhone(const char* text) {
  characteristic->setValue(text);
  if (NimBLEDevice::getServer()->getConnectedCount() > 0) {
    characteristic->notify();
    Serial.printf("Notificación: %s\n", text);
  } else Serial.printf("Sin celular: %s (no enviada)\n", text);
}
// ============================================================

// ===== 11. FUNCIÓN DE CONTROL DEL ANUNCIO DE ALERTA =====
void setAlertAdvertisement(bool enabled) {
  advertising->stop();
  if (enabled) advertising->addServiceUUID(NimBLEUUID(ALERT_UUID));
  else advertising->removeServiceUUID(NimBLEUUID(ALERT_UUID));
  announcing = enabled;
  advertising->start();
}

// ===== FUNCIÓN GESTOS BOTÓN (MÁQUINA DE CLICS) =====
// ============================================================
void onClick(uint32_t now) {
  if (clickCount == 0) {
    clickCount = 1;
    firstClickAt = now;
  } else {
    ++clickCount;
    if (clickCount == 3) {
      clickCount = 0;
      notifyPhone("SOS_TRIPLE");
      announceAt = now;
      setAlertAdvertisement(true);
    }
  }
}
// ============================================================

// ===== FUNCIÓN LECTURA PULSADOR =====
// ============================================================
void handleButton(uint32_t now) {
  const bool reading = digitalRead(BUTTON_PIN);
  if (reading != rawButton) {
    rawButton = reading;
    rawChangedAt = now;
  }
  if (now - rawChangedAt >= DEBOUNCE_MS && rawButton != stableButton) {
    stableButton = rawButton;
    if (stableButton == LOW) onClick(now);
  }
  // Para evitar SOS! seguido de SOS_TRIPLE, el segundo clic debe llegar
  // antes de 700 ms. Una vez recibido, el tercero tiene hasta 3 s desde el primero.
  if (clickCount == 1 && now - firstClickAt >= SINGLE_MS) {
    clickCount = 0;
    notifyPhone("SOS!");
  } else if (clickCount >= 2 && now - firstClickAt >= CLICK_WINDOW_MS) {
    clickCount = 0; // dos clics no generan ningún mensaje
  }
}
// ============================================================

// ===== 14. SETUP =====
void setup() {
  pinMode(BUZZER_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, HIGH);
#if ESP_ARDUINO_VERSION_MAJOR >= 3
  ledcAttach(BUZZER_PIN, BUZZ_FREQ, 8);
#else
  ledcSetup(BUZZER_CHANNEL, BUZZ_FREQ, 8);
  ledcAttachPin(BUZZER_PIN, BUZZER_CHANNEL);
#endif
  setBuzzer(false);
  pinMode(BUTTON_PIN, INPUT_PULLUP);
  Serial.begin(115200);

  NimBLEDevice::init("WAYKI");
  ownAddress = NimBLEDevice::getAddress().toString();
  String suffix(ownAddress.c_str());
  suffix = suffix.substring(12);
  suffix.replace(":", "");
  suffix.toUpperCase();
  String name = "WAYKI_" + suffix;
  NimBLEDevice::setDeviceName(name.c_str());

  auto server = NimBLEDevice::createServer();
  server->setCallbacks(&serverCallbacks);
  auto service = server->createService(SERVICE_UUID);
  characteristic = service->createCharacteristic(CHAR_UUID,
      NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::NOTIFY);
  // NimBLE crea automáticamente el descriptor de suscripción 0x2902.
  characteristic->setValue("OK");
  service->start();

  advertising = NimBLEDevice::getAdvertising();
  advertising->addServiceUUID(SERVICE_UUID);
  NimBLEAdvertisementData response;
  response.setName(name.c_str());
  advertising->setScanResponseData(response);
  advertising->enableScanResponse(true);
  advertising->start();

  scanner = NimBLEDevice::getScan();
  scanner->setScanCallbacks(&scanCallbacks, true);
  scanner->setMaxResults(0);
  scanner->setActiveScan(false);
  scanner->setInterval(100);
  scanner->setWindow(80);
  scanner->start(0);

  Serial.printf("%s listo: botón GPIO14, buzzer GPIO4\n", name.c_str());
}

// ===== 15. LOOP: ORQUESTADOR PRINCIPAL =====
void loop() {
  const uint32_t now = millis();

  handleButton(now);

  bool received, restart;
  uint32_t seen;
  int rssi;
  portENTER_CRITICAL(&mux);
  received = remotePending;
  remotePending = false;
  seen = remoteSeenAt;
  rssi = remoteRssi;
  restart = restartAdvertising;
  restartAdvertising = false;
  portEXIT_CRITICAL(&mux);

  if (restart) advertising->start();

  // ===== DETECCIÓN REMOTA -> ARRANQUE DE ALARMA =====
  // ============================================================
  if (received) {
    lastSeen = seen;
    filteredRssi = (filteredRssi * 7 + rssi * 3) / 10;
    if (!alarming) {
      alarming = true;
      alarmAt = now;
      beepAt = now;
      beepOn = true;
      setBuzzer(true);
      notifyPhone("SOS_ALERT");
    }
  }
  // ============================================================

  if (announcing && now - announceAt >= ALARM_MS) setAlertAdvertisement(false);

  if (alarming) {
    if (now - alarmAt >= ALARM_MS || now - lastSeen > 4000) {
      alarming = false;
      beepOn = false;
      setBuzzer(false);
    } else {
      const uint32_t offMs = filteredRssi >= -60 ? 100 :
                              filteredRssi >= -75 ? 300 : 700;
      if (now - beepAt >= (beepOn ? 100UL : offMs)) {
        beepOn = !beepOn;
        beepAt = now;
        setBuzzer(beepOn);
      }
    }
  }

  if (!scanner->isScanning() && now - scanRetryAt >= 1000) {
    scanRetryAt = now;
    scanner->start(0);
  }
}
