package at.aau.appdev.colorpicker.camera

data class Coordinate(
    val x: Float,
    val y: Float,
) {
    constructor(coordinates: FloatArray) : this(
        x = coordinates[0], y = coordinates[1]
    ) {
        require(coordinates.size == 2) { "Array must contain exactly two elements." }
    }
}