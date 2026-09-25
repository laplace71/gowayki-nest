// Comentarios y nombres en español (Wayki, hormiga, rutas...).
@file:Suppress("SpellCheckingInspection")
package com.gowayki.nesh.app.common.ui.shared

// Capa compartida de Wayki Nest: tokens, tipografías y componentes que usan
// todas las vistas (welcome, login, registro). Un solo archivo.

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gowayki.nesh.app.common.modules.auth.AuthCopy
import gowaykinesh.shared.generated.resources.Res
import gowaykinesh.shared.generated.resources.hormiga
import gowaykinesh.shared.generated.resources.inter
import gowaykinesh.shared.generated.resources.unbounded
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.imageResource
import kotlin.time.Duration.Companion.seconds

// ---------- Colores (mismos tokens que la web) ----------
@Suppress("unused") // reservado para branding futuro
val PageBg = Color(0xFFEDEAF3)
val Surface = Color(0xFFF4F2F0)
val Ink = Color(0xFF2D2B4E)
val Muted = Color(0xFF7B7FA3)
val Lavender = Color(0xFFC4BDDE)
val BlobLight = Color(0xFFDEDAE8)
val BlobMid = Color(0xFFB7B8C9)
val BlobPink = Color(0xFFB8A8C8)
val Road = Color(0xFF969CB4)

// ---------- Tipografías (fuentes variables en composeResources/font) ----------
// Font(FontResource) es @Composable en Compose MP: se exponen como getters composables.
val Unbounded
    @Composable get() = FontFamily(
        Font(Res.font.unbounded, FontWeight.Bold),
        Font(Res.font.unbounded, FontWeight.ExtraBold),
    )
val Inter
    @Composable get() = FontFamily(
        Font(Res.font.inter, FontWeight.Normal),
        Font(Res.font.inter, FontWeight.Medium),
    )

// ---------- Círculo flotante animado (flota en elipse, rebote suave) ----------
@Composable
fun FloatingBlob(
    baseX: Dp,
    baseY: Dp,
    sizeX: Dp,
    sizeY: Dp = sizeX,
    color: Color,
    durationMillis: Int = 4000,
    delayMillis: Int = 0,
    travelX: Dp = 16.dp,
    travelY: Dp = 22.dp,
) {
    val transition = rememberInfiniteTransition(label = "blob")
    val fx by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, delayMillis = delayMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fx",
    )
    val fy by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis + 700, delayMillis = delayMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fy",
    )
    Box(
        Modifier.size(sizeX, sizeY)
            .offset(x = baseX + travelX * fx, y = baseY + travelY * fy)
            .background(color, CircleShape),
    )
}

// ---------- Fondo compartido: pantalla llena + blobs sutiles ----------
// withRoads = true agrega las rutas punteadas con hormigas (login/registro).
@Composable
fun WaykiBackground(
    withRoads: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Surface)
            .clipToBounds(),
    ) {
        FloatingBlob((-90).dp, (-70).dp, 260.dp, 240.dp, BlobLight.copy(alpha = 0.7f), durationMillis = 4200)
        FloatingBlob(280.dp, (-50).dp, 180.dp, 170.dp, Muted.copy(alpha = 0.5f), durationMillis = 5200, delayMillis = 400)
        FloatingBlob(150.dp, (-30).dp, 80.dp, 80.dp, BlobMid.copy(alpha = 0.85f), durationMillis = 3400, delayMillis = 200)
        FloatingBlob((-60).dp, 320.dp, 140.dp, 130.dp, BlobMid.copy(alpha = 0.4f), durationMillis = 5600, delayMillis = 300)
        FloatingBlob(320.dp, 480.dp, 90.dp, 90.dp, BlobLight.copy(alpha = 0.8f), durationMillis = 4400, delayMillis = 1000)
        if (withRoads) {
            WaykiRoads()
        }
        content()
    }
}

// ---------- Títulos ----------
@Composable
fun WaykiTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier,
        color = Ink, fontFamily = Unbounded,
        fontWeight = FontWeight.Bold, fontSize = 38.sp,
        lineHeight = 46.sp,
    )
}

@Composable
fun WaykiSubtitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier,
        color = Muted, fontFamily = Inter,
        fontWeight = FontWeight.Normal, fontSize = 16.sp,
    )
}

// ---------- Botón primario (pastilla lavanda, igual que ENTRAR) ----------
@Composable
private fun ArrowIcon() {
    val density = LocalDensity.current.density
    Canvas(Modifier.size(32.dp, 32.dp)) {
        scale(density, density, pivot = Offset.Zero) {
            drawCircle(Muted, 16f, Offset(16f, 16f))
            drawLine(Surface, Offset(9f, 16f), Offset(23f, 16f), 2.5f, StrokeCap.Round)
            drawLine(Surface, Offset(23f, 16f), Offset(16f, 9f), 2.5f, StrokeCap.Round)
            drawLine(Surface, Offset(23f, 16f), Offset(16f, 23f), 2.5f, StrokeCap.Round)
        }
    }
}

@Composable
fun WaykiPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showArrow: Boolean = true,
    fullWidth: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier.width(210.dp))
            .height(if (fullWidth) 72.dp else 56.dp)
            .shadow(
                24.dp, RoundedCornerShape(50.dp),
                ambientColor = Muted.copy(alpha = 0.27f), spotColor = Muted.copy(alpha = 0.27f),
            ),
        shape = RoundedCornerShape(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Lavender, contentColor = Ink),
    ) {
        Text(
            text, fontFamily = Unbounded,
            fontWeight = FontWeight.Bold, fontSize = if (fullWidth) 18.sp else 15.sp,
        )
        if (showArrow) {
            Box(Modifier.padding(start = 12.dp)) { ArrowIcon() }
        }
    }
}

// ---------- Botón Google circular (estilo diseño) ----------
// El login real con Google (Credential Manager + SHA-1 + Client ID) se
// conecta en onClick cuando tengas Firebase/Google Cloud configurado.
@Composable
fun WaykiGoogleCircle(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.size(52.dp)
            .border(1.5.dp, Lavender.copy(alpha = 0.6f), CircleShape)
            .background(Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "G", fontFamily = Unbounded,
            fontWeight = FontWeight.ExtraBold, fontSize = 22.sp,
            color = Ink,
        )
    }
}

// ---------- Flecha atrás ----------
@Composable
fun WaykiBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.size(48.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(22.dp)) {
            val s = size.width
            drawLine(Ink, Offset(s * 0.65f, s * 0.2f), Offset(s * 0.3f, s * 0.5f), 2.5f, StrokeCap.Round)
            drawLine(Ink, Offset(s * 0.3f, s * 0.5f), Offset(s * 0.65f, s * 0.8f), 2.5f, StrokeCap.Round)
        }
    }
}

// ---------- Divisor "— o —" ----------
@Composable
fun WaykiOrDivider(
    modifier: Modifier = Modifier,
    text: String = "o",
) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(Muted.copy(alpha = 0.4f)))
        Text(
            "  $text  ", fontFamily = Inter,
            fontWeight = FontWeight.Normal, fontSize = 13.sp,
            color = Muted,
        )
        Box(Modifier.weight(1f).height(1.dp).background(Muted.copy(alpha = 0.4f)))
    }
}

// ---------- Texto inferior "¿No tienes cuenta? Regístrate" ----------
@Composable
fun WaykiSwitchAuth(
    prefix: String,
    link: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            prefix, fontFamily = Inter,
            fontWeight = FontWeight.Normal, fontSize = 13.sp,
            color = Muted,
        )
        TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 4.dp)) {
            Text(
                link, fontFamily = Inter,
                fontWeight = FontWeight.Medium, fontSize = 13.sp,
                color = Ink,
            )
        }
    }
}

// ---------- Enlace (estilo "Soporte técnico") ----------
@Composable
fun WaykiLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Text(
            text, color = Muted, fontFamily = Inter,
            fontWeight = FontWeight.Medium, fontSize = 14.sp,
            textDecoration = TextDecoration.Underline,
        )
    }
}

// ---------- Campo de texto (pastilla blanca estilo diseño) ----------
@Composable
fun WaykiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(label, fontFamily = Inter, color = Muted, fontSize = 14.sp) },
        singleLine = true,
        textStyle = TextStyle(
            fontFamily = Inter,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            color = Ink,
        ),
        shape = RoundedCornerShape(50.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Ink,
            unfocusedBorderColor = Lavender.copy(alpha = 0.5f),
            disabledBorderColor = Color.Transparent,
            errorBorderColor = Color.Transparent,
            focusedLabelColor = Ink,
            unfocusedLabelColor = Muted,
            cursorColor = Ink,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            errorContainerColor = Color.White,
            focusedTextColor = Ink,
            unfocusedTextColor = Ink,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword) {
            {
                TextButton(onClick = { visible = !visible }) {
                    Text(
                        if (visible) AuthCopy.passwordHide else AuthCopy.passwordShow,
                        fontFamily = Inter, color = Muted, fontSize = 13.sp,
                    )
                }
            }
        } else {
            null
        },
    )
}

// ---------- PIN de casillas (crear contraseña numérica) ----------
// Un solo campo invisible guarda el código + casillas solo visuales:
// así el borrado siempre funciona (en casillas separadas el foco salta
// y el backspace en casilla vacía no hace nada).
// El último dígito se muestra 1 s y luego se vuelve ● (estilo iOS).
@Composable
fun WaykiPinInput(
    modifier: Modifier = Modifier,
    length: Int = 4,
    boxSize: Dp = 62.dp,
    // false = siempre ● (login); true = último dígito visible 1 s (crear PIN).
    revealLast: Boolean = true,
    onComplete: (String) -> Unit = {},
) {
    var code by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var fieldFocused by remember { mutableStateOf(false) }
    var revealedIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(revealedIndex, code) {
        if (revealedIndex != -1) {
            kotlinx.coroutines.delay(1.seconds)
            revealedIndex = -1
        }
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Campo real: invisible, guarda todo el código.
    BasicTextField(
        value = code,
        onValueChange = { v ->
            val clean = v.filter(Char::isDigit).take(length)
            if (clean.length > code.length) {
                if (revealLast) revealedIndex = clean.length - 1
            } else if (clean.length < code.length) {
                revealedIndex = -1
            }
            code = clean
            if (clean.length == length) {
                onComplete(clean)
            }
        },
        modifier = Modifier.size(1.dp)
            .alpha(0f)
            .focusRequester(focusRequester)
            .onFocusChanged { fieldFocused = it.isFocused },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        decorationBox = {},
    )

    // Casillas visuales: tocan el campo invisible para abrir el teclado.
    val activeBox = if (fieldFocused) minOf(code.length, length - 1) else -1
    Row(
        modifier.fillMaxWidth()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = { focusRequester.requestFocus() },
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(length) { i ->
            val digit = code.getOrNull(i)?.toString().orEmpty()
            val shown = when {
                digit.isEmpty() -> ""
                revealLast && i == revealedIndex -> digit
                else -> "●"
            }
            Box(
                Modifier.size(boxSize)
                    .border(
                        1.5.dp,
                        if (activeBox == i) Ink else Lavender.copy(alpha = 0.6f),
                        RoundedCornerShape(20.dp),
                    )
                    .background(Color.White, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    shown,
                    fontFamily = Unbounded,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Ink,
                )
            }
        }
    }
}

// ---------- Carga + error (los maneja el ViewModel) ----------
@Composable
fun WaykiLoadingOverlay(visible: Boolean) {
    if (visible) {
        Box(
            Modifier.fillMaxSize().background(Ink.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = Surface)
        }
    }
}

@Composable
fun WaykiErrorText(
    error: String?,
    modifier: Modifier = Modifier,
) {
    if (error != null) {
        Text(
            error, modifier.fillMaxWidth(),
            color = Color(0xFFBA1A1A), fontFamily = Inter,
            fontWeight = FontWeight.Medium, fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
    }
}

// ---------- Rutas punteadas animadas (fondo login/registro/welcome) ----------
// 1. Las rayas "marchan" (fase del dash animada).
// 2. Una hormiga recorre cada ruta (medida con PathMeasure),
//    12 dp por encima de la línea y girada según la tangente.
// Para cambiar velocidades: los `tween(ms)` de phase*/trip* (más ms = más lento).
// Para mover una ruta: `canvasSizeX/Y` + `offsetX/Y` de cada RoadCanvas.
// Para cambiar la forma: `moveTo/cubicTo` de cada road*.
@Composable
fun WaykiRoads() {
    val density = LocalDensity.current.density
    val flow = rememberInfiniteTransition(label = "roads")
    // 20f = 10 + 10 (suma del dash) para un bucle sin salto.
    val phaseA by flow.animateFloat(
        initialValue = 0f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "phaseA",
    )
    val phaseB by flow.animateFloat(
        initialValue = 20f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "phaseB",
    )
    val tripA by flow.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart),
        label = "tripA",
    )
    val tripB by flow.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart),
        label = "tripB",
    )
    val phaseC by flow.animateFloat(
        initialValue = 0f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Restart),
        label = "phaseC",
    )
    val tripC by flow.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "tripC",
    )

    val roadA = remember {
        Path().apply {
            moveTo(54.14f, 625.11f)
            cubicTo(-12.01f, 305.0f, 524.43f, 417.46f, 468.54f, 38.23f)
        }
    }
    val roadB = remember {
        Path().apply {
            moveTo(77.40f, 453.2f)
            cubicTo(11.24f, 133.09f, 348.75f, 119.08f, 518.75f, 167.58f)
        }
    }
    // Tercera ruta: parte del lateral izquierdo medio y sube en onda (serpenteo)
    // hasta la esquina superior derecha, al lado de las otras dos.
    val roadC = remember {
        Path().apply {
            moveTo(-10f, 470f)
            cubicTo(120f, 420f, 80f, 300f, 200f, 250f)
            cubicTo(320f, 200f, 300f, 100f, 442f, 0f)
        }
    }
    val measureA = remember { PathMeasure().apply { setPath(roadA, false) } }
    val measureB = remember { PathMeasure().apply { setPath(roadB, false) } }
    val measureC = remember { PathMeasure().apply { setPath(roadC, false) } }
    // Hormiga (1792x878): se dibuja de ~38 dp de ancho, girada según la tangente.
    val ant = imageResource(Res.drawable.hormiga)
    val antW = 38f
    val antH = antW * ant.height / ant.width

    RoadCanvas(
        canvasSizeX = 534.dp, canvasSizeY = 671.dp, offsetX = (-56).dp, offsetY = 247.dp,
        road = roadA, measure = measureA,
        trip = tripA, phase = phaseA, density = density, ant = ant, antW = antW, antH = antH,
    )
    RoadCanvas(
        canvasSizeX = 519.dp, canvasSizeY = 567.dp, offsetX = (-50).dp, offsetY = 546.dp,
        road = roadB, measure = measureB,
        trip = tripB, phase = phaseB, density = density, ant = ant, antW = antW, antH = antH,
    )
    RoadCanvas(
        canvasSizeX = 500.dp, canvasSizeY = 560.dp, offsetX = (-30).dp, offsetY = 90.dp,
        road = roadC, measure = measureC,
        trip = tripC, phase = phaseC, density = density, ant = ant, antW = antW, antH = antH,
    )
}

// Una ruta: línea punteada que marcha + hormiga encima.
@Composable
private fun RoadCanvas(
    canvasSizeX: Dp,
    canvasSizeY: Dp,
    offsetX: Dp,
    offsetY: Dp,
    road: Path,
    measure: PathMeasure,
    trip: Float,
    phase: Float,
    density: Float,
    ant: ImageBitmap,
    antW: Float,
    antH: Float,
) {
    Canvas(Modifier.size(canvasSizeX, canvasSizeY).offset(offsetX, offsetY)) {
        scale(density, density, pivot = Offset.Zero) {
            drawPath(
                road,
                Road, style = Stroke(5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), phase)),
            )
            val pos = measure.getPosition(measure.length * trip)
            val tan = measure.getTangent(measure.length * trip)
            // Encima de la línea: desplazamiento perpendicular (normal hacia arriba).
            val len = kotlin.math.sqrt(tan.x * tan.x + tan.y * tan.y).coerceAtLeast(0.001f)
            var nx = -tan.y / len
            var ny = tan.x / len
            if (ny > 0) { nx = -nx; ny = -ny }
            val center = Offset(pos.x + nx * 12f, pos.y + ny * 12f)
            val angle = kotlin.math.atan2(tan.y, tan.x) * 180f / kotlin.math.PI.toFloat()
            rotate(angle, center) {
                drawImage(
                    image = ant,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(ant.width, ant.height),
                    dstOffset = IntOffset(
                        (center.x - antW / 2).roundToInt(),
                        (center.y - antH / 2).roundToInt(),
                    ),
                    dstSize = IntSize(antW.roundToInt(), antH.roundToInt()),
                    colorFilter = ColorFilter.tint(Lavender),
                )
            }
        }
    }
}