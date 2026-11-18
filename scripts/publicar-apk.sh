#!/usr/bin/env bash
# Publica el APK en la sección Releases del repo, igual que el CI
# (.github/workflows/apk.yml), pero desde tu máquina.
#
#   ./scripts/publicar-apk.sh              # publica/actualiza la release "beta"
#   ./scripts/publicar-apk.sh v1.0.0-beta3 # publica una release con ese tag
#
# Requisitos: JDK 17, Android SDK, gh autenticado con permiso de escritura.
# La primera vez crea un keystore estable en ~/.wayki/wayki-beta.jks (o usa
# androidApp/keystore/wayki-beta.jks si existe), para que las betas se instalen
# una encima de otra sin desinstalar.
set -euo pipefail

REPO="${REPO:-laplace71/gowayki-nest}"
TAG="${1:-beta}"

raiz="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$raiz"

if [[ "$TAG" == "beta" ]]; then
  version="1.0.0-beta.$(date +%Y%m%d%H%M)"
  asset="wayki-nest-beta.apk"
else
  version="${TAG#v}"
  asset="wayki-nest-${version}.apk"
fi
version_code="$(( $(date +%s) / 100 ))"

# --- Keystore estable -------------------------------------------------------
keystore="$HOME/.wayki/wayki-beta.jks"
if [[ -f "androidApp/keystore/wayki-beta.jks" ]]; then
  keystore="androidApp/keystore/wayki-beta.jks"
elif [[ ! -f "$keystore" ]]; then
  mkdir -p "$(dirname "$keystore")"
  echo "Creando keystore de firma en $keystore"
  keytool -genkeypair -keystore "$keystore" \
    -storepass "${BETA_KEYSTORE_PASSWORD:-wayki-beta}" \
    -keypass "${BETA_KEY_PASSWORD:-wayki-beta}" \
    -alias "${BETA_KEY_ALIAS:-wayki}" -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Wayki Nest, OU=Wayki, O=Wayki, L=Arequipa, ST=Arequipa, C=PE"
fi

# --- Build ------------------------------------------------------------------
echo "Compilando $version (versionCode $version_code)"
./gradlew :androidApp:assembleRelease --no-daemon \
  -Pwayki.versionName="$version" -Pwayki.versionCode="$version_code"

build_tools="$(find "${ANDROID_HOME:-$HOME/Android/Sdk}/build-tools" -maxdepth 1 -mindepth 1 -type d | sort -V | tail -1)"
origen="$(find androidApp/build/outputs/apk/release -maxdepth 1 -name '*.apk' -print -quit)"
salida="$raiz/build/releases/$asset"
mkdir -p "$(dirname "$salida")"

"$build_tools/zipalign" -f -p 4 "$origen" "$raiz/build/releases/alineado.apk"
"$build_tools/apksigner" sign \
  --ks "$keystore" \
  --ks-pass "pass:${BETA_KEYSTORE_PASSWORD:-wayki-beta}" \
  --key-pass "pass:${BETA_KEY_PASSWORD:-wayki-beta}" \
  --ks-key-alias "${BETA_KEY_ALIAS:-wayki}" \
  --out "$salida" "$raiz/build/releases/alineado.apk"
rm -f "$raiz/build/releases/alineado.apk"

# --- Release ----------------------------------------------------------------
notas="APK de release de Wayki Nest, versión $version.

Commit: $(git rev-parse --short HEAD)
Compilado localmente con \`scripts/publicar-apk.sh\`."

if gh release view "$TAG" -R "$REPO" >/dev/null 2>&1; then
  gh release upload "$TAG" "$salida" -R "$REPO" --clobber
else
  gh release create "$TAG" "$salida" -R "$REPO" \
    --title "Wayki Nest $version" \
    --notes "$notas" \
    --prerelease \
    --target main
fi

echo
echo "Listo: https://github.com/$REPO/releases/tag/$TAG"
