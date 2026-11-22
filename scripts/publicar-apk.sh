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

# Las notas se generan desde HEAD. Si el build incluyera cambios sin commitear,
# el APK publicado no coincidiría con el commit que anuncian las notas.
cambios_pendientes=$(git status --porcelain)
if [ -n "$cambios_pendientes" ]; then
  echo "ERROR: hay cambios sin commitear; el APK no coincidiría con las notas de la release." >&2
  echo "$cambios_pendientes" >&2
  echo "Commitea (o guarda con git stash) antes de publicar." >&2
  exit 1
fi

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

# Es prerelease si el tag/version lleva sufijo (beta, rc…). Un tag suelto
# v1.0.0 es estable; v1.0.0-beta3 es prerelease aunque TAG != "beta".
case "$version" in
  *-*) es_prerelease=true ;;
  *)   es_prerelease=false ;;
esac

# Unica secuencia monotónica, compartida con el CI (nunca reloj/100 ni run number).
version_code="$(./scripts/release-state.sh next-code "$BETA_TAG")"

# Solo un escritor a la vez: CI y publicación local comparten el mismo lock.
# Se libera en el trap, incluso si la compilación o la subida falla.
./scripts/release-state.sh lock-acquire
# Trap inmediato: cualquier salida posterior (incluida la validación de
# secretos) debe liberar el lock, no solo los errores de compilación.
trap './scripts/release-state.sh lock-release' EXIT

# --- Keystore privado (el mismo que usa el CI) ------------------------------
if [[ -z "${RELEASE_KEYSTORE_BASE64:-}" || -z "${RELEASE_KEYSTORE_PASSWORD:-}" || \
      -z "${RELEASE_KEY_ALIAS:-}" || -z "${RELEASE_KEY_PASSWORD:-}" ]]; then
  echo "ERROR: faltan RELEASE_KEYSTORE_BASE64, RELEASE_KEYSTORE_PASSWORD, RELEASE_KEY_ALIAS o RELEASE_KEY_PASSWORD." >&2
  echo "Exporta los mismos secretos que usa .github/workflows/apk.yml; no se genera una clave local." >&2
  exit 1
fi
keystore="$(mktemp "${TMPDIR:-/tmp}/wayki-release.XXXXXX.jks")"
trap 'rm -f "$keystore"; ./scripts/release-state.sh lock-release' EXIT
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
build_sha=$(git rev-parse HEAD)
# macOS (BSD) no trae sha256sum ni `stat -c`: usar herramientas portables.
if command -v sha256sum >/dev/null 2>&1; then
  sha256=$(sha256sum "$salida" | cut -d' ' -f1)
elif command -v shasum >/dev/null 2>&1; then
  sha256=$(shasum -a 256 "$salida" | cut -d' ' -f1)
else
  echo "ERROR: se necesita sha256sum (Linux) o shasum (macOS)." >&2
  exit 1
fi
bytes=$(wc -c < "$salida" | tr -d '[:space:]')
cert=$("$build_tools/apksigner" verify --print-certs "$salida" | \
  sed -n 's/^Signer #1 certificate SHA-256 digest: //p')
[[ -n "$cert" ]] || { echo "ERROR: no se pudo leer la huella SHA-256 del certificado" >&2; exit 1; }
tamano=$(awk -v b="$bytes" 'BEGIN { printf "%.1f MB", b / 1048576 }')

# No publicar un APK más antiguo (ni con otra firma) que CUALQUIER release
# publicada, incluida la release de destino cuando es un tag v*.
./scripts/release-state.sh assert "$version_code" "$cert" "$TAG"

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
# Cada escritura re-verifica la propiedad del lock pegada al comando: el lock
# se tomó antes del build y pudo cambiar de manos (recuperación manual) mientras
# se compilaba. Sin un token, `gh release view` no cuenta como escritura.
pub() {
  ./scripts/release-state.sh lock-check
  "$@"
}

es_draft=$(gh release view "$TAG" -R "$REPO" --json isDraft -q .isDraft 2>/dev/null || echo false)

if gh release view "$TAG" -R "$REPO" >/dev/null 2>&1; then
  pub gh release upload "$TAG" "$salida" -R "$REPO" --clobber
  # Publicar de verdad (una draft no expone el asset) y fijar prerelease.
  # Un draft puede reservar el tag sin crear el ref de Git: al publicarlo,
  # GitHub crea el tag desde --target. Si no lo enviamos, el tag nace en el
  # commit guardado en la draft (quizá distinto del APK que acabamos de
  # compilar) y el source archive no coincide con las notas.
  edit_opts=(--draft=false --prerelease="$es_prerelease")
  if [ "$es_draft" = "true" ]; then
    # Sin --verify-tag: gh 2.23 (Debian 12) no lo acepta en `release edit`
    # y el valor por defecto ya es false en versiones que sí lo tienen.
    edit_opts+=(--target "$build_sha")
  fi
  pub gh release edit "$TAG" -R "$REPO" \
    --title "Wayki Nest $version" --notes-file "$notas" \
    "${edit_opts[@]}"
else
  pub gh release create "$TAG" "$salida" -R "$REPO" \
    --title "Wayki Nest $version" \
    --notes-file "$notas" \
    --prerelease="$es_prerelease" \
    --target "$build_sha"
fi
./scripts/release-state.sh lock-check

echo
echo "Listo: https://github.com/$REPO/releases/tag/$TAG"
