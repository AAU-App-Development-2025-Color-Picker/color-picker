package at.aau.appdev.colorpicker.camera

import android.opengl.GLSurfaceView
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import at.aau.appdev.colorpicker.MainActivity
import at.aau.appdev.colorpicker.R
import at.aau.appdev.colorpicker.ui.theme.ColorPickerTheme

@Composable
fun CameraScreen(navController: NavController, viewModel: CameraViewModel = hiltViewModel()) {
    val session = (LocalActivity.current as MainActivity).session
    val display = LocalContext.current.display
    val renderer = CameraRenderer(
        session,
        display,
        ARCoreInteractionHandler.consumeTapsAndProduceAnchors(
            viewModel::getAllTaps, viewModel::putAllAnchors,
        ),
        ARCoreSampler.projectPointsAndSampleColors(
            viewModel::getAllAnchors, viewModel::putAllProbes
        ),
        ARCoreCaptureHandler.consumeStatusAndProduceImage(
            viewModel::getCaptureStatus, viewModel::onImageAvailable
        ),
    )
    val lifecycleOwner = LocalLifecycleOwner.current

    // https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/views-in-compose
    AndroidView(
        factory = { context ->
        // https://developer.android.com/reference/android/opengl/GLSurfaceView
        // https://github.com/google-ar/arcore-android-sdk/tree/main/samples/hello_ar_kotlin
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            setRenderer(renderer)
        }
    }, modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { offset ->
                    Log.d(
                        "CameraView.CameraScreen", "Tapped at coordinates ${offset.x}, ${offset.y}."
                    )
                    viewModel.produceTap(offset.x, offset.y)
                })
            }, update = { view ->
        val glView = view as GLSurfaceView

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    session.resume()
                }

                Lifecycle.Event.ON_PAUSE -> {
                    glView.onPause()
                }

                Lifecycle.Event.ON_RESUME -> {
                    glView.onResume()
                }

                Lifecycle.Event.ON_STOP -> {
                    session.pause()
                }

                else -> Log.d("GLSurfaceView", "Lifecycle: $event")
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
    })

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ColorProbeOverlay(uiState.probes)
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PhotoLibraryNavButton(navController = navController)
            CaptureButton(
                onClick = viewModel::onCaptureProbeClicked,
                onLongClick = viewModel::onCaptureAllProbesClicked
            )
            ColorGalleryNavButton(navController = navController)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CameraScreenPreview() {
    ColorPickerTheme {
        val fakeNavController = rememberNavController()
        CameraScreen(fakeNavController)
    }
}

@Composable
fun ColorProbeOverlay(
    probes: List<Probe>,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        probes.forEach { point ->
            key(point.id) {
                val color = point.color
                val offset = IntOffset(
                    (point.position.x).toInt(),
                    (point.position.y).toInt(),
                )

                // https://developer.android.com/develop/ui/compose/animation/composables-modifiers#animatedcontent
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.BottomEnd)
                ) {
                    ColorProbe(
                        color = color,
                        offset = offset,
                        onSingleTap = {},
                        onDoubleTap = {},
                        onDragStart = {},
                        onDragEnd = {},
                        isActive = false
                    )
                }
            }
        }
    }
}

@Composable
fun ColorProbe(
    color: Color,
    offset: IntOffset,
    ringRadiusDp: Dp = 12.dp,
    ringThicknessDp: Dp = 4.dp,
    onSingleTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    isActive: Boolean
) {
    Box(
        modifier = Modifier
            .offset { offset }
            .size(ringRadiusDp * 2)
            .border(
                width = ringThicknessDp, color = Color.White, shape = CircleShape
            )
            .background(
                color = color, shape = CircleShape
            )
            .clip(CircleShape)
            // https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures
        // https://developer.android.com/reference/kotlin/androidx/compose/foundation/gestures/package-summary.html
        .pointerInput(isActive) {
            detectTapGestures(onTap = { offset ->
                Log.d(
                    "CameraView.ColorProbe", "Tap gesture detected: onTap()"
                )
            }, onDoubleTap = { offset ->
                Log.d(
                    "CameraView.ColorProbe", "Tap gesture detected: onDoubleTap()"
                )
            }, onPress = { offset ->
                Log.d(
                    "CameraView.ColorProbe", "Tap gesture detected: onPress()"
                )
            })
        }
            .pointerInput(isActive) {
                detectDragGestures(onDragStart = {
                    Log.d(
                        "CameraView.ColorProbe", "Drag gesture detected: onDragStart()"
                    )
                }, onDragEnd = {
                    Log.d(
                        "CameraView.ColorProbe", "Drag gesture detected: onDragEnd()"
                    )
                }, onDrag = { change, offset ->
                    Log.d(
                        "CameraView.ColorProbe", "Drag gesture detected: onDrag()"
                    )
                })
            }
            .pointerInput(isActive) {
                // TODO: This gesture is equivalent to the drag gesture but also activates a magnifying
                // TODO: glass as well as slower movement for better accuracy.
                detectDragGesturesAfterLongPress(onDragStart = {
                    Log.d(
                        "CameraView.ColorProbe",
                        "Drag gesture after long press detected: onDragStart()"
                    )
                }, onDragEnd = {
                    Log.d(
                        "CameraView.ColorProbe",
                        "Drag gesture after long press detected: onDragEnd()"
                    )
                }, onDrag = { change, offset ->
                    Log.d(
                        "CameraView.ColorProbe", "Drag gesture after long press detected: onDrag()"
                    )
                })
            })
}

@Composable
fun CaptureButton(
    modifier: Modifier = Modifier,
    color: Color = Color.DarkGray,
    shape: Shape = RoundedCornerShape(50),
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(160.dp, 60.dp)
            .shadow(8.dp, shape, clip = false)
            .background(color, shape)
            .border(4.dp, Color.White, shape)
            .combinedClickable(onClick = {
                Log.d("CameraView.CaptureButton", "Short press of capture button.")
                onClick()
            }, onLongClick = {
                Log.d("CameraView.CaptureButton", "Long press of capture button.")
                onLongClick()
            })
    )
}

@Composable
fun DropShadowIconButton(resource: Int, contentDescription: String?, onClick: () -> Unit) {
    Box(modifier = Modifier.size(64.dp)) {
        Icon(
            painter = painterResource(resource),
            contentDescription = null,
            modifier = Modifier
                .blur(12.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .alpha(0.2f),
            tint = Color.Black
        )
        IconButton(
            onClick = onClick, modifier = Modifier.size(64.dp)
        ) {
            Icon(
                painter = painterResource(resource),
                contentDescription = contentDescription,
                tint = Color.White
            )
        }
    }
}

@Composable
fun PhotoLibraryNavButton(navController: NavController) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            Log.d("CameraView.PhotoLibraryNavButton", "URI = $uri")
        }
    DropShadowIconButton(R.drawable.ic_photo_library, "Photo Library", {
        launcher.launch(
            PickVisualMediaRequest(
                mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    })
}

@Composable
fun ColorGalleryNavButton(navController: NavController) {
    DropShadowIconButton(
        R.drawable.ic_color_palette, "Color Palette", {
            navController.navigate("gallery")
        })
}