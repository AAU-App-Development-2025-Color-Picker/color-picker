package at.aau.appdev.colorpicker

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

fun generateColor(): Color {
    val hue = Random.nextFloat() * 360f
    val saturation = Random.nextFloat() * 0.5f + 0.3f
    val lightness = Random.nextFloat() * 0.8f + 0.1f
    return Color.Companion.hsl(hue, saturation, lightness)
}

fun generateColors(): List<Color> {
    val outcomes = listOf(2, 3, 4, 5, 6)
    val weights = listOf(0.25, 0.25, 0.25, 0.15, 0.10)

    val distribution = DiscreteDistribution(outcomes, weights)
    val count = distribution.sample()

    val base1 = generateColor()
    val base2 = generateColor()

    return List(count) { i ->
        val t = i / (count - 1).toFloat()

        val r = interpolate(base1.red, base2.red, t) + randomJitter()
        val g = interpolate(base1.green, base2.green, t) + randomJitter()
        val b = interpolate(base1.blue, base2.blue, t) + randomJitter()

        Color(
            red = r.coerceIn(0f, 1f),
            green = g.coerceIn(0f, 1f),
            blue = b.coerceIn(0f, 1f),
            alpha = 1f
        )
    }
}

fun interpolate(a: Float, b: Float, t: Float): Float {
    return a * (1 - t) + b * t
}

fun randomJitter(): Float {
    return Random.nextFloat() * 0.1f - 0.05f
}

class DiscreteDistribution(private val outcomes: List<Int>, weights: List<Double>) {
    private val cumulative: List<Double>

    init {
        val total = weights.sum()
        cumulative = weights.runningFold(0.0) { acc, weight -> acc + weight / total }.drop(1)
    }

    fun sample(): Int {
        val r = Random.nextDouble()
        val index = cumulative.indexOfFirst { r <= it }
        return outcomes[index]
    }
}