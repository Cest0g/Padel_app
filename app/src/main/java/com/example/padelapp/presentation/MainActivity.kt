package com.example.padelapp.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import kotlinx.coroutines.delay

/**
 * MainActivity voor de Wear OS Padel-app met exact gewenste UI-ontwerp, zwart thema en Undo-functionaliteit.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FancyPadelTheme {
                PadelScoreApp()
            }
        }
    }
}

@Composable
fun FancyPadelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme(
            primary = Color(0xFF64B5F6),
            secondary = Color(0xFF81C784),
            background = Color.Black,
            surfaceContainer = Color(0xFF1B5E20),
            onSurface = Color.White
        ),
        content = content
    )
}

enum class Screen { START, GAME, FINISH }
enum class ThirdSetType { NORMAL, SUPER_TIEBREAK }
data class SetResult(val teamA: Int, val teamB: Int, val isSuperTiebreak: Boolean = false)

data class MatchStateSnapshot(
    val pointsA: Int,
    val pointsB: Int,
    val gamesA: Int,
    val gamesB: Int,
    val setsA: Int,
    val setsB: Int,
    val currentScreen: Screen,
    val isTimerRunning: Boolean,
    val setResults: List<SetResult>
)

@Composable
fun PadelScoreApp() {
    var currentScreen by remember { mutableStateOf(Screen.START) }
    var thirdSetType by remember { mutableStateOf(ThirdSetType.NORMAL) }

    // Match State
    var pointsA by remember { mutableIntStateOf(0) }
    var pointsB by remember { mutableIntStateOf(0) }
    var gamesA by remember { mutableIntStateOf(0) }
    var gamesB by remember { mutableIntStateOf(0) }
    var setsA by remember { mutableIntStateOf(0) }
    var setsB by remember { mutableIntStateOf(0) }
    val setResults = remember { mutableStateListOf<SetResult>() }
    val history = remember { mutableStateListOf<MatchStateSnapshot>() }

    // Stopwatch logica
    var elapsedTime by remember { mutableLongStateOf(0L) }
    var isTimerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            val startTime = System.currentTimeMillis() - elapsedTime
            while (isTimerRunning) {
                elapsedTime = System.currentTimeMillis() - startTime
                delay(1000)
            }
        }
    }

    val isThirdSet = (setsA + setsB == 2)
    val isSuperTiebreakActive = isThirdSet && thirdSetType == ThirdSetType.SUPER_TIEBREAK
    val isNormalTiebreakActive = !isSuperTiebreakActive && gamesA == 6 && gamesB == 6

    fun saveToHistory() {
        history.add(
            MatchStateSnapshot(
                pointsA = pointsA,
                pointsB = pointsB,
                gamesA = gamesA,
                gamesB = gamesB,
                setsA = setsA,
                setsB = setsB,
                currentScreen = currentScreen,
                isTimerRunning = isTimerRunning,
                setResults = setResults.toList()
            )
        )
    }

    fun performUndo() {
        if (history.isNotEmpty()) {
            val lastState = history.removeAt(history.lastIndex)
            pointsA = lastState.pointsA
            pointsB = lastState.pointsB
            gamesA = lastState.gamesA
            gamesB = lastState.gamesB
            setsA = lastState.setsA
            setsB = lastState.setsB
            currentScreen = lastState.currentScreen
            isTimerRunning = lastState.isTimerRunning
            setResults.clear()
            setResults.addAll(lastState.setResults)
        }
    }

    fun finishSet(winnerA: Boolean) {
        if (winnerA) {
            setsA++
            setResults.add(SetResult(gamesA, gamesB, isSuperTiebreakActive || isNormalTiebreakActive))
        } else {
            setsB++
            setResults.add(SetResult(gamesA, gamesB, isSuperTiebreakActive || isNormalTiebreakActive))
        }

        gamesA = 0
        gamesB = 0
        pointsA = 0
        pointsB = 0

        if (setsA == 2 || setsB == 2) {
            currentScreen = Screen.FINISH
            isTimerRunning = false
        }
    }

    fun winGame(winnerA: Boolean) {
        pointsA = 0
        pointsB = 0
        if (winnerA) {
            gamesA++
            if (gamesA >= 6 && (gamesA - gamesB) >= 2) finishSet(true)
            else if (gamesA == 7 && gamesB == 6) finishSet(true)
        } else {
            gamesB++
            if (gamesB >= 6 && (gamesB - gamesA) >= 2) finishSet(false)
            else if (gamesB == 7 && gamesA == 6) finishSet(false)
        }
    }

    fun addPoint(winnerA: Boolean) {
        saveToHistory()
        if (isSuperTiebreakActive) {
            if (winnerA) pointsA++ else pointsB++
            if (pointsA >= 10 && (pointsA - pointsB) >= 2) {
                gamesA = pointsA; gamesB = pointsB; finishSet(true)
            } else if (pointsB >= 10 && (pointsB - pointsA) >= 2) {
                gamesA = pointsA; gamesB = pointsB; finishSet(false)
            }
        } else if (isNormalTiebreakActive) {
            if (winnerA) pointsA++ else pointsB++
            if (pointsA >= 7 && (pointsA - pointsB) >= 2) {
                gamesA = 7; gamesB = 6; finishSet(true)
            } else if (pointsB >= 7 && (pointsB - pointsA) >= 2) {
                gamesB = 7; gamesA = 6; finishSet(false)
            }
        } else {
            if (winnerA) {
                if (pointsA == 3 && pointsB < 3) winGame(true)
                else if (pointsA == 3 && pointsB == 3) pointsA = 4
                else if (pointsA == 3 && pointsB == 4) pointsB = 3
                else if (pointsA == 4) winGame(true)
                else pointsA++
            } else {
                if (pointsB == 3 && pointsA < 3) winGame(false)
                else if (pointsB == 3 && pointsA == 3) pointsB = 4
                else if (pointsB == 3 && pointsA == 4) pointsA = 3
                else if (pointsB == 4) winGame(false)
                else pointsB++
            }
        }
    }

    fun resetMatch() {
        pointsA = 0; pointsB = 0; gamesA = 0; gamesB = 0; setsA = 0; setsB = 0
        setResults.clear()
        history.clear()
        elapsedTime = 0L
        isTimerRunning = false
        currentScreen = Screen.START
    }

    AppScaffold {
        when (currentScreen) {
            Screen.START -> {
                StartScreen(onStart = { type ->
                    thirdSetType = type
                    currentScreen = Screen.GAME
                    elapsedTime = 0L
                    isTimerRunning = true
                })
            }
            Screen.GAME -> {
                GameScreen(
                    pointsA = pointsA,
                    pointsB = pointsB,
                    gamesA = gamesA,
                    gamesB = gamesB,
                    setsA = setsA,
                    setsB = setsB,
                    isTieBreak = isNormalTiebreakActive || isSuperTiebreakActive,
                    elapsedTime = elapsedTime,
                    onPointA = { addPoint(true) },
                    onPointB = { addPoint(false) },
                    onUndo = { performUndo() },
                    isUndoAvailable = history.isNotEmpty()
                )
            }
            Screen.FINISH -> {
                FinishScreen(
                    setsA = setsA,
                    setsB = setsB,
                    setResults = setResults,
                    onNewMatch = { resetMatch() }
                )
            }
        }
    }
}

@Composable
fun StartScreen(onStart: (ThirdSetType) -> Unit) {
    var selectedType by remember { mutableStateOf<ThirdSetType?>(null) }

    LaunchedEffect(selectedType) {
        selectedType?.let { type ->
            delay(300)
            onStart(type)
        }
    }

    val columnState = rememberTransformingLazyColumnState()

    ScreenScaffold(scrollState = columnState) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Text(
                    text = "Padel Score",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF81C784),
                    textAlign = TextAlign.Center
                )
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item {
                val isNormalSelected = selectedType == ThirdSetType.NORMAL
                val scale by animateFloatAsState(
                    targetValue = if (isNormalSelected) 0.85f else 1f,
                    animationSpec = tween(durationMillis = 150),
                    label = "normalScale"
                )
                val buttonColor by animateColorAsState(
                    targetValue = if (isNormalSelected) Color(0xFFFFEB3B) else Color(0xFF1E88E5),
                    animationSpec = tween(durationMillis = 150),
                    label = "normalColor"
                )

                Button(
                    onClick = { if (selectedType == null) selectedType = ThirdSetType.NORMAL },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .scale(scale)
                        .drawBehind {
                            if (isNormalSelected) {
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.4f),
                                    size = size,
                                    cornerRadius = CornerRadius(24.dp.toPx()),
                                    style = Stroke(width = 4.dp.toPx())
                                )
                            }
                        }
                ) {
                    Text("Normale 3e set", textAlign = TextAlign.Center)
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item {
                val isSuperSelected = selectedType == ThirdSetType.SUPER_TIEBREAK
                val scale by animateFloatAsState(
                    targetValue = if (isSuperSelected) 0.85f else 1f,
                    animationSpec = tween(durationMillis = 150),
                    label = "superScale"
                )
                val buttonColor by animateColorAsState(
                    targetValue = if (isSuperSelected) Color(0xFFFFEB3B) else Color(0xFF2E7D32),
                    animationSpec = tween(durationMillis = 150),
                    label = "superColor"
                )

                Button(
                    onClick = { if (selectedType == null) selectedType = ThirdSetType.SUPER_TIEBREAK },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .scale(scale)
                        .drawBehind {
                            if (isSuperSelected) {
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.4f),
                                    size = size,
                                    cornerRadius = CornerRadius(24.dp.toPx()),
                                    style = Stroke(width = 4.dp.toPx())
                                )
                            }
                        }
                ) {
                    Text("Super Tie-break", textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun GameScreen(
    pointsA: Int,
    pointsB: Int,
    gamesA: Int,
    gamesB: Int,
    setsA: Int,
    setsB: Int,
    isTieBreak: Boolean,
    elapsedTime: Long,
    onPointA: () -> Unit,
    onPointB: () -> Unit,
    onUndo: () -> Unit,
    isUndoAvailable: Boolean
) {
    val totalSeconds = elapsedTime / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val timeStr = "%02d:%02d".format(minutes, seconds)

    val pointsTextA = if (isTieBreak) pointsA.toString() else when (pointsA) {
        0 -> "0"
        1 -> "15"
        2 -> "30"
        3 -> "40"
        4 -> "AV"
        else -> "0"
    }

    val pointsTextB = if (isTieBreak) pointsB.toString() else when (pointsB) {
        0 -> "0"
        1 -> "15"
        2 -> "30"
        3 -> "40"
        4 -> "AV"
        else -> "0"
    }

    val interactionSourceA = remember { MutableInteractionSource() }
    val isPressedA by interactionSourceA.collectIsPressedAsState()
    val scaleA by animateFloatAsState(
        targetValue = if (isPressedA) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scaleA"
    )

    val interactionSourceB = remember { MutableInteractionSource() }
    val isPressedB by interactionSourceB.collectIsPressedAsState()
    val scaleB by animateFloatAsState(
        targetValue = if (isPressedB) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scaleB"
    )

    ScreenScaffold { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(contentPadding)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "SETS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$setsA",
                        color = Color(0xFF2196F3),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = " - ",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "$setsB",
                        color = Color(0xFFF44336),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "($gamesA-$gamesB)",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp
                    )
                }
            }

            // Midden (Scorekaarten)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Linker knop (Blauw)
                Button(
                    onClick = onPointA,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .scale(scaleA),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    interactionSource = interactionSourceA
                ) {
                    Text(
                        text = pointsTextA,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        ),
                        color = Color.White
                    )
                }

                // Rechter knop (Rood)
                Button(
                    onClick = onPointB,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .scale(scaleB),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                    interactionSource = interactionSourceB
                ) {
                    Text(
                        text = pointsTextB,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        ),
                        color = Color.White
                    )
                }
            }

            // Onderkant (Undo knop)
            Button(
                onClick = onUndo,
                enabled = isUndoAvailable,
                modifier = Modifier
                    .size(36.dp)
                    .padding(bottom = 2.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF303030),
                    disabledContainerColor = Color(0xFF1C1C1C)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    modifier = Modifier.size(16.dp),
                    tint = if (isUndoAvailable) Color.White else Color.Gray
                )
            }
        }
    }
}

@Composable
fun FinishScreen(setsA: Int, setsB: Int, setResults: List<SetResult>, onNewMatch: () -> Unit) {
    val columnState = rememberTransformingLazyColumnState()
    val winner = if (setsA > setsB) "Ik" else "Tegenstander"

    ScreenScaffold(scrollState = columnState) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "Match Resultaat",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
            item {
                Text(
                    text = "Winnaar: $winner",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFFFEB3B),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
            items(setResults.size) { index ->
                val res = setResults[index]
                Text(
                    text = "Set ${index + 1}: ${res.teamA} - ${res.teamB}${if (res.isSuperTiebreak) " (ST)" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
            item {
                Button(
                    onClick = onNewMatch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Nieuwe Match", textAlign = TextAlign.Center)
                }
            }
        }
    }
}
