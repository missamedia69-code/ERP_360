package com.missa.b360.ui.stock

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Icônes du module Stock — jeu Material Design Icons servi par IconVaultKit
 * (iconvaultkit.com), converti en ImageVector Compose.
 *
 * Toujours teinter via `Icon(tint = ...)` ; le remplissage noir n'est qu'un
 * gabarit.
 */
object StockIv {

    /** plus — jeu Material Design Icons (IconVaultKit). */
    val Add: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvAdd",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 13f)
                relativeHorizontalLineTo(-6f)
                relativeVerticalLineTo(6f)
                relativeHorizontalLineTo(-2f)
                relativeVerticalLineTo(-6f)
                horizontalLineTo(5f)
                relativeVerticalLineTo(-2f)
                relativeHorizontalLineTo(6f)
                verticalLineTo(5f)
                relativeHorizontalLineTo(2f)
                relativeVerticalLineTo(6f)
                relativeHorizontalLineTo(6f)
                close()
            }
        }.build()
    }

    /** wrench-outline — jeu Material Design Icons (IconVaultKit). */
    val Build: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvBuild",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                relativeMoveTo(22.61f, 19f)
                relativeLineTo(-9.08f, -9.09f)
                relativeCurveTo(0.93f, -2.34f, 0.47f, -5.1f, -1.44f, -7f)
                curveTo(9.79f, 0.61f, 6.21f, 0.4f, 3.66f, 2.26f)
                lineTo(7.5f, 6.11f)
                lineTo(6.08f, 7.5f)
                lineTo(2.25f, 3.69f)
                curveTo(0.39f, 6.23f, 0.6f, 9.82f, 2.9f, 12.11f)
                relativeCurveTo(1.86f, 1.86f, 4.57f, 2.35f, 6.89f, 1.48f)
                relativeLineTo(9.11f, 9.11f)
                relativeCurveTo(0.39f, 0.39f, 1.02f, 0.39f, 1.41f, 0f)
                relativeLineTo(2.3f, -2.3f)
                relativeCurveTo(0.39f, -0.4f, 0.39f, -1.01f, 0f, -1.4f)
                relativeLineTo(-3f, 1.59f)
                relativeLineTo(-9.46f, -9.46f)
                relativeCurveTo(-0.61f, 0.45f, -1.29f, 0.72f, -2f, 0.82f)
                relativeCurveTo(-1.36f, 0.2f, -2.79f, -0.21f, -3.83f, -1.25f)
                curveTo(3.37f, 9.76f, 2.93f, 8.5f, 3f, 7.26f)
                relativeLineTo(3.09f, 3.09f)
                relativeLineTo(4.24f, -4.24f)
                lineTo(7.24f, 3f)
                relativeCurveTo(1.26f, -0.05f, 2.49f, 0.39f, 3.44f, 1.33f)
                relativeArcTo(4.47f, 4.47f, 0f, false, true, 1.24f, 3.96f)
                relativeArcTo(4.35f, 4.35f, 0f, false, true, -0.88f, 1.96f)
                relativeLineTo(9.46f, 9.45f)
                close()
            }
        }.build()
    }

    /** hammer-wrench — jeu Material Design Icons (IconVaultKit). */
    val BuildCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvBuildCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                relativeMoveTo(13.78f, 15.3f)
                relativeLineTo(6f, 6f)
                relativeLineTo(2.11f, -2.16f)
                relativeLineTo(-6f, -6f)
                close()
                relativeLineTo(3.72f, -5.2f)
                relativeCurveTo(-0.39f, 0f, -0.81f, -0.05f, -1.14f, -0.19f)
                lineTo(4.97f, 21.25f)
                relativeLineTo(-2.11f, -2.11f)
                relativeLineTo(7.41f, -7.4f)
                lineTo(8.5f, 9.96f)
                relativeLineTo(-0.72f, 0.7f)
                relativeLineTo(-1.45f, -1.41f)
                relativeVerticalLineTo(2.86f)
                relativeLineTo(-0.7f, 0.7f)
                relativeLineTo(-3.52f, -3.56f)
                relativeLineTo(0.7f, -0.7f)
                relativeHorizontalLineTo(2.81f)
                relativeLineTo(-1.4f, -1.41f)
                relativeLineTo(3.56f, -3.56f)
                relativeArcTo(2.976f, 2.976f, 0f, false, true, 4.22f, 0f)
                lineTo(9.89f, 5.74f)
                relativeLineTo(1.41f, 1.4f)
                relativeLineTo(-0.71f, 0.71f)
                relativeLineTo(1.79f, 1.78f)
                relativeLineTo(1.82f, -1.88f)
                relativeCurveTo(-0.14f, -0.33f, -0.2f, -0.75f, -0.2f, -1.12f)
                relativeArcTo(3.49f, 3.49f, 0f, false, true, 3.5f, -3.52f)
                relativeCurveTo(0.59f, 0f, 1.11f, 0.14f, 1.58f, 0.42f)
                lineTo(16.41f, 6.2f)
                relativeLineTo(1.5f, 1.5f)
                relativeLineTo(2.67f, -2.67f)
                relativeCurveTo(0.28f, 0.47f, 0.42f, 0.97f, 0.42f, 1.6f)
                relativeCurveTo(0f, 1.92f, -1.55f, 3.47f, -3.5f, 3.47f)
            }
        }.build()
    }

    /** shape-outline — jeu Material Design Icons (IconVaultKit). */
    val Category: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvCategory",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11f, 13.5f)
                relativeVerticalLineTo(8f)
                horizontalLineTo(3f)
                relativeVerticalLineTo(-8f)
                close()
                relativeLineTo(-2f, 2f)
                horizontalLineTo(5f)
                relativeVerticalLineTo(4f)
                relativeHorizontalLineTo(4f)
                close()
                lineTo(12f, 2f)
                relativeLineTo(5.5f, 9f)
                relativeHorizontalLineTo(-11f)
                close()
                relativeLineTo(0f, 3.86f)
                lineTo(10.08f, 9f)
                relativeHorizontalLineTo(3.84f)
                close()
                lineTo(17.5f, 13f)
                relativeCurveTo(2.5f, 0f, 4.5f, 2f, 4.5f, 4.5f)
                reflectCurveTo(20f, 22f, 17.5f, 22f)
                reflectCurveTo(13f, 20f, 13f, 17.5f)
                relativeReflectCurveTo(2f, -4.5f, 4.5f, -4.5f)
                relativeLineTo(0f, 2f)
                relativeArcTo(2.5f, 2.5f, 0f, false, false, -2.5f, 2.5f)
                relativeArcTo(2.5f, 2.5f, 0f, false, false, 2.5f, 2.5f)
                relativeArcTo(2.5f, 2.5f, 0f, false, false, 2.5f, -2.5f)
                relativeArcTo(2.5f, 2.5f, 0f, false, false, -2.5f, -2.5f)
            }
        }.build()
    }

    /** check-circle-outline — jeu Material Design Icons (IconVaultKit). */
    val CheckCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvCheckCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                curveTo(6.5f, 2f, 2f, 6.5f, 2f, 12f)
                relativeReflectCurveTo(4.5f, 10f, 10f, 10f)
                relativeReflectCurveTo(10f, -4.5f, 10f, -10f)
                reflectCurveTo(17.5f, 2f, 12f, 2f)
                relativeLineTo(0f, 18f)
                relativeCurveTo(-4.41f, 0f, -8f, -3.59f, -8f, -8f)
                relativeReflectCurveTo(3.59f, -8f, 8f, -8f)
                relativeReflectCurveTo(8f, 3.59f, 8f, 8f)
                relativeReflectCurveTo(-3.59f, 8f, -8f, 8f)
                relativeLineTo(4.59f, -12.42f)
                lineTo(10f, 14.17f)
                relativeLineTo(-2.59f, -2.58f)
                lineTo(6f, 13f)
                relativeLineTo(4f, 4f)
                relativeLineTo(8f, -8f)
                close()
            }
        }.build()
    }

    /** chevron-right — jeu Material Design Icons (IconVaultKit). */
    val ChevronRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvChevronRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(8.59f, 16.58f)
                lineTo(13.17f, 12f)
                lineTo(8.59f, 7.41f)
                lineTo(10f, 6f)
                relativeLineTo(6f, 6f)
                relativeLineTo(-6f, 6f)
                close()
            }
        }.build()
    }

    /** leaf — jeu Material Design Icons (IconVaultKit). */
    val Eco: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvEco",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(17f, 8f)
                curveTo(8f, 10f, 5.9f, 16.17f, 3.82f, 21.34f)
                relativeLineTo(1.89f, 0.66f)
                relativeLineTo(0.95f, -2.3f)
                relativeCurveTo(0.48f, 0.17f, 0.98f, 0.3f, 1.34f, 0.3f)
                curveTo(19f, 20f, 22f, 3f, 22f, 3f)
                relativeCurveTo(-1f, 2f, -8f, 2.25f, -13f, 3.25f)
                reflectCurveTo(2f, 11.5f, 2f, 13.5f)
                relativeReflectCurveTo(1.75f, 3.75f, 1.75f, 3.75f)
                curveTo(7f, 8f, 17f, 8f, 17f, 8f)
            }
        }.build()
    }

    /** pencil-outline — jeu Material Design Icons (IconVaultKit). */
    val Edit: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvEdit",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                relativeMoveTo(14.06f, 9f)
                relativeLineTo(0.94f, 0.94f)
                lineTo(5.92f, 19f)
                horizontalLineTo(5f)
                relativeVerticalLineTo(-0.92f)
                close()
                relativeLineTo(3.6f, -6f)
                relativeCurveTo(-0.25f, 0f, -0.51f, 0.1f, -0.7f, 0.29f)
                relativeLineTo(-1.83f, 1.83f)
                relativeLineTo(3.75f, 3.75f)
                relativeLineTo(1.83f, -1.83f)
                relativeCurveTo(0.39f, -0.39f, 0.39f, -1.04f, 0f, -1.41f)
                relativeLineTo(-2.34f, -2.34f)
                relativeCurveTo(-0.2f, -0.2f, -0.45f, -0.29f, -0.71f, -0.29f)
                relativeLineTo(-3.6f, 3.19f)
                lineTo(3f, 17.25f)
                verticalLineTo(21f)
                relativeHorizontalLineTo(3.75f)
                lineTo(17.81f, 9.94f)
                close()
            }
        }.build()
    }

    /** alert-circle-outline — jeu Material Design Icons (IconVaultKit). */
    val Error: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvError",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11f, 15f)
                relativeHorizontalLineTo(2f)
                relativeVerticalLineTo(2f)
                relativeHorizontalLineTo(-2f)
                close()
                relativeLineTo(0f, -8f)
                relativeHorizontalLineTo(2f)
                relativeVerticalLineTo(6f)
                relativeHorizontalLineTo(-2f)
                close()
                relativeLineTo(1f, -5f)
                curveTo(6.47f, 2f, 2f, 6.5f, 2f, 12f)
                relativeArcTo(10f, 10f, 0f, false, false, 10f, 10f)
                relativeArcTo(10f, 10f, 0f, false, false, 10f, -10f)
                arcTo(10f, 10f, 0f, false, false, 12f, 2f)
                relativeLineTo(0f, 18f)
                relativeArcTo(8f, 8f, 0f, false, true, -8f, -8f)
                relativeArcTo(8f, 8f, 0f, false, true, 8f, -8f)
                relativeArcTo(8f, 8f, 0f, false, true, 8f, 8f)
                relativeArcTo(8f, 8f, 0f, false, true, -8f, 8f)
            }
        }.build()
    }

    /** chevron-down — jeu Material Design Icons (IconVaultKit). */
    val ExpandMore: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvExpandMore",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(7.41f, 8.58f)
                lineTo(12f, 13.17f)
                relativeLineTo(4.59f, -4.59f)
                lineTo(18f, 10f)
                relativeLineTo(-6f, 6f)
                relativeLineTo(-6f, -6f)
                close()
            }
        }.build()
    }

    /** factory — jeu Material Design Icons (IconVaultKit). */
    val Factory: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvFactory",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 18f)
                relativeVerticalLineTo(2f)
                relativeHorizontalLineTo(4f)
                relativeVerticalLineTo(-2f)
                close()
                relativeLineTo(0f, -4f)
                relativeVerticalLineTo(2f)
                relativeHorizontalLineTo(10f)
                relativeVerticalLineTo(-2f)
                close()
                relativeLineTo(6f, 4f)
                relativeVerticalLineTo(2f)
                relativeHorizontalLineTo(4f)
                relativeVerticalLineTo(-2f)
                close()
                relativeLineTo(6f, -4f)
                relativeVerticalLineTo(2f)
                relativeHorizontalLineTo(4f)
                relativeVerticalLineTo(-2f)
                close()
                relativeLineTo(0f, 4f)
                relativeVerticalLineTo(2f)
                relativeHorizontalLineTo(4f)
                relativeVerticalLineTo(-2f)
                close()
                lineTo(2f, 22f)
                verticalLineTo(8f)
                relativeLineTo(5f, 4f)
                verticalLineTo(8f)
                relativeLineTo(5f, 4f)
                verticalLineTo(8f)
                relativeLineTo(5f, 4f)
                relativeLineTo(1f, -10f)
                relativeHorizontalLineTo(3f)
                relativeLineTo(1f, 10f)
                relativeVerticalLineTo(10f)
                close()
            }
        }.build()
    }

    /** package-variant — jeu Material Design Icons (IconVaultKit). */
    val Inventory2: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvInventory2",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(2f, 10.96f)
                relativeArcTo(0.985f, 0.985f, 0f, false, true, -0.37f, -1.37f)
                lineTo(3.13f, 7f)
                relativeCurveTo(0.11f, -0.2f, 0.28f, -0.34f, 0.47f, -0.42f)
                relativeLineTo(7.83f, -4.4f)
                relativeCurveTo(0.16f, -0.12f, 0.36f, -0.18f, 0.57f, -0.18f)
                relativeReflectCurveTo(0.41f, 0.06f, 0.57f, 0.18f)
                relativeLineTo(7.9f, 4.44f)
                relativeCurveTo(0.19f, 0.1f, 0.35f, 0.26f, 0.44f, 0.46f)
                relativeLineTo(1.45f, 2.52f)
                relativeCurveTo(0.28f, 0.48f, 0.11f, 1.09f, -0.36f, 1.36f)
                relativeLineTo(-1f, 0.58f)
                relativeVerticalLineTo(4.96f)
                relativeCurveTo(0f, 0.38f, -0.21f, 0.71f, -0.53f, 0.88f)
                relativeLineTo(-7.9f, 4.44f)
                relativeCurveTo(-0.16f, 0.12f, -0.36f, 0.18f, -0.57f, 0.18f)
                relativeReflectCurveTo(-0.41f, -0.06f, -0.57f, -0.18f)
                relativeLineTo(-7.9f, -4.44f)
                arcTo(0.99f, 0.99f, 0f, false, true, 3f, 16.5f)
                relativeVerticalLineTo(-5.54f)
                relativeCurveTo(-0.3f, 0.17f, -0.68f, 0.18f, -1f, 0f)
                relativeLineTo(10f, -6.81f)
                relativeVerticalLineTo(6.7f)
                relativeLineTo(5.96f, -3.35f)
                close()
                lineTo(5f, 15.91f)
                relativeLineTo(6f, 3.38f)
                relativeVerticalLineTo(-6.71f)
                lineTo(5f, 9.21f)
                close()
                relativeLineTo(14f, 0f)
                relativeVerticalLineTo(-3.22f)
                relativeLineTo(-5f, 2.9f)
                relativeCurveTo(-0.33f, 0.18f, -0.7f, 0.17f, -1f, 0.01f)
                relativeVerticalLineTo(3.69f)
                close()
                relativeLineTo(-5.15f, -2.55f)
                relativeLineTo(6.28f, -3.63f)
                relativeLineTo(-0.58f, -1.01f)
                relativeLineTo(-6.28f, 3.63f)
                close()
            }
        }.build()
    }

    /** monitor — jeu Material Design Icons (IconVaultKit). */
    val Monitor: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvMonitor",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(21f, 16f)
                horizontalLineTo(3f)
                verticalLineTo(4f)
                relativeHorizontalLineTo(18f)
                relativeLineTo(0f, -2f)
                horizontalLineTo(3f)
                relativeCurveTo(-1.11f, 0f, -2f, 0.89f, -2f, 2f)
                relativeVerticalLineTo(12f)
                relativeArcTo(2f, 2f, 0f, false, false, 2f, 2f)
                relativeHorizontalLineTo(7f)
                relativeVerticalLineTo(2f)
                horizontalLineTo(8f)
                relativeVerticalLineTo(2f)
                relativeHorizontalLineTo(8f)
                relativeVerticalLineTo(-2f)
                relativeHorizontalLineTo(-2f)
                relativeVerticalLineTo(-2f)
                relativeHorizontalLineTo(7f)
                relativeArcTo(2f, 2f, 0f, false, false, 2f, -2f)
                verticalLineTo(4f)
                relativeArcTo(2f, 2f, 0f, false, false, -2f, -2f)
            }
        }.build()
    }

    /** bell-outline — jeu Material Design Icons (IconVaultKit). */
    val Notifications: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvNotifications",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(10f, 21f)
                relativeHorizontalLineTo(4f)
                relativeCurveTo(0f, 1.1f, -0.9f, 2f, -2f, 2f)
                relativeReflectCurveTo(-2f, -0.9f, -2f, -2f)
                relativeLineTo(11f, -2f)
                relativeVerticalLineTo(1f)
                horizontalLineTo(3f)
                relativeVerticalLineTo(-1f)
                relativeLineTo(2f, -2f)
                relativeVerticalLineTo(-6f)
                relativeCurveTo(0f, -3.1f, 2f, -5.8f, 5f, -6.7f)
                verticalLineTo(4f)
                relativeCurveTo(0f, -1.1f, 0.9f, -2f, 2f, -2f)
                relativeReflectCurveTo(2f, 0.9f, 2f, 2f)
                relativeVerticalLineTo(0.3f)
                relativeCurveTo(3f, 0.9f, 5f, 3.6f, 5f, 6.7f)
                relativeVerticalLineTo(6f)
                close()
                relativeLineTo(-4f, -8f)
                relativeCurveTo(0f, -2.8f, -2.2f, -5f, -5f, -5f)
                relativeReflectCurveTo(-5f, 2.2f, -5f, 5f)
                relativeVerticalLineTo(7f)
                relativeHorizontalLineTo(10f)
                close()
            }
        }.build()
    }

    /** magnify — jeu Material Design Icons (IconVaultKit). */
    val Search: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvSearch",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(9.5f, 3f)
                arcTo(6.5f, 6.5f, 0f, false, true, 16f, 9.5f)
                relativeCurveTo(0f, 1.61f, -0.59f, 3.09f, -1.56f, 4.23f)
                relativeLineTo(0.27f, 0.27f)
                relativeHorizontalLineTo(0.79f)
                relativeLineTo(5f, 5f)
                relativeLineTo(-1.5f, 1.5f)
                relativeLineTo(-5f, -5f)
                relativeVerticalLineTo(-0.79f)
                relativeLineTo(-0.27f, -0.27f)
                arcTo(6.52f, 6.52f, 0f, false, true, 9.5f, 16f)
                arcTo(6.5f, 6.5f, 0f, false, true, 3f, 9.5f)
                arcTo(6.5f, 6.5f, 0f, false, true, 9.5f, 3f)
                relativeLineTo(0f, 2f)
                curveTo(7f, 5f, 5f, 7f, 5f, 9.5f)
                reflectCurveTo(7f, 14f, 9.5f, 14f)
                reflectCurveTo(14f, 12f, 14f, 9.5f)
                reflectCurveTo(12f, 5f, 9.5f, 5f)
            }
        }.build()
    }

    /** cog-outline — jeu Material Design Icons (IconVaultKit). */
    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvSettings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 8f)
                relativeArcTo(4f, 4f, 0f, false, true, 4f, 4f)
                relativeArcTo(4f, 4f, 0f, false, true, -4f, 4f)
                relativeArcTo(4f, 4f, 0f, false, true, -4f, -4f)
                relativeArcTo(4f, 4f, 0f, false, true, 4f, -4f)
                relativeLineTo(0f, 2f)
                relativeArcTo(2f, 2f, 0f, false, false, -2f, 2f)
                relativeArcTo(2f, 2f, 0f, false, false, 2f, 2f)
                relativeArcTo(2f, 2f, 0f, false, false, 2f, -2f)
                relativeArcTo(2f, 2f, 0f, false, false, -2f, -2f)
                relativeLineTo(-2f, 12f)
                relativeCurveTo(-0.25f, 0f, -0.46f, -0.18f, -0.5f, -0.42f)
                relativeLineTo(-0.37f, -2.65f)
                relativeCurveTo(-0.63f, -0.25f, -1.17f, -0.59f, -1.69f, -0.99f)
                relativeLineTo(-2.49f, 1.01f)
                relativeCurveTo(-0.22f, 0.08f, -0.49f, 0f, -0.61f, -0.22f)
                relativeLineTo(-2f, -3.46f)
                relativeArcTo(0.493f, 0.493f, 0f, false, true, 0.12f, -0.64f)
                relativeLineTo(2.11f, -1.66f)
                lineTo(4.5f, 12f)
                relativeLineTo(0.07f, -1f)
                relativeLineTo(-2.11f, -1.63f)
                relativeArcTo(0.493f, 0.493f, 0f, false, true, -0.12f, -0.64f)
                relativeLineTo(2f, -3.46f)
                relativeCurveTo(0.12f, -0.22f, 0.39f, -0.31f, 0.61f, -0.22f)
                relativeLineTo(2.49f, 1f)
                relativeCurveTo(0.52f, -0.39f, 1.06f, -0.73f, 1.69f, -0.98f)
                relativeLineTo(0.37f, -2.65f)
                relativeCurveTo(0.04f, -0.24f, 0.25f, -0.42f, 0.5f, -0.42f)
                relativeHorizontalLineTo(4f)
                relativeCurveTo(0.25f, 0f, 0.46f, 0.18f, 0.5f, 0.42f)
                relativeLineTo(0.37f, 2.65f)
                relativeCurveTo(0.63f, 0.25f, 1.17f, 0.59f, 1.69f, 0.98f)
                relativeLineTo(2.49f, -1f)
                relativeCurveTo(0.22f, -0.09f, 0.49f, 0f, 0.61f, 0.22f)
                relativeLineTo(2f, 3.46f)
                relativeCurveTo(0.13f, 0.22f, 0.07f, 0.49f, -0.12f, 0.64f)
                lineTo(19.43f, 11f)
                relativeLineTo(0.07f, 1f)
                relativeLineTo(-0.07f, 1f)
                relativeLineTo(2.11f, 1.63f)
                relativeCurveTo(0.19f, 0.15f, 0.25f, 0.42f, 0.12f, 0.64f)
                relativeLineTo(-2f, 3.46f)
                relativeCurveTo(-0.12f, 0.22f, -0.39f, 0.31f, -0.61f, 0.22f)
                relativeLineTo(-2.49f, -1f)
                relativeCurveTo(-0.52f, 0.39f, -1.06f, 0.73f, -1.69f, 0.98f)
                relativeLineTo(-0.37f, 2.65f)
                relativeCurveTo(-0.04f, 0.24f, -0.25f, 0.42f, -0.5f, 0.42f)
                close()
                relativeLineTo(1.25f, -18f)
                relativeLineTo(-0.37f, 2.61f)
                relativeCurveTo(-1.2f, 0.25f, -2.26f, 0.89f, -3.03f, 1.78f)
                lineTo(5.44f, 7.35f)
                relativeLineTo(-0.75f, 1.3f)
                lineTo(6.8f, 10.2f)
                relativeArcTo(5.55f, 5.55f, 0f, false, false, 0f, 3.6f)
                relativeLineTo(-2.12f, 1.56f)
                relativeLineTo(0.75f, 1.3f)
                relativeLineTo(2.43f, -1.04f)
                relativeCurveTo(0.77f, 0.88f, 1.82f, 1.52f, 3.01f, 1.76f)
                relativeLineTo(0.37f, 2.62f)
                relativeHorizontalLineTo(1.52f)
                relativeLineTo(0.37f, -2.61f)
                relativeCurveTo(1.19f, -0.25f, 2.24f, -0.89f, 3.01f, -1.77f)
                relativeLineTo(2.43f, 1.04f)
                relativeLineTo(0.75f, -1.3f)
                relativeLineTo(-2.12f, -1.55f)
                relativeCurveTo(0.4f, -1.17f, 0.4f, -2.44f, 0f, -3.61f)
                relativeLineTo(2.11f, -1.55f)
                relativeLineTo(-0.75f, -1.3f)
                relativeLineTo(-2.41f, 1.04f)
                relativeArcTo(5.42f, 5.42f, 0f, false, false, -3.03f, -1.77f)
                lineTo(12.75f, 4f)
                close()
            }
        }.build()
    }

    /** cart-outline — jeu Material Design Icons (IconVaultKit). */
    val ShoppingCart: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvShoppingCart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(17f, 18f)
                relativeArcTo(2f, 2f, 0f, false, true, 2f, 2f)
                relativeArcTo(2f, 2f, 0f, false, true, -2f, 2f)
                relativeArcTo(2f, 2f, 0f, false, true, -2f, -2f)
                relativeCurveTo(0f, -1.11f, 0.89f, -2f, 2f, -2f)
                lineTo(1f, 2f)
                relativeHorizontalLineTo(3.27f)
                relativeLineTo(0.94f, 2f)
                horizontalLineTo(20f)
                relativeArcTo(1f, 1f, 0f, false, true, 1f, 1f)
                relativeCurveTo(0f, 0.17f, -0.05f, 0.34f, -0.12f, 0.5f)
                relativeLineTo(-3.58f, 6.47f)
                relativeCurveTo(-0.34f, 0.61f, -1f, 1.03f, -1.75f, 1.03f)
                horizontalLineTo(8.1f)
                relativeLineTo(-0.9f, 1.63f)
                relativeLineTo(-0.03f, 0.12f)
                relativeArcTo(0.25f, 0.25f, 0f, false, false, 0.25f, 0.25f)
                horizontalLineTo(19f)
                relativeVerticalLineTo(2f)
                horizontalLineTo(7f)
                relativeArcTo(2f, 2f, 0f, false, true, -2f, -2f)
                relativeCurveTo(0f, -0.35f, 0.09f, -0.68f, 0.24f, -0.96f)
                relativeLineTo(1.36f, -2.45f)
                lineTo(3f, 4f)
                horizontalLineTo(1f)
                close()
                relativeLineTo(6f, 16f)
                relativeArcTo(2f, 2f, 0f, false, true, 2f, 2f)
                relativeArcTo(2f, 2f, 0f, false, true, -2f, 2f)
                relativeArcTo(2f, 2f, 0f, false, true, -2f, -2f)
                relativeCurveTo(0f, -1.11f, 0.89f, -2f, 2f, -2f)
                relativeLineTo(9f, -7f)
                relativeLineTo(2.78f, -5f)
                horizontalLineTo(6.14f)
                relativeLineTo(2.36f, 5f)
                close()
            }
        }.build()
    }

    /** star-outline — jeu Material Design Icons (IconVaultKit). */
    val Star: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvStar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                relativeMoveTo(12f, 15.39f)
                relativeLineTo(-3.76f, 2.27f)
                relativeLineTo(0.99f, -4.28f)
                relativeLineTo(-3.32f, -2.88f)
                relativeLineTo(4.38f, -0.37f)
                lineTo(12f, 6.09f)
                relativeLineTo(1.71f, 4.04f)
                relativeLineTo(4.38f, 0.37f)
                relativeLineTo(-3.32f, 2.88f)
                relativeLineTo(0.99f, 4.28f)
                lineTo(22f, 9.24f)
                relativeLineTo(-7.19f, -0.61f)
                lineTo(12f, 2f)
                lineTo(9.19f, 8.63f)
                lineTo(2f, 9.24f)
                relativeLineTo(5.45f, 4.73f)
                lineTo(5.82f, 21f)
                lineTo(12f, 17.27f)
                lineTo(18.18f, 21f)
                relativeLineTo(-1.64f, -7.03f)
                close()
            }
        }.build()
    }

    /** sync — jeu Material Design Icons (IconVaultKit). */
    val Sync: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvSync",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 18f)
                relativeArcTo(6f, 6f, 0f, false, true, -6f, -6f)
                relativeCurveTo(0f, -1f, 0.25f, -1.97f, 0.7f, -2.8f)
                lineTo(5.24f, 7.74f)
                arcTo(7.93f, 7.93f, 0f, false, false, 4f, 12f)
                relativeArcTo(8f, 8f, 0f, false, false, 8f, 8f)
                relativeVerticalLineTo(3f)
                relativeLineTo(4f, -4f)
                relativeLineTo(-4f, -4f)
                relativeLineTo(0f, -11f)
                verticalLineTo(1f)
                lineTo(8f, 5f)
                relativeLineTo(4f, 4f)
                verticalLineTo(6f)
                relativeArcTo(6f, 6f, 0f, false, true, 6f, 6f)
                relativeCurveTo(0f, 1f, -0.25f, 1.97f, -0.7f, 2.8f)
                relativeLineTo(1.46f, 1.46f)
                arcTo(7.93f, 7.93f, 0f, false, false, 20f, 12f)
                relativeArcTo(8f, 8f, 0f, false, false, -8f, -8f)
            }
        }.build()
    }

    /** trending-down — jeu Material Design Icons (IconVaultKit). */
    val TrendingDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvTrendingDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                relativeMoveTo(16f, 18f)
                relativeLineTo(2.29f, -2.29f)
                relativeLineTo(-4.88f, -4.88f)
                relativeLineTo(-4f, 4f)
                lineTo(2f, 7.41f)
                lineTo(3.41f, 6f)
                relativeLineTo(6f, 6f)
                relativeLineTo(4f, -4f)
                relativeLineTo(6.3f, 6.29f)
                lineTo(22f, 12f)
                relativeVerticalLineTo(6f)
                close()
            }
        }.build()
    }

    /** trending-up — jeu Material Design Icons (IconVaultKit). */
    val TrendingUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvTrendingUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                relativeMoveTo(16f, 6f)
                relativeLineTo(2.29f, 2.29f)
                relativeLineTo(-4.88f, 4.88f)
                relativeLineTo(-4f, -4f)
                lineTo(2f, 16.59f)
                lineTo(3.41f, 18f)
                relativeLineTo(6f, -6f)
                relativeLineTo(4f, 4f)
                relativeLineTo(6.3f, -6.29f)
                lineTo(22f, 12f)
                verticalLineTo(6f)
                close()
            }
        }.build()
    }

    /** archive-arrow-up-outline — jeu Material Design Icons (IconVaultKit). */
    val Unarchive: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvUnarchive",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20f, 21f)
                horizontalLineTo(4f)
                verticalLineTo(10f)
                relativeHorizontalLineTo(2f)
                relativeVerticalLineTo(9f)
                relativeHorizontalLineTo(12f)
                relativeVerticalLineTo(-9f)
                relativeHorizontalLineTo(2f)
                close()
                lineTo(3f, 3f)
                relativeHorizontalLineTo(18f)
                relativeVerticalLineTo(6f)
                horizontalLineTo(3f)
                close()
                relativeLineTo(2f, 2f)
                relativeVerticalLineTo(2f)
                relativeHorizontalLineTo(14f)
                verticalLineTo(5f)
                relativeLineTo(-8.5f, 12f)
                relativeVerticalLineTo(-3f)
                horizontalLineTo(8f)
                relativeLineTo(4f, -4f)
                relativeLineTo(4f, 4f)
                relativeHorizontalLineTo(-2.5f)
                relativeVerticalLineTo(3f)
            }
        }.build()
    }

    /** alert-outline — jeu Material Design Icons (IconVaultKit). */
    val Warning: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvWarning",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                lineTo(1f, 21f)
                relativeHorizontalLineTo(22f)
                lineTo(12f, 6f)
                relativeLineTo(7.53f, 13f)
                horizontalLineTo(4.47f)
                lineTo(11f, 10f)
                relativeVerticalLineTo(4f)
                relativeHorizontalLineTo(2f)
                relativeVerticalLineTo(-4f)
                relativeLineTo(-2f, 6f)
                relativeVerticalLineTo(2f)
                relativeHorizontalLineTo(2f)
                relativeVerticalLineTo(-2f)
            }
        }.build()
    }
}
