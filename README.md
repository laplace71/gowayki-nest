# Wayki Nest

## Descargar el APK (Android)

[**Descargar la última beta**](https://github.com/laplace71/gowayki-nest/releases/download/beta/wayki-nest-beta.apk)
· [todas las releases](https://github.com/laplace71/gowayki-nest/releases)

El APK se compila, firma y publica solo: cada push a `main` reemplaza el asset
`wayki-nest-beta.apk` de la release `beta` (la URL de descarga nunca cambia).
El pipeline está en [`.github/workflows/apk.yml`](.github/workflows/apk.yml) y
también se puede lanzar a mano desde *Actions > APK beta > Run workflow*.
Para publicar sin esperar al CI: `./scripts/publicar-apk.sh`.

Los tags `v*` generan una release propia (`v1.0.0`, `v1.1.0`, …). Los `.apk`
nunca se commitean al repo: GitHub rechaza archivos de más de 100 MB.

Las notas de la release se generan solas: changelog desde la beta anterior
(commits de `main`), instrucciones de instalación y una tabla verificable con
versión, `versionCode`, tamaño, commit, SHA-256 del APK y huella SHA-256 del
certificado.

### Quién puede publicar

Cualquier colaborador con permiso de escritura en el repo, sin configurar nada:

- **Merge a `main`** (o push directo): el CI publica solo. No hace falta que tu
  máquina esté encendida; usa el `GITHUB_TOKEN` del repo.
- **A mano**: *Actions > APK beta > Run workflow*, eligiendo la rama. Ojo:
  publica el código de esa rama tal cual, aunque no esté mergeado.
- **Desde local**: `./scripts/publicar-apk.sh` (requiere `gh` autenticado, el
  SDK de Android y los secretos de firma exportados).

Todas las publicaciones que van a `beta` comparten el mismo grupo de
`concurrency`: si entran dos, se cancela la anterior y solo publica la última.
El `versionCode` sale de **una sola secuencia monotónica** compartida por CI y
publicación local ([`scripts/release-state.sh`](scripts/release-state.sh)): se
siembra con el tiempo Unix y nunca baja del último código publicado + 1, así
ninguna beta puede degenerar en un downgrade que Android rechace.

### Firma y configuración

Para que Android instale una beta encima de la anterior, todas deben estar
firmadas con la **misma clave privada**. Esa clave no está en el repo: un
keystore cuyo privado puede leer cualquiera con acceso de lectura permitiría
falsificar una "beta oficial" y Android no podría distinguirla (CWE-321).
Se configura una vez como secretos del repo y se exportan con los mismos
nombres desde local:

1. `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`,
   `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` — clave de firma.
2. `BACKEND_URL` — URL del backend. Lo normal es `https://`: el inicio de
   sesión envía DNI y PIN y el build rechaza `http://` (CWE-319).
3. `BACKEND_ALLOW_HTTP` — variable **no secreta** del repo (no un secret).
   Hoy el backend de la beta es la IP `31.220.72.2` sin TLS, así que está
   puesta a `1` como opt-in temporal y asumido: con ese valor se permite
   `http://` y las notas avisan de que el tráfico va sin cifrar. Poniéndola a
   `0` se vuelve a exigir HTTPS; cuando el backend tenga TLS, configura
   `BACKEND_URL` con la URL `https://` y borra la variable.

Si falta la clave de firma, el CI **falla** en lugar de caer en un keystore
público o temporal. Antes de subir el APK se comprueba que su `versionCode` es
mayor que el de la beta publicada y que su huella de certificado coincide; si
no, la publicación se aborta.

> Si instalaste una beta antigua firmada con el keystore público que estaba en
> el repo, desinstálala una vez e instala la nueva: el cambio a una clave
> privada es irreversible para esa instalación. Es el coste único de que una
> beta falsificada ya no pueda suplantar a la oficial.

## Iconos (SVG → XML)

1. Coloca el `.svg` en `shared/src/commonMain/iconsSvg/`
2. Corre `./gradlew :shared:convertSvgIcons` (también corre solo al build)
3. Usa el recurso: `painterResource(Res.drawable.nombre_archivo)`

Salida: `shared/src/commonMain/composeResources/drawable/*.xml`  
Requisito: Node.js (`npx svg2vectordrawable`)
