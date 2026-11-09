package eu.bfss.puzzlebubbles.presentation.games.whatsthat

import androidx.compose.ui.tooling.preview.Preview
import eu.bfss.puzzlebubbles.ui.theme.PuzzleBubblesTheme
import androidx.compose.ui.platform.LocalInspectionMode
import eu.bfss.puzzlebubbles.domain.WhatsThatSessionFactory
import eu.bfss.puzzlebubbles.data.LocalGameRepository
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.key
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import eu.bfss.puzzlebubbles.R
import eu.bfss.puzzlebubbles.data.AudioPlayer
import eu.bfss.puzzlebubbles.domain.WhatsThatChoice
import eu.bfss.puzzlebubbles.domain.WhatsThatPhase
import eu.bfss.puzzlebubbles.presentation.components.AssetImage
import eu.bfss.puzzlebubbles.presentation.components.BubbleButton
import eu.bfss.puzzlebubbles.ui.theme.GameTitleFontFamily
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

private val GameTitleColor = Color(0xFF166CAF)

@Composable
fun WhatsThatGameScreen(
    title: String,
    uiState: WhatsThatUiState,
    audioPlayer: AudioPlayer,
    onAnswer: (String) -> Unit,
    onReplay: () -> Unit,
    onFeedbackFinished: () -> Unit,
    onBack: () -> Unit,
    onGameComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inspection = LocalInspectionMode.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(audioPlayer, lifecycle, uiState.audioRequestId) {
        if (!inspection && uiState.phase != WhatsThatPhase.GAME_COMPLETE) {
            // Navigation keeps incoming destinations STARTED during its animation.
            // Start the prompt only once this destination is fully on screen.
            lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
            audioPlayer.play(uiState.round.audioAssetId)
        }
    }
    DisposableEffect(Unit) { onDispose(audioPlayer::stop) }

    Box(
        modifier = modifier.background(
            Brush.linearGradient(listOf(Color(0xFFF0FCFF), Color(0xFFF2EEFF), Color(0xFFFFF8F0))),
        ),
    ) {
        GameBubbleBackdrop(Modifier.fillMaxSize())
        if (uiState.phase == WhatsThatPhase.GAME_COMPLETE) {
            GameCompleteScreen(audioPlayer = audioPlayer, onFinished = onGameComplete)
        } else {
            val roundDescription = stringResource(
                R.string.round_progress,
                uiState.roundNumber,
                uiState.roundCount,
            )
            Column(
                Modifier.fillMaxSize().semantics { contentDescription = roundDescription },
            ) {
                GameTopBar(title = title, onBack = {
                    audioPlayer.stop()
                    onBack()
                })
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds(),
                    contentAlignment = Alignment.Center,
                ) {
                    val choiceSize = minOf((maxWidth - 44.dp) / 2, maxHeight * .72f, 280.dp)
                    val density = LocalDensity.current
                    val centerDistance = with(density) { ((choiceSize + 12.dp) / 2).toPx() }
                    val maxSuccessScale = minOf(1.3f, (maxWidth / choiceSize), (maxHeight / choiceSize))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        uiState.round.choices.forEachIndexed { index, choice ->
                            key(uiState.round.id, choice.id) {
                            ChoiceBubble(
                                choice = choice,
                                roundId = uiState.round.id,
                                bubbleSize = choiceSize,
                                centerTranslation = if (index == 0) centerDistance else -centerDistance,
                                maxSuccessScale = maxSuccessScale,
                                hidden = uiState.phase == WhatsThatPhase.CORRECT_FEEDBACK &&
                                    uiState.feedbackChoiceId != choice.id,
                                enabled = uiState.phase == WhatsThatPhase.WAITING_FOR_ANSWER &&
                                    (inspection || lifecycleState.isAtLeast(Lifecycle.State.RESUMED)),
                                isWrong = uiState.phase == WhatsThatPhase.WRONG_FEEDBACK &&
                                    uiState.feedbackChoiceId == choice.id,
                                isCorrect = uiState.phase == WhatsThatPhase.CORRECT_FEEDBACK &&
                                    uiState.feedbackChoiceId == choice.id,
                                onClick = { onAnswer(choice.id) },
                                onWrongFinished = onFeedbackFinished,
                                onSuccessSound = { audioPlayer.playToEnd("sfx-pop") },
                                onCorrectFinished = onFeedbackFinished,
                            )
                            }
                        }
                    }
                }
                ReplayBubble(
                    enabled = (uiState.phase == WhatsThatPhase.WAITING_FOR_ANSWER ||
                        uiState.phase == WhatsThatPhase.WRONG_FEEDBACK) &&
                        (inspection || lifecycleState.isAtLeast(Lifecycle.State.RESUMED)),
                    onClick = onReplay,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun GameTopBar(title: String, onBack: () -> Unit) {
    val backDescription = stringResource(R.string.back)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BubbleButton(
            onClick = onBack, iconAssetId = "ui-back", accessibilityLabel = backDescription,
            colors = listOf(GameTitleColor), showBorder = false,
        )
        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = GameTitleColor,
            fontFamily = GameTitleFontFamily,
            fontSize = 32.sp,
            modifier = Modifier.weight(1f).padding(end = 56.dp),
        )
    }
}

@Composable
private fun ChoiceBubble(
    choice: WhatsThatChoice,
    roundId: String,
    bubbleSize: androidx.compose.ui.unit.Dp,
    centerTranslation: Float,
    maxSuccessScale: Float,
    hidden: Boolean,
    enabled: Boolean,
    isWrong: Boolean,
    isCorrect: Boolean,
    onClick: () -> Unit,
    onWrongFinished: () -> Unit,
    onSuccessSound: suspend () -> Unit,
    onCorrectFinished: () -> Unit,
) {
    val inspection = LocalInspectionMode.current
    val shake = remember(roundId) { Animatable(0f) }
    val selectChoice by rememberUpdatedState(onClick)
    val decorations = remember(roundId) { Animatable(if (inspection && isWrong) 1f else 0f) }
    val successScale = remember(roundId) { Animatable(if (inspection && isCorrect) maxSuccessScale else 1f) }
    val successTranslation = remember(roundId) { Animatable(if (inspection && isCorrect) centerTranslation else 0f) }
    val successAlpha = remember(roundId) { Animatable(1f) }
    val entranceScale = remember(roundId) { Animatable(if (inspection) 1f else .86f) }
    val entranceAlpha = remember(roundId) { Animatable(if (inspection) 1f else 0f) }

    LaunchedEffect(roundId) {
        if (inspection) return@LaunchedEffect
        coroutineScope {
            launch { entranceScale.animateTo(1f, tween(240, easing = FastOutSlowInEasing)) }
            launch { entranceAlpha.animateTo(1f, tween(220)) }
        }
    }
    LaunchedEffect(isWrong) {
        if (isWrong && !inspection) {
            coroutineScope {
                launch {
                    decorations.snapTo(1f)
                    decorations.animateTo(0f, tween(340))
                }
                launch {
                    listOf(-10f, 9f, -7f, 5f, 0f).forEach { value ->
                        shake.animateTo(value, tween(60))
                    }
                }
            }
            onWrongFinished()
        }
    }
    LaunchedEffect(isCorrect) {
        if (isCorrect && !inspection) {
            // Start together and keep the bubble visible until both sound and growth finish.
            coroutineScope {
                launch { onSuccessSound() }
                launch {
                    successTranslation.animateTo(
                        centerTranslation, tween(450, easing = FastOutSlowInEasing),
                    )
                }
                launch {
                    successScale.animateTo(
                        maxSuccessScale, tween(1_200, easing = FastOutSlowInEasing),
                    )
                }
            }
            successAlpha.animateTo(0f, tween(140))
            onCorrectFinished()
        }
    }

    Box(
        modifier = Modifier
            .size(bubbleSize)
            .zIndex(if (isCorrect) 2f else 0f)
            .graphicsLayer {
                translationX = shake.value + successTranslation.value
                scaleX = entranceScale.value * successScale.value
                scaleY = entranceScale.value * successScale.value
                alpha = if (hidden) 0f else entranceAlpha.value * successAlpha.value
            }
            .semantics {
                contentDescription = choice.id
                role = Role.Button
            }
            .clip(CircleShape)
            .pointerInput(roundId, enabled) {
                if (enabled) {
                    // A clumsy tap may become a drag. Select its starting bubble
                    // once movement crosses touch slop, regardless of direction.
                    detectDragGestures(
                        onDragStart = { selectChoice() },
                        onDrag = { change, _ -> change.consume() },
                    )
                }
            }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawSoapBubble()
        }
        val libraryPhoto = choice.imageAssetId.startsWith("library-photo:")
        AssetImage(
            assetId = choice.imageAssetId,
            contentScale = if (libraryPhoto) ContentScale.Crop else ContentScale.Fit,
            modifier = if (libraryPhoto) Modifier.fillMaxSize().padding(7.dp).clip(CircleShape)
                else Modifier.fillMaxSize().padding(bubbleSize * .13f),
        )
        if (decorations.value > 0f) {
            WrongBubbles(alpha = decorations.value, modifier = Modifier.fillMaxSize())
        }
    }
}

private fun DrawScope.drawSoapBubble() {
    val radius = size.minDimension / 2 - 7.dp.toPx()
    val rim = Brush.sweepGradient(
        listOf(
            Color(0xFF19DCEF), Color(0xFF9E8BFF), Color(0xFFF56DDC),
            Color(0xFF85F0EA), Color(0xFF9AC8FF), Color(0xFF19DCEF),
        ),
        center = center,
    )
    // A transparent film and a soft halo, rather than a thick solid frame.
    drawCircle(
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = .10f), Color(0xFFDDFBFF).copy(alpha = .35f),
                Color(0xFFE9DAFF).copy(alpha = .25f)),
            center = center - Offset(radius * .3f, radius * .3f),
            radius = radius * 1.5f,
        ),
        radius = radius,
    )
    drawCircle(rim, radius, alpha = .04f, style = Stroke(7.dp.toPx()))
    drawCircle(rim, radius, alpha = .1f, style = Stroke(3.dp.toPx()))
    drawCircle(rim, radius, style = Stroke(1.2.dp.toPx()))
    drawCircle(Color.White.copy(alpha = .2f), radius, style = Stroke(.4.dp.toPx()))
    drawCircle(Color.White.copy(alpha = .1f), radius - 3.dp.toPx(),
        style = Stroke(.7.dp.toPx()))
    val highlightRadius = radius - 2.dp.toPx()
    drawArc(
        Color.White.copy(alpha = .35f), 212f, 38f, false,
        topLeft = center - Offset(highlightRadius, highlightRadius),
        size = androidx.compose.ui.geometry.Size(highlightRadius * 2, highlightRadius * 2),
        style = Stroke(1.4.dp.toPx(), cap = StrokeCap.Round),
    )
    drawArc(
        Color(0xFFFFDCF6).copy(alpha = .3f), 32f, 26f, false,
        topLeft = center - Offset(highlightRadius, highlightRadius),
        size = androidx.compose.ui.geometry.Size(highlightRadius * 2, highlightRadius * 2),
        style = Stroke(1.dp.toPx(), cap = StrokeCap.Round),
    )
}

@Composable
private fun WrongBubbles(alpha: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val positions = listOf(
            Offset(size.width * .18f, size.height * .18f) to 8.dp,
            Offset(size.width * .82f, size.height * .16f) to 6.dp,
            Offset(size.width * .88f, size.height * .72f) to 9.dp,
            Offset(size.width * .14f, size.height * .78f) to 5.dp,
        )
        positions.forEach { (center, radius) ->
            drawCircle(Color(0xFF8DE5FA), radius.toPx(), center, alpha = alpha * .8f)
            drawCircle(Color.White, radius.toPx(), center, alpha = alpha, style = Stroke(1.dp.toPx()))
        }
    }
}

@Composable
private fun ReplayBubble(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val replayDescription = stringResource(R.string.replay_sound)
    BubbleButton(
        onClick = onClick,
        iconAssetId = "ui-sound",
        accessibilityLabel = replayDescription,
        diameter = 88.dp,
        colors = listOf(GameTitleColor),
        showBorder = true,
        neon = true,
        enabled = enabled,
        modifier = modifier,
    )
}

@Composable
private fun GameCompleteScreen(audioPlayer: AudioPlayer, onFinished: () -> Unit) {
    val inspection = LocalInspectionMode.current
    LaunchedEffect(audioPlayer) {
        if (inspection) return@LaunchedEffect
        coroutineScope {
            launch { audioPlayer.playToEnd("sfx-game-complete") }
            delay(3_000)
        }
        onFinished()
    }
    val transition = rememberInfiniteTransition(label = "completion pulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(850), repeatMode = RepeatMode.Reverse),
        label = "trophy scale",
    )
    val description = stringResource(R.string.game_complete)
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AssetImage(
            assetId = "completion-background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        // The large bubble's center is at (50%, 48.5%) in the 887 × 1774 master.
        val imageScale = maxOf(maxWidth / 887f, maxHeight / 1774f)
        val bubbleOffset = imageScale * (860f - 1774f / 2)
        val gold = Brush.linearGradient(
            listOf(Color(0xFFFFE66A), Color(0xFFFFB91D), Color(0xFFE99A11)),
        )
        val trophyTint = Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithCache {
                onDrawWithContent {
                    drawContent()
                    drawRect(gold, blendMode = BlendMode.SrcIn)
                }
            }
        Box(
            modifier = Modifier
                .offset(y = bubbleOffset)
                .size(minOf(maxWidth * .48f, maxHeight * .25f))
                .scale(scale)
                .semantics { contentDescription = description },
        ) {
            AssetImage(assetId = "ui-trophy",
                modifier = trophyTint.alpha(.4f).blur(3.dp, BlurredEdgeTreatment.Unbounded))
            AssetImage(assetId = "ui-trophy", modifier = trophyTint)
        }
    }
}

@Composable
private fun GameBubbleBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        listOf(.08f to .2f, .92f to .18f, .06f to .72f, .94f to .8f).forEachIndexed { i, p ->
            drawCircle(
                Color(0xFF8DE5FA).copy(alpha = .18f),
                radius = (if (i % 2 == 0) 14.dp else 9.dp).toPx(),
                center = Offset(size.width * p.first, size.height * p.second),
            )
        }
    }
}

@Composable
private fun WhatsThatPreviewContent(phase: WhatsThatPhase) {
    val round = remember {
        WhatsThatSessionFactory(kotlin.random.Random(0))
            .create(LocalGameRepository().getWhatsThatGame("whats-that")).rounds.first()
    }
    val silentAudio = remember {
        object : AudioPlayer {
            override fun play(assetId: String) = Unit
            override suspend fun playToEnd(assetId: String) = Unit
            override fun stop() = Unit
            override fun release() = Unit
        }
    }
    val feedback = when (phase) {
        WhatsThatPhase.CORRECT_FEEDBACK -> round.correctChoiceId
        WhatsThatPhase.WRONG_FEEDBACK -> round.choices.first { it.id != round.correctChoiceId }.id
        else -> null
    }
    PuzzleBubblesTheme {
        WhatsThatGameScreen("What's That?", WhatsThatUiState(round, 1, 10, phase, feedback, 0),
            silentAudio, {}, {}, {}, {}, {}, Modifier.fillMaxSize())
    }
}

@Preview(name = "What's That - round", widthDp = 360, heightDp = 800)
@Preview(name = "What's That - landscape", widthDp = 800, heightDp = 360)
@Composable
private fun WhatsThatRoundPreview() = WhatsThatPreviewContent(WhatsThatPhase.WAITING_FOR_ANSWER)

@Preview(name = "What's That - wrong feedback", widthDp = 360, heightDp = 800)
@Composable
private fun WhatsThatWrongPreview() = WhatsThatPreviewContent(WhatsThatPhase.WRONG_FEEDBACK)

@Preview(name = "What's That - correct feedback", widthDp = 360, heightDp = 800)
@Composable
private fun WhatsThatCorrectPreview() = WhatsThatPreviewContent(WhatsThatPhase.CORRECT_FEEDBACK)

@Preview(name = "Game complete", widthDp = 360, heightDp = 800)
@Preview(name = "Game complete - landscape", widthDp = 800, heightDp = 360)
@Composable
private fun GameCompletePreview() = WhatsThatPreviewContent(WhatsThatPhase.GAME_COMPLETE)
