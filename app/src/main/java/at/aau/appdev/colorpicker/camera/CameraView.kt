package at.aau.appdev.colorpicker.camera

import android.opengl.GLSurfaceView
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import at.aau.appdev.colorpicker.MainActivity
import at.aau.appdev.colorpicker.R
import at.aau.appdev.colorpicker.generateColor
import at.aau.appdev.colorpicker.ui.theme.ColorPickerTheme
import com.google.ar.core.HitResult
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun CameraScreen(navController: NavController) {
    val viewModel: CameraViewModel = viewModel()

    val session = (LocalActivity.current as MainActivity).session!!
    val display = LocalContext.current.display
    val renderer = CameraRenderer(session, display)
    val lifecycleOwner = LocalLifecycleOwner.current

    val colorProbes by viewModel.colorProbes.collectAsState()
    val cameraState by viewModel.cameraState.collectAsState()
    val activeProbeId by viewModel.activeProbeId.collectAsState()

    // https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/views-in-compose
    AndroidView(factory = { context ->
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
                // INFO: Currently, ARCore is used for camera and position data. This may change in
                // INFO: the future. A custom interface should be added to allow for different
                // INFO: implementations.
                Log.d(
                    "CameraView.CameraScreen", "Tapped at coordinates ${offset.x}, ${offset.y}."
                )
                var hitResults = emptyList<HitResult>()
                do {
                    hitResults = renderer.frame.hitTestInstantPlacement(offset.x, offset.x, 3.0f)
                    Log.d("CameraView.CameraScreen", "Hit test failed.")
                } while (hitResults.isEmpty())
                Log.d("CameraView.CameraScreen", "Hit test succeeded.")
                // INFO: Currently leads to a crash; tracking state needs to be checked beforehand.
                // anchor = hitResults.get(0).createAnchor()

                // Extract hit test logic into CameraViewModel
                viewModel.onCameraTap(offset, hitResults, renderer)
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

    // Use ViewModel data for ColorProbeOverlay
    ColorProbeOverlay(
        colorProbes = colorProbes,
        activeProbeId = activeProbeId,
        onProbeSingleTap = viewModel::onProbeSingleTap,
        onProbeDoubleTap = viewModel::onProbeDoubleTap,
        onProbeDragStart = viewModel::onProbeDragStart,
        onProbeDragEnd = viewModel::onProbeDragEnd,
        onProbePositionUpdate = viewModel::updateProbePosition
    )

    Box(modifier = Modifier.fillMaxSize()) {
        ControlRow(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            navController = navController,
            viewModel = viewModel
        )
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
    colorProbes: List<ColorProbeData>,
    activeProbeId: Int?,
    onProbeSingleTap: (Int) -> Unit,
    onProbeDoubleTap: (Int) -> Unit,
    onProbeDragStart: (Int, Offset) -> Unit,
    onProbeDragEnd: (Int) -> Unit,
    onProbePositionUpdate: (Int, IntOffset) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        colorProbes.forEach { probe ->
            ColorProbe(
                color = probe.color,
                offset = probe.position,
                onSingleTap = { onProbeSingleTap(probe.id) },
                onDoubleTap = { onProbeDoubleTap(probe.id) },
                onDragStart = { offset -> onProbeDragStart(probe.id, offset) },
                onDragEnd = { onProbeDragEnd(probe.id) },
                isActive = probe.id == activeProbeId
            )
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
    Box(modifier = Modifier
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
                    "CameraView.ColorProbe", "Drag gesture after long press detected: onDragStart()"
                )
            }, onDragEnd = {
                Log.d(
                    "CameraView.ColorProbe", "Drag gesture after long press detected: onDragEnd()"
                )
            }, onDrag = { change, offset ->
                Log.d(
                    "CameraView.ColorProbe", "Drag gesture after long press detected: onDrag()"
                )
            })
        })
}

@Composable
fun ControlRow(
    modifier: Modifier,
    navController: NavController,
    viewModel: CameraViewModel,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        PhotoLibraryNavButton(
            navController = navController,
            viewModel = viewModel
        )
        CaptureButton(viewModel = viewModel)
        ColorGalleryNavButton(navController = navController)
    }
}

@Composable
fun CaptureButton(
    modifier: Modifier = Modifier,
    innerColor: Color = Color.DarkGray,
    viewModel: CameraViewModel,
) {
    Box(
        modifier = modifier
            .size(160.dp, 60.dp)
            .shadow(8.dp, RoundedCornerShape(50), clip = false)
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .padding(4.dp)
            .clip(RoundedCornerShape(50))
            .background(innerColor)
            .combinedClickable(
                onClick = {
                    // Save single probe to gallery (using 'viewModel').
                    Log.d("CameraView.CaptureButton", "Short press of capture button.")
                    viewModel.captureCurrentProbe()
                },
                onLongClick = {
                    // Save multiple probes to gallery (using 'viewModel').
                    Log.d("CameraView.CaptureButton", "Long press of capture button.")
                    viewModel.captureAllProbes()
                }
            )
    )
}

@Composable
fun DropShadowIconButton(resource: Int, contentDescription: String?, onClick: () -> Unit) {
    Box(modifier = Modifier.size(64.dp)) {
        Icon(
            painter = painterResource(resource),
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier
                .blur(12.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .alpha(0.2f)
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
fun PhotoLibraryNavButton(
    navController: NavController,
    viewModel: CameraViewModel,
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        // After the image has been selected, multiple things happen:
        // - The ARCore session has to be stopped;
        // - The image has to be loaded;
        // - The OpenGL surface has to be filled with the image;
        // - The photo library icon to be changed to 'R.drawable.ic_photo_camera'.
        Log.d("CameraView.PhotoLibraryNavButton", "URI = $uri")
        uri?.let { viewModel.onPhotoSelected(it) }
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
