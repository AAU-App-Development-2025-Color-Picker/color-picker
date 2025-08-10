package at.aau.appdev.colorpicker.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
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
fun DetailScreen(
    navController: NavController,
    navId: String? = null,
) {
    val viewModel: DetailViewModel = viewModel()

    Scaffold { padding ->
        // https://developer.android.com/develop/ui/compose/animation/shared-elements
        SharedTransitionLayout {
            var isFullscreen by remember { mutableStateOf(false) }
            AnimatedContent(isFullscreen) { targetState ->
                if (targetState) {
                    Box(
                        modifier = Modifier
                            .sharedElement(
                                rememberSharedContentState(key = "color-swatch"),
                                animatedVisibilityScope = this@AnimatedContent
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .background(generateColor())
                            .fillMaxSize()
                            .clickable() {
                                isFullscreen = !isFullscreen
                            }) {
                        Text(
                            "Name of the Color",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .sharedElement(
                                    rememberSharedContentState(key = "color-name"),
                                    animatedVisibilityScope = this@AnimatedContent
                                )
                                .align(Alignment.BottomStart)
                        )
                    }

                } else {
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

                        Box(
                            modifier = Modifier
                                .sharedElement(
                                    rememberSharedContentState(key = "color-swatch"),
                                    animatedVisibilityScope = this@AnimatedContent
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .background(generateColor())
                                .fillMaxWidth()
                                .aspectRatio(2.0f)
                                .clickable() {
                                    isFullscreen = !isFullscreen
                                })

                        Text(
                            "Name of the Color",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.sharedElement(
                                rememberSharedContentState(key = "color-name"),
                                animatedVisibilityScope = this@AnimatedContent
                            )
                        )

                        val coroutineScope = rememberCoroutineScope()
                        val pagerState = rememberPagerState(pageCount = { 3 })
                        val titles = listOf("Color", "Palette", "Photo")
                        PrimaryTabRow(pagerState.currentPage) {
                            titles.forEachIndexed { index, title ->
                                Tab(
                                    selected = pagerState.currentPage == index,
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                                    text = {
                                        Text(
                                            text = title,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                )
                            }
                        }
                        HorizontalPager(state = pagerState) { page ->
                            when (page) {
                                0 -> ColorTab()
                                1 -> PaletteTab()
                                2 -> PhotoTab()
                            }
                        }
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
        val sliderStates = listOf(
            rememberSliderState(0.5f),
            rememberSliderState(0.6f),
            rememberSliderState(0.9f),
        )

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            sliderStates.forEach { sliderState -> ColorSlider(sliderState) }
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
            label = { Text("256") },
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
fun PaletteTab() {
    Text("PalettesTab")
}

@Composable
fun PhotoTab() {
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