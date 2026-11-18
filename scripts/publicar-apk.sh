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

# --- Notas de la release ----------------------------------------------------
short_sha=$(git rev-parse --short HEAD)
sha256=$(sha256sum "$salida" | cut -d' ' -f1)
bytes=$(stat -c%s "$salida")
cert=$("$build_tools/apksigner" verify --print-certs "$salida" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')
tamano=$(awk -v b="$bytes" 'BEGIN { printf "%.1f MB", b / 1048576 }')

prev=$(git rev-parse -q --verify "refs/tags/$TAG^{commit}" 2>/dev/null || true)
if [ -z "$prev" ]; then
  prev=$(git ls-remote --tags origin "refs/tags/$TAG" 2>/dev/null | cut -f1 || true)
fi
if [ -n "$prev" ] && [ "$(git rev-list --count "$prev..HEAD" 2>/dev/null || echo 0)" -gt 0 ]; then
  cambios=$(git log --no-merges --pretty='- %s (`%h`)' "$prev..HEAD" | head -40)
  comparar="https://github.com/$REPO/compare/${prev}...$(git rev-parse HEAD)"
else
  cambios="- Sin cambios de código desde la publicación anterior."
  comparar=""
fi

notas="$raiz/build/releases/notas.md"
{
  echo "## Wayki Nest $version"
  echo
  echo "APK beta firmada y compilada localmente con \`scripts/publicar-apk.sh\`."
  echo
  echo "### Cambios"
  echo
  echo "$cambios"
  if [ -n "$comparar" ]; then
    echo
    echo "[Ver comparación completa]($comparar)"
  fi
  echo
  echo "### Instalación"
  echo
  echo "1. Descarga \`$asset\`."
  echo "2. Permite *instalar apps de origen desconocido* cuando Android lo pida."
  echo "3. Si ya tienes una beta instalada, se actualiza encima: no hace falta desinstalar."
  echo
  echo "| Dato | Valor |"
  echo "| --- | --- |"
  echo "| Versión | $version (versionCode $version_code) |"
  echo "| Tamaño | $tamano |"
  echo "| Requisitos | Android 7.0 (API 24) o superior |"
  echo "| Commit | [\`$short_sha\`](https://github.com/$REPO/commit/$(git rev-parse HEAD)) |"
  echo "| SHA-256 | \`$sha256\` |"
  echo "| Certificado | \`$cert\` |"
  echo
  echo "### Verificar la descarga"
  echo
  echo '```bash'
  echo "sha256sum $asset"
  echo "# debe imprimir el SHA-256 de la tabla de arriba"
  echo '```'
  echo
  echo "### Notas"
  echo
  echo "- Build de pruebas (canal *beta*): no usar en producción ni publicar en Play Store."
  echo "- Este asset se reemplaza en cada publicación de la misma release."
} > "$notas"

# --- Release ----------------------------------------------------------------
if gh release view "$TAG" -R "$REPO" >/dev/null 2>&1; then
  gh release upload "$TAG" "$salida" -R "$REPO" --clobber
  gh release edit "$TAG" -R "$REPO" --title "Wayki Nest $version" --notes-file "$notas"
else
  gh release create "$TAG" "$salida" -R "$REPO" \
    --title "Wayki Nest $version" \
    --notes-file "$notas" \
    --prerelease \
    --target main
fi

echo
echo "Listo: https://github.com/$REPO/releases/tag/$TAG"
