package com.hutube.app.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.hutube.app.HuTubeApplication
import com.hutube.app.data.model.MediaItem
import com.hutube.app.ui.theme.BrandRed
import com.hutube.app.ui.theme.CardBackground
import kotlinx.coroutines.delay
import java.io.Serializable

@OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null
    private lateinit var currentMedia: MediaItem
    private var nextMedia: MediaItem? = null
    private var accessToken: String = ""

    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideSystemUI()

        @Suppress("DEPRECATION")
        currentMedia = intent.getSerializableExtra(EXTRA_MEDIA) as? MediaItem
            ?: run { finish(); return }
        @Suppress("DEPRECATION")
        nextMedia = intent.getSerializableExtra(EXTRA_NEXT_MEDIA) as? MediaItem
        accessToken = intent.getStringExtra(EXTRA_TOKEN) ?: ""

        setupPlayer()

        setContent {
            PlayerScreen(
                player = player,
                media = currentMedia,
                nextMedia = nextMedia,
                onBack = { finish() },
                onPlayNext = { next ->
                    val intent = Intent(this, PlayerActivity::class.java).apply {
                        putExtra(EXTRA_MEDIA, next)
                        putExtra(EXTRA_TOKEN, accessToken)
                    }
                    startActivity(intent)
                    finish()
                },
                onEnterPiP = { enterPiPMode() }
            )
        }
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED && nextMedia != null) {
                        val intent = Intent(this@PlayerActivity, PlayerActivity::class.java).apply {
                            putExtra(EXTRA_MEDIA, nextMedia)
                            putExtra(EXTRA_TOKEN, accessToken)
                        }
                        startActivity(intent)
                        finish()
                    }
                }
            })
        }

        if (currentMedia.id.startsWith("file://")) {
            val source = androidx.media3.common.MediaItem.fromUri(currentMedia.id)
            player?.setMediaItem(source)
        } else {
            val factory = DriveMediaSourceFactory(this) { accessToken }
            val source = factory.createMediaSource(currentMedia.id)
            player?.setMediaSource(source)
        }
        player?.prepare()

        val savedPos = HuTubeApplication.instance.watchHistoryManager.getProgress(currentMedia.id)
        if (savedPos > 10000) {
            player?.seekTo(savedPos)
        }

        startProgressTracking()
    }

    private fun startProgressTracking() {
        progressRunnable = object : Runnable {
            override fun run() {
                val p = player
                if (p != null && p.isPlaying && p.duration > 0) {
                    HuTubeApplication.instance.watchHistoryManager.saveProgress(
                        currentMedia,
                        p.currentPosition,
                        p.duration
                    )
                }
                handler.postDelayed(this, 3000)
            }
        }
        handler.post(progressRunnable!!)
    }

    private fun enterPiPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (player?.isPlaying == true) {
            enterPiPMode()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!isInPictureInPictureMode && !isFinishing) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        progressRunnable?.let { handler.removeCallbacks(it) }
        player?.let {
            if (it.duration > 0) {
                HuTubeApplication.instance.watchHistoryManager.saveProgress(
                    currentMedia,
                    it.currentPosition,
                    it.duration
                )
            }
            it.release()
        }
        player = null
    }

    companion object {
        const val EXTRA_MEDIA = "extra_media"
        const val EXTRA_NEXT_MEDIA = "extra_next_media"
        const val EXTRA_TOKEN = "extra_token"

        fun start(context: Context, media: MediaItem, nextMedia: MediaItem? = null, token: String) {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_MEDIA, media)
                putExtra(EXTRA_NEXT_MEDIA, nextMedia)
                putExtra(EXTRA_TOKEN, token)
            }
            context.startActivity(intent)
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    player: ExoPlayer?,
    media: MediaItem,
    nextMedia: MediaItem?,
    onBack: () -> Unit,
    onPlayNext: (MediaItem) -> Unit,
    onEnterPiP: () -> Unit
) {
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPos by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    // Poll player state
    LaunchedEffect(player) {
        while (true) {
            player?.let {
                isPlaying = it.isPlaying
                currentPos = it.currentPosition
                duration = it.duration.coerceAtLeast(0L)
            }
            delay(500)
        }
    }

    // Auto-hide controls after 5 seconds
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(5000)
            showControls = false
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            player?.seekTo((player.currentPosition - 10000).coerceAtLeast(0L))
                            showControls = true
                            true
                        }
                        Key.DirectionRight -> {
                            player?.seekTo((player.currentPosition + 10000).coerceAtMost(player.duration))
                            showControls = true
                            true
                        }
                        Key.DirectionCenter, Key.Enter -> {
                            showControls = !showControls
                            true
                        }
                        Key.MediaPlayPause -> {
                            player?.let { if (it.isPlaying) it.pause() else it.play() }
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
    ) {
        // ExoPlayer View
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    this.resizeMode = resizeMode
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.player = player
                view.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Top gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                )
                // Bottom gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                )

                // Top Bar — Title and actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                        .align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = media.title,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            media.seriesTitle?.let {
                                Text(
                                    text = "$it · Episode ${media.episode ?: 1}",
                                    color = Color.LightGray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Skip Opening pill
                        Button(
                            onClick = {
                                player?.seekTo((player.currentPosition + 85000).coerceAtMost(player.duration))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Skip Opening", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // Aspect Ratio Toggle
                        IconButton(onClick = {
                            resizeMode = when (resizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        }) {
                            Icon(Icons.Default.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White)
                        }

                        // PiP Button
                        IconButton(onClick = onEnterPiP) {
                            Icon(Icons.Default.PictureInPicture, contentDescription = "PiP", tint = Color.White)
                        }
                    }
                }

                // Center Play / Skip Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(56.dp)
                ) {
                    // Rewind 10s
                    IconButton(
                        onClick = { player?.seekTo((player.currentPosition - 10000).coerceAtLeast(0L)) },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(44.dp))
                    }

                    // Play / Pause
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable {
                                player?.let {
                                    if (it.isPlaying) it.pause() else it.play()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(52.dp)
                        )
                    }

                    // Forward 10s
                    IconButton(
                        onClick = { player?.seekTo((player.currentPosition + 10000).coerceAtMost(player.duration)) },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(44.dp))
                    }
                }

                // Bottom Controls & Scrubber
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    // Scrubber
                    Slider(
                        value = if (duration > 0) currentPos.toFloat() / duration.toFloat() else 0f,
                        onValueChange = { percent ->
                            player?.seekTo((percent * duration).toLong())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = BrandRed,
                            activeTrackColor = BrandRed,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth().height(24.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPos),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        if (nextMedia != null) {
                            TextButton(onClick = { onPlayNext(nextMedia) }) {
                                Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Next Episode", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = formatTime(duration),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
