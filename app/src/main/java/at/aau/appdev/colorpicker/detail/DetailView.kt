package at.aau.appdev.colorpicker.detail

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import at.aau.appdev.colorpicker.R
import at.aau.appdev.colorpicker.generateColor
import at.aau.appdev.colorpicker.ui.theme.ColorPickerTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreen(navController: NavController) {
    val viewModel: DetailViewModel = viewModel()

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {}, modifier = Modifier.size(64.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back to Gallery",
                            tint = Color.Black
                        )
                    }
                }

                // TODO: Maybe it makes sense to
                var isFullscreen by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(generateColor())
                        .animateContentSize(animationSpec = tween(durationMillis = 400))
                        .clickable() {
                            isFullscreen = !isFullscreen
                        }
                        .then(
                            if (isFullscreen) {
                                Modifier.fillMaxSize()
                            } else {
                                Modifier.fillMaxWidth().aspectRatio(2.0f)
                            }
                        ),
                )

                Text("Name of the Color", fontSize = 36.sp, fontWeight = FontWeight.Bold)

                // TODO: The tab row and the horizontal pager should always use the same index!
                val coroutineScope = rememberCoroutineScope()
                val pagerState = rememberPagerState(pageCount = { 3 })
                val titles = listOf("Color", "Palette", "Photo")
                var selected by remember { mutableStateOf(0) }
                PrimaryTabRow(selected) {
                    titles.forEachIndexed { index, title ->
                        Tab(
                            selected = selected == index,
                            onClick = {
                                selected = index
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(selected)
                                }
                            },
                            text = {
                                Text(
                                    text = title, maxLines = 2, overflow = TextOverflow.Ellipsis
                                )
                            },
                        )
                    }
                }

                HorizontalPager(state = pagerState) { page ->
                    when (page) {
                        0 -> ColorTab()
                        1 -> PalettesTab()
                        2 -> PhotosTab()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ColorTab() {
    Column {
        val options = listOf("RGB", "HSL", "HSB", "CMYK", "LAB")
        var selectedIndex by remember { mutableIntStateOf(0) }

        FlowRow(
            Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            options.forEachIndexed { index, label ->
                ToggleButton(
                    checked = selectedIndex == index,
                    onCheckedChange = { selectedIndex = index },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    modifier = Modifier
                        .weight(1.0f)
                        .semantics { role = Role.RadioButton },
                ) {
                    Text(label)
                }
            }
        }

        // TODO: The slider state should be bound to the 'viewModel'!
        val red = rememberSliderState(0.5f)
        val green = rememberSliderState(0.5f)
        val blue = rememberSliderState(0.5f)

        // TODO: This code only works for the RGB representation of the color;
        // TODO: instead, the layout should be generated dynamically based on
        // TODO: the selected representation!
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            ColorSlider(red)
            ColorSlider(green)
            ColorSlider(blue)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.ColorSlider(state: SliderState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1.0f)
    ) {
        OutlinedTextField(
            TextFieldState(),
            label = { Text("Ehhh?") },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Slider(
            state, modifier = Modifier
                .rotate(270f)
                // https://developer.android.com/develop/ui/compose/layouts/custom
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        Constraints(
                            minWidth = constraints.minHeight,
                            maxWidth = constraints.maxHeight,
                            minHeight = constraints.minWidth,
                            maxHeight = constraints.maxHeight,
                        )
                    )
                    layout(placeable.height, placeable.width) {
                        // INFO: The 'rotate' modifier rotates around the center, so we
                        // INFO: have to offset the layout relative to the center as well.
                        val offsetX = (placeable.height - placeable.width) / 2
                        val offsetY = (placeable.width - placeable.height) / 2
                        placeable.placeRelative(offsetX, offsetY)
                    }
                })
    }
}

@Composable
fun PalettesTab() {
    Text("PalettesTab")
}

@Composable
fun PhotosTab() {
    Text("PhotosTab")
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ColorPickerTheme {
        val fakeNavController = rememberNavController()
        DetailScreen(fakeNavController)
    }
}