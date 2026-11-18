#!/usr/bin/env bash
# Publica el APK en la sección Releases del repo, igual que el CI
# (.github/workflows/apk.yml), pero desde tu máquina.
#
#   ./scripts/publicar-apk.sh           # crea una release nueva (v1.0.<fecha>)
#   ./scripts/publicar-apk.sh v1.1.0    # publica una release con ese tag
#
# Cada release lleva su propio tag, así la lista de Releases crece como en
# cualquier proyecto publicado. El APK se sube siempre con el mismo nombre, así
# que esta URL sigue sirviendo la última versión:
#   https://github.com/laplace71/gowayki-nest/releases/latest/download/wayki-nest-beta.apk
#
# Requisitos: JDK 17, Android SDK, gh autenticado con permiso de escritura.
# La primera vez crea un keystore estable en ~/.wayki/wayki-beta.jks (o usa
# androidApp/keystore/wayki-beta.jks si existe), para que las betas se instalen
# una encima de otra sin desinstalar.
set -euo pipefail

REPO="${REPO:-laplace71/gowayki-nest}"
TAG="${1:-}"

raiz="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$raiz"

if [[ -z "$TAG" ]]; then
  version="1.0.$(date +%Y%m%d%H%M)"
  tag="v${version}"
else
  tag="$TAG"
  version="${TAG#v}"
fi
asset="wayki-nest-beta.apk"

prerelease=false
case "$version" in *-*) prerelease=true ;; esac

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
prev=$(git tag --list 'v*' --sort=-v:refname | grep -vx "$tag" | head -1 || true)
if [ -z "$prev" ]; then
  prev=$(git ls-remote --tags origin 'refs/tags/v*' 2>/dev/null | awk '{print $2}' | sed 's#refs/tags/##' | grep -vx "$tag" | sort -rV | head -1 || true)
fi
if [ -n "$prev" ]; then
  prev=$(git rev-parse -q --verify "$prev^{commit}" 2>/dev/null || echo "$prev")
fi
if [ -n "$prev" ] && [ "$(git rev-list --count "$prev..HEAD" 2>/dev/null || echo 0)" -gt 0 ]; then
  cambios=$(git log --no-merges --pretty='- %s' "$prev..HEAD" | head -40)
  comparar="https://github.com/$REPO/compare/${prev}...$(git rev-parse HEAD)"
else
  cambios="- Sin cambios de código desde la publicación anterior."
  comparar=""
fi

notas="$raiz/build/releases/notas.md"
{
  echo "## Wayki Nest $version"
  echo
  echo "APK firmada y compilada localmente con \`scripts/publicar-apk.sh\`."
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
  echo "3. Si ya tienes una versión instalada, se actualiza encima: no hace falta desinstalar."
  echo
  echo "### Notas"
  echo
  echo "- Versión **$version**. Descarga siempre la última en [releases/latest](https://github.com/$REPO/releases/latest)."
} > "$notas"

# --- Release ----------------------------------------------------------------
if gh release view "$tag" -R "$REPO" >/dev/null 2>&1; then
  gh release upload "$tag" "$salida" -R "$REPO" --clobber
  gh release edit "$tag" -R "$REPO" --title "Wayki Nest $version" --notes-file "$notas"
elif [[ "$prerelease" == true ]]; then
  gh release create "$tag" "$salida" -R "$REPO" \
    --title "Wayki Nest $version" \
    --notes-file "$notas" \
    --prerelease \
    --target main
else
  gh release create "$tag" "$salida" -R "$REPO" \
    --title "Wayki Nest $version" \
    --notes-file "$notas" \
    --latest \
    --target main
fi

echo
echo "Listo: https://github.com/$REPO/releases/tag/$tag"
