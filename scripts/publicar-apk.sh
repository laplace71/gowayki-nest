#!/usr/bin/env bash
# Publica el APK en la sección Releases del repo, igual que el CI
# (.github/workflows/apk.yml), pero desde tu máquina.
#
#   ./scripts/publicar-apk.sh              # publica/actualiza la release "beta"
#   ./scripts/publicar-apk.sh v1.0.0-beta3 # publica una release con ese tag
#
# Requisitos: JDK 17, Android SDK, gh autenticado con permiso de escritura y
# los secretos de firma exportados en el entorno (los MISMOS que usa el CI):
#
#   export RELEASE_KEYSTORE_BASE64=...   # keystore privado en base64
#   export RELEASE_KEYSTORE_PASSWORD=...
#   export RELEASE_KEY_ALIAS=...
#   export RELEASE_KEY_PASSWORD=...
#   export BACKEND_URL=http://31.220.72.2:8080/api
#   export BACKEND_ALLOW_HTTP=1          # opt-in beta mientras no haya TLS
#
# Antes este script generaba su propio keystore y usaba un versionCode basado
# en el reloj, distinto del del CI: una beta local podía bloquear las betas
# posteriores del CI por downgrade o por cambio de certificado. Ahora ambos
# caminos comparten scripts/release-state.sh (código y firma) y el mismo
# keystore privado.
set -euo pipefail

REPO="${REPO:-laplace71/gowayki-nest}"
TAG="${1:-beta}"
BETA_TAG="beta"

raiz="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$raiz"

if [[ -z "${BACKEND_URL:-}" ]]; then
  echo "ERROR: exporta BACKEND_URL con la URL del backend antes de publicar." >&2
  exit 1
fi
insecure_backend=no
case "$BACKEND_URL" in
  https://*) ;;
  http://*)
    if [[ "${BACKEND_ALLOW_HTTP:-}" != "1" && "${BACKEND_ALLOW_HTTP:-}" != "true" ]]; then
      echo "ERROR: BACKEND_URL usa http:// (DNI y PIN sin cifrar). Usa https:// o exporta BACKEND_ALLOW_HTTP=1 para asumir el riesgo." >&2
      exit 1
    fi
    insecure_backend=yes
    echo "AVISO: BACKEND_URL usa http://; DNI y PIN viajan sin cifrar (riesgo asumido, CWE-319 abierto)." >&2
    ;;
  *) echo "ERROR: BACKEND_URL debe empezar por http:// o https://" >&2; exit 1 ;;
esac

if [[ "$TAG" == "beta" ]]; then
  version="1.0.0-beta.$(date +%Y%m%d%H%M)"
  asset="wayki-nest-beta.apk"
else
  version="${TAG#v}"
  asset="wayki-nest-${version}.apk"
fi

# Unica secuencia monotónica, compartida con el CI (nunca reloj/100 ni run number).
version_code="$(./scripts/release-state.sh next-code "$BETA_TAG")"

# --- Keystore privado (el mismo que usa el CI) ------------------------------
if [[ -z "${RELEASE_KEYSTORE_BASE64:-}" || -z "${RELEASE_KEYSTORE_PASSWORD:-}" || \
      -z "${RELEASE_KEY_ALIAS:-}" || -z "${RELEASE_KEY_PASSWORD:-}" ]]; then
  echo "ERROR: faltan RELEASE_KEYSTORE_BASE64, RELEASE_KEYSTORE_PASSWORD, RELEASE_KEY_ALIAS o RELEASE_KEY_PASSWORD." >&2
  echo "Exporta los mismos secretos que usa .github/workflows/apk.yml; no se genera una clave local." >&2
  exit 1
fi
keystore="$(mktemp "${TMPDIR:-/tmp}/wayki-release.XXXXXX.jks")"
trap 'rm -f "$keystore"' EXIT
printf '%s' "$RELEASE_KEYSTORE_BASE64" | base64 -d > "$keystore"

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
  --ks-pass "pass:$RELEASE_KEYSTORE_PASSWORD" \
  --key-pass "pass:$RELEASE_KEY_PASSWORD" \
  --ks-key-alias "$RELEASE_KEY_ALIAS" \
  --out "$salida" "$raiz/build/releases/alineado.apk"
rm -f "$raiz/build/releases/alineado.apk"
"$build_tools/apksigner" verify "$salida"

# --- Datos verificables y control de publicación ----------------------------
short_sha=$(git rev-parse --short HEAD)
sha256=$(sha256sum "$salida" | cut -d' ' -f1)
bytes=$(stat -c%s "$salida")
cert=$("$build_tools/apksigner" verify --print-certs "$salida" | \
  sed -n 's/^Signer #1 certificate SHA-256 digest: //p')
[[ -n "$cert" ]] || { echo "ERROR: no se pudo leer la huella SHA-256 del certificado" >&2; exit 1; }
tamano=$(awk -v b="$bytes" 'BEGIN { printf "%.1f MB", b / 1048576 }')

# No publicar un APK más antiguo ni con otra firma que el beta vigente.
./scripts/release-state.sh assert "$version_code" "$cert" "$BETA_TAG"

# --- Notas de la release ----------------------------------------------------
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
  if [ "$insecure_backend" = "yes" ]; then
    echo "- ⚠️ Esta beta usa \`http://\`: DNI y PIN viajan sin cifrar (CWE-319, riesgo asumido mientras el backend no tenga TLS)."
    echo
  fi
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
