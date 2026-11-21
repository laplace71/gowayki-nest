#!/usr/bin/env bash
# Estado de la release rodante "beta", leído de sus propias notas.
#
# CI y las publicaciones locales comparten AQUÍ la secuencia de versionCode y
# la identidad de firma. Antes cada camino usaba un esquema distinto (run
# number vs. reloj; keystore público vs. propio) y una publicación podía
# degradar a la siguiente. Las notas de la release son la fuente de verdad:
# el generador de notas escribe la fila "versionCode N" y la huella del
# certificado, y este script las lee antes de publicar.
#
#   scripts/release-state.sh next-code [TAG]        # siguiente versionCode
#   scripts/release-state.sh cert [TAG]             # huella publicada (o vacía)
#   scripts/release-state.sh assert CODE CERT [TAG] # valida monotonicidad + firma
#
# Requiere `gh` autenticado (GH_TOKEN en CI).
set -euo pipefail

REPO="${REPO:-laplace71/gowayki-nest}"
cmd="${1:?Uso: release-state.sh next-code|cert|assert CODE CERT [TAG]}"
shift

body=""
read_body() {
  local tag="$1"
  body=$(gh release view "$tag" -R "$REPO" --json body -q .body 2>/dev/null || true)
}

published_code() {
  printf '%s\n' "$body" | sed -n 's/.*versionCode \([0-9][0-9]*\).*/\1/p' | head -1
}

published_cert() {
  printf '%s\n' "$body" | sed -n 's/^| Certificado | `\([^`]*\)` |.*/\1/p' | head -1
}

case "$cmd" in
  next-code)
    tag="${1:-beta}"
    read_body "$tag"
    prev=$(published_code)
    [[ "$prev" =~ ^[0-9]+$ ]] || prev=0

    # Suelo: epoch en segundos. Ya está por encima de cualquier código
    # distribuido con los esquemas antiguos (run number ~1e3, reloj/100 ~1e7).
    seed=$(date +%s)
    if (( seed > 2000000000 )); then
      echo "ERROR: el suelo basado en tiempo supera el versionCode máximo de Android" >&2
      exit 1
    fi
    if (( prev >= seed )); then
      code=$((prev + 1))
    else
      code=$seed
    fi
    if (( code > 2100000000 )); then
      echo "ERROR: el versionCode calculado supera el máximo de Android (2100000000)" >&2
      exit 1
    fi
    printf '%s\n' "$code"
    ;;

  cert)
    tag="${1:-beta}"
    read_body "$tag"
    printf '%s\n' "$(published_cert)"
    ;;

  assert)
    code="${1:?falta versionCode}"
    cert="${2:?falta huella del certificado}"
    tag="${3:-beta}"
    [[ "$code" =~ ^[0-9]+$ ]] || { echo "ERROR: versionCode no numérico: $code" >&2; exit 1; }
    read_body "$tag"
    prev=$(published_code)
    if [[ "$prev" =~ ^[0-9]+$ ]] && (( code <= prev )); then
      echo "ERROR: versionCode $code no es mayor que el publicado ($prev). Se aborta para no degradar la beta." >&2
      exit 1
    fi
    old_cert=$(published_cert)
    if [[ -n "$old_cert" && "$cert" != "$old_cert" ]]; then
      echo "ERROR: la firma del APK ($cert) difiere de la del beta publicada ($old_cert)." >&2
      echo "Android rechazaría la actualización. Usa la misma clave de firma." >&2
      exit 1
    fi
    ;;

  *)
    echo "Uso: release-state.sh next-code|cert|assert CODE CERT [TAG]" >&2
    exit 2
    ;;
esac
