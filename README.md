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
(commits de `main`) e instrucciones de instalación.

### Quién puede publicar

Cualquier colaborador con permiso de escritura en el repo, sin configurar nada:

- **Merge a `main`** (o push directo): el CI publica solo. No hace falta que tu
  máquina esté encendida ni tener secretos configurados: usa el `GITHUB_TOKEN`
  del repo y el keystore beta commiteado.
- **A mano**: *Actions > APK beta > Run workflow*, eligiendo la rama. Ojo:
  publica el código de esa rama tal cual, aunque no esté mergeado.
- **Desde local**: `./scripts/publicar-apk.sh` (requiere `gh` autenticado con
  permiso de escritura y el SDK de Android).

Si entran dos merges seguidos, el run anterior se cancela y solo se publica el
último commit. El `versionCode` sale de `1000 + número de run`, así que siempre
sube y cada beta se instala encima de la anterior.

### Firma

Para que Android instale una beta encima de la anterior, todas deben estar
firmadas con el mismo keystore. Se usa, en este orden:

1. Secretos `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`,
   `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` (requiere ser admin del repo).
2. `androidApp/keystore/wayki-beta.jks`, el keystore beta del repo: alias
   `wayki`, contraseña `wayki-beta` (se puede sobreescribir con las variables
   `BETA_KEYSTORE_PASSWORD`, `BETA_KEY_ALIAS` y `BETA_KEY_PASSWORD`).
   Es público a propósito (builds de prueba): no usar para publicar en Play
   Store; para eso, genera un keystore privado y súbelo como secretos.
   Si algún día cambia ese archivo, el certificado cambia y todos tendrán que
   desinstalar una vez antes de instalar la beta nueva.
3. Un keystore temporal: sirve para probar, pero obliga a desinstalar antes de
   instalar la siguiente beta.

## Iconos (SVG → XML)

1. Coloca el `.svg` en `shared/src/commonMain/iconsSvg/`
2. Corre `./gradlew :shared:convertSvgIcons` (también corre solo al build)
3. Usa el recurso: `painterResource(Res.drawable.nombre_archivo)`

Salida: `shared/src/commonMain/composeResources/drawable/*.xml`  
Requisito: Node.js (`npx svg2vectordrawable`)

