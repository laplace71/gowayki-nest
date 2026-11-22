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
#   scripts/release-state.sh lock-acquire           # reserva exclusiva de publicación
#   scripts/release-state.sh lock-check             # comprueba que el lock sigue siendo nuestro
#   scripts/release-state.sh lock-release           # libera la reserva propia
#   WAYKI_FORCE_UNLOCK=1 scripts/release-state.sh lock-recover
#                                                    # recuperación manual (solo con el publicador detenido)
#
# Requiere `gh` autenticado (GH_TOKEN en CI). El lock usa la API de GitHub,
# así que funciona igual en clones HTTPS, forks y worktrees enlazados.
set -euo pipefail

REPO="${REPO:-laplace71/gowayki-nest}"
BETA_TAG="${BETA_TAG:-beta}"
LOCK_REF_NAME="beta-publish-lock"
LOCK_REF="refs/tags/$LOCK_REF_NAME"
LOCK_FILE="${LOCK_FILE:-}"

cmd="${1:?Uso: release-state.sh next-code|cert|assert CODE CERT [TAG]|lock-acquire|lock-check|lock-release|lock-recover}"
shift

body=""
read_body() {
  local tag="$1"
  body=$(gh release view "$tag" -R "$REPO" --json body -q .body 2>/dev/null || true)
}

# Lee SOLO la fila de la tabla de notas:
#   | Versión | 1.0.0-beta.N (versionCode 1795271746) |
# Antes se escaneaba todo el texto y un commit que mencionara "versionCode"
# (por ejemplo el límite 2100000000) se tomaba como versión publicada.
code_from_body() {
  printf '%s\n' "$1" \
    | sed -n 's/^| *Versión *| .*versionCode *\([0-9][0-9]*\).*|$/\1/p' \
    | head -1
}

published_code() {
  code_from_body "$body"
}

published_cert() {
  printf '%s\n' "$body" | sed -n 's/^| Certificado | `\([^`]*\)` |.*/\1/p' | head -1
}

# Todos los cuerpos de release (beta y tags v*) en una sola llamada paginada.
all_release_bodies() {
  gh api --paginate "repos/$REPO/releases?per_page=100" --jq '.[].body'
}

# Mayor versionCode publicado en CUALQUIER release. Así un APK con tag v*
# también avanza la secuencia y la beta nunca queda por debajo de él.
max_published_code() {
  local out max
  if ! out=$(all_release_bodies); then
    echo "ERROR: no se pudo consultar las releases de $REPO" >&2
    return 1
  fi
  max=$(printf '%s\n' "$out" \
    | sed -n 's/^| *Versión *| .*versionCode *\([0-9][0-9]*\).*|$/\1/p' \
    | sort -n | tail -1)
  printf '%s\n' "${max:-0}"
}

# --- Lock de publicación -----------------------------------------------------
# Un tag anotado en el repo CANÓNICO ($REPO) es la reserva exclusiva: crear el
# ref falla si ya existe, así que la carrera la gana uno solo. Se usa la API
# (gh) en vez de git push por cuatro razones:
#   * no expone commits sin push: el tag apunta al HEAD público de $REPO,
#     nunca al HEAD local (CWE-200);
#   * locked el repo de destino aunque el checkout sea un fork;
#   * no pide credenciales de git además de GH_TOKEN;
#   * funciona en worktrees enlazados (donde .git es un archivo).
# NO hay expiración por tiempo: un build que sigue corriendo conserva el lock.
# Si el publicador murió, se recupera de forma explícita con lock-recover.
lock_file_path() {
  if [[ -n "$LOCK_FILE" ]]; then
    printf '%s\n' "$LOCK_FILE"
  elif git rev-parse --git-dir >/dev/null 2>&1; then
    # En un worktree enlazado .git es un archivo: --git-path da la ruta real.
    git rev-parse --git-path wayki-publish.lock
  else
    printf '%s\n' "${TMPDIR:-/tmp}/wayki-${UID:-0}-publish.lock"
  fi
}

# Código HTTP de una respuesta de `gh api -i` (vacío si no se pudo leer).
http_status() {
  printf '%s\n' "$1" | sed -n 's#^HTTP/[0-9.]* \([0-9][0-9]*\).*#\1#p' | head -1
}

# Mensaje del lock remoto.
#   rc 0 -> imprime "token timestamp"
#   rc 2 -> el ref no existe (ausencia CONFIRMADA)
#   rc 1 -> fallo de lectura (red, 5xx, permisos…), no sabemos qué pasó
# Distinguir 404 de otros fallos es lo que evita borrar el registro local
# cuando GitHub siguió teniendo el lock.
lock_remote_msg() {
  local out status ref_sha msg

  out=$(gh api -i "repos/$REPO/git/ref/tags/$LOCK_REF_NAME" 2>/dev/null) || true
  status=$(http_status "$out")
  case "$status" in
    200) ;;
    404) return 2 ;;
    *) return 1 ;;
  esac

  ref_sha=$(printf '%s\n' "$out" \
    | grep -oE '"sha": *"[0-9a-f]{40}"' | head -1 \
    | grep -oE '[0-9a-f]{40}') || true
  [[ -n "$ref_sha" ]] || return 1

  out=$(gh api -i "repos/$REPO/git/tags/$ref_sha" 2>/dev/null) || true
  status=$(http_status "$out")
  case "$status" in
    200) ;;
    404) return 2 ;;
    *) return 1 ;;
  esac
  msg=$(printf '%s\n' "$out" \
    | grep -oE '"message": *"[^"]*"' | head -1 \
    | sed 's/.*"message": *"//; s/"$//')
  [[ -n "$msg" ]] || return 1
  printf '%s\n' "$msg"
}

# Borra el ref del lock. Éxito solo si la API confirma el borrado o si el ref
# ya no existe: un 503 no debe interpretarse como "desbloqueado".
lock_delete_or_absent() {
  local out status
  if gh api -X DELETE "repos/$REPO/git/refs/tags/$LOCK_REF_NAME" >/dev/null 2>&1; then
    return 0
  fi
  out=$(gh api -i "repos/$REPO/git/ref/tags/$LOCK_REF_NAME" 2>/dev/null) || true
  status=$(http_status "$out")
  [[ "$status" == "404" ]]
}

lock_acquire() {
  local token now target tag_sha path rc msg
  token="$$-$(date +%s)-${RANDOM}"
  now=$(date +%s)
  path=$(lock_file_path)
  mkdir -p "$(dirname "$path")"

  # Commit YA público: si apuntáramos al HEAD local, el tag publicaría
  # commits que el chofer aún no había subido.
  target=$(gh api "repos/$REPO/commits/HEAD" --jq '.sha') || {
    echo "ERROR: no se pudo resolver el commit público de $REPO." >&2
    return 1
  }
  [[ "$target" =~ ^[0-9a-f]{40}$ ]] || {
    echo "ERROR: SHA inesperado para el HEAD público de $REPO: $target" >&2
    return 1
  }

  tag_sha=$(gh api -X POST "repos/$REPO/git/tags" \
    -f tag="$LOCK_REF_NAME" -f message="$token $now" \
    -f object="$target" -f type=commit --jq '.sha') || {
    echo "ERROR: no se pudo crear el objeto del lock en $REPO (¿permisos contents: write?)." >&2
    return 1
  }
  [[ "$tag_sha" =~ ^[0-9a-f]{40}$ ]] || {
    echo "ERROR: SHA inesperado al crear el tag del lock: $tag_sha" >&2
    return 1
  }

  if gh api -X POST "repos/$REPO/git/refs" -f ref="$LOCK_REF" -f sha="$tag_sha" >/dev/null 2>&1; then
    printf '%s %s\n' "$token" "$now" > "$path"
    echo "Publicación bloqueada en $REPO ($LOCK_REF)."
    return 0
  fi

  rc=0
  msg=$(lock_remote_msg) || rc=$?
  case "$rc" in
    0)
      echo "ERROR: otra publicación tiene el lock de $REPO ($LOCK_REF)." >&2
      echo "El lock NO se libera por antigüedad: un build de 40 min sigue siendo dueño." >&2
      echo "Cuando confirmes que el publicador original terminó, ejecuta:" >&2
      echo "  WAYKI_FORCE_UNLOCK=1 scripts/release-state.sh lock-recover" >&2
      ;;
    2)
      echo "ERROR: no se pudo crear el lock $LOCK_REF en $REPO." >&2
      echo "Revisa GH_TOKEN (contents: write) y la conexión con GitHub." >&2
      ;;
    *)
      echo "ERROR: no se pudo confirmar si el lock $LOCK_REF ya existe en $REPO." >&2
      echo "No se asume que está libre: reintenta cuando vuelva la conectividad." >&2
      ;;
  esac
  return 1
}

lock_check() {
  local token _ msg path rc
  path=$(lock_file_path)
  if [[ ! -f "$path" ]]; then
    echo "ERROR: no hay lock local; esta publicación no es dueña del lock." >&2
    return 1
  fi
  read -r token _ < "$path"
  rc=0
  msg=$(lock_remote_msg) || rc=$?
  case "$rc" in
    0)
      if [[ -z "$token" || "$msg" != "$token "* ]]; then
        echo "ERROR: el lock de $REPO ya no nos pertenece. Se aborta la subida." >&2
        return 1
      fi
      ;;
    2)
      echo "ERROR: el lock de $REPO ya no existe. No se puede publicar sin ser dueño." >&2
      return 1
      ;;
    *)
      echo "ERROR: no se pudo leer el lock remoto de $REPO antes de publicar." >&2
      return 1
      ;;
  esac
}

lock_release() {
  local token _ msg path rc
  path=$(lock_file_path)
  [[ -f "$path" ]] || return 0
  read -r token _ < "$path"

  rc=0
  msg=$(lock_remote_msg) || rc=$?
  case "$rc" in
    0)
      if [[ -z "$token" || "$msg" != "$token "* ]]; then
        # Ausencia de dueño confirmada: nadie lo quitará por nosotros.
        echo "AVISO: el lock remoto de $REPO ya no nos pertenece; no se toca." >&2
        rm -f "$path"
        return 0
      fi
      if ! lock_delete_or_absent; then
        echo "ERROR: no se pudo eliminar el lock remoto de $REPO." >&2
        echo "Se conserva el registro local: sin resolverlo, las siguientes" >&2
        echo "publicaciones no podrán pasar el lock." >&2
        return 1
      fi
      rm -f "$path"
      ;;
    2)
      # Confirmado ausente: nada que liberar.
      rm -f "$path"
      ;;
    *)
      # Lectura fallida: no sabemos si el ref sigue vivo. Conservar el registro
      # y fallar, en vez de mentir y bloquear todas las publicaciones.
      echo "ERROR: no se pudo leer el lock remoto de $REPO; se conserva el registro local." >&2
      return 1
      ;;
  esac
}

lock_recover() {
  local path
  if [[ "${WAYKI_FORCE_UNLOCK:-}" != "1" ]]; then
    echo "ERROR: recuperación explícita. Verifica que el publicador original terminó y vuelve a ejecutar con:" >&2
    echo "  WAYKI_FORCE_UNLOCK=1 scripts/release-state.sh lock-recover" >&2
    return 1
  fi
  if ! lock_delete_or_absent; then
    echo "ERROR: no se pudo eliminar el lock de $REPO (revisa GH_TOKEN/red)." >&2
    echo "No se reporta como desbloqueado; reintenta." >&2
    return 1
  fi
  path=$(lock_file_path)
  rm -f "$path"
  echo "Lock de $REPO eliminado manualmente."
}

case "$cmd" in
  next-code)
    prev=$(max_published_code) || exit 1
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
    tag="${1:-$BETA_TAG}"
    read_body "$tag"
    printf '%s\n' "$(published_cert)"
    ;;

  assert)
    code="${1:?falta versionCode}"
    cert="${2:?falta huella del certificado}"
    tag="${3:-$BETA_TAG}"
    [[ "$code" =~ ^[0-9]+$ ]] || { echo "ERROR: versionCode no numérico: $code" >&2; exit 1; }

    # Se compara contra el mayor de TODAS las releases: un tag v* también
    # cuenta, no solo la beta rodante.
    prev=$(max_published_code) || exit 1
    if [[ "$prev" =~ ^[0-9]+$ ]] && (( code <= prev )); then
      echo "ERROR: versionCode $code no es mayor que el mayor publicado ($prev). Se aborta para no degradar la app." >&2
      exit 1
    fi

    # Firma: la del tag destino si ya existe; si es nuevo, la de la beta.
    read_body "$tag"
    old_cert=$(published_cert)
    if [[ -z "$old_cert" && "$tag" != "$BETA_TAG" ]]; then
      read_body "$BETA_TAG"
      old_cert=$(published_cert)
    fi
    if [[ -n "$old_cert" && "$cert" != "$old_cert" ]]; then
      echo "ERROR: la firma del APK ($cert) difiere de la publicada ($old_cert)." >&2
      echo "Android rechazaría la actualización. Usa la misma clave de firma." >&2
      exit 1
    fi
    ;;

  lock-acquire)
    lock_acquire
    ;;

  lock-check)
    lock_check
    ;;

  lock-release)
    lock_release
    ;;

  lock-recover)
    lock_recover
    ;;

  *)
    echo "Uso: release-state.sh next-code|cert|assert CODE CERT [TAG]|lock-acquire|lock-check|lock-release|lock-recover" >&2
    exit 2
    ;;
esac
