package app.sidereply.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Curated Material Symbols Rounded vector icons (24x24 viewport) so the app has
 * authentic Material 3 iconography without pulling in the multi-megabyte extended icon pack.
 */
object SideReplyIcons {

    private inline fun icon(
        name: String,
        block: ImageVector.Builder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

    private val black = SolidColor(Color.Black)

    val Sparkle: ImageVector by lazy {
        icon("Sparkle") {
            path(fill = black) {
                moveTo(10.5f, 2.5f)
                lineTo(12.6f, 8.4f)
                lineTo(18.5f, 10.5f)
                lineTo(12.6f, 12.6f)
                lineTo(10.5f, 18.5f)
                lineTo(8.4f, 12.6f)
                lineTo(2.5f, 10.5f)
                lineTo(8.4f, 8.4f)
                close()
                moveTo(18.5f, 13.5f)
                lineTo(19.55f, 16.45f)
                lineTo(22.5f, 17.5f)
                lineTo(19.55f, 18.55f)
                lineTo(18.5f, 21.5f)
                lineTo(17.45f, 18.55f)
                lineTo(14.5f, 17.5f)
                lineTo(17.45f, 16.45f)
                close()
            }
        }
    }

    val HomeOutlined: ImageVector by lazy {
        icon("HomeOutlined") {
            path(fill = black) {
                moveTo(12f, 3f)
                lineTo(4f, 9f)
                verticalLineTo(21f)
                horizontalLineTo(10f)
                verticalLineTo(15f)
                horizontalLineTo(14f)
                verticalLineTo(21f)
                horizontalLineTo(20f)
                verticalLineTo(9f)
                close()
                moveTo(18f, 19f)
                horizontalLineTo(16f)
                verticalLineTo(13f)
                horizontalLineTo(8f)
                verticalLineTo(19f)
                horizontalLineTo(6f)
                verticalLineTo(10f)
                lineTo(12f, 5.5f)
                lineTo(18f, 10f)
                close()
            }
        }
    }

    val HomeFilled: ImageVector by lazy {
        icon("HomeFilled") {
            path(fill = black) {
                moveTo(12f, 3f)
                lineTo(4f, 9f)
                verticalLineTo(21f)
                horizontalLineTo(9.5f)
                verticalLineTo(14.5f)
                horizontalLineTo(14.5f)
                verticalLineTo(21f)
                horizontalLineTo(20f)
                verticalLineTo(9f)
                close()
            }
        }
    }

    val ModelOutlined: ImageVector by lazy {
        icon("ModelOutlined") {
            path(fill = black) {
                moveTo(19f, 5f)
                horizontalLineTo(5f)
                curveTo(3.9f, 5f, 3f, 5.9f, 3f, 7f)
                verticalLineTo(17f)
                curveTo(3f, 18.1f, 3.9f, 19f, 5f, 19f)
                horizontalLineTo(19f)
                curveTo(20.1f, 19f, 21f, 18.1f, 21f, 17f)
                verticalLineTo(7f)
                curveTo(21f, 5.9f, 20.1f, 5f, 19f, 5f)
                close()
                moveTo(19f, 17f)
                horizontalLineTo(5f)
                verticalLineTo(7f)
                horizontalLineTo(19f)
                verticalLineTo(17f)
                close()
                moveTo(9.5f, 9f)
                lineTo(10.6f, 11.4f)
                lineTo(13f, 12.5f)
                lineTo(10.6f, 13.6f)
                lineTo(9.5f, 16f)
                lineTo(8.4f, 13.6f)
                lineTo(6f, 12.5f)
                lineTo(8.4f, 11.4f)
                close()
                moveTo(15.5f, 8.5f)
                lineTo(16.15f, 9.85f)
                lineTo(17.5f, 10.5f)
                lineTo(16.15f, 11.15f)
                lineTo(15.5f, 12.5f)
                lineTo(14.85f, 11.15f)
                lineTo(13.5f, 10.5f)
                lineTo(14.85f, 9.85f)
                close()
            }
        }
    }

    val ModelFilled: ImageVector by lazy {
        icon("ModelFilled") {
            path(fill = black, pathFillType = PathFillType.EvenOdd) {
                moveTo(19f, 4f)
                horizontalLineTo(5f)
                curveTo(3.9f, 4f, 3f, 4.9f, 3f, 6f)
                verticalLineTo(18f)
                curveTo(3f, 19.1f, 3.9f, 20f, 5f, 20f)
                horizontalLineTo(19f)
                curveTo(20.1f, 20f, 21f, 19.1f, 21f, 18f)
                verticalLineTo(6f)
                curveTo(21f, 4.9f, 20.1f, 4f, 19f, 4f)
                close()
                moveTo(10f, 8f)
                lineTo(11.3f, 10.7f)
                lineTo(14f, 12f)
                lineTo(11.3f, 13.3f)
                lineTo(10f, 16f)
                lineTo(8.7f, 13.3f)
                lineTo(6f, 12f)
                lineTo(8.7f, 10.7f)
                close()
                moveTo(16f, 7.5f)
                lineTo(16.75f, 9.25f)
                lineTo(18.5f, 10f)
                lineTo(16.75f, 10.75f)
                lineTo(16f, 12.5f)
                lineTo(15.25f, 10.75f)
                lineTo(13.5f, 10f)
                lineTo(15.25f, 9.25f)
                close()
            }
        }
    }

    val TuneOutlined: ImageVector by lazy {
        icon("TuneOutlined") {
            path(fill = black) {
                moveTo(3f, 17f)
                verticalLineTo(19f)
                horizontalLineTo(9f)
                verticalLineTo(17f)
                horizontalLineTo(3f)
                close()
                moveTo(3f, 5f)
                verticalLineTo(7f)
                horizontalLineTo(13f)
                verticalLineTo(5f)
                horizontalLineTo(3f)
                close()
                moveTo(13f, 21f)
                verticalLineTo(19f)
                horizontalLineTo(21f)
                verticalLineTo(17f)
                horizontalLineTo(13f)
                verticalLineTo(15f)
                horizontalLineTo(11f)
                verticalLineTo(21f)
                horizontalLineTo(13f)
                close()
                moveTo(7f, 9f)
                verticalLineTo(11f)
                horizontalLineTo(3f)
                verticalLineTo(13f)
                horizontalLineTo(7f)
                verticalLineTo(15f)
                horizontalLineTo(9f)
                verticalLineTo(9f)
                horizontalLineTo(7f)
                close()
                moveTo(21f, 13f)
                verticalLineTo(11f)
                horizontalLineTo(11f)
                verticalLineTo(13f)
                horizontalLineTo(21f)
                close()
                moveTo(15f, 9f)
                horizontalLineTo(17f)
                verticalLineTo(7f)
                horizontalLineTo(21f)
                verticalLineTo(5f)
                horizontalLineTo(17f)
                verticalLineTo(3f)
                horizontalLineTo(15f)
                verticalLineTo(9f)
                close()
            }
        }
    }

    val TuneFilled: ImageVector by lazy {
        icon("TuneFilled") {
            path(fill = black) {
                moveTo(3f, 16.75f)
                verticalLineTo(19.25f)
                horizontalLineTo(9.5f)
                verticalLineTo(16.75f)
                close()
                moveTo(3f, 4.75f)
                verticalLineTo(7.25f)
                horizontalLineTo(13.5f)
                verticalLineTo(4.75f)
                close()
                moveTo(13.5f, 21f)
                verticalLineTo(19.25f)
                horizontalLineTo(21f)
                verticalLineTo(16.75f)
                horizontalLineTo(13.5f)
                verticalLineTo(15f)
                horizontalLineTo(11f)
                verticalLineTo(21f)
                close()
                moveTo(6.5f, 9f)
                verticalLineTo(10.75f)
                horizontalLineTo(3f)
                verticalLineTo(13.25f)
                horizontalLineTo(6.5f)
                verticalLineTo(15f)
                horizontalLineTo(9f)
                verticalLineTo(9f)
                close()
                moveTo(21f, 13.25f)
                verticalLineTo(10.75f)
                horizontalLineTo(10.5f)
                verticalLineTo(13.25f)
                close()
                moveTo(15f, 9f)
                horizontalLineTo(17.5f)
                verticalLineTo(7.25f)
                horizontalLineTo(21f)
                verticalLineTo(4.75f)
                horizontalLineTo(17.5f)
                verticalLineTo(3f)
                horizontalLineTo(15f)
                close()
            }
        }
    }

    val Check: ImageVector by lazy {
        icon("Check") {
            path(fill = black) {
                moveTo(9.55f, 18f)
                lineTo(3.85f, 12.3f)
                lineTo(5.275f, 10.875f)
                lineTo(9.55f, 15.15f)
                lineTo(18.725f, 5.975f)
                lineTo(20.15f, 7.4f)
                close()
            }
        }
    }

    val CheckCircle: ImageVector by lazy {
        icon("CheckCircle") {
            path(fill = black) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                close()
                moveTo(10f, 17f)
                lineTo(5f, 12f)
                lineTo(6.41f, 10.59f)
                lineTo(10f, 14.17f)
                lineTo(17.59f, 6.58f)
                lineTo(19f, 8f)
                close()
            }
        }
    }

    val Close: ImageVector by lazy {
        icon("Close") {
            path(fill = black) {
                moveTo(6.4f, 19f)
                lineTo(5f, 17.6f)
                lineTo(10.6f, 12f)
                lineTo(5f, 6.4f)
                lineTo(6.4f, 5f)
                lineTo(12f, 10.6f)
                lineTo(17.6f, 5f)
                lineTo(19f, 6.4f)
                lineTo(13.4f, 12f)
                lineTo(19f, 17.6f)
                lineTo(17.6f, 19f)
                lineTo(12f, 13.4f)
                close()
            }
        }
    }

    val Copy: ImageVector by lazy {
        icon("Copy") {
            path(fill = black) {
                moveTo(9f, 18f)
                curveTo(8.45f, 18f, 7.98f, 17.8f, 7.59f, 17.41f)
                curveTo(7.2f, 17.02f, 7f, 16.55f, 7f, 16f)
                verticalLineTo(4f)
                curveTo(7f, 3.45f, 7.2f, 2.98f, 7.59f, 2.59f)
                curveTo(7.98f, 2.2f, 8.45f, 2f, 9f, 2f)
                horizontalLineTo(18f)
                curveTo(18.55f, 2f, 19.02f, 2.2f, 19.41f, 2.59f)
                curveTo(19.8f, 2.98f, 20f, 3.45f, 20f, 4f)
                verticalLineTo(16f)
                curveTo(20f, 16.55f, 19.8f, 17.02f, 19.41f, 17.41f)
                curveTo(19.02f, 17.8f, 18.55f, 18f, 18f, 18f)
                close()
                moveTo(9f, 16f)
                horizontalLineTo(18f)
                verticalLineTo(4f)
                horizontalLineTo(9f)
                close()
                moveTo(5f, 22f)
                curveTo(4.45f, 22f, 3.98f, 21.8f, 3.59f, 21.41f)
                curveTo(3.2f, 21.02f, 3f, 20.55f, 3f, 20f)
                verticalLineTo(6f)
                horizontalLineTo(5f)
                verticalLineTo(20f)
                horizontalLineTo(16f)
                verticalLineTo(22f)
                close()
            }
        }
    }

    val Refresh: ImageVector by lazy {
        icon("Refresh") {
            path(fill = black) {
                moveTo(12f, 20f)
                curveTo(9.77f, 20f, 7.88f, 19.22f, 6.33f, 17.67f)
                curveTo(4.78f, 16.12f, 4f, 14.23f, 4f, 12f)
                curveTo(4f, 9.77f, 4.78f, 7.88f, 6.33f, 6.33f)
                curveTo(7.88f, 4.78f, 9.77f, 4f, 12f, 4f)
                curveTo(13.15f, 4f, 14.25f, 4.24f, 15.3f, 4.71f)
                curveTo(16.35f, 5.19f, 17.25f, 5.87f, 18f, 6.75f)
                verticalLineTo(4f)
                horizontalLineTo(20f)
                verticalLineTo(11f)
                horizontalLineTo(13f)
                verticalLineTo(9f)
                horizontalLineTo(17.2f)
                curveTo(16.67f, 8.07f, 15.94f, 7.33f, 15.01f, 6.8f)
                curveTo(14.09f, 6.27f, 13.08f, 6f, 12f, 6f)
                curveTo(10.33f, 6f, 8.92f, 6.58f, 7.75f, 7.75f)
                curveTo(6.58f, 8.92f, 6f, 10.33f, 6f, 12f)
                curveTo(6f, 13.67f, 6.58f, 15.08f, 7.75f, 16.25f)
                curveTo(8.92f, 17.42f, 10.33f, 18f, 12f, 18f)
                curveTo(13.28f, 18f, 14.44f, 17.63f, 15.48f, 16.9f)
                curveTo(16.51f, 16.17f, 17.23f, 15.2f, 17.65f, 14f)
                horizontalLineTo(19.75f)
                curveTo(19.28f, 15.77f, 18.35f, 17.21f, 16.95f, 18.33f)
                curveTo(15.55f, 19.44f, 13.9f, 20f, 12f, 20f)
                close()
            }
        }
    }

    val Power: ImageVector by lazy {
        icon("Power") {
            path(fill = black) {
                moveTo(11f, 3f)
                horizontalLineTo(13f)
                verticalLineTo(12f)
                horizontalLineTo(11f)
                close()
                moveTo(12f, 21f)
                curveTo(7.58f, 21f, 4f, 17.42f, 4f, 13f)
                curveTo(4f, 10.22f, 5.42f, 7.77f, 7.58f, 6.33f)
                lineTo(8.7f, 7.99f)
                curveTo(7.07f, 9.07f, 6f, 10.91f, 6f, 13f)
                curveTo(6f, 16.31f, 8.69f, 19f, 12f, 19f)
                curveTo(15.31f, 19f, 18f, 16.31f, 18f, 13f)
                curveTo(18f, 10.91f, 16.93f, 9.07f, 15.3f, 7.99f)
                lineTo(16.42f, 6.33f)
                curveTo(18.58f, 7.77f, 20f, 10.22f, 20f, 13f)
                curveTo(20f, 17.42f, 16.42f, 21f, 12f, 21f)
                close()
            }
        }
    }

    val StopCircle: ImageVector by lazy {
        icon("StopCircle") {
            path(fill = black) {
                moveTo(8f, 8f)
                horizontalLineTo(16f)
                verticalLineTo(16f)
                horizontalLineTo(8f)
                close()
                moveTo(12f, 22f)
                curveTo(6.48f, 22f, 2f, 17.52f, 2f, 12f)
                curveTo(2f, 6.48f, 6.48f, 2f, 12f, 2f)
                curveTo(17.52f, 2f, 22f, 6.48f, 22f, 12f)
                curveTo(22f, 17.52f, 17.52f, 22f, 12f, 22f)
                close()
                moveTo(12f, 20f)
                curveTo(16.42f, 20f, 20f, 16.42f, 20f, 12f)
                curveTo(20f, 7.58f, 16.42f, 4f, 12f, 4f)
                curveTo(7.58f, 4f, 4f, 7.58f, 4f, 12f)
                curveTo(4f, 16.42f, 7.58f, 20f, 12f, 20f)
                close()
            }
        }
    }

    val Visibility: ImageVector by lazy {
        icon("Visibility") {
            path(fill = black) {
                moveTo(12f, 16f)
                curveTo(13.25f, 16f, 14.31f, 15.56f, 15.19f, 14.69f)
                curveTo(16.06f, 13.81f, 16.5f, 12.75f, 16.5f, 11.5f)
                curveTo(16.5f, 10.25f, 16.06f, 9.19f, 15.19f, 8.31f)
                curveTo(14.31f, 7.44f, 13.25f, 7f, 12f, 7f)
                curveTo(10.75f, 7f, 9.69f, 7.44f, 8.81f, 8.31f)
                curveTo(7.94f, 9.19f, 7.5f, 10.25f, 7.5f, 11.5f)
                curveTo(7.5f, 12.75f, 7.94f, 13.81f, 8.81f, 14.69f)
                curveTo(9.69f, 15.56f, 10.75f, 16f, 12f, 16f)
                close()
                moveTo(12f, 14.2f)
                curveTo(11.25f, 14.2f, 10.61f, 13.94f, 10.09f, 13.41f)
                curveTo(9.56f, 12.89f, 9.3f, 12.25f, 9.3f, 11.5f)
                curveTo(9.3f, 10.75f, 9.56f, 10.11f, 10.09f, 9.59f)
                curveTo(10.61f, 9.06f, 11.25f, 8.8f, 12f, 8.8f)
                curveTo(12.75f, 8.8f, 13.39f, 9.06f, 13.91f, 9.59f)
                curveTo(14.44f, 10.11f, 14.7f, 10.75f, 14.7f, 11.5f)
                curveTo(14.7f, 12.25f, 14.44f, 12.89f, 13.91f, 13.41f)
                curveTo(13.39f, 13.94f, 12.75f, 14.2f, 12f, 14.2f)
                close()
                moveTo(12f, 19f)
                curveTo(9.57f, 19f, 7.35f, 18.32f, 5.35f, 16.96f)
                curveTo(3.35f, 15.6f, 1.9f, 13.78f, 1f, 11.5f)
                curveTo(1.9f, 9.22f, 3.35f, 7.4f, 5.35f, 6.04f)
                curveTo(7.35f, 4.68f, 9.57f, 4f, 12f, 4f)
                curveTo(14.43f, 4f, 16.65f, 4.68f, 18.65f, 6.04f)
                curveTo(20.65f, 7.4f, 22.1f, 9.22f, 23f, 11.5f)
                curveTo(22.1f, 13.78f, 20.65f, 15.6f, 18.65f, 16.96f)
                curveTo(16.65f, 18.32f, 14.43f, 19f, 12f, 19f)
                close()
                moveTo(12f, 17f)
                curveTo(13.95f, 17f, 15.73f, 16.49f, 17.35f, 15.46f)
                curveTo(18.97f, 14.44f, 20.12f, 13.12f, 20.8f, 11.5f)
                curveTo(20.12f, 9.88f, 18.97f, 8.56f, 17.35f, 7.54f)
                curveTo(15.73f, 6.51f, 13.95f, 6f, 12f, 6f)
                curveTo(10.05f, 6f, 8.27f, 6.51f, 6.65f, 7.54f)
                curveTo(5.03f, 8.56f, 3.88f, 9.88f, 3.2f, 11.5f)
                curveTo(3.88f, 13.12f, 5.03f, 14.44f, 6.65f, 15.46f)
                curveTo(8.27f, 16.49f, 10.05f, 17f, 12f, 17f)
                close()
            }
        }
    }

    val VisibilityOff: ImageVector by lazy {
        icon("VisibilityOff") {
            path(fill = black) {
                moveTo(12f, 7f)
                curveTo(14.76f, 7f, 17f, 9.24f, 17f, 12f)
                curveTo(17f, 12.65f, 16.87f, 13.26f, 16.64f, 13.83f)
                lineTo(19.56f, 16.75f)
                curveTo(21.07f, 15.49f, 22.26f, 13.86f, 23f, 12f)
                curveTo(21.27f, 7.61f, 17f, 4.5f, 12f, 4.5f)
                curveTo(10.6f, 4.5f, 9.26f, 4.75f, 8.02f, 5.2f)
                lineTo(10.19f, 7.37f)
                curveTo(10.74f, 7.13f, 11.35f, 7f, 12f, 7f)
                close()
                moveTo(2f, 4.27f)
                lineTo(4.28f, 6.55f)
                lineTo(4.74f, 7.01f)
                curveTo(3.08f, 8.3f, 1.78f, 10.02f, 1f, 12f)
                curveTo(2.73f, 16.39f, 7f, 19.5f, 12f, 19.5f)
                curveTo(13.55f, 19.5f, 15.03f, 19.2f, 16.38f, 18.66f)
                lineTo(16.8f, 19.08f)
                lineTo(19.73f, 22f)
                lineTo(21f, 20.73f)
                lineTo(3.27f, 3f)
                close()
                moveTo(7.53f, 9.8f)
                lineTo(9.08f, 11.35f)
                curveTo(9.03f, 11.56f, 9f, 11.78f, 9f, 12f)
                curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
                curveTo(12.22f, 15f, 12.44f, 14.97f, 12.65f, 14.92f)
                lineTo(14.2f, 16.47f)
                curveTo(13.53f, 16.8f, 12.79f, 17f, 12f, 17f)
                curveTo(9.24f, 17f, 7f, 14.76f, 7f, 12f)
                curveTo(7f, 11.21f, 7.2f, 10.47f, 7.53f, 9.8f)
                close()
            }
        }
    }

    val Key: ImageVector by lazy {
        icon("Key") {
            path(fill = black, pathFillType = PathFillType.EvenOdd) {
                moveTo(12.65f, 10f)
                curveTo(11.83f, 7.67f, 9.61f, 6f, 7f, 6f)
                curveTo(3.69f, 6f, 1f, 8.69f, 1f, 12f)
                curveTo(1f, 15.31f, 3.69f, 18f, 7f, 18f)
                curveTo(9.61f, 18f, 11.83f, 16.33f, 12.65f, 14f)
                horizontalLineTo(17f)
                verticalLineTo(17f)
                horizontalLineTo(19f)
                verticalLineTo(14f)
                horizontalLineTo(21f)
                verticalLineTo(17f)
                horizontalLineTo(23f)
                verticalLineTo(10f)
                horizontalLineTo(12.65f)
                close()
                moveTo(7f, 14.5f)
                curveTo(5.62f, 14.5f, 4.5f, 13.38f, 4.5f, 12f)
                curveTo(4.5f, 10.62f, 5.62f, 9.5f, 7f, 9.5f)
                curveTo(8.38f, 9.5f, 9.5f, 10.62f, 9.5f, 12f)
                curveTo(9.5f, 13.38f, 8.38f, 14.5f, 7f, 14.5f)
                close()
            }
        }
    }

    val Link: ImageVector by lazy {
        icon("Link") {
            path(fill = black) {
                moveTo(11f, 17f)
                horizontalLineTo(7f)
                curveTo(5.62f, 17f, 4.44f, 16.51f, 3.46f, 15.54f)
                curveTo(2.49f, 14.56f, 2f, 13.38f, 2f, 12f)
                curveTo(2f, 10.62f, 2.49f, 9.44f, 3.46f, 8.46f)
                curveTo(4.44f, 7.49f, 5.62f, 7f, 7f, 7f)
                horizontalLineTo(11f)
                verticalLineTo(9f)
                horizontalLineTo(7f)
                curveTo(6.17f, 9f, 5.46f, 9.29f, 4.88f, 9.88f)
                curveTo(4.29f, 10.46f, 4f, 11.17f, 4f, 12f)
                curveTo(4f, 12.83f, 4.29f, 13.54f, 4.88f, 14.12f)
                curveTo(5.46f, 14.71f, 6.17f, 15f, 7f, 15f)
                horizontalLineTo(11f)
                close()
                moveTo(8f, 13f)
                verticalLineTo(11f)
                horizontalLineTo(16f)
                verticalLineTo(13f)
                close()
                moveTo(13f, 17f)
                verticalLineTo(15f)
                horizontalLineTo(17f)
                curveTo(17.83f, 15f, 18.54f, 14.71f, 19.12f, 14.12f)
                curveTo(19.71f, 13.54f, 20f, 12.83f, 20f, 12f)
                curveTo(20f, 11.17f, 19.71f, 10.46f, 19.12f, 9.88f)
                curveTo(18.54f, 9.29f, 17.83f, 9f, 17f, 9f)
                horizontalLineTo(13f)
                verticalLineTo(7f)
                horizontalLineTo(17f)
                curveTo(18.38f, 7f, 19.56f, 7.49f, 20.54f, 8.46f)
                curveTo(21.51f, 9.44f, 22f, 10.62f, 22f, 12f)
                curveTo(22f, 13.38f, 21.51f, 14.56f, 20.54f, 15.54f)
                curveTo(19.56f, 16.51f, 18.38f, 17f, 17f, 17f)
                close()
            }
        }
    }

    val Shield: ImageVector by lazy {
        icon("Shield") {
            path(fill = black) {
                moveTo(12f, 22f)
                curveTo(9.68f, 21.42f, 7.77f, 20.09f, 6.26f, 18.01f)
                curveTo(4.75f, 15.94f, 4f, 13.63f, 4f, 11.1f)
                verticalLineTo(5f)
                lineTo(12f, 2f)
                lineTo(20f, 5f)
                verticalLineTo(11.1f)
                curveTo(20f, 13.63f, 19.25f, 15.94f, 17.74f, 18.01f)
                curveTo(16.23f, 20.09f, 14.32f, 21.42f, 12f, 22f)
                close()
                moveTo(12f, 19.9f)
                curveTo(13.73f, 19.35f, 15.17f, 18.25f, 16.3f, 16.6f)
                curveTo(17.43f, 14.95f, 18f, 13.12f, 18f, 11.1f)
                verticalLineTo(6.38f)
                lineTo(12f, 4.12f)
                lineTo(6f, 6.38f)
                verticalLineTo(11.1f)
                curveTo(6f, 13.12f, 6.57f, 14.95f, 7.7f, 16.6f)
                curveTo(8.83f, 18.25f, 10.27f, 19.35f, 12f, 19.9f)
                close()
            }
        }
    }

    val Terminal: ImageVector by lazy {
        icon("Terminal") {
            path(fill = black) {
                moveTo(4f, 20f)
                curveTo(3.45f, 20f, 2.98f, 19.8f, 2.59f, 19.41f)
                curveTo(2.2f, 19.02f, 2f, 18.55f, 2f, 18f)
                verticalLineTo(6f)
                curveTo(2f, 5.45f, 2.2f, 4.98f, 2.59f, 4.59f)
                curveTo(2.98f, 4.2f, 3.45f, 4f, 4f, 4f)
                horizontalLineTo(20f)
                curveTo(20.55f, 4f, 21.02f, 4.2f, 21.41f, 4.59f)
                curveTo(21.8f, 4.98f, 22f, 5.45f, 22f, 6f)
                verticalLineTo(18f)
                curveTo(22f, 18.55f, 21.8f, 19.02f, 21.41f, 19.41f)
                curveTo(21.02f, 19.8f, 20.55f, 20f, 20f, 20f)
                close()
                moveTo(4f, 18f)
                horizontalLineTo(20f)
                verticalLineTo(8f)
                horizontalLineTo(4f)
                close()
                moveTo(7.5f, 16.5f)
                lineTo(6.1f, 15.1f)
                lineTo(8.7f, 12.5f)
                lineTo(6.1f, 9.9f)
                lineTo(7.5f, 8.5f)
                lineTo(11.5f, 12.5f)
                close()
                moveTo(12f, 16f)
                verticalLineTo(14f)
                horizontalLineTo(18f)
                verticalLineTo(16f)
                close()
            }
        }
    }

    val ChatBubble: ImageVector by lazy {
        icon("ChatBubble") {
            path(fill = black) {
                moveTo(4f, 22f)
                verticalLineTo(5f)
                curveTo(4f, 4.45f, 4.2f, 3.98f, 4.59f, 3.59f)
                curveTo(4.98f, 3.2f, 5.45f, 3f, 6f, 3f)
                horizontalLineTo(18f)
                curveTo(18.55f, 3f, 19.02f, 3.2f, 19.41f, 3.59f)
                curveTo(19.8f, 3.98f, 20f, 4.45f, 20f, 5f)
                verticalLineTo(15f)
                curveTo(20f, 15.55f, 19.8f, 16.02f, 19.41f, 16.41f)
                curveTo(19.02f, 16.8f, 18.55f, 17f, 18f, 17f)
                horizontalLineTo(8f)
                close()
                moveTo(7.15f, 15f)
                horizontalLineTo(18f)
                verticalLineTo(5f)
                horizontalLineTo(6f)
                verticalLineTo(16.12f)
                close()
            }
        }
    }

    val DualApps: ImageVector by lazy {
        icon("DualApps") {
            path(fill = black) {
                moveTo(4f, 18f)
                curveTo(3.45f, 18f, 2.98f, 17.8f, 2.59f, 17.41f)
                curveTo(2.2f, 17.02f, 2f, 16.55f, 2f, 16f)
                verticalLineTo(6f)
                curveTo(2f, 5.45f, 2.2f, 4.98f, 2.59f, 4.59f)
                curveTo(2.98f, 4.2f, 3.45f, 4f, 4f, 4f)
                horizontalLineTo(14f)
                curveTo(14.55f, 4f, 15.02f, 4.2f, 15.41f, 4.59f)
                curveTo(15.8f, 4.98f, 16f, 5.45f, 16f, 6f)
                verticalLineTo(16f)
                curveTo(16f, 16.55f, 15.8f, 17.02f, 15.41f, 17.41f)
                curveTo(15.02f, 17.8f, 14.55f, 18f, 14f, 18f)
                close()
                moveTo(4f, 16f)
                horizontalLineTo(14f)
                verticalLineTo(6f)
                horizontalLineTo(4f)
                close()
                moveTo(8f, 22f)
                verticalLineTo(20f)
                horizontalLineTo(18f)
                verticalLineTo(10f)
                horizontalLineTo(20f)
                verticalLineTo(20f)
                curveTo(20f, 20.55f, 19.8f, 21.02f, 19.41f, 21.41f)
                curveTo(19.02f, 21.8f, 18.55f, 22f, 18f, 22f)
                close()
            }
        }
    }

    val Palette: ImageVector by lazy {
        icon("Palette") {
            path(fill = black) {
                moveTo(12f, 22f)
                curveTo(6.49f, 22f, 2f, 17.51f, 2f, 12f)
                curveTo(2f, 6.49f, 6.49f, 2f, 12f, 2f)
                curveTo(17.51f, 2f, 22f, 6.04f, 22f, 11f)
                curveTo(22f, 14.31f, 19.31f, 17f, 16f, 17f)
                horizontalLineTo(14.23f)
                curveTo(13.84f, 17f, 13.52f, 17.32f, 13.52f, 17.71f)
                curveTo(13.52f, 17.89f, 13.59f, 18.06f, 13.71f, 18.19f)
                curveTo(14.08f, 18.6f, 14.3f, 19.14f, 14.3f, 19.73f)
                curveTo(14.3f, 20.98f, 13.27f, 22f, 12f, 22f)
                close()
                moveTo(12f, 4f)
                curveTo(7.59f, 4f, 4f, 7.59f, 4f, 12f)
                curveTo(4f, 16.41f, 7.59f, 20f, 12f, 20f)
                curveTo(12.17f, 20f, 12.3f, 19.87f, 12.3f, 19.73f)
                curveTo(12.3f, 19.65f, 12.27f, 19.58f, 12.22f, 19.52f)
                curveTo(11.77f, 19.02f, 11.52f, 18.38f, 11.52f, 17.71f)
                curveTo(11.52f, 16.22f, 12.74f, 15f, 14.23f, 15f)
                horizontalLineTo(16f)
                curveTo(18.21f, 15f, 20f, 13.21f, 20f, 11f)
                curveTo(20f, 7.14f, 16.41f, 4f, 12f, 4f)
                close()
                moveTo(6.5f, 12.5f)
                curveTo(5.67f, 12.5f, 5f, 11.83f, 5f, 11f)
                curveTo(5f, 10.17f, 5.67f, 9.5f, 6.5f, 9.5f)
                curveTo(7.33f, 9.5f, 8f, 10.17f, 8f, 11f)
                curveTo(8f, 11.83f, 7.33f, 12.5f, 6.5f, 12.5f)
                close()
                moveTo(9.5f, 8.5f)
                curveTo(8.67f, 8.5f, 8f, 7.83f, 8f, 7f)
                curveTo(8f, 6.17f, 8.67f, 5.5f, 9.5f, 5.5f)
                curveTo(10.33f, 5.5f, 11f, 6.17f, 11f, 7f)
                curveTo(11f, 7.83f, 10.33f, 8.5f, 9.5f, 8.5f)
                close()
                moveTo(14.5f, 8.5f)
                curveTo(13.67f, 8.5f, 13f, 7.83f, 13f, 7f)
                curveTo(13f, 6.17f, 13.67f, 5.5f, 14.5f, 5.5f)
                curveTo(15.33f, 5.5f, 16f, 6.17f, 16f, 7f)
                curveTo(16f, 7.83f, 15.33f, 8.5f, 14.5f, 8.5f)
                close()
                moveTo(17.5f, 12.5f)
                curveTo(16.67f, 12.5f, 16f, 11.83f, 16f, 11f)
                curveTo(16f, 10.17f, 16.67f, 9.5f, 17.5f, 9.5f)
                curveTo(18.33f, 9.5f, 19f, 10.17f, 19f, 11f)
                curveTo(19f, 11.83f, 18.33f, 12.5f, 17.5f, 12.5f)
                close()
            }
        }
    }

    val ErrorOutline: ImageVector by lazy {
        icon("ErrorOutline") {
            path(fill = black) {
                moveTo(12f, 17f)
                curveTo(12.28f, 17f, 12.52f, 16.9f, 12.71f, 16.71f)
                curveTo(12.9f, 16.52f, 13f, 16.28f, 13f, 16f)
                curveTo(13f, 15.72f, 12.9f, 15.48f, 12.71f, 15.29f)
                curveTo(12.52f, 15.1f, 12.28f, 15f, 12f, 15f)
                curveTo(11.72f, 15f, 11.48f, 15.1f, 11.29f, 15.29f)
                curveTo(11.1f, 15.48f, 11f, 15.72f, 11f, 16f)
                curveTo(11f, 16.28f, 11.1f, 16.52f, 11.29f, 16.71f)
                curveTo(11.48f, 16.9f, 11.72f, 17f, 12f, 17f)
                close()
                moveTo(11f, 13f)
                horizontalLineTo(13f)
                verticalLineTo(7f)
                horizontalLineTo(11f)
                close()
                moveTo(12f, 22f)
                curveTo(6.48f, 22f, 2f, 17.52f, 2f, 12f)
                curveTo(2f, 6.48f, 6.48f, 2f, 12f, 2f)
                curveTo(17.52f, 2f, 22f, 6.48f, 22f, 12f)
                curveTo(22f, 17.52f, 17.52f, 22f, 12f, 22f)
                close()
                moveTo(12f, 20f)
                curveTo(16.42f, 20f, 20f, 16.42f, 20f, 12f)
                curveTo(20f, 7.58f, 16.42f, 4f, 12f, 4f)
                curveTo(7.58f, 4f, 4f, 7.58f, 4f, 12f)
                curveTo(4f, 16.42f, 7.58f, 20f, 12f, 20f)
                close()
            }
        }
    }

    val ChevronRight: ImageVector by lazy {
        icon("ChevronRight") {
            path(fill = black) {
                moveTo(9.4f, 18f)
                lineTo(8f, 16.6f)
                lineTo(12.6f, 12f)
                lineTo(8f, 7.4f)
                lineTo(9.4f, 6f)
                lineTo(15.4f, 12f)
                close()
            }
        }
    }

    val ExpandMore: ImageVector by lazy {
        icon("ExpandMore") {
            path(fill = black) {
                moveTo(12f, 15.4f)
                lineTo(6f, 9.4f)
                lineTo(7.4f, 8f)
                lineTo(12f, 12.6f)
                lineTo(16.6f, 8f)
                lineTo(18f, 9.4f)
                close()
            }
        }
    }

    val ExpandLess: ImageVector by lazy {
        icon("ExpandLess") {
            path(fill = black) {
                moveTo(7.4f, 15.4f)
                lineTo(6f, 14f)
                lineTo(12f, 8f)
                lineTo(18f, 14f)
                lineTo(16.6f, 15.4f)
                lineTo(12f, 10.8f)
                close()
            }
        }
    }

    val Bolt: ImageVector by lazy {
        icon("Bolt") {
            path(fill = black) {
                moveTo(11f, 21f)
                lineTo(12f, 14f)
                horizontalLineTo(7f)
                lineTo(13f, 3f)
                horizontalLineTo(14f)
                lineTo(13f, 10f)
                horizontalLineTo(18f)
                lineTo(12f, 21f)
                close()
            }
        }
    }

    val Delete: ImageVector by lazy {
        icon("Delete") {
            path(fill = black) {
                moveTo(7f, 21f)
                curveTo(6.45f, 21f, 5.98f, 20.8f, 5.59f, 20.41f)
                curveTo(5.2f, 20.02f, 5f, 19.55f, 5f, 19f)
                verticalLineTo(6f)
                horizontalLineTo(4f)
                verticalLineTo(4f)
                horizontalLineTo(9f)
                verticalLineTo(3f)
                horizontalLineTo(15f)
                verticalLineTo(4f)
                horizontalLineTo(20f)
                verticalLineTo(6f)
                horizontalLineTo(19f)
                verticalLineTo(19f)
                curveTo(19f, 19.55f, 18.8f, 20.02f, 18.41f, 20.41f)
                curveTo(18.02f, 20.8f, 17.55f, 21f, 17f, 21f)
                close()
                moveTo(17f, 6f)
                horizontalLineTo(7f)
                verticalLineTo(19f)
                horizontalLineTo(17f)
                close()
                moveTo(9f, 17f)
                horizontalLineTo(11f)
                verticalLineTo(8f)
                horizontalLineTo(9f)
                close()
                moveTo(13f, 17f)
                horizontalLineTo(15f)
                verticalLineTo(8f)
                horizontalLineTo(13f)
                close()
            }
        }
    }

    val Settings: ImageVector by lazy {
        icon("Settings") {
            path(
                fill = null,
                stroke = black,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 15f)
                curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
                curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
                curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
                curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
                close()
                moveTo(19.4f, 15f)
                curveTo(19.12f, 15.63f, 19.27f, 16.36f, 19.76f, 16.84f)
                lineTo(19.82f, 16.9f)
                curveTo(20.53f, 17.61f, 20.53f, 18.77f, 19.82f, 19.48f)
                curveTo(19.11f, 20.19f, 17.95f, 20.19f, 17.24f, 19.48f)
                lineTo(17.18f, 19.42f)
                curveTo(16.7f, 18.93f, 15.97f, 18.78f, 15.34f, 19.06f)
                curveTo(14.72f, 19.33f, 14.32f, 19.95f, 14.32f, 20.63f)
                verticalLineTo(20.72f)
                curveTo(14.32f, 21.72f, 13.5f, 22.54f, 12.5f, 22.54f)
                curveTo(11.5f, 22.54f, 10.68f, 21.72f, 10.68f, 20.72f)
                verticalLineTo(20.63f)
                curveTo(10.66f, 19.92f, 10.22f, 19.3f, 9.57f, 19.05f)
                curveTo(8.94f, 18.77f, 8.21f, 18.92f, 7.73f, 19.41f)
                lineTo(7.67f, 19.47f)
                curveTo(6.96f, 20.18f, 5.8f, 20.18f, 5.09f, 19.47f)
                curveTo(4.38f, 18.76f, 4.38f, 17.6f, 5.09f, 16.89f)
                lineTo(5.15f, 16.83f)
                curveTo(5.64f, 16.35f, 5.79f, 15.62f, 5.51f, 14.99f)
                curveTo(5.24f, 14.37f, 4.62f, 13.97f, 3.94f, 13.97f)
                horizontalLineTo(3.85f)
                curveTo(2.85f, 13.97f, 2.03f, 13.15f, 2.03f, 12.15f)
                curveTo(2.03f, 11.15f, 2.85f, 10.33f, 3.85f, 10.33f)
                horizontalLineTo(3.94f)
                curveTo(4.65f, 10.31f, 5.27f, 9.87f, 5.52f, 9.22f)
                curveTo(5.8f, 8.59f, 5.65f, 7.86f, 5.16f, 7.38f)
                lineTo(5.1f, 7.32f)
                curveTo(4.39f, 6.61f, 4.39f, 5.45f, 5.1f, 4.74f)
                curveTo(5.81f, 4.03f, 6.97f, 4.03f, 7.68f, 4.74f)
                lineTo(7.74f, 4.8f)
                curveTo(8.22f, 5.29f, 8.95f, 5.44f, 9.58f, 5.16f)
                horizontalLineTo(9.65f)
                curveTo(10.27f, 4.89f, 10.67f, 4.27f, 10.67f, 3.59f)
                verticalLineTo(3.5f)
                curveTo(10.67f, 2.5f, 11.49f, 1.68f, 12.49f, 1.68f)
                curveTo(13.49f, 1.68f, 14.31f, 2.5f, 14.31f, 3.5f)
                verticalLineTo(3.59f)
                curveTo(14.31f, 4.27f, 14.71f, 4.89f, 15.33f, 5.16f)
                curveTo(15.96f, 5.44f, 16.69f, 5.29f, 17.17f, 4.8f)
                lineTo(17.23f, 4.74f)
                curveTo(17.94f, 4.03f, 19.1f, 4.03f, 19.81f, 4.74f)
                curveTo(20.52f, 5.45f, 20.52f, 6.61f, 19.81f, 7.32f)
                lineTo(19.75f, 7.38f)
                curveTo(19.26f, 7.86f, 19.11f, 8.59f, 19.39f, 9.22f)
                verticalLineTo(9.29f)
                curveTo(19.66f, 9.91f, 20.28f, 10.31f, 20.96f, 10.31f)
                horizontalLineTo(21.05f)
                curveTo(22.05f, 10.31f, 22.87f, 11.13f, 22.87f, 12.13f)
                curveTo(22.87f, 13.13f, 22.05f, 13.95f, 21.05f, 13.95f)
                horizontalLineTo(20.96f)
                curveTo(20.28f, 13.95f, 19.66f, 14.35f, 19.39f, 14.97f)
                close()
            }
        }
    }

    val History: ImageVector by lazy {
        icon("History") {
            path(fill = black) {
                moveTo(12f, 21f)
                curveTo(9.63f, 21f, 7.6f, 20.19f, 5.9f, 18.56f)
                curveTo(4.2f, 16.94f, 3.24f, 14.92f, 3.03f, 12.5f)
                horizontalLineTo(5.05f)
                curveTo(5.25f, 14.37f, 6.02f, 15.92f, 7.35f, 17.15f)
                curveTo(8.68f, 18.38f, 10.23f, 19f, 12f, 19f)
                curveTo(13.95f, 19f, 15.6f, 18.32f, 16.96f, 16.96f)
                curveTo(18.32f, 15.6f, 19f, 13.95f, 19f, 12f)
                curveTo(19f, 10.05f, 18.32f, 8.4f, 16.96f, 7.04f)
                curveTo(15.6f, 5.68f, 13.95f, 5f, 12f, 5f)
                curveTo(10.85f, 5f, 9.77f, 5.27f, 8.78f, 5.8f)
                curveTo(7.78f, 6.33f, 6.94f, 7.07f, 6.25f, 8f)
                horizontalLineTo(9f)
                verticalLineTo(10f)
                horizontalLineTo(3f)
                verticalLineTo(4f)
                horizontalLineTo(5f)
                verticalLineTo(6.35f)
                curveTo(5.85f, 5.28f, 6.89f, 4.46f, 8.11f, 3.88f)
                curveTo(9.34f, 3.29f, 10.63f, 3f, 12f, 3f)
                curveTo(14.5f, 3f, 16.63f, 3.88f, 18.38f, 5.63f)
                curveTo(20.13f, 7.38f, 21f, 9.5f, 21f, 12f)
                curveTo(21f, 14.5f, 20.13f, 16.63f, 18.38f, 18.38f)
                curveTo(16.63f, 20.13f, 14.5f, 21f, 12f, 21f)
                close()
                moveTo(14.8f, 16.2f)
                lineTo(11f, 12.4f)
                verticalLineTo(7f)
                horizontalLineTo(13f)
                verticalLineTo(11.6f)
                lineTo(16.2f, 14.8f)
                close()
            }
        }
    }
}
