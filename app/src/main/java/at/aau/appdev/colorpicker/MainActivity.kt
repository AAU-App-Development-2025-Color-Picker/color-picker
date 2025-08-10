package at.aau.appdev.colorpicker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import at.aau.appdev.colorpicker.camera.CameraScreen
import at.aau.appdev.colorpicker.detail.DetailScreen
import at.aau.appdev.colorpicker.gallery.GalleryScreen
import at.aau.appdev.colorpicker.ui.theme.ColorPickerTheme
import com.google.ar.core.Config
import com.google.ar.core.Session
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    internal var session: Session? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        session = Session(applicationContext).apply {
            configure(Config(this).apply {
                // FIXME: Depth mode is not supported by the emulator, but should work on real hardware.
                // FIXME: Change the configuration below accordingly when using real hardware!
                // depthMode = Config.DepthMode.AUTOMATIC
                // depthMode = Config.DepthMode.RAW_DEPTH_ONLY
                depthMode = Config.DepthMode.DISABLED
                focusMode = Config.FocusMode.AUTO
                instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                lightEstimationMode = Config.LightEstimationMode.DISABLED
                planeFindingMode = Config.PlaneFindingMode.DISABLED
            })
        }

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {}
            LaunchedEffect(Unit) {
                if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }

            ColorPickerTheme {
                val navController = rememberNavController()
                NavigationHost(
                    modifier = Modifier, navController = navController
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        session?.close()
    }

    override fun onPause() {
        super.onPause()
        session?.pause()
    }

    override fun onResume() {
        super.onResume()
        session?.resume()
    }
}

@Composable
fun NavigationHost(modifier: Modifier = Modifier, navController: NavHostController) {
    NavHost(modifier = modifier, navController = navController, startDestination = "camera") {
        composable(
            route = "camera", arguments = listOf(navArgument("label") {
                type = NavType.StringType; defaultValue = "Camera"
            })
        ) { CameraScreen(navController) }

        composable(
            route = "gallery", arguments = listOf(navArgument("label") {
                type = NavType.StringType; defaultValue = "Gallery"
            })
        ) { GalleryScreen(navController) }

        composable(
            route = "gallery/{id}", arguments = listOf(navArgument("label") {
                type = NavType.StringType; defaultValue = "Gallery"
            })
        ) { navBackStackEntry ->
            GalleryScreen(navController, navBackStackEntry.id)
        }

        composable(
            route = "detail/{id}", arguments = listOf(navArgument("label") {
                type = NavType.StringType; defaultValue = "Detail"
            }, navArgument("id") {
                type = NavType.IntType; defaultValue = 0
            })
        ) { navBackStackEntry ->
            DetailScreen(navController, navBackStackEntry.id)
        }
    }
}
