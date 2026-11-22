package com.dpjha.slidingnumbersplus.ui

import android.app.Activity
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import com.dpjha.slidingnumbersplus.R
import com.dpjha.slidingnumbersplus.data.GameProgressRepository
import com.dpjha.slidingnumbersplus.game.City
import com.dpjha.slidingnumbersplus.game.GameProgress
import com.dpjha.slidingnumbersplus.game.PuzzleEngine
import com.dpjha.slidingnumbersplus.services.PlayGamesClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

private enum class AppScreen { HOME, GAME, LEVELS, SETTINGS }

private data class GameSession(
    val id: Int,
    val level: Int,
    val size: Int,
    val moveLimit: Int,
    val movesLeft: Int,
    val board: List<Int>,
    val result: GameResult? = null,
)

private sealed interface GameResult {
    data class Won(val stars: Int) : GameResult
    data object Lost : GameResult
}

@Composable
fun SlidingNumbersApp(activity: Activity) {
    val context = activity.applicationContext
    val repository = remember(context) { GameProgressRepository(context) }
    val playGames = remember(activity) { PlayGamesClient(activity) }
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    var progress by remember { mutableStateOf(GameProgress()) }
    var loaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    var screen by remember { mutableStateOf(AppScreen.HOME) }
    var session by remember { mutableStateOf<GameSession?>(null) }
    var sessionCounter by remember { mutableIntStateOf(0) }
    var resetConfirmation by remember { mutableStateOf(false) }

    fun persist(value: GameProgress) {
        val normalized = value.normalized()
        progress = normalized
        scope.launch {
            try {
                repository.save(normalized)
            } catch (exception: IOException) {
                snackbarHost.showSnackbar("Progress could not be saved. Check device storage.")
            }
        }
    }

    fun startLevel(level: Int, resume: Boolean) {
        val size = GameProgress.gridSizeForLevel(level)
        val canResume = resume &&
            progress.level == level &&
            progress.movesLeft >= 0 &&
            PuzzleEngine.isSolvable(progress.sequence, size)
        val engine = if (canResume) {
            PuzzleEngine(size, progress.sequence)
        } else {
            PuzzleEngine(size).apply {
                val increment = GameProgress.incrementForLevel(level)
                shuffle(size * (1 shl increment) * 20)
            }
        }
        val moveLimit = GameProgress.moveLimitForLevel(level)
        val movesLeft = if (canResume) progress.movesLeft else moveLimit
        val nextProgress = progress.copy(
            level = level,
            maxLevel = maxOf(progress.maxLevel, level),
            movesLeft = movesLeft,
            sequence = engine.snapshot(),
        )
        persist(nextProgress)
        sessionCounter++
        session = GameSession(
            id = sessionCounter,
            level = level,
            size = size,
            moveLimit = moveLimit,
            movesLeft = movesLeft,
            board = engine.snapshot(),
            result = if (movesLeft == 0) GameResult.Lost else null,
        )
        screen = AppScreen.GAME
    }

    fun moveTile(index: Int): Boolean {
        val current = session ?: return false
        if (current.result != null || current.movesLeft <= 0) return false
        val engine = PuzzleEngine(current.size, current.board)
        if (engine.move(index).isEmpty()) return false

        val movesLeft = current.movesLeft - 1
        val board = engine.snapshot()
        if (engine.isSolved()) {
            val stars = GameProgress.starsForWin(movesLeft, current.moveLimit)
            val score = current.level * 1_000L + stars * 100L + movesLeft.coerceAtLeast(0)
            val updated = progress.copy(
                level = current.level + 1,
                maxLevel = maxOf(progress.maxLevel, current.level + 1),
                starCount = progress.starCount + stars,
                movesLeft = -1,
                sequence = emptyList(),
                totalScore = progress.totalScore + score,
                levelsCompleted = progress.levelsCompleted + 1,
                bestStars = maxOf(progress.bestStars, stars),
            )
            persist(updated)
            playGames.recordWin(updated)
            session = current.copy(board = board, movesLeft = movesLeft, result = GameResult.Won(stars))
        } else if (movesLeft == 0) {
            val updated = progress.copy(movesLeft = 0, sequence = board)
            persist(updated)
            session = current.copy(board = board, movesLeft = 0, result = GameResult.Lost)
        } else {
            val updated = progress.copy(movesLeft = movesLeft, sequence = board)
            persist(updated)
            session = current.copy(board = board, movesLeft = movesLeft)
        }
        return true
    }

    BackHandler(enabled = screen != AppScreen.HOME) {
        screen = AppScreen.HOME
    }

    LaunchedEffect(repository) {
        try {
            progress = repository.load()
        } catch (exception: IOException) {
            loadError = true
        }
        loaded = true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        containerColor = Color.Transparent,
    ) { _ ->
        Box(Modifier.fillMaxSize()) {
            CityBackdrop(
                city = session?.let { GameProgress.cityForLevel(it.level) }
                    ?: GameProgress.cityForLevel(progress.maxLevel),
            )
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                if (!loaded) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else if (loadError) {
                    LoadFailure(
                        onRetry = {
                            scope.launch {
                                try {
                                    progress = repository.load()
                                    loadError = false
                                } catch (exception: IOException) {
                                    snackbarHost.showSnackbar("Saved progress is still unavailable.")
                                }
                            }
                        },
                    )
                } else {
                    when (screen) {
                        AppScreen.HOME -> HomeScreen(
                            progress = progress,
                            canResume = progress.hasSavedBoard,
                            playGamesConfigured = playGames.isConfigured(),
                            onPlay = {
                                val level = if (progress.hasSavedBoard) progress.level else progress.maxLevel
                                startLevel(level, progress.hasSavedBoard)
                            },
                            onLevels = { screen = AppScreen.LEVELS },
                            onSettings = { screen = AppScreen.SETTINGS },
                            onSignIn = playGames::signIn,
                            onLeaderboard = playGames::showLeaderboards,
                            onAchievements = playGames::showAchievements,
                        )

                        AppScreen.LEVELS -> LevelsScreen(
                            progress = progress,
                            onBack = { screen = AppScreen.HOME },
                            onSelectLevel = { level ->
                                val resume = level == progress.level && progress.hasSavedBoard
                                startLevel(level, resume)
                            },
                        )

                        AppScreen.SETTINGS -> SettingsScreen(
                            progress = progress,
                            onBack = { screen = AppScreen.HOME },
                            onHapticsChange = { persist(progress.copy(hapticsOn = it)) },
                            onReducedMotionChange = { persist(progress.copy(reducedMotion = it)) },
                            onReset = { resetConfirmation = true },
                        )

                        AppScreen.GAME -> session?.let { current ->
                            GameScreen(
                                session = current,
                                starCount = progress.starCount,
                                hapticsOn = progress.hapticsOn,
                                reducedMotion = progress.reducedMotion,
                                canBuyMoves = progress.starCount >= 5,
                                onBack = { screen = AppScreen.HOME },
                                onMove = ::moveTile,
                                onRetry = { startLevel(current.level, resume = false) },
                                onBuyMoves = {
                                    if (progress.starCount >= 5) {
                                        val updated = progress.copy(
                                            starCount = progress.starCount - 5,
                                            movesLeft = current.movesLeft + 20,
                                            sequence = current.board,
                                        )
                                        persist(updated)
                                        session = current.copy(
                                            movesLeft = current.movesLeft + 20,
                                            result = null,
                                        )
                                    }
                                },
                                onNextLevel = {
                                    startLevel(progress.level, resume = false)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (resetConfirmation) {
        AlertDialog(
            onDismissRequest = { resetConfirmation = false },
            title = { Text("Reset all progress?") },
            text = { Text("This clears saved boards, levels, stars, and local score.") },
            confirmButton = {
                TextButton(onClick = {
                    resetConfirmation = false
                    session = null
                    persist(GameProgress())
                    screen = AppScreen.HOME
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { resetConfirmation = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun LoadFailure(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Could not load your saved progress.", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Try again") }
    }
}

@Composable
private fun CityBackdrop(city: City) {
    val image = when (city) {
        City.LONDON -> R.drawable.london
        City.PARIS -> R.drawable.paris
        City.NEW_YORK -> R.drawable.newyork
    }
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0xDD111426),
                    0.48f to Color(0xE8111426),
                    1f to Color(0xF5111426),
                ),
            ),
        )
    }
}

@Composable
private fun HomeScreen(
    progress: GameProgress,
    canResume: Boolean,
    playGamesConfigured: Boolean,
    onPlay: () -> Unit,
    onLevels: () -> Unit,
    onSettings: () -> Unit,
    onSignIn: () -> Unit,
    onLeaderboard: () -> Unit,
    onAchievements: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(6.dp))
            Text("${progress.starCount}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
        }
        Spacer(Modifier.height(26.dp))
        Text(
            "SLIDING",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 5.sp,
        )
        Text(
            "NUMBERS",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 4.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "LEVEL ${progress.maxLevel}  ·  ${GameProgress.cityForLevel(progress.maxLevel).title.uppercase()}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(30.dp))
        Button(
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth().height(62.dp),
            shape = RoundedCornerShape(22.dp),
        ) {
            Text(
                if (canResume) "CONTINUE LEVEL ${progress.level}" else "PLAY LEVEL ${progress.maxLevel}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onLevels,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(20.dp),
        ) {
            Text("CITY LEVELS", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onLeaderboard,
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Default.Leaderboard, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("RANKS")
            }
            OutlinedButton(
                onClick = onAchievements,
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("BADGES")
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSignIn) {
            Text(if (playGamesConfigured) "Connect Google Play Games" else "Set up Google Play Games")
        }
        TextButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("SETTINGS")
        }
        Spacer(Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xAA202944)),
            shape = RoundedCornerShape(18.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem("BEST", "${progress.bestStars} ★")
                StatItem("PUZZLES", progress.levelsCompleted.toString())
                StatItem("SCORE", progress.totalScore.toString())
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LevelsScreen(
    progress: GameProgress,
    onBack: () -> Unit,
    onSelectLevel: (Int) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        ScreenHeader("CITY LEVELS", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            City.entries.forEach { city ->
                val levelCount = when (city) {
                    City.NEW_YORK -> {
                        val unlockedGroups = ((progress.maxLevel - city.firstLevel).coerceAtLeast(0) / 5) + 1
                        (unlockedGroups * 5).coerceAtMost(100)
                    }
                    else -> 5
                }
                CityLevelCard(
                    city = city,
                    progress = progress,
                    levelCount = levelCount,
                    onSelectLevel = onSelectLevel,
                )
            }
        }
    }
}

@Composable
private fun CityLevelCard(
    city: City,
    progress: GameProgress,
    levelCount: Int,
    onSelectLevel: (Int) -> Unit,
) {
    val image = when (city) {
        City.LONDON -> R.drawable.london
        City.PARIS -> R.drawable.paris
        City.NEW_YORK -> R.drawable.newyork
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xBB202944)),
        shape = RoundedCornerShape(22.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(108.dp)) {
            Image(
                painter = painterResource(image),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Color(0x99101422)))
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(city.title.uppercase(), fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                Text(
                    if (city == City.NEW_YORK) "5×5 GRID  ·  LEVELS 11+"
                    else "${GameProgress.gridSizeForLevel(city.firstLevel)}×${GameProgress.gridSizeForLevel(city.firstLevel)} GRID  ·  LEVELS ${city.firstLevel}–${city.lastLevel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFD8DEEE),
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat((levelCount + 4) / 5) { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(5) { column ->
                        val offset = row * 5 + column
                        if (offset < levelCount) {
                            val level = city.firstLevel + offset
                            val unlocked = level <= progress.maxLevel
                            val current = level == progress.level
                            Surface(
                                modifier = Modifier.weight(1f).height(56.dp),
                                enabled = unlocked,
                                onClick = { onSelectLevel(level) },
                                shape = RoundedCornerShape(14.dp),
                                color = when {
                                    current -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    unlocked -> Color(0xFF303A56)
                                    else -> Color(0xFF171C2C)
                                },
                                border = if (current) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        if (unlocked) level.toString() else "🔒",
                                        color = if (unlocked) Color.White else Color(0xFF7C859C),
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.weight(1f).height(56.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    progress: GameProgress,
    onBack: () -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onReducedMotionChange: (Boolean) -> Unit,
    onReset: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        ScreenHeader("SETTINGS", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SettingToggle(
                title = "Haptic feedback",
                subtitle = "A light response for each valid slide",
                checked = progress.hapticsOn,
                onCheckedChange = onHapticsChange,
            )
            SettingToggle(
                title = "Reduce animations",
                subtitle = "Use instant tile movement and quieter transitions",
                checked = progress.reducedMotion,
                onCheckedChange = onReducedMotionChange,
            )
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xCC202944))) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("How to play", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Tap or drag any tile in the same row or column as the empty space. The tiles between it and the gap slide together. Put every number in order before your moves run out.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Earn 1–3 stars based on moves remaining. Spend 5 stars to continue with 20 extra moves.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xCC202944))) {
                Row(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Reset progress", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text("Clears levels, stars, and local scores", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onReset) { Text("Reset") }
                }
            }
            Text(
                "Sliding Numbers Plus · Native Android · 2.0.0",
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xCC202944))) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun GameScreen(
    session: GameSession,
    starCount: Int,
    hapticsOn: Boolean,
    reducedMotion: Boolean,
    canBuyMoves: Boolean,
    onBack: () -> Unit,
    onMove: (Int) -> Boolean,
    onRetry: () -> Unit,
    onBuyMoves: () -> Unit,
    onNextLevel: () -> Unit,
) {
    val city = GameProgress.cityForLevel(session.level)
    val haptic = LocalHapticFeedback.current
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.fillMaxWidth().height(58.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to home")
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("LEVEL ${session.level}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(city.title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("MOVES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    session.movesLeft.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = when {
                        session.movesLeft <= 5 -> MaterialTheme.colorScheme.error
                        session.movesLeft <= 10 -> MaterialTheme.colorScheme.secondary
                        else -> Color.White
                    },
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Star, contentDescription = "Stars", tint = MaterialTheme.colorScheme.secondary)
                Text(starCount.toString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            }
        }
        Spacer(Modifier.height(8.dp))
        PuzzleBoard(
            modifier = Modifier.weight(1f),
            size = session.size,
            board = session.board,
            reducedMotion = reducedMotion,
            enabled = session.result == null,
            onTileClick = { index ->
                if (onMove(index) && hapticsOn) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "TAP OR DRAG A TILE ALONG THE OPEN LINE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(vertical = 14.dp),
        )
    }

    when (val result = session.result) {
        is GameResult.Won -> VictoryDialog(
            sessionId = session.id,
            stars = result.stars,
            reducedMotion = reducedMotion,
            onNextLevel = onNextLevel,
        )

        GameResult.Lost -> FailureDialog(
            canBuyMoves = canBuyMoves,
            onBuyMoves = onBuyMoves,
            onRetry = onRetry,
        )

        null -> Unit
    }
}

@Composable
private fun PuzzleBoard(
    modifier: Modifier = Modifier,
    size: Int,
    board: List<Int>,
    reducedMotion: Boolean,
    enabled: Boolean,
    onTileClick: (Int) -> Unit,
) {
    BoxWithConstraints(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val boardSize = minOf(maxWidth, maxHeight, 440.dp)
        val spacing = 7.dp
        val tileSize = (boardSize - spacing * (size - 1)) / size
        Box(Modifier.size(boardSize)) {
            repeat(size * size) { index ->
                Box(
                    Modifier
                        .offset(tileSize * (index % size) + spacing * (index % size), tileSize * (index / size) + spacing * (index / size))
                        .size(tileSize)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x77313A55)),
                )
            }
            board.forEachIndexed { index, value ->
                if (value != PuzzleEngine.EMPTY) {
                    val emptyIndex = board.indexOf(PuzzleEngine.EMPTY)
                    val sameRow = index / size == emptyIndex / size
                    val sameColumn = index % size == emptyIndex % size
                    val dragXSign = if (sameRow) (emptyIndex % size - index % size).compareTo(0) else 0
                    val dragYSign = if (sameColumn) (emptyIndex / size - index / size).compareTo(0) else 0
                    key(value) {
                        val targetX = tileSize * (index % size) + spacing * (index % size)
                        val targetY = tileSize * (index / size) + spacing * (index / size)
                        val animation: FiniteAnimationSpec<Dp> =
                            if (reducedMotion) tween(0) else spring(dampingRatio = 0.76f, stiffness = 520f)
                        val x by animateDpAsState(targetX, animationSpec = animation, label = "tile-x-$value")
                        val y by animateDpAsState(targetY, animationSpec = animation, label = "tile-y-$value")
                        PuzzleTile(
                            value = value,
                            correct = value == index + 1,
                            size = tileSize,
                            x = x,
                            y = y,
                            enabled = enabled,
                            reducedMotion = reducedMotion,
                            dragXSign = dragXSign,
                            dragYSign = dragYSign,
                            onClick = { onTileClick(index) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PuzzleTile(
    value: Int,
    correct: Boolean,
    size: Dp,
    x: Dp,
    y: Dp,
    enabled: Boolean,
    reducedMotion: Boolean,
    dragXSign: Int,
    dragYSign: Int,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(if (reducedMotion) 0 else 90),
        label = "tile-press-$value",
    )
    val density = LocalDensity.current
    val dragThreshold = with(density) { (size * 0.28f).toPx() }
    val background = if (correct) Color(0xFF19766F) else Color(0xFF263657)
    val textSize = (size.value * 0.36f).coerceIn(18f, 42f).sp

    Box(
        Modifier
            .offset(x, y)
            .size(size)
            .scale(scale)
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(background.copy(alpha = 0.98f), background.copy(alpha = 0.76f)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = if (correct) 0.36f else 0.16f), RoundedCornerShape(16.dp))
            .semantics { contentDescription = "Number tile $value${if (correct) ", in place" else ""}" }
            .pointerInput(value, enabled, dragThreshold, dragXSign, dragYSign) {
                var dragDistance = Offset.Zero
                detectDragGestures(
                    onDragStart = { dragDistance = Offset.Zero },
                    onDrag = { change, amount ->
                        change.consume()
                        dragDistance += amount
                    },
                    onDragEnd = {
                        val towardGap = maxOf(dragDistance.x * dragXSign, dragDistance.y * dragYSign)
                        if (enabled && towardGap >= dragThreshold) onClick()
                    },
                )
            }
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            value.toString(),
            fontSize = textSize,
            fontWeight = FontWeight.Black,
            color = Color.White,
        )
    }
}

@Composable
private fun VictoryDialog(
    sessionId: Int,
    stars: Int,
    reducedMotion: Boolean,
    onNextLevel: () -> Unit,
) {
    var visibleStars by remember(sessionId) { mutableIntStateOf(if (reducedMotion) stars else 0) }
    LaunchedEffect(sessionId, reducedMotion) {
        if (!reducedMotion) {
            repeat(stars) { index ->
                delay(240)
                visibleStars = index + 1
            }
        }
    }
    val scale by animateFloatAsState(
        if (visibleStars > 0) 1f else 0.75f,
        animationSpec = if (reducedMotion) tween(0) else spring(dampingRatio = 0.55f),
        label = "victory-star-scale",
    )
    AlertDialog(
        onDismissRequest = {},
        icon = {
            Text(
                "🏆",
                Modifier.graphicsLayer(scaleX = scale, scaleY = scale),
                fontSize = 58.sp,
            )
        },
        title = { Text("PUZZLE SOLVED!", textAlign = TextAlign.Center, fontWeight = FontWeight.Black) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Great run. Your city score is climbing.")
                Spacer(Modifier.height(12.dp))
                Text(
                    buildString {
                        repeat(3) { index -> append(if (index < visibleStars) "★" else "☆") }
                    },
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 36.sp,
                    letterSpacing = 5.sp,
                )
            }
        },
        confirmButton = {
            Button(onClick = onNextLevel) {
                Text("NEXT LEVEL")
            }
        },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    )
}

@Composable
private fun FailureDialog(
    canBuyMoves: Boolean,
    onBuyMoves: () -> Unit,
    onRetry: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        icon = { Text("🧩", fontSize = 52.sp) },
        title = { Text("OUT OF MOVES", fontWeight = FontWeight.Black) },
        text = {
            Text(
                if (canBuyMoves) "Spend 5 stars to keep this board and continue."
                else "Try again and find a more efficient path through the city.",
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (canBuyMoves) {
                    Button(onClick = onBuyMoves) { Text("5 STARS  ·  +20 MOVES") }
                    Spacer(Modifier.height(6.dp))
                }
                TextButton(onClick = onRetry) { Text("RETRY LEVEL") }
            }
        },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    )
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(60.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
            title,
            Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp,
        )
        Spacer(Modifier.size(48.dp))
    }
}
