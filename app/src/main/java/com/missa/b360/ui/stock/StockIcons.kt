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
                lineTo(13f, 13f)
                lineTo(13f, 19f)
                lineTo(11f, 19f)
                lineTo(11f, 13f)
                lineTo(5f, 13f)
                lineTo(5f, 11f)
                lineTo(11f, 11f)
                lineTo(11f, 5f)
                lineTo(13f, 5f)
                lineTo(13f, 11f)
                lineTo(19f, 11f)
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
                moveTo(22.61f, 19f)
                lineTo(13.53f, 9.91f)
                curveTo(14.46f, 7.57f, 14f, 4.81f, 12.09f, 2.91f)
                curveTo(9.79f, 0.61f, 6.21f, 0.4f, 3.66f, 2.26f)
                lineTo(7.5f, 6.11f)
                lineTo(6.08f, 7.5f)
                lineTo(2.25f, 3.69f)
                curveTo(0.39f, 6.23f, 0.6f, 9.82f, 2.9f, 12.11f)
                curveTo(4.76f, 13.97f, 7.47f, 14.46f, 9.79f, 13.59f)
                lineTo(18.9f, 22.7f)
                curveTo(19.29f, 23.09f, 19.92f, 23.09f, 20.31f, 22.7f)
                lineTo(22.61f, 20.4f)
                curveTo(23f, 20f, 23f, 19.39f, 22.61f, 19f)
                lineTo(19.61f, 20.59f)
                lineTo(10.15f, 11.13f)
                curveTo(9.54f, 11.58f, 8.86f, 11.85f, 8.15f, 11.95f)
                curveTo(6.79f, 12.15f, 5.36f, 11.74f, 4.32f, 10.7f)
                curveTo(3.37f, 9.76f, 2.93f, 8.5f, 3f, 7.26f)
                lineTo(6.09f, 10.35f)
                lineTo(10.33f, 6.11f)
                lineTo(7.24f, 3f)
                curveTo(8.5f, 2.95f, 9.73f, 3.39f, 10.68f, 4.33f)
                arcTo(4.47f, 4.47f, 0f, false, true, 11.92f, 8.29f)
                arcTo(4.35f, 4.35f, 0f, false, true, 11.04f, 10.25f)
                lineTo(20.5f, 19.7f)
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
                moveTo(13.78f, 15.3f)
                lineTo(19.78f, 21.3f)
                lineTo(21.89f, 19.14f)
                lineTo(15.89f, 13.14f)
                close()
                lineTo(17.5f, 10.1f)
                curveTo(17.11f, 10.1f, 16.69f, 10.05f, 16.36f, 9.91f)
                lineTo(4.97f, 21.25f)
                lineTo(2.86f, 19.14f)
                lineTo(10.27f, 11.74f)
                lineTo(8.5f, 9.96f)
                lineTo(7.78f, 10.66f)
                lineTo(6.33f, 9.25f)
                lineTo(6.33f, 12.11f)
                lineTo(5.63f, 12.81f)
                lineTo(2.11f, 9.25f)
                lineTo(2.81f, 8.55f)
                lineTo(5.62f, 8.55f)
                lineTo(4.22f, 7.14f)
                lineTo(7.78f, 3.58f)
                arcTo(2.976f, 2.976f, 0f, false, true, 12f, 3.58f)
                lineTo(9.89f, 5.74f)
                lineTo(11.3f, 7.14f)
                lineTo(10.59f, 7.85f)
                lineTo(12.38f, 9.63f)
                lineTo(14.2f, 7.75f)
                curveTo(14.06f, 7.42f, 14f, 7f, 14f, 6.63f)
                arcTo(3.49f, 3.49f, 0f, false, true, 17.5f, 3.11f)
                curveTo(18.09f, 3.11f, 18.61f, 3.25f, 19.08f, 3.53f)
                lineTo(16.41f, 6.2f)
                lineTo(17.91f, 7.7f)
                lineTo(20.58f, 5.03f)
                curveTo(20.86f, 5.5f, 21f, 6f, 21f, 6.63f)
                curveTo(21f, 8.55f, 19.45f, 10.1f, 17.5f, 10.1f)
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
                lineTo(11f, 21.5f)
                lineTo(3f, 21.5f)
                lineTo(3f, 13.5f)
                close()
                lineTo(9f, 15.5f)
                lineTo(5f, 15.5f)
                lineTo(5f, 19.5f)
                lineTo(9f, 19.5f)
                close()
                lineTo(12f, 2f)
                lineTo(17.5f, 11f)
                lineTo(6.5f, 11f)
                close()
                lineTo(12f, 5.86f)
                lineTo(10.08f, 9f)
                lineTo(13.92f, 9f)
                close()
                lineTo(17.5f, 13f)
                curveTo(20f, 13f, 22f, 15f, 22f, 17.5f)
                curveTo(22f, 20f, 20f, 22f, 17.5f, 22f)
                curveTo(15f, 22f, 13f, 20f, 13f, 17.5f)
                curveTo(13f, 15f, 15f, 13f, 17.5f, 13f)
                lineTo(17.5f, 15f)
                arcTo(2.5f, 2.5f, 0f, false, false, 15f, 17.5f)
                arcTo(2.5f, 2.5f, 0f, false, false, 17.5f, 20f)
                arcTo(2.5f, 2.5f, 0f, false, false, 20f, 17.5f)
                arcTo(2.5f, 2.5f, 0f, false, false, 17.5f, 15f)
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
                curveTo(2f, 17.5f, 6.5f, 22f, 12f, 22f)
                curveTo(17.5f, 22f, 22f, 17.5f, 22f, 12f)
                curveTo(22f, 6.5f, 17.5f, 2f, 12f, 2f)
                lineTo(12f, 20f)
                curveTo(7.59f, 20f, 4f, 16.41f, 4f, 12f)
                curveTo(4f, 7.59f, 7.59f, 4f, 12f, 4f)
                curveTo(16.41f, 4f, 20f, 7.59f, 20f, 12f)
                curveTo(20f, 16.41f, 16.41f, 20f, 12f, 20f)
                lineTo(16.59f, 7.58f)
                lineTo(10f, 14.17f)
                lineTo(7.41f, 11.59f)
                lineTo(6f, 13f)
                lineTo(10f, 17f)
                lineTo(18f, 9f)
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
                lineTo(16f, 12f)
                lineTo(10f, 18f)
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
                lineTo(5.71f, 22f)
                lineTo(6.66f, 19.7f)
                curveTo(7.14f, 19.87f, 7.64f, 20f, 8f, 20f)
                curveTo(19f, 20f, 22f, 3f, 22f, 3f)
                curveTo(21f, 5f, 14f, 5.25f, 9f, 6.25f)
                curveTo(4f, 7.25f, 2f, 11.5f, 2f, 13.5f)
                curveTo(2f, 15.5f, 3.75f, 17.25f, 3.75f, 17.25f)
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
                moveTo(14.06f, 9f)
                lineTo(15f, 9.94f)
                lineTo(5.92f, 19f)
                lineTo(5f, 19f)
                lineTo(5f, 18.08f)
                close()
                lineTo(17.66f, 3f)
                curveTo(17.41f, 3f, 17.15f, 3.1f, 16.96f, 3.29f)
                lineTo(15.13f, 5.12f)
                lineTo(18.88f, 8.87f)
                lineTo(20.71f, 7.04f)
                curveTo(21.1f, 6.65f, 21.1f, 6f, 20.71f, 5.63f)
                lineTo(18.37f, 3.29f)
                curveTo(18.17f, 3.09f, 17.92f, 3f, 17.66f, 3f)
                lineTo(14.06f, 6.19f)
                lineTo(3f, 17.25f)
                lineTo(3f, 21f)
                lineTo(6.75f, 21f)
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
                lineTo(13f, 15f)
                lineTo(13f, 17f)
                lineTo(11f, 17f)
                close()
                lineTo(11f, 7f)
                lineTo(13f, 7f)
                lineTo(13f, 13f)
                lineTo(11f, 13f)
                close()
                lineTo(12f, 2f)
                curveTo(6.47f, 2f, 2f, 6.5f, 2f, 12f)
                arcTo(10f, 10f, 0f, false, false, 12f, 22f)
                arcTo(10f, 10f, 0f, false, false, 22f, 12f)
                arcTo(10f, 10f, 0f, false, false, 12f, 2f)
                lineTo(12f, 20f)
                arcTo(8f, 8f, 0f, false, true, 4f, 12f)
                arcTo(8f, 8f, 0f, false, true, 12f, 4f)
                arcTo(8f, 8f, 0f, false, true, 20f, 12f)
                arcTo(8f, 8f, 0f, false, true, 12f, 20f)
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
                lineTo(16.59f, 8.58f)
                lineTo(18f, 10f)
                lineTo(12f, 16f)
                lineTo(6f, 10f)
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
                lineTo(4f, 20f)
                lineTo(8f, 20f)
                lineTo(8f, 18f)
                close()
                lineTo(4f, 14f)
                lineTo(4f, 16f)
                lineTo(14f, 16f)
                lineTo(14f, 14f)
                close()
                lineTo(10f, 18f)
                lineTo(10f, 20f)
                lineTo(14f, 20f)
                lineTo(14f, 18f)
                close()
                lineTo(16f, 14f)
                lineTo(16f, 16f)
                lineTo(20f, 16f)
                lineTo(20f, 14f)
                close()
                lineTo(16f, 18f)
                lineTo(16f, 20f)
                lineTo(20f, 20f)
                lineTo(20f, 18f)
                close()
                lineTo(2f, 22f)
                lineTo(2f, 8f)
                lineTo(7f, 12f)
                lineTo(7f, 8f)
                lineTo(12f, 12f)
                lineTo(12f, 8f)
                lineTo(17f, 12f)
                lineTo(18f, 2f)
                lineTo(21f, 2f)
                lineTo(22f, 12f)
                lineTo(22f, 22f)
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
                arcTo(0.985f, 0.985f, 0f, false, true, 1.63f, 9.59f)
                lineTo(3.13f, 7f)
                curveTo(3.24f, 6.8f, 3.41f, 6.66f, 3.6f, 6.58f)
                lineTo(11.43f, 2.18f)
                curveTo(11.59f, 2.06f, 11.79f, 2f, 12f, 2f)
                curveTo(12.21f, 2f, 12.41f, 2.06f, 12.57f, 2.18f)
                lineTo(20.47f, 6.62f)
                curveTo(20.66f, 6.72f, 20.82f, 6.88f, 20.91f, 7.08f)
                lineTo(22.36f, 9.6f)
                curveTo(22.64f, 10.08f, 22.47f, 10.69f, 22f, 10.96f)
                lineTo(21f, 11.54f)
                lineTo(21f, 16.5f)
                curveTo(21f, 16.88f, 20.79f, 17.21f, 20.47f, 17.38f)
                lineTo(12.57f, 21.82f)
                curveTo(12.41f, 21.94f, 12.21f, 22f, 12f, 22f)
                curveTo(11.79f, 22f, 11.59f, 21.94f, 11.43f, 21.82f)
                lineTo(3.53f, 17.38f)
                arcTo(0.99f, 0.99f, 0f, false, true, 3f, 16.5f)
                lineTo(3f, 10.96f)
                curveTo(2.7f, 11.13f, 2.32f, 11.14f, 2f, 10.96f)
                lineTo(12f, 4.15f)
                lineTo(12f, 10.85f)
                lineTo(17.96f, 7.5f)
                close()
                lineTo(5f, 15.91f)
                lineTo(11f, 19.29f)
                lineTo(11f, 12.58f)
                lineTo(5f, 9.21f)
                close()
                lineTo(19f, 15.91f)
                lineTo(19f, 12.69f)
                lineTo(14f, 15.59f)
                curveTo(13.67f, 15.77f, 13.3f, 15.76f, 13f, 15.6f)
                lineTo(13f, 19.29f)
                close()
                lineTo(13.85f, 13.36f)
                lineTo(20.13f, 9.73f)
                lineTo(19.55f, 8.72f)
                lineTo(13.27f, 12.35f)
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
                lineTo(3f, 16f)
                lineTo(3f, 4f)
                lineTo(21f, 4f)
                lineTo(21f, 2f)
                lineTo(3f, 2f)
                curveTo(1.89f, 2f, 1f, 2.89f, 1f, 4f)
                lineTo(1f, 16f)
                arcTo(2f, 2f, 0f, false, false, 3f, 18f)
                lineTo(10f, 18f)
                lineTo(10f, 20f)
                lineTo(8f, 20f)
                lineTo(8f, 22f)
                lineTo(16f, 22f)
                lineTo(16f, 20f)
                lineTo(14f, 20f)
                lineTo(14f, 18f)
                lineTo(21f, 18f)
                arcTo(2f, 2f, 0f, false, false, 23f, 16f)
                lineTo(23f, 4f)
                arcTo(2f, 2f, 0f, false, false, 21f, 2f)
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
                lineTo(14f, 21f)
                curveTo(14f, 22.1f, 13.1f, 23f, 12f, 23f)
                curveTo(10.9f, 23f, 10f, 22.1f, 10f, 21f)
                lineTo(21f, 19f)
                lineTo(21f, 20f)
                lineTo(3f, 20f)
                lineTo(3f, 19f)
                lineTo(5f, 17f)
                lineTo(5f, 11f)
                curveTo(5f, 7.9f, 7f, 5.2f, 10f, 4.3f)
                lineTo(10f, 4f)
                curveTo(10f, 2.9f, 10.9f, 2f, 12f, 2f)
                curveTo(13.1f, 2f, 14f, 2.9f, 14f, 4f)
                lineTo(14f, 4.3f)
                curveTo(17f, 5.2f, 19f, 7.9f, 19f, 11f)
                lineTo(19f, 17f)
                close()
                lineTo(17f, 11f)
                curveTo(17f, 8.2f, 14.8f, 6f, 12f, 6f)
                curveTo(9.2f, 6f, 7f, 8.2f, 7f, 11f)
                lineTo(7f, 18f)
                lineTo(17f, 18f)
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
                curveTo(16f, 11.11f, 15.41f, 12.59f, 14.44f, 13.73f)
                lineTo(14.71f, 14f)
                lineTo(15.5f, 14f)
                lineTo(20.5f, 19f)
                lineTo(19f, 20.5f)
                lineTo(14f, 15.5f)
                lineTo(14f, 14.71f)
                lineTo(13.73f, 14.44f)
                arcTo(6.52f, 6.52f, 0f, false, true, 9.5f, 16f)
                arcTo(6.5f, 6.5f, 0f, false, true, 3f, 9.5f)
                arcTo(6.5f, 6.5f, 0f, false, true, 9.5f, 3f)
                lineTo(9.5f, 5f)
                curveTo(7f, 5f, 5f, 7f, 5f, 9.5f)
                curveTo(5f, 12f, 7f, 14f, 9.5f, 14f)
                curveTo(12f, 14f, 14f, 12f, 14f, 9.5f)
                curveTo(14f, 7f, 12f, 5f, 9.5f, 5f)
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
                arcTo(4f, 4f, 0f, false, true, 16f, 12f)
                arcTo(4f, 4f, 0f, false, true, 12f, 16f)
                arcTo(4f, 4f, 0f, false, true, 8f, 12f)
                arcTo(4f, 4f, 0f, false, true, 12f, 8f)
                lineTo(12f, 10f)
                arcTo(2f, 2f, 0f, false, false, 10f, 12f)
                arcTo(2f, 2f, 0f, false, false, 12f, 14f)
                arcTo(2f, 2f, 0f, false, false, 14f, 12f)
                arcTo(2f, 2f, 0f, false, false, 12f, 10f)
                lineTo(10f, 22f)
                curveTo(9.75f, 22f, 9.54f, 21.82f, 9.5f, 21.58f)
                lineTo(9.13f, 18.93f)
                curveTo(8.5f, 18.68f, 7.96f, 18.34f, 7.44f, 17.94f)
                lineTo(4.95f, 18.95f)
                curveTo(4.73f, 19.03f, 4.46f, 18.95f, 4.34f, 18.73f)
                lineTo(2.34f, 15.27f)
                arcTo(0.493f, 0.493f, 0f, false, true, 2.46f, 14.63f)
                lineTo(4.57f, 12.97f)
                lineTo(4.5f, 12f)
                lineTo(4.57f, 11f)
                lineTo(2.46f, 9.37f)
                arcTo(0.493f, 0.493f, 0f, false, true, 2.34f, 8.73f)
                lineTo(4.34f, 5.27f)
                curveTo(4.46f, 5.05f, 4.73f, 4.96f, 4.95f, 5.05f)
                lineTo(7.44f, 6.05f)
                curveTo(7.96f, 5.66f, 8.5f, 5.32f, 9.13f, 5.07f)
                lineTo(9.5f, 2.42f)
                curveTo(9.54f, 2.18f, 9.75f, 2f, 10f, 2f)
                lineTo(14f, 2f)
                curveTo(14.25f, 2f, 14.46f, 2.18f, 14.5f, 2.42f)
                lineTo(14.87f, 5.07f)
                curveTo(15.5f, 5.32f, 16.04f, 5.66f, 16.56f, 6.05f)
                lineTo(19.05f, 5.05f)
                curveTo(19.27f, 4.96f, 19.54f, 5.05f, 19.66f, 5.27f)
                lineTo(21.66f, 8.73f)
                curveTo(21.79f, 8.95f, 21.73f, 9.22f, 21.54f, 9.37f)
                lineTo(19.43f, 11f)
                lineTo(19.5f, 12f)
                lineTo(19.43f, 13f)
                lineTo(21.54f, 14.63f)
                curveTo(21.73f, 14.78f, 21.79f, 15.05f, 21.66f, 15.27f)
                lineTo(19.66f, 18.73f)
                curveTo(19.54f, 18.95f, 19.27f, 19.04f, 19.05f, 18.95f)
                lineTo(16.56f, 17.95f)
                curveTo(16.04f, 18.34f, 15.5f, 18.68f, 14.87f, 18.93f)
                lineTo(14.5f, 21.58f)
                curveTo(14.46f, 21.82f, 14.25f, 22f, 14f, 22f)
                close()
                lineTo(11.25f, 4f)
                lineTo(10.88f, 6.61f)
                curveTo(9.68f, 6.86f, 8.62f, 7.5f, 7.85f, 8.39f)
                lineTo(5.44f, 7.35f)
                lineTo(4.69f, 8.65f)
                lineTo(6.8f, 10.2f)
                arcTo(5.55f, 5.55f, 0f, false, false, 6.8f, 13.8f)
                lineTo(4.68f, 15.36f)
                lineTo(5.43f, 16.66f)
                lineTo(7.86f, 15.62f)
                curveTo(8.63f, 16.5f, 9.68f, 17.14f, 10.87f, 17.38f)
                lineTo(11.24f, 20f)
                lineTo(12.76f, 20f)
                lineTo(13.13f, 17.39f)
                curveTo(14.32f, 17.14f, 15.37f, 16.5f, 16.14f, 15.62f)
                lineTo(18.57f, 16.66f)
                lineTo(19.32f, 15.36f)
                lineTo(17.2f, 13.81f)
                curveTo(17.6f, 12.64f, 17.6f, 11.37f, 17.2f, 10.2f)
                lineTo(19.31f, 8.65f)
                lineTo(18.56f, 7.35f)
                lineTo(16.15f, 8.39f)
                arcTo(5.42f, 5.42f, 0f, false, false, 13.12f, 6.62f)
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
                arcTo(2f, 2f, 0f, false, true, 19f, 20f)
                arcTo(2f, 2f, 0f, false, true, 17f, 22f)
                arcTo(2f, 2f, 0f, false, true, 15f, 20f)
                curveTo(15f, 18.89f, 15.89f, 18f, 17f, 18f)
                lineTo(1f, 2f)
                lineTo(4.27f, 2f)
                lineTo(5.21f, 4f)
                lineTo(20f, 4f)
                arcTo(1f, 1f, 0f, false, true, 21f, 5f)
                curveTo(21f, 5.17f, 20.95f, 5.34f, 20.88f, 5.5f)
                lineTo(17.3f, 11.97f)
                curveTo(16.96f, 12.58f, 16.3f, 13f, 15.55f, 13f)
                lineTo(8.1f, 13f)
                lineTo(7.2f, 14.63f)
                lineTo(7.17f, 14.75f)
                arcTo(0.25f, 0.25f, 0f, false, false, 7.42f, 15f)
                lineTo(19f, 15f)
                lineTo(19f, 17f)
                lineTo(7f, 17f)
                arcTo(2f, 2f, 0f, false, true, 5f, 15f)
                curveTo(5f, 14.65f, 5.09f, 14.32f, 5.24f, 14.04f)
                lineTo(6.6f, 11.59f)
                lineTo(3f, 4f)
                lineTo(1f, 4f)
                close()
                lineTo(7f, 18f)
                arcTo(2f, 2f, 0f, false, true, 9f, 20f)
                arcTo(2f, 2f, 0f, false, true, 7f, 22f)
                arcTo(2f, 2f, 0f, false, true, 5f, 20f)
                curveTo(5f, 18.89f, 5.89f, 18f, 7f, 18f)
                lineTo(16f, 11f)
                lineTo(18.78f, 6f)
                lineTo(6.14f, 6f)
                lineTo(8.5f, 11f)
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
                moveTo(12f, 15.39f)
                lineTo(8.24f, 17.66f)
                lineTo(9.23f, 13.38f)
                lineTo(5.91f, 10.5f)
                lineTo(10.29f, 10.13f)
                lineTo(12f, 6.09f)
                lineTo(13.71f, 10.13f)
                lineTo(18.09f, 10.5f)
                lineTo(14.77f, 13.38f)
                lineTo(15.76f, 17.66f)
                lineTo(22f, 9.24f)
                lineTo(14.81f, 8.63f)
                lineTo(12f, 2f)
                lineTo(9.19f, 8.63f)
                lineTo(2f, 9.24f)
                lineTo(7.45f, 13.97f)
                lineTo(5.82f, 21f)
                lineTo(12f, 17.27f)
                lineTo(18.18f, 21f)
                lineTo(16.54f, 13.97f)
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
                arcTo(6f, 6f, 0f, false, true, 6f, 12f)
                curveTo(6f, 11f, 6.25f, 10.03f, 6.7f, 9.2f)
                lineTo(5.24f, 7.74f)
                arcTo(7.93f, 7.93f, 0f, false, false, 4f, 12f)
                arcTo(8f, 8f, 0f, false, false, 12f, 20f)
                lineTo(12f, 23f)
                lineTo(16f, 19f)
                lineTo(12f, 15f)
                lineTo(12f, 4f)
                lineTo(12f, 1f)
                lineTo(8f, 5f)
                lineTo(12f, 9f)
                lineTo(12f, 6f)
                arcTo(6f, 6f, 0f, false, true, 18f, 12f)
                curveTo(18f, 13f, 17.75f, 13.97f, 17.3f, 14.8f)
                lineTo(18.76f, 16.26f)
                arcTo(7.93f, 7.93f, 0f, false, false, 20f, 12f)
                arcTo(8f, 8f, 0f, false, false, 12f, 4f)
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
                moveTo(16f, 18f)
                lineTo(18.29f, 15.71f)
                lineTo(13.41f, 10.83f)
                lineTo(9.41f, 14.83f)
                lineTo(2f, 7.41f)
                lineTo(3.41f, 6f)
                lineTo(9.41f, 12f)
                lineTo(13.41f, 8f)
                lineTo(19.71f, 14.29f)
                lineTo(22f, 12f)
                lineTo(22f, 18f)
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
                moveTo(16f, 6f)
                lineTo(18.29f, 8.29f)
                lineTo(13.41f, 13.17f)
                lineTo(9.41f, 9.17f)
                lineTo(2f, 16.59f)
                lineTo(3.41f, 18f)
                lineTo(9.41f, 12f)
                lineTo(13.41f, 16f)
                lineTo(19.71f, 9.71f)
                lineTo(22f, 12f)
                lineTo(22f, 6f)
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
                lineTo(4f, 21f)
                lineTo(4f, 10f)
                lineTo(6f, 10f)
                lineTo(6f, 19f)
                lineTo(18f, 19f)
                lineTo(18f, 10f)
                lineTo(20f, 10f)
                close()
                lineTo(3f, 3f)
                lineTo(21f, 3f)
                lineTo(21f, 9f)
                lineTo(3f, 9f)
                close()
                lineTo(5f, 5f)
                lineTo(5f, 7f)
                lineTo(19f, 7f)
                lineTo(19f, 5f)
                lineTo(10.5f, 17f)
                lineTo(10.5f, 14f)
                lineTo(8f, 14f)
                lineTo(12f, 10f)
                lineTo(16f, 14f)
                lineTo(13.5f, 14f)
                lineTo(13.5f, 17f)
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
                lineTo(23f, 21f)
                lineTo(12f, 6f)
                lineTo(19.53f, 19f)
                lineTo(4.47f, 19f)
                lineTo(11f, 10f)
                lineTo(11f, 14f)
                lineTo(13f, 14f)
                lineTo(13f, 10f)
                lineTo(11f, 16f)
                lineTo(11f, 18f)
                lineTo(13f, 18f)
                lineTo(13f, 16f)
            }
        }.build()
    }
}
