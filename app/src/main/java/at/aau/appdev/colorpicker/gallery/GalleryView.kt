package at.aau.appdev.colorpicker.gallery

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import at.aau.appdev.colorpicker.R
import at.aau.appdev.colorpicker.ui.theme.ColorPickerTheme

@Composable
fun GalleryScreen(
    navController: NavController,
    navId: String? = null,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Gallery", fontSize = 40.sp, fontWeight = FontWeight.Medium)
                    IconButton(onClick = {}, modifier = Modifier.size(64.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_menu_open),
                            contentDescription = "Sort By",
                            tint = Color.Black
                        )
                    }
                }
                CardStaggeredGrid(uiState.items)
            }
            CameraNavButton(navController)
            ActionButton(navController)
        }
    }
}

@Composable
fun BoxScope.CameraNavButton(navController: NavController) {
    FloatingActionButton(
        onClick = { navController.navigate("detail/1") },
        modifier = Modifier
            .padding(24.dp)
            .size(64.dp)
            .align(Alignment.BottomStart)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_view_in_ar), contentDescription = "Camera View"
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BoxScope.ActionButton(navController: NavController) {
    var isExpanded by remember { mutableStateOf(false) }
    FloatingActionButtonMenu(
        expanded = isExpanded,
        modifier = Modifier
            .padding(24.dp)
            .align(Alignment.BottomEnd),
        button = {
            ToggleFloatingActionButton(
                modifier = Modifier.animateFloatingActionButton(
                    visible = true,
                    alignment = Alignment.TopStart,
                ),
                checked = isExpanded,
                onCheckedChange = { isExpanded = !isExpanded },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_open_fullscreen),
                    contentDescription = "Camera View"
                )
            }
        }) {
        FloatingActionButtonMenuItem(onClick = {}, text = { Text("One") }, icon = {})
        FloatingActionButtonMenuItem(onClick = {}, text = { Text("Two") }, icon = {})
        FloatingActionButtonMenuItem(onClick = {}, text = { Text("Three") }, icon = {})

    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ColorPickerTheme {
        val fakeNavController = rememberNavController()
        GalleryScreen(fakeNavController)
    }
}

@Composable
fun CardStaggeredGrid(items: List<GalleryItem> = emptyList()) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalItemSpacing = 2.dp,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(items) { item ->
            when (item) {
                is GalleryItem.Swatch -> {
                    val color = Color(
                        red = item.color.red,
                        green = item.color.green,
                        blue = item.color.blue,
                    )
                    ColorSwatch(color = color)
                }

                is GalleryItem.Palette -> {
                    val colors = item.palette.colors.map { colorEntity ->
                        Color(
                            red = colorEntity.red,
                            green = colorEntity.green,
                            blue = colorEntity.blue,
                        )
                    }
                    ColorFan(colors = colors)
                }
            }
        }
    }
}

val offsets = arrayOf(
    floatArrayOf(1.0f),
    floatArrayOf(0.6875f, 1.0f),
    floatArrayOf(0.625f, 0.8375f, 1.0f),
    floatArrayOf(0.5625f, 0.75f, 0.8875f, 1.0f),
    floatArrayOf(0.5f, 0.675f, 0.8125f, 0.9125f, 1.0f),
    floatArrayOf(0.4375f, 0.625f, 0.7625f, 0.8625f, 0.9375f, 1.0f)
)

@OptIn(ExperimentalStdlibApi::class)
@Composable
fun ColorSwatch(modifier: Modifier = Modifier, color: Color) {
    // TODO: 'isActive' should be set elsewhere!
    val isActive = false
    Box(
        modifier = modifier
            .aspectRatio(1.6f)
            .padding(8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .pointerInput(isActive) {
                detectDragGesturesAfterLongPress(onDragStart = {
                    Log.d(
                        "GalleryView.ColorSwatch",
                        "Drag gesture after long press detected: onDragStart()"
                    )
                }, onDragEnd = {
                    Log.d(
                        "GalleryView.ColorSwatch",
                        "Drag gesture after long press detected: onDragEnd()"
                    )
                }, onDrag = { change, offset ->
                    Log.d(
                        "GalleryView.ColorSwatch",
                        "Drag gesture after long press detected: onDrag()"
                    )
                })
            }) {
        Label(color.toArgb().toHexString(HexFormat.UpperCase).slice(IntRange(2, 7)))
    }
}

@Composable
fun ColorFan(modifier: Modifier = Modifier, colors: List<Color>) {
    // TODO: 'isActive' should be set elsewhere!
    val isActive = false
    val count = colors.size
    Box(
        modifier = modifier
            .aspectRatio(1.25f)
            .padding(8.dp)
            .pointerInput(isActive) {
                detectTapGestures(onLongPress = { offset ->
                    Log.d(
                        "GalleryView.ColorFan", "Tap gesture detected: onLongPress()"
                    )
                })
            }) {
        for (i in (count - 1) downTo 0) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(offsets.get(count - 1).get(i))
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.get(i))
            )
        }

        Box(
            modifier = Modifier
                .padding(12.dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
        )

        Label("Muddy Greens")
    }
}

@Composable
private fun BoxScope.Label(label: String) {
    Box(
        modifier = Modifier.align(Alignment.BottomEnd)
    ) {
        Text(
            label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.inverseOnSurface,
            modifier = Modifier
                .padding(6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.6f))
                .padding(6.dp)
                .width(IntrinsicSize.Min)
        )
    }
}