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
(commits de `main`), instrucciones de instalación y tabla con versión,
`versionCode`, tamaño, commit, SHA-256 del APK y huella del certificado, para
que cualquiera pueda verificar lo que descarga.

### Firma

Para que Android instale una beta encima de la anterior, todas deben estar
firmadas con el mismo keystore. Se usa, en este orden:

1. Secretos `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`,
   `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` (requiere ser admin del repo).
2. `androidApp/keystore/wayki-beta.jks`, el keystore beta del repo: alias
   `wayki`, contraseña `wayki-beta` (se puede sobreescribir con las variables
   `BETA_KEYSTORE_PASSWORD`, `BETA_KEY_ALIAS` y `BETA_KEY_PASSWORD`).
   Su huella SHA-256 es
   `01:59:18:B9:43:DC:5A:A8:2B:DF:05:81:B5:21:F6:1B:8A:F0:D5:63:C2:26:6E:21:D4:09:29:5E:5A:64:5D:3A`.
   Es público a propósito (builds de prueba): no usar para publicar en Play
   Store; para eso, genera un keystore privado y súbelo como secretos.
3. Un keystore temporal: sirve para probar, pero obliga a desinstalar antes de
   instalar la siguiente beta.

## Iconos (SVG → XML)

1. Coloca el `.svg` en `shared/src/commonMain/iconsSvg/`
2. Corre `./gradlew :shared:convertSvgIcons` (también corre solo al build)
3. Usa el recurso: `painterResource(Res.drawable.nombre_archivo)`

Salida: `shared/src/commonMain/composeResources/drawable/*.xml`  
Requisito: Node.js (`npx svg2vectordrawable`)

