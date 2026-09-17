package com.missa.b360.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Jeu d'icônes de l'application — Material Design Icons servi par IconVaultKit
 * (iconvaultkit.com), converti en ImageVector Compose.
 *
 * Toujours teinter via `Icon(tint = ...)` ; le remplissage noir n'est qu'un
 * gabarit. Les noms reprennent ceux de Material pour une migration directe.
 */
object Iv {

    /** chart-box-outline — Material Design Icons (IconVaultKit). */
    val Analytics: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvAnalytics",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(9f, 17f)
                lineTo(7f, 17f)
                lineTo(7f, 10f)
                lineTo(9f, 10f)
                close()
                lineTo(13f, 17f)
                lineTo(11f, 17f)
                lineTo(11f, 7f)
                lineTo(13f, 7f)
                close()
                lineTo(17f, 17f)
                lineTo(15f, 17f)
                lineTo(15f, 13f)
                lineTo(17f, 13f)
                close()
                lineTo(19f, 19f)
                lineTo(5f, 19f)
                lineTo(5f, 5f)
                lineTo(19f, 5f)
                lineTo(19f, 19.1f)
                lineTo(19f, 3f)
                lineTo(5f, 3f)
                curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
                lineTo(3f, 19f)
                curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
                lineTo(19f, 21f)
                curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
                lineTo(21f, 5f)
                curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
            }
        }.build()
    }

    /** plus — Material Design Icons (IconVaultKit). */
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

    /** menu-down — Material Design Icons (IconVaultKit). */
    val ArrowDropDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvArrowDropDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(7f, 10f)
                lineTo(12f, 15f)
                lineTo(17f, 10f)
                close()
            }
        }.build()
    }

    /** cloud-upload-outline — Material Design Icons (IconVaultKit). */
    val Backup: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvBackup",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6.5f, 20f)
                quadTo(4.22f, 20f, 2.61f, 18.43f)
                quadTo(1f, 16.85f, 1f, 14.58f)
                quadTo(1f, 12.63f, 2.17f, 11.1f)
                quadTo(3.35f, 9.57f, 5.25f, 9.15f)
                quadTo(5.88f, 6.85f, 7.75f, 5.43f)
                quadTo(9.63f, 4f, 12f, 4f)
                quadTo(14.93f, 4f, 16.96f, 6.04f)
                quadTo(19f, 8.07f, 19f, 11f)
                quadTo(20.73f, 11.2f, 21.86f, 12.5f)
                quadTo(23f, 13.78f, 23f, 15.5f)
                quadTo(23f, 17.38f, 21.69f, 18.69f)
                quadTo(20.38f, 20f, 18.5f, 20f)
                lineTo(13f, 20f)
                quadTo(12.18f, 20f, 11.59f, 19.41f)
                quadTo(11f, 18.83f, 11f, 18f)
                lineTo(11f, 12.85f)
                lineTo(9.4f, 14.4f)
                lineTo(8f, 13f)
                lineTo(12f, 9f)
                lineTo(16f, 13f)
                lineTo(14.6f, 14.4f)
                lineTo(13f, 12.85f)
                lineTo(13f, 18f)
                lineTo(18.5f, 18f)
                quadTo(19.55f, 18f, 20.27f, 17.27f)
                quadTo(21f, 16.55f, 21f, 15.5f)
                quadTo(21f, 14.45f, 20.27f, 13.73f)
                quadTo(19.55f, 13f, 18.5f, 13f)
                lineTo(17f, 13f)
                lineTo(17f, 11f)
                quadTo(17f, 8.93f, 15.54f, 7.46f)
                quadTo(14.08f, 6f, 12f, 6f)
                quadTo(9.93f, 6f, 8.46f, 7.46f)
                quadTo(7f, 8.93f, 7f, 11f)
                lineTo(6.5f, 11f)
                quadTo(5.05f, 11f, 4.03f, 12.03f)
                quadTo(3f, 13.05f, 3f, 14.5f)
                quadTo(3f, 15.95f, 4.03f, 17f)
                quadTo(5.05f, 18f, 6.5f, 18f)
                lineTo(9f, 18f)
                lineTo(9f, 20f)
                lineTo(12f, 13f)
            }
        }.build()
    }

    /** badge-account-outline — Material Design Icons (IconVaultKit). */
    val Badge: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvBadge",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(17f, 3f)
                lineTo(14f, 3f)
                lineTo(14f, 5f)
                lineTo(17f, 5f)
                lineTo(17f, 21f)
                lineTo(7f, 21f)
                lineTo(7f, 5f)
                lineTo(10f, 5f)
                lineTo(10f, 3f)
                lineTo(7f, 3f)
                arcTo(2f, 2f, 0f, false, false, 5f, 5f)
                lineTo(5f, 21f)
                arcTo(2f, 2f, 0f, false, false, 7f, 23f)
                lineTo(17f, 23f)
                arcTo(2f, 2f, 0f, false, false, 19f, 21f)
                lineTo(19f, 5f)
                arcTo(2f, 2f, 0f, false, false, 17f, 3f)
                lineTo(12f, 7f)
                arcTo(2f, 2f, 0f, false, true, 14f, 9f)
                arcTo(2f, 2f, 0f, false, true, 12f, 11f)
                arcTo(2f, 2f, 0f, false, true, 10f, 9f)
                arcTo(2f, 2f, 0f, false, true, 12f, 7f)
                lineTo(16f, 15f)
                lineTo(8f, 15f)
                lineTo(8f, 14f)
                curveTo(8f, 12.67f, 10.67f, 12f, 12f, 12f)
                curveTo(13.33f, 12f, 16f, 12.67f, 16f, 14f)
                close()
                lineTo(16f, 18f)
                lineTo(8f, 18f)
                lineTo(8f, 17f)
                lineTo(16f, 17f)
                close()
                lineTo(12f, 20f)
                lineTo(8f, 20f)
                lineTo(8f, 19f)
                lineTo(12f, 19f)
                close()
                lineTo(13f, 5f)
                lineTo(11f, 5f)
                lineTo(11f, 1f)
                lineTo(13f, 1f)
                close()
            }
        }.build()
    }

    /** chart-bar — Material Design Icons (IconVaultKit). */
    val BarChart: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvBarChart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(22f, 21f)
                lineTo(2f, 21f)
                lineTo(2f, 3f)
                lineTo(4f, 3f)
                lineTo(4f, 19f)
                lineTo(6f, 19f)
                lineTo(6f, 10f)
                lineTo(10f, 10f)
                lineTo(10f, 19f)
                lineTo(12f, 19f)
                lineTo(12f, 6f)
                lineTo(16f, 6f)
                lineTo(16f, 19f)
                lineTo(18f, 19f)
                lineTo(18f, 14f)
                lineTo(22f, 14f)
                close()
            }
        }.build()
    }

    /** office-building-outline — Material Design Icons (IconVaultKit). */
    val Business: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvBusiness",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 3f)
                lineTo(19f, 21f)
                lineTo(13f, 21f)
                lineTo(13f, 17.5f)
                lineTo(11f, 17.5f)
                lineTo(11f, 21f)
                lineTo(5f, 21f)
                lineTo(5f, 3f)
                close()
                lineTo(15f, 7f)
                lineTo(17f, 7f)
                lineTo(17f, 5f)
                lineTo(15f, 5f)
                close()
                lineTo(11f, 7f)
                lineTo(13f, 7f)
                lineTo(13f, 5f)
                lineTo(11f, 5f)
                close()
                lineTo(7f, 7f)
                lineTo(9f, 7f)
                lineTo(9f, 5f)
                lineTo(7f, 5f)
                close()
                lineTo(15f, 11f)
                lineTo(17f, 11f)
                lineTo(17f, 9f)
                lineTo(15f, 9f)
                close()
                lineTo(11f, 11f)
                lineTo(13f, 11f)
                lineTo(13f, 9f)
                lineTo(11f, 9f)
                close()
                lineTo(7f, 11f)
                lineTo(9f, 11f)
                lineTo(9f, 9f)
                lineTo(7f, 9f)
                close()
                lineTo(15f, 15f)
                lineTo(17f, 15f)
                lineTo(17f, 13f)
                lineTo(15f, 13f)
                close()
                lineTo(11f, 15f)
                lineTo(13f, 15f)
                lineTo(13f, 13f)
                lineTo(11f, 13f)
                close()
                lineTo(7f, 15f)
                lineTo(9f, 15f)
                lineTo(9f, 13f)
                lineTo(7f, 13f)
                close()
                lineTo(15f, 19f)
                lineTo(17f, 19f)
                lineTo(17f, 17f)
                lineTo(15f, 17f)
                close()
                lineTo(7f, 19f)
                lineTo(9f, 19f)
                lineTo(9f, 17f)
                lineTo(7f, 17f)
                close()
                lineTo(21f, 1f)
                lineTo(3f, 1f)
                lineTo(3f, 23f)
                lineTo(21f, 23f)
                close()
            }
        }.build()
    }

    /** wrench-outline — Material Design Icons (IconVaultKit). */
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

    /** phone-outline — Material Design Icons (IconVaultKit). */
    val Call: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvCall",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20f, 15.5f)
                curveTo(18.8f, 15.5f, 17.5f, 15.3f, 16.4f, 14.9f)
                lineTo(16.1f, 14.9f)
                curveTo(15.8f, 14.9f, 15.6f, 15f, 15.4f, 15.2f)
                lineTo(13.2f, 17.4f)
                curveTo(10.4f, 15.9f, 8f, 13.6f, 6.6f, 10.8f)
                lineTo(8.8f, 8.6f)
                curveTo(9.1f, 8.3f, 9.2f, 7.9f, 9f, 7.6f)
                curveTo(8.7f, 6.5f, 8.5f, 5.2f, 8.5f, 4f)
                curveTo(8.5f, 3.5f, 8f, 3f, 7.5f, 3f)
                lineTo(4f, 3f)
                curveTo(3.5f, 3f, 3f, 3.5f, 3f, 4f)
                curveTo(3f, 13.4f, 10.6f, 21f, 20f, 21f)
                curveTo(20.5f, 21f, 21f, 20.5f, 21f, 20f)
                lineTo(21f, 16.5f)
                curveTo(21f, 16f, 20.5f, 15.5f, 20f, 15.5f)
                lineTo(5f, 5f)
                lineTo(6.5f, 5f)
                curveTo(6.6f, 5.9f, 6.8f, 6.8f, 7f, 7.6f)
                lineTo(5.8f, 8.8f)
                curveTo(5.4f, 7.6f, 5.1f, 6.3f, 5f, 5f)
                lineTo(19f, 19f)
                curveTo(17.7f, 18.9f, 16.4f, 18.6f, 15.2f, 18.2f)
                lineTo(16.4f, 17f)
                curveTo(17.2f, 17.2f, 18.1f, 17.4f, 19f, 17.4f)
                close()
            }
        }.build()
    }

    /** bullhorn-outline — Material Design Icons (IconVaultKit). */
    val Campaign: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvCampaign",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 8f)
                lineTo(4f, 8f)
                arcTo(2f, 2f, 0f, false, false, 2f, 10f)
                lineTo(2f, 14f)
                arcTo(2f, 2f, 0f, false, false, 4f, 16f)
                lineTo(5f, 16f)
                lineTo(5f, 20f)
                arcTo(1f, 1f, 0f, false, false, 6f, 21f)
                lineTo(8f, 21f)
                arcTo(1f, 1f, 0f, false, false, 9f, 20f)
                lineTo(9f, 16f)
                lineTo(12f, 16f)
                lineTo(17f, 20f)
                lineTo(17f, 4f)
                close()
                lineTo(15f, 15.6f)
                lineTo(13f, 14f)
                lineTo(4f, 14f)
                lineTo(4f, 10f)
                lineTo(13f, 10f)
                lineTo(15f, 8.4f)
                close()
                lineTo(21.5f, 12f)
                curveTo(21.5f, 13.71f, 20.54f, 15.26f, 19f, 16f)
                lineTo(19f, 8f)
                curveTo(20.53f, 8.75f, 21.5f, 10.3f, 21.5f, 12f)
            }
        }.build()
    }

    /** close-circle-outline — Material Design Icons (IconVaultKit). */
    val Cancel: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvCancel",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 20f)
                curveTo(7.59f, 20f, 4f, 16.41f, 4f, 12f)
                curveTo(4f, 7.59f, 7.59f, 4f, 12f, 4f)
                curveTo(16.41f, 4f, 20f, 7.59f, 20f, 12f)
                curveTo(20f, 16.41f, 16.41f, 20f, 12f, 20f)
                lineTo(12f, 2f)
                curveTo(6.47f, 2f, 2f, 6.47f, 2f, 12f)
                curveTo(2f, 17.53f, 6.47f, 22f, 12f, 22f)
                curveTo(17.53f, 22f, 22f, 17.53f, 22f, 12f)
                curveTo(22f, 6.47f, 17.53f, 2f, 12f, 2f)
                lineTo(14.59f, 8f)
                lineTo(12f, 10.59f)
                lineTo(9.41f, 8f)
                lineTo(8f, 9.41f)
                lineTo(10.59f, 12f)
                lineTo(8f, 14.59f)
                lineTo(9.41f, 16f)
                lineTo(12f, 13.41f)
                lineTo(14.59f, 16f)
                lineTo(16f, 14.59f)
                lineTo(13.41f, 12f)
                lineTo(16f, 9.41f)
                close()
            }
        }.build()
    }

    /** shape-outline — Material Design Icons (IconVaultKit). */
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

    /** check — Material Design Icons (IconVaultKit). */
    val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(21f, 7f)
                lineTo(9f, 19f)
                lineTo(3.5f, 13.5f)
                lineTo(4.91f, 12.09f)
                lineTo(9f, 16.17f)
                lineTo(19.59f, 5.59f)
                close()
            }
        }.build()
    }

    /** check-circle-outline — Material Design Icons (IconVaultKit). */
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

    /** format-list-checks — Material Design Icons (IconVaultKit). */
    val Checklist: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvChecklist",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 5f)
                lineTo(9f, 5f)
                lineTo(9f, 11f)
                lineTo(3f, 11f)
                close()
                lineTo(5f, 7f)
                lineTo(5f, 9f)
                lineTo(7f, 9f)
                lineTo(7f, 7f)
                close()
                lineTo(11f, 7f)
                lineTo(21f, 7f)
                lineTo(21f, 9f)
                lineTo(11f, 9f)
                close()
                lineTo(11f, 15f)
                lineTo(21f, 15f)
                lineTo(21f, 17f)
                lineTo(11f, 17f)
                close()
                lineTo(5f, 20f)
                lineTo(1.5f, 16.5f)
                lineTo(2.91f, 15.09f)
                lineTo(5f, 17.17f)
                lineTo(9.59f, 12.59f)
                lineTo(11f, 14f)
                close()
            }
        }.build()
    }

    /** chevron-right — Material Design Icons (IconVaultKit). */
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

    /** close — Material Design Icons (IconVaultKit). */
    val Close: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvClose",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 6.41f)
                lineTo(17.59f, 5f)
                lineTo(12f, 10.59f)
                lineTo(6.41f, 5f)
                lineTo(5f, 6.41f)
                lineTo(10.59f, 12f)
                lineTo(5f, 17.59f)
                lineTo(6.41f, 19f)
                lineTo(12f, 13.41f)
                lineTo(17.59f, 19f)
                lineTo(19f, 17.59f)
                lineTo(13.41f, 12f)
                close()
            }
        }.build()
    }

    /** cloud-check-outline — Material Design Icons (IconVaultKit). */
    val CloudDone: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvCloudDone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(13f, 19f)
                curveTo(13f, 19.34f, 13.04f, 19.67f, 13.09f, 20f)
                lineTo(6.5f, 20f)
                curveTo(5f, 20f, 3.69f, 19.5f, 2.61f, 18.43f)
                curveTo(1.54f, 17.38f, 1f, 16.09f, 1f, 14.58f)
                quadTo(1f, 12.63f, 2.17f, 11.1f)
                curveTo(3.34f, 9.57f, 4f, 9.43f, 5.25f, 9.15f)
                curveTo(5.67f, 7.62f, 6.5f, 6.38f, 7.75f, 5.43f)
                curveTo(9f, 4.48f, 10.42f, 4f, 12f, 4f)
                curveTo(13.95f, 4f, 15.6f, 4.68f, 16.96f, 6.04f)
                curveTo(18.32f, 7.4f, 19f, 9.05f, 19f, 11f)
                curveTo(20.15f, 11.13f, 21.1f, 11.63f, 21.86f, 12.5f)
                curveTo(22.37f, 13.07f, 22.7f, 13.71f, 22.86f, 14.42f)
                arcTo(5.9f, 5.9f, 0f, false, false, 19f, 13f)
                lineTo(17f, 13f)
                lineTo(17f, 11f)
                curveTo(17f, 9.62f, 16.5f, 8.44f, 15.54f, 7.46f)
                curveTo(14.56f, 6.5f, 13.38f, 6f, 12f, 6f)
                curveTo(10.62f, 6f, 9.44f, 6.5f, 8.46f, 7.46f)
                curveTo(7.5f, 8.44f, 7f, 9.62f, 7f, 11f)
                lineTo(6.5f, 11f)
                curveTo(5.53f, 11f, 4.71f, 11.34f, 4.03f, 12.03f)
                curveTo(3.34f, 12.71f, 3f, 13.53f, 3f, 14.5f)
                curveTo(3f, 15.47f, 3.34f, 16.29f, 4.03f, 17f)
                curveTo(4.71f, 17.66f, 5.53f, 18f, 6.5f, 18f)
                lineTo(13.09f, 18f)
                curveTo(13.04f, 18.33f, 13f, 18.66f, 13f, 19f)
                lineTo(17.75f, 19.43f)
                lineTo(16.16f, 17.84f)
                lineTo(15f, 19f)
                lineTo(17.75f, 22f)
                lineTo(22.5f, 17.25f)
                lineTo(21.34f, 15.84f)
                close()
            }
        }.build()
    }

    /** hammer-wrench — Material Design Icons (IconVaultKit). */
    val Construction: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvConstruction",
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

    /** content-copy — Material Design Icons (IconVaultKit). */
    val ContentCopy: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvContentCopy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 21f)
                lineTo(8f, 21f)
                lineTo(8f, 7f)
                lineTo(19f, 7f)
                lineTo(19f, 5f)
                lineTo(8f, 5f)
                arcTo(2f, 2f, 0f, false, false, 6f, 7f)
                lineTo(6f, 21f)
                arcTo(2f, 2f, 0f, false, false, 8f, 23f)
                lineTo(19f, 23f)
                arcTo(2f, 2f, 0f, false, false, 21f, 21f)
                lineTo(21f, 7f)
                arcTo(2f, 2f, 0f, false, false, 19f, 5f)
                lineTo(16f, 1f)
                lineTo(4f, 1f)
                arcTo(2f, 2f, 0f, false, false, 2f, 3f)
                lineTo(2f, 17f)
                lineTo(4f, 17f)
                lineTo(4f, 3f)
                lineTo(16f, 3f)
                close()
            }
        }.build()
    }

    /** trash-can-outline — Material Design Icons (IconVaultKit). */
    val DeleteOutline: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvDeleteOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(9f, 3f)
                lineTo(9f, 4f)
                lineTo(4f, 4f)
                lineTo(4f, 6f)
                lineTo(5f, 6f)
                lineTo(5f, 19f)
                arcTo(2f, 2f, 0f, false, false, 7f, 21f)
                lineTo(17f, 21f)
                arcTo(2f, 2f, 0f, false, false, 19f, 19f)
                lineTo(19f, 6f)
                lineTo(20f, 6f)
                lineTo(20f, 4f)
                lineTo(15f, 4f)
                lineTo(15f, 3f)
                close()
                lineTo(7f, 6f)
                lineTo(17f, 6f)
                lineTo(17f, 19f)
                lineTo(7f, 19f)
                close()
                lineTo(9f, 8f)
                lineTo(9f, 17f)
                lineTo(11f, 17f)
                lineTo(11f, 8f)
                close()
                lineTo(13f, 8f)
                lineTo(13f, 17f)
                lineTo(15f, 17f)
                lineTo(15f, 8f)
                close()
            }
        }.build()
    }

    /** file-document-outline — Material Design Icons (IconVaultKit). */
    val Description: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvDescription",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 2f)
                arcTo(2f, 2f, 0f, false, false, 4f, 4f)
                lineTo(4f, 20f)
                arcTo(2f, 2f, 0f, false, false, 6f, 22f)
                lineTo(18f, 22f)
                arcTo(2f, 2f, 0f, false, false, 20f, 20f)
                lineTo(20f, 8f)
                lineTo(14f, 2f)
                close()
                lineTo(6f, 4f)
                lineTo(13f, 4f)
                lineTo(13f, 9f)
                lineTo(18f, 9f)
                lineTo(18f, 20f)
                lineTo(6f, 20f)
                close()
                lineTo(8f, 12f)
                lineTo(8f, 14f)
                lineTo(16f, 14f)
                lineTo(16f, 12f)
                close()
                lineTo(8f, 16f)
                lineTo(8f, 18f)
                lineTo(13f, 18f)
                lineTo(13f, 16f)
                close()
            }
        }.build()
    }

    /** download — Material Design Icons (IconVaultKit). */
    val Download: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvDownload",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(5f, 20f)
                lineTo(19f, 20f)
                lineTo(19f, 18f)
                lineTo(5f, 18f)
                lineTo(19f, 9f)
                lineTo(15f, 9f)
                lineTo(15f, 3f)
                lineTo(9f, 3f)
                lineTo(9f, 9f)
                lineTo(5f, 9f)
                lineTo(12f, 16f)
                close()
            }
        }.build()
    }

    /** email-outline — Material Design Icons (IconVaultKit). */
    val Email: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvEmail",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(22f, 6f)
                curveTo(22f, 4.9f, 21.1f, 4f, 20f, 4f)
                lineTo(4f, 4f)
                curveTo(2.9f, 4f, 2f, 4.9f, 2f, 6f)
                lineTo(2f, 18f)
                curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
                lineTo(20f, 20f)
                curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
                close()
                lineTo(20f, 6f)
                lineTo(12f, 11f)
                lineTo(4f, 6f)
                close()
                lineTo(20f, 18f)
                lineTo(4f, 18f)
                lineTo(4f, 8f)
                lineTo(12f, 13f)
                lineTo(20f, 8f)
                close()
            }
        }.build()
    }

    /** chevron-up — Material Design Icons (IconVaultKit). */
    val ExpandLess: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvExpandLess",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(7.41f, 15.41f)
                lineTo(12f, 10.83f)
                lineTo(16.59f, 15.41f)
                lineTo(18f, 14f)
                lineTo(12f, 8f)
                lineTo(6f, 14f)
                close()
            }
        }.build()
    }

    /** chevron-down — Material Design Icons (IconVaultKit). */
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

    /** gavel — Material Design Icons (IconVaultKit). */
    val Gavel: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvGavel",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(2.3f, 20.28f)
                lineTo(11.9f, 10.68f)
                lineTo(10.5f, 9.26f)
                lineTo(9.78f, 9.97f)
                arcTo(0.996f, 0.996f, 0f, false, true, 8.37f, 9.97f)
                lineTo(7.66f, 9.26f)
                arcTo(0.996f, 0.996f, 0f, false, true, 7.66f, 7.85f)
                lineTo(13.32f, 2.19f)
                arcTo(0.996f, 0.996f, 0f, false, true, 14.73f, 2.19f)
                lineTo(15.44f, 2.9f)
                curveTo(15.83f, 3.29f, 15.83f, 3.92f, 15.44f, 4.31f)
                lineTo(14.73f, 5f)
                lineTo(16.15f, 6.43f)
                arcTo(0.996f, 0.996f, 0f, false, true, 17.56f, 6.43f)
                curveTo(17.95f, 6.82f, 17.95f, 7.46f, 17.56f, 7.85f)
                lineTo(18.97f, 9.26f)
                lineTo(19.68f, 8.55f)
                curveTo(20.07f, 8.16f, 20.71f, 8.16f, 21.1f, 8.55f)
                lineTo(21.8f, 9.26f)
                curveTo(22.19f, 9.65f, 22.19f, 10.29f, 21.8f, 10.68f)
                lineTo(16.15f, 16.33f)
                curveTo(15.76f, 16.72f, 15.12f, 16.72f, 14.73f, 16.33f)
                lineTo(14.03f, 15.63f)
                arcTo(0.99f, 0.99f, 0f, false, true, 14.03f, 14.21f)
                lineTo(14.73f, 13.5f)
                lineTo(13.32f, 12.09f)
                lineTo(3.71f, 21.7f)
                arcTo(0.996f, 0.996f, 0f, false, true, 2.3f, 21.7f)
                curveTo(1.91f, 21.31f, 1.91f, 20.67f, 2.3f, 20.28f)
                lineTo(20f, 19f)
                arcTo(2f, 2f, 0f, false, true, 22f, 21f)
                lineTo(22f, 22f)
                lineTo(12f, 22f)
                lineTo(12f, 21f)
                arcTo(2f, 2f, 0f, false, true, 14f, 19f)
                close()
            }
        }.build()
    }

    /** account-group-outline — Material Design Icons (IconVaultKit). */
    val Group: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvGroup",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 5f)
                arcTo(3.5f, 3.5f, 0f, false, false, 8.5f, 8.5f)
                arcTo(3.5f, 3.5f, 0f, false, false, 12f, 12f)
                arcTo(3.5f, 3.5f, 0f, false, false, 15.5f, 8.5f)
                arcTo(3.5f, 3.5f, 0f, false, false, 12f, 5f)
                lineTo(12f, 7f)
                arcTo(1.5f, 1.5f, 0f, false, true, 13.5f, 8.5f)
                arcTo(1.5f, 1.5f, 0f, false, true, 12f, 10f)
                arcTo(1.5f, 1.5f, 0f, false, true, 10.5f, 8.5f)
                arcTo(1.5f, 1.5f, 0f, false, true, 12f, 7f)
                lineTo(5.5f, 8f)
                arcTo(2.5f, 2.5f, 0f, false, false, 3f, 10.5f)
                curveTo(3f, 11.44f, 3.53f, 12.25f, 4.29f, 12.68f)
                curveTo(4.65f, 12.88f, 5.06f, 13f, 5.5f, 13f)
                curveTo(5.94f, 13f, 6.35f, 12.88f, 6.71f, 12.68f)
                curveTo(7.08f, 12.47f, 7.39f, 12.17f, 7.62f, 11.81f)
                arcTo(5.42f, 5.42f, 0f, false, true, 6.5f, 8.5f)
                lineTo(6.5f, 8.22f)
                curveTo(6.2f, 8.08f, 5.86f, 8f, 5.5f, 8f)
                lineTo(18.5f, 8f)
                curveTo(18.14f, 8f, 17.8f, 8.08f, 17.5f, 8.22f)
                lineTo(17.5f, 8.5f)
                curveTo(17.5f, 9.7f, 17.11f, 10.86f, 16.38f, 11.81f)
                curveTo(16.5f, 12f, 16.63f, 12.15f, 16.78f, 12.3f)
                arcTo(2.48f, 2.48f, 0f, false, false, 18.5f, 13f)
                curveTo(18.94f, 13f, 19.35f, 12.88f, 19.71f, 12.68f)
                curveTo(20.47f, 12.25f, 21f, 11.44f, 21f, 10.5f)
                arcTo(2.5f, 2.5f, 0f, false, false, 18.5f, 8f)
                lineTo(12f, 14f)
                curveTo(9.66f, 14f, 5f, 15.17f, 5f, 17.5f)
                lineTo(5f, 19f)
                lineTo(19f, 19f)
                lineTo(19f, 17.5f)
                curveTo(19f, 15.17f, 14.34f, 14f, 12f, 14f)
                lineTo(4.71f, 14.55f)
                curveTo(2.78f, 14.78f, 0f, 15.76f, 0f, 17.5f)
                lineTo(0f, 19f)
                lineTo(3f, 19f)
                lineTo(3f, 17.07f)
                curveTo(3f, 16.06f, 3.69f, 15.22f, 4.71f, 14.55f)
                lineTo(19.29f, 14.55f)
                curveTo(20.31f, 15.22f, 21f, 16.06f, 21f, 17.07f)
                lineTo(21f, 19f)
                lineTo(24f, 19f)
                lineTo(24f, 17.5f)
                curveTo(24f, 15.76f, 21.22f, 14.78f, 19.29f, 14.55f)
                lineTo(12f, 16f)
                curveTo(13.53f, 16f, 15.24f, 16.5f, 16.23f, 17f)
                lineTo(7.77f, 17f)
                curveTo(8.76f, 16.5f, 10.47f, 16f, 12f, 16f)
            }
        }.build()
    }

    /** account-group-outline — Material Design Icons (IconVaultKit). */
    val Groups: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvGroups",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 5f)
                arcTo(3.5f, 3.5f, 0f, false, false, 8.5f, 8.5f)
                arcTo(3.5f, 3.5f, 0f, false, false, 12f, 12f)
                arcTo(3.5f, 3.5f, 0f, false, false, 15.5f, 8.5f)
                arcTo(3.5f, 3.5f, 0f, false, false, 12f, 5f)
                lineTo(12f, 7f)
                arcTo(1.5f, 1.5f, 0f, false, true, 13.5f, 8.5f)
                arcTo(1.5f, 1.5f, 0f, false, true, 12f, 10f)
                arcTo(1.5f, 1.5f, 0f, false, true, 10.5f, 8.5f)
                arcTo(1.5f, 1.5f, 0f, false, true, 12f, 7f)
                lineTo(5.5f, 8f)
                arcTo(2.5f, 2.5f, 0f, false, false, 3f, 10.5f)
                curveTo(3f, 11.44f, 3.53f, 12.25f, 4.29f, 12.68f)
                curveTo(4.65f, 12.88f, 5.06f, 13f, 5.5f, 13f)
                curveTo(5.94f, 13f, 6.35f, 12.88f, 6.71f, 12.68f)
                curveTo(7.08f, 12.47f, 7.39f, 12.17f, 7.62f, 11.81f)
                arcTo(5.42f, 5.42f, 0f, false, true, 6.5f, 8.5f)
                lineTo(6.5f, 8.22f)
                curveTo(6.2f, 8.08f, 5.86f, 8f, 5.5f, 8f)
                lineTo(18.5f, 8f)
                curveTo(18.14f, 8f, 17.8f, 8.08f, 17.5f, 8.22f)
                lineTo(17.5f, 8.5f)
                curveTo(17.5f, 9.7f, 17.11f, 10.86f, 16.38f, 11.81f)
                curveTo(16.5f, 12f, 16.63f, 12.15f, 16.78f, 12.3f)
                arcTo(2.48f, 2.48f, 0f, false, false, 18.5f, 13f)
                curveTo(18.94f, 13f, 19.35f, 12.88f, 19.71f, 12.68f)
                curveTo(20.47f, 12.25f, 21f, 11.44f, 21f, 10.5f)
                arcTo(2.5f, 2.5f, 0f, false, false, 18.5f, 8f)
                lineTo(12f, 14f)
                curveTo(9.66f, 14f, 5f, 15.17f, 5f, 17.5f)
                lineTo(5f, 19f)
                lineTo(19f, 19f)
                lineTo(19f, 17.5f)
                curveTo(19f, 15.17f, 14.34f, 14f, 12f, 14f)
                lineTo(4.71f, 14.55f)
                curveTo(2.78f, 14.78f, 0f, 15.76f, 0f, 17.5f)
                lineTo(0f, 19f)
                lineTo(3f, 19f)
                lineTo(3f, 17.07f)
                curveTo(3f, 16.06f, 3.69f, 15.22f, 4.71f, 14.55f)
                lineTo(19.29f, 14.55f)
                curveTo(20.31f, 15.22f, 21f, 16.06f, 21f, 17.07f)
                lineTo(21f, 19f)
                lineTo(24f, 19f)
                lineTo(24f, 17.5f)
                curveTo(24f, 15.76f, 21.22f, 14.78f, 19.29f, 14.55f)
                lineTo(12f, 16f)
                curveTo(13.53f, 16f, 15.24f, 16.5f, 16.23f, 17f)
                lineTo(7.77f, 17f)
                curveTo(8.76f, 16.5f, 10.47f, 16f, 12f, 16f)
            }
        }.build()
    }

    /** handshake-outline — Material Design Icons (IconVaultKit). */
    val Handshake: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvHandshake",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(21.71f, 8.71f)
                curveTo(22.96f, 7.46f, 22.39f, 6f, 21.71f, 5.29f)
                lineTo(18.71f, 2.29f)
                curveTo(17.45f, 1.04f, 16f, 1.61f, 15.29f, 2.29f)
                lineTo(13.59f, 4f)
                lineTo(11f, 4f)
                curveTo(9.1f, 4f, 8f, 5f, 7.44f, 6.15f)
                lineTo(3f, 10.59f)
                lineTo(3f, 14.59f)
                lineTo(2.29f, 15.29f)
                curveTo(1.04f, 16.55f, 1.61f, 18f, 2.29f, 18.71f)
                lineTo(5.29f, 21.71f)
                curveTo(5.83f, 22.25f, 6.41f, 22.45f, 6.96f, 22.45f)
                curveTo(7.67f, 22.45f, 8.32f, 22.1f, 8.71f, 21.71f)
                lineTo(11.41f, 19f)
                lineTo(15f, 19f)
                curveTo(16.7f, 19f, 17.56f, 17.94f, 17.87f, 16.9f)
                curveTo(19f, 16.6f, 19.62f, 15.74f, 19.87f, 14.9f)
                curveTo(21.42f, 14.5f, 22f, 13.03f, 22f, 12f)
                lineTo(22f, 9f)
                lineTo(21.41f, 9f)
                close()
                lineTo(20f, 12f)
                curveTo(20f, 12.45f, 19.81f, 13f, 19f, 13f)
                lineTo(18f, 13f)
                lineTo(18f, 14f)
                curveTo(18f, 14.45f, 17.81f, 15f, 17f, 15f)
                lineTo(16f, 15f)
                lineTo(16f, 16f)
                curveTo(16f, 16.45f, 15.81f, 17f, 15f, 17f)
                lineTo(10.59f, 17f)
                lineTo(7.31f, 20.28f)
                curveTo(7f, 20.57f, 6.82f, 20.4f, 6.71f, 20.29f)
                lineTo(3.72f, 17.31f)
                curveTo(3.43f, 17f, 3.6f, 16.82f, 3.71f, 16.71f)
                lineTo(5f, 15.41f)
                lineTo(5f, 11.41f)
                lineTo(7f, 9.41f)
                lineTo(7f, 11f)
                curveTo(7f, 12.21f, 7.8f, 14f, 10f, 14f)
                curveTo(12.2f, 14f, 13f, 12.21f, 13f, 11f)
                lineTo(20f, 11f)
                close()
                lineTo(20.29f, 7.29f)
                lineTo(18.59f, 9f)
                lineTo(11f, 9f)
                lineTo(11f, 11f)
                curveTo(11f, 11.45f, 10.81f, 12f, 10f, 12f)
                curveTo(9.19f, 12f, 9f, 11.45f, 9f, 11f)
                lineTo(9f, 8f)
                curveTo(9f, 7.54f, 9.17f, 6f, 11f, 6f)
                lineTo(14.41f, 6f)
                lineTo(16.69f, 3.72f)
                curveTo(17f, 3.43f, 17.18f, 3.6f, 17.29f, 3.71f)
                lineTo(20.28f, 6.69f)
                curveTo(20.57f, 7f, 20.4f, 7.18f, 20.29f, 7.29f)
            }
        }.build()
    }

    /** history — Material Design Icons (IconVaultKit). */
    val History: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvHistory",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(13.5f, 8f)
                lineTo(12f, 8f)
                lineTo(12f, 13f)
                lineTo(16.28f, 15.54f)
                lineTo(17f, 14.33f)
                lineTo(13.5f, 12.25f)
                close()
                lineTo(13f, 3f)
                arcTo(9f, 9f, 0f, false, false, 4f, 12f)
                lineTo(1f, 12f)
                lineTo(4.96f, 16.03f)
                lineTo(9f, 12f)
                lineTo(6f, 12f)
                arcTo(7f, 7f, 0f, false, true, 13f, 5f)
                arcTo(7f, 7f, 0f, false, true, 20f, 12f)
                arcTo(7f, 7f, 0f, false, true, 13f, 19f)
                curveTo(11.07f, 19f, 9.32f, 18.21f, 8.06f, 16.94f)
                lineTo(6.64f, 18.36f)
                arcTo(8.9f, 8.9f, 0f, false, false, 13f, 21f)
                arcTo(9f, 9f, 0f, false, false, 22f, 12f)
                arcTo(9f, 9f, 0f, false, false, 13f, 3f)
            }
        }.build()
    }

    /** home-outline — Material Design Icons (IconVaultKit). */
    val Home: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvHome",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 5.69f)
                lineTo(17f, 10.19f)
                lineTo(17f, 18f)
                lineTo(15f, 18f)
                lineTo(15f, 12f)
                lineTo(9f, 12f)
                lineTo(9f, 18f)
                lineTo(7f, 18f)
                lineTo(7f, 10.19f)
                close()
                lineTo(12f, 3f)
                lineTo(2f, 12f)
                lineTo(5f, 12f)
                lineTo(5f, 20f)
                lineTo(11f, 20f)
                lineTo(11f, 14f)
                lineTo(13f, 14f)
                lineTo(13f, 20f)
                lineTo(19f, 20f)
                lineTo(19f, 12f)
                lineTo(22f, 12f)
            }
        }.build()
    }

    /** image-outline — Material Design Icons (IconVaultKit). */
    val Image: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvImage",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 19f)
                lineTo(5f, 19f)
                lineTo(5f, 5f)
                lineTo(19f, 5f)
                lineTo(19f, 3f)
                lineTo(5f, 3f)
                arcTo(2f, 2f, 0f, false, false, 3f, 5f)
                lineTo(3f, 19f)
                arcTo(2f, 2f, 0f, false, false, 5f, 21f)
                lineTo(19f, 21f)
                arcTo(2f, 2f, 0f, false, false, 21f, 19f)
                lineTo(21f, 5f)
                arcTo(2f, 2f, 0f, false, false, 19f, 3f)
                lineTo(13.96f, 12.29f)
                lineTo(11.21f, 15.83f)
                lineTo(9.25f, 13.47f)
                lineTo(6.5f, 17f)
                lineTo(17.5f, 17f)
                close()
            }
        }.build()
    }

    /** information-outline — Material Design Icons (IconVaultKit). */
    val Info: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvInfo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11f, 9f)
                lineTo(13f, 9f)
                lineTo(13f, 7f)
                lineTo(11f, 7f)
                lineTo(12f, 20f)
                curveTo(7.59f, 20f, 4f, 16.41f, 4f, 12f)
                curveTo(4f, 7.59f, 7.59f, 4f, 12f, 4f)
                curveTo(16.41f, 4f, 20f, 7.59f, 20f, 12f)
                curveTo(20f, 16.41f, 16.41f, 20f, 12f, 20f)
                lineTo(12f, 2f)
                arcTo(10f, 10f, 0f, false, false, 2f, 12f)
                arcTo(10f, 10f, 0f, false, false, 12f, 22f)
                arcTo(10f, 10f, 0f, false, false, 22f, 12f)
                arcTo(10f, 10f, 0f, false, false, 12f, 2f)
                lineTo(11f, 17f)
                lineTo(13f, 17f)
                lineTo(13f, 11f)
                lineTo(11f, 11f)
                close()
            }
        }.build()
    }

    /** package-variant — Material Design Icons (IconVaultKit). */
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

    /** drag-horizontal-variant — Material Design Icons (IconVaultKit). */
    val LineWeight: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvLineWeight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(21f, 11f)
                lineTo(3f, 11f)
                lineTo(3f, 9f)
                lineTo(21f, 9f)
                close()
                lineTo(21f, 13f)
                lineTo(3f, 13f)
                lineTo(3f, 15f)
                lineTo(21f, 15f)
                close()
            }
        }.build()
    }

    /** truck-outline — Material Design Icons (IconVaultKit). */
    val LocalShipping: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvLocalShipping",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18f, 18.5f)
                curveTo(18.83f, 18.5f, 19.5f, 17.83f, 19.5f, 17f)
                curveTo(19.5f, 16.17f, 18.83f, 15.5f, 18f, 15.5f)
                curveTo(17.17f, 15.5f, 16.5f, 16.17f, 16.5f, 17f)
                curveTo(16.5f, 17.83f, 17.17f, 18.5f, 18f, 18.5f)
                lineTo(19.5f, 9.5f)
                lineTo(17f, 9.5f)
                lineTo(17f, 12f)
                lineTo(21.46f, 12f)
                close()
                lineTo(6f, 18.5f)
                curveTo(6.83f, 18.5f, 7.5f, 17.83f, 7.5f, 17f)
                curveTo(7.5f, 16.17f, 6.83f, 15.5f, 6f, 15.5f)
                curveTo(5.17f, 15.5f, 4.5f, 16.17f, 4.5f, 17f)
                curveTo(4.5f, 17.83f, 5.17f, 18.5f, 6f, 18.5f)
                lineTo(20f, 8f)
                lineTo(23f, 12f)
                lineTo(23f, 17f)
                lineTo(21f, 17f)
                curveTo(21f, 18.66f, 19.66f, 20f, 18f, 20f)
                curveTo(16.34f, 20f, 15f, 18.66f, 15f, 17f)
                lineTo(9f, 17f)
                curveTo(9f, 18.66f, 7.66f, 20f, 6f, 20f)
                curveTo(4.34f, 20f, 3f, 18.66f, 3f, 17f)
                lineTo(1f, 17f)
                lineTo(1f, 6f)
                curveTo(1f, 4.89f, 1.89f, 4f, 3f, 4f)
                lineTo(17f, 4f)
                lineTo(17f, 8f)
                close()
                lineTo(3f, 6f)
                lineTo(3f, 15f)
                lineTo(3.76f, 15f)
                curveTo(4.31f, 14.39f, 5.11f, 14f, 6f, 14f)
                curveTo(6.89f, 14f, 7.69f, 14.39f, 8.24f, 15f)
                lineTo(15f, 15f)
                lineTo(15f, 6f)
                close()
            }
        }.build()
    }

    /** lock-outline — Material Design Icons (IconVaultKit). */
    val Lock: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvLock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 17f)
                arcTo(2f, 2f, 0f, false, true, 10f, 15f)
                curveTo(10f, 13.89f, 10.89f, 13f, 12f, 13f)
                arcTo(2f, 2f, 0f, false, true, 14f, 15f)
                arcTo(2f, 2f, 0f, false, true, 12f, 17f)
                lineTo(18f, 20f)
                lineTo(18f, 10f)
                lineTo(6f, 10f)
                lineTo(6f, 20f)
                close()
                lineTo(18f, 8f)
                arcTo(2f, 2f, 0f, false, true, 20f, 10f)
                lineTo(20f, 20f)
                arcTo(2f, 2f, 0f, false, true, 18f, 22f)
                lineTo(6f, 22f)
                arcTo(2f, 2f, 0f, false, true, 4f, 20f)
                lineTo(4f, 10f)
                curveTo(4f, 8.89f, 4.89f, 8f, 6f, 8f)
                lineTo(7f, 8f)
                lineTo(7f, 6f)
                arcTo(5f, 5f, 0f, false, true, 12f, 1f)
                arcTo(5f, 5f, 0f, false, true, 17f, 6f)
                lineTo(17f, 8f)
                close()
                lineTo(12f, 3f)
                arcTo(3f, 3f, 0f, false, false, 9f, 6f)
                lineTo(9f, 8f)
                lineTo(15f, 8f)
                lineTo(15f, 6f)
                arcTo(3f, 3f, 0f, false, false, 12f, 3f)
            }
        }.build()
    }

    /** email-outline — Material Design Icons (IconVaultKit). */
    val MailOutline: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvMailOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(22f, 6f)
                curveTo(22f, 4.9f, 21.1f, 4f, 20f, 4f)
                lineTo(4f, 4f)
                curveTo(2.9f, 4f, 2f, 4.9f, 2f, 6f)
                lineTo(2f, 18f)
                curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
                lineTo(20f, 20f)
                curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
                close()
                lineTo(20f, 6f)
                lineTo(12f, 11f)
                lineTo(4f, 6f)
                close()
                lineTo(20f, 18f)
                lineTo(4f, 18f)
                lineTo(4f, 8f)
                lineTo(12f, 13f)
                lineTo(20f, 8f)
                close()
            }
        }.build()
    }

    /** menu — Material Design Icons (IconVaultKit). */
    val Menu: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvMenu",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 6f)
                lineTo(21f, 6f)
                lineTo(21f, 8f)
                lineTo(3f, 8f)
                close()
                lineTo(3f, 11f)
                lineTo(21f, 11f)
                lineTo(21f, 13f)
                lineTo(3f, 13f)
                close()
                lineTo(3f, 16f)
                lineTo(21f, 16f)
                lineTo(21f, 18f)
                lineTo(3f, 18f)
                close()
            }
        }.build()
    }

    /** dots-horizontal — Material Design Icons (IconVaultKit). */
    val MoreHoriz: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvMoreHoriz",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(16f, 12f)
                arcTo(2f, 2f, 0f, false, true, 18f, 10f)
                arcTo(2f, 2f, 0f, false, true, 20f, 12f)
                arcTo(2f, 2f, 0f, false, true, 18f, 14f)
                arcTo(2f, 2f, 0f, false, true, 16f, 12f)
                lineTo(10f, 12f)
                arcTo(2f, 2f, 0f, false, true, 12f, 10f)
                arcTo(2f, 2f, 0f, false, true, 14f, 12f)
                arcTo(2f, 2f, 0f, false, true, 12f, 14f)
                arcTo(2f, 2f, 0f, false, true, 10f, 12f)
                lineTo(4f, 12f)
                arcTo(2f, 2f, 0f, false, true, 6f, 10f)
                arcTo(2f, 2f, 0f, false, true, 8f, 12f)
                arcTo(2f, 2f, 0f, false, true, 6f, 14f)
                arcTo(2f, 2f, 0f, false, true, 4f, 12f)
            }
        }.build()
    }

    /** dots-vertical — Material Design Icons (IconVaultKit). */
    val MoreVert: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvMoreVert",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 16f)
                arcTo(2f, 2f, 0f, false, true, 14f, 18f)
                arcTo(2f, 2f, 0f, false, true, 12f, 20f)
                arcTo(2f, 2f, 0f, false, true, 10f, 18f)
                arcTo(2f, 2f, 0f, false, true, 12f, 16f)
                lineTo(12f, 10f)
                arcTo(2f, 2f, 0f, false, true, 14f, 12f)
                arcTo(2f, 2f, 0f, false, true, 12f, 14f)
                arcTo(2f, 2f, 0f, false, true, 10f, 12f)
                arcTo(2f, 2f, 0f, false, true, 12f, 10f)
                lineTo(12f, 4f)
                arcTo(2f, 2f, 0f, false, true, 14f, 6f)
                arcTo(2f, 2f, 0f, false, true, 12f, 8f)
                arcTo(2f, 2f, 0f, false, true, 10f, 6f)
                arcTo(2f, 2f, 0f, false, true, 12f, 4f)
            }
        }.build()
    }

    /** bell-outline — Material Design Icons (IconVaultKit). */
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

    /** credit-card-outline — Material Design Icons (IconVaultKit). */
    val Payments: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvPayments",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20f, 8f)
                lineTo(4f, 8f)
                lineTo(4f, 6f)
                lineTo(20f, 6f)
                lineTo(20f, 18f)
                lineTo(4f, 18f)
                lineTo(4f, 12f)
                lineTo(20f, 12f)
                lineTo(20f, 4f)
                lineTo(4f, 4f)
                curveTo(2.89f, 4f, 2f, 4.89f, 2f, 6f)
                lineTo(2f, 18f)
                arcTo(2f, 2f, 0f, false, false, 4f, 20f)
                lineTo(20f, 20f)
                arcTo(2f, 2f, 0f, false, false, 22f, 18f)
                lineTo(22f, 6f)
                arcTo(2f, 2f, 0f, false, false, 20f, 4f)
            }
        }.build()
    }

    /** account-group-outline — Material Design Icons (IconVaultKit). */
    val People: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvPeople",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 5f)
                arcTo(3.5f, 3.5f, 0f, false, false, 8.5f, 8.5f)
                arcTo(3.5f, 3.5f, 0f, false, false, 12f, 12f)
                arcTo(3.5f, 3.5f, 0f, false, false, 15.5f, 8.5f)
                arcTo(3.5f, 3.5f, 0f, false, false, 12f, 5f)
                lineTo(12f, 7f)
                arcTo(1.5f, 1.5f, 0f, false, true, 13.5f, 8.5f)
                arcTo(1.5f, 1.5f, 0f, false, true, 12f, 10f)
                arcTo(1.5f, 1.5f, 0f, false, true, 10.5f, 8.5f)
                arcTo(1.5f, 1.5f, 0f, false, true, 12f, 7f)
                lineTo(5.5f, 8f)
                arcTo(2.5f, 2.5f, 0f, false, false, 3f, 10.5f)
                curveTo(3f, 11.44f, 3.53f, 12.25f, 4.29f, 12.68f)
                curveTo(4.65f, 12.88f, 5.06f, 13f, 5.5f, 13f)
                curveTo(5.94f, 13f, 6.35f, 12.88f, 6.71f, 12.68f)
                curveTo(7.08f, 12.47f, 7.39f, 12.17f, 7.62f, 11.81f)
                arcTo(5.42f, 5.42f, 0f, false, true, 6.5f, 8.5f)
                lineTo(6.5f, 8.22f)
                curveTo(6.2f, 8.08f, 5.86f, 8f, 5.5f, 8f)
                lineTo(18.5f, 8f)
                curveTo(18.14f, 8f, 17.8f, 8.08f, 17.5f, 8.22f)
                lineTo(17.5f, 8.5f)
                curveTo(17.5f, 9.7f, 17.11f, 10.86f, 16.38f, 11.81f)
                curveTo(16.5f, 12f, 16.63f, 12.15f, 16.78f, 12.3f)
                arcTo(2.48f, 2.48f, 0f, false, false, 18.5f, 13f)
                curveTo(18.94f, 13f, 19.35f, 12.88f, 19.71f, 12.68f)
                curveTo(20.47f, 12.25f, 21f, 11.44f, 21f, 10.5f)
                arcTo(2.5f, 2.5f, 0f, false, false, 18.5f, 8f)
                lineTo(12f, 14f)
                curveTo(9.66f, 14f, 5f, 15.17f, 5f, 17.5f)
                lineTo(5f, 19f)
                lineTo(19f, 19f)
                lineTo(19f, 17.5f)
                curveTo(19f, 15.17f, 14.34f, 14f, 12f, 14f)
                lineTo(4.71f, 14.55f)
                curveTo(2.78f, 14.78f, 0f, 15.76f, 0f, 17.5f)
                lineTo(0f, 19f)
                lineTo(3f, 19f)
                lineTo(3f, 17.07f)
                curveTo(3f, 16.06f, 3.69f, 15.22f, 4.71f, 14.55f)
                lineTo(19.29f, 14.55f)
                curveTo(20.31f, 15.22f, 21f, 16.06f, 21f, 17.07f)
                lineTo(21f, 19f)
                lineTo(24f, 19f)
                lineTo(24f, 17.5f)
                curveTo(24f, 15.76f, 21.22f, 14.78f, 19.29f, 14.55f)
                lineTo(12f, 16f)
                curveTo(13.53f, 16f, 15.24f, 16.5f, 16.23f, 17f)
                lineTo(7.77f, 17f)
                curveTo(8.76f, 16.5f, 10.47f, 16f, 12f, 16f)
            }
        }.build()
    }

    /** percent-outline — Material Design Icons (IconVaultKit). */
    val Percent: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvPercent",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18.5f, 3.5f)
                lineTo(20.5f, 5.5f)
                lineTo(5.5f, 20.5f)
                lineTo(3.5f, 18.5f)
                close()
                lineTo(7f, 4f)
                curveTo(8.66f, 4f, 10f, 5.34f, 10f, 7f)
                curveTo(10f, 8.66f, 8.66f, 10f, 7f, 10f)
                curveTo(5.34f, 10f, 4f, 8.66f, 4f, 7f)
                curveTo(4f, 5.34f, 5.34f, 4f, 7f, 4f)
                lineTo(17f, 14f)
                curveTo(18.66f, 14f, 20f, 15.34f, 20f, 17f)
                curveTo(20f, 18.66f, 18.66f, 20f, 17f, 20f)
                curveTo(15.34f, 20f, 14f, 18.66f, 14f, 17f)
                curveTo(14f, 15.34f, 15.34f, 14f, 17f, 14f)
                lineTo(7f, 6f)
                curveTo(6.45f, 6f, 6f, 6.45f, 6f, 7f)
                curveTo(6f, 7.55f, 6.45f, 8f, 7f, 8f)
                curveTo(7.55f, 8f, 8f, 7.55f, 8f, 7f)
                curveTo(8f, 6.45f, 7.55f, 6f, 7f, 6f)
                lineTo(17f, 16f)
                curveTo(16.45f, 16f, 16f, 16.45f, 16f, 17f)
                curveTo(16f, 17.55f, 16.45f, 18f, 17f, 18f)
                curveTo(17.55f, 18f, 18f, 17.55f, 18f, 17f)
                curveTo(18f, 16.45f, 17.55f, 16f, 17f, 16f)
            }
        }.build()
    }

    /** account-outline — Material Design Icons (IconVaultKit). */
    val Person: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvPerson",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 4f)
                arcTo(4f, 4f, 0f, false, true, 16f, 8f)
                arcTo(4f, 4f, 0f, false, true, 12f, 12f)
                arcTo(4f, 4f, 0f, false, true, 8f, 8f)
                arcTo(4f, 4f, 0f, false, true, 12f, 4f)
                lineTo(12f, 6f)
                arcTo(2f, 2f, 0f, false, false, 10f, 8f)
                arcTo(2f, 2f, 0f, false, false, 12f, 10f)
                arcTo(2f, 2f, 0f, false, false, 14f, 8f)
                arcTo(2f, 2f, 0f, false, false, 12f, 6f)
                lineTo(12f, 13f)
                curveTo(14.67f, 13f, 20f, 14.33f, 20f, 17f)
                lineTo(20f, 20f)
                lineTo(4f, 20f)
                lineTo(4f, 17f)
                curveTo(4f, 14.33f, 9.33f, 13f, 12f, 13f)
                lineTo(12f, 14.9f)
                curveTo(9.03f, 14.9f, 5.9f, 16.36f, 5.9f, 17f)
                lineTo(5.9f, 18.1f)
                lineTo(18.1f, 18.1f)
                lineTo(18.1f, 17f)
                curveTo(18.1f, 16.36f, 14.97f, 14.9f, 12f, 14.9f)
            }
        }.build()
    }

    /** account-plus-outline — Material Design Icons (IconVaultKit). */
    val PersonAdd: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvPersonAdd",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(15f, 4f)
                arcTo(4f, 4f, 0f, false, false, 11f, 8f)
                arcTo(4f, 4f, 0f, false, false, 15f, 12f)
                arcTo(4f, 4f, 0f, false, false, 19f, 8f)
                arcTo(4f, 4f, 0f, false, false, 15f, 4f)
                lineTo(15f, 5.9f)
                arcTo(2.1f, 2.1f, 0f, true, true, 15f, 10.1f)
                arcTo(2.1f, 2.1f, 0f, false, true, 12.9f, 8f)
                arcTo(2.1f, 2.1f, 0f, false, true, 15f, 5.9f)
                lineTo(4f, 7f)
                lineTo(4f, 10f)
                lineTo(1f, 10f)
                lineTo(1f, 12f)
                lineTo(4f, 12f)
                lineTo(4f, 15f)
                lineTo(6f, 15f)
                lineTo(6f, 12f)
                lineTo(9f, 12f)
                lineTo(9f, 10f)
                lineTo(6f, 10f)
                lineTo(6f, 7f)
                close()
                lineTo(15f, 13f)
                curveTo(12.33f, 13f, 7f, 14.33f, 7f, 17f)
                lineTo(7f, 20f)
                lineTo(23f, 20f)
                lineTo(23f, 17f)
                curveTo(23f, 14.33f, 17.67f, 13f, 15f, 13f)
                lineTo(15f, 14.9f)
                curveTo(17.97f, 14.9f, 21.1f, 16.36f, 21.1f, 17f)
                lineTo(21.1f, 18.1f)
                lineTo(8.9f, 18.1f)
                lineTo(8.9f, 17f)
                curveTo(8.9f, 16.36f, 12f, 14.9f, 15f, 14.9f)
            }
        }.build()
    }

    /** account-outline — Material Design Icons (IconVaultKit). */
    val PersonOutline: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvPersonOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 4f)
                arcTo(4f, 4f, 0f, false, true, 16f, 8f)
                arcTo(4f, 4f, 0f, false, true, 12f, 12f)
                arcTo(4f, 4f, 0f, false, true, 8f, 8f)
                arcTo(4f, 4f, 0f, false, true, 12f, 4f)
                lineTo(12f, 6f)
                arcTo(2f, 2f, 0f, false, false, 10f, 8f)
                arcTo(2f, 2f, 0f, false, false, 12f, 10f)
                arcTo(2f, 2f, 0f, false, false, 14f, 8f)
                arcTo(2f, 2f, 0f, false, false, 12f, 6f)
                lineTo(12f, 13f)
                curveTo(14.67f, 13f, 20f, 14.33f, 20f, 17f)
                lineTo(20f, 20f)
                lineTo(4f, 20f)
                lineTo(4f, 17f)
                curveTo(4f, 14.33f, 9.33f, 13f, 12f, 13f)
                lineTo(12f, 14.9f)
                curveTo(9.03f, 14.9f, 5.9f, 16.36f, 5.9f, 17f)
                lineTo(5.9f, 18.1f)
                lineTo(18.1f, 18.1f)
                lineTo(18.1f, 17f)
                curveTo(18.1f, 16.36f, 14.97f, 14.9f, 12f, 14.9f)
            }
        }.build()
    }

    /** file-pdf-box — Material Design Icons (IconVaultKit). */
    val PictureAsPdf: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvPictureAsPdf",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 3f)
                lineTo(5f, 3f)
                curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
                lineTo(3f, 19f)
                curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
                lineTo(19f, 21f)
                curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
                lineTo(21f, 5f)
                curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
                lineTo(9.5f, 11.5f)
                curveTo(9.5f, 12.3f, 8.8f, 13f, 8f, 13f)
                lineTo(7f, 13f)
                lineTo(7f, 15f)
                lineTo(5.5f, 15f)
                lineTo(5.5f, 9f)
                lineTo(8f, 9f)
                curveTo(8.8f, 9f, 9.5f, 9.7f, 9.5f, 10.5f)
                close()
                lineTo(14.5f, 13.5f)
                curveTo(14.5f, 14.3f, 13.8f, 15f, 13f, 15f)
                lineTo(10.5f, 15f)
                lineTo(10.5f, 9f)
                lineTo(13f, 9f)
                curveTo(13.8f, 9f, 14.5f, 9.7f, 14.5f, 10.5f)
                close()
                lineTo(18.5f, 10.5f)
                lineTo(17f, 10.5f)
                lineTo(17f, 11.5f)
                lineTo(18.5f, 11.5f)
                lineTo(18.5f, 13f)
                lineTo(17f, 13f)
                lineTo(17f, 15f)
                lineTo(15.5f, 15f)
                lineTo(15.5f, 9f)
                lineTo(18.5f, 9f)
                close()
                lineTo(12f, 10.5f)
                lineTo(13f, 10.5f)
                lineTo(13f, 13.5f)
                lineTo(12f, 13.5f)
                close()
                lineTo(7f, 10.5f)
                lineTo(8f, 10.5f)
                lineTo(8f, 11.5f)
                lineTo(7f, 11.5f)
                close()
            }
        }.build()
    }

    /** map-marker-outline — Material Design Icons (IconVaultKit). */
    val Place: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvPlace",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 6.5f)
                arcTo(2.5f, 2.5f, 0f, false, true, 14.5f, 9f)
                arcTo(2.5f, 2.5f, 0f, false, true, 12f, 11.5f)
                arcTo(2.5f, 2.5f, 0f, false, true, 9.5f, 9f)
                arcTo(2.5f, 2.5f, 0f, false, true, 12f, 6.5f)
                lineTo(12f, 2f)
                arcTo(7f, 7f, 0f, false, true, 19f, 9f)
                curveTo(19f, 14.25f, 12f, 22f, 12f, 22f)
                curveTo(12f, 22f, 5f, 14.25f, 5f, 9f)
                arcTo(7f, 7f, 0f, false, true, 12f, 2f)
                lineTo(12f, 4f)
                arcTo(5f, 5f, 0f, false, false, 7f, 9f)
                curveTo(7f, 10f, 7f, 12f, 12f, 18.71f)
                curveTo(17f, 12f, 17f, 10f, 17f, 9f)
                arcTo(5f, 5f, 0f, false, false, 12f, 4f)
            }
        }.build()
    }

    /** earth — Material Design Icons (IconVaultKit). */
    val Public: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvPublic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(17.9f, 17.39f)
                curveTo(17.64f, 16.59f, 16.89f, 16f, 16f, 16f)
                lineTo(15f, 16f)
                lineTo(15f, 13f)
                arcTo(1f, 1f, 0f, false, false, 14f, 12f)
                lineTo(8f, 12f)
                lineTo(8f, 10f)
                lineTo(10f, 10f)
                arcTo(1f, 1f, 0f, false, false, 11f, 9f)
                lineTo(11f, 7f)
                lineTo(13f, 7f)
                arcTo(2f, 2f, 0f, false, false, 15f, 5f)
                lineTo(15f, 4.59f)
                arcTo(7.984f, 7.984f, 0f, false, true, 17.9f, 17.39f)
                lineTo(11f, 19.93f)
                curveTo(7.05f, 19.44f, 4f, 16.08f, 4f, 12f)
                curveTo(4f, 11.38f, 4.08f, 10.78f, 4.21f, 10.21f)
                lineTo(9f, 15f)
                lineTo(9f, 16f)
                arcTo(2f, 2f, 0f, false, false, 11f, 18f)
                lineTo(12f, 2f)
                arcTo(10f, 10f, 0f, false, false, 2f, 12f)
                arcTo(10f, 10f, 0f, false, false, 12f, 22f)
                arcTo(10f, 10f, 0f, false, false, 22f, 12f)
                arcTo(10f, 10f, 0f, false, false, 12f, 2f)
            }
        }.build()
    }

    /** file-document-outline — Material Design Icons (IconVaultKit). */
    val RequestQuote: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvRequestQuote",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 2f)
                arcTo(2f, 2f, 0f, false, false, 4f, 4f)
                lineTo(4f, 20f)
                arcTo(2f, 2f, 0f, false, false, 6f, 22f)
                lineTo(18f, 22f)
                arcTo(2f, 2f, 0f, false, false, 20f, 20f)
                lineTo(20f, 8f)
                lineTo(14f, 2f)
                close()
                lineTo(6f, 4f)
                lineTo(13f, 4f)
                lineTo(13f, 9f)
                lineTo(18f, 9f)
                lineTo(18f, 20f)
                lineTo(6f, 20f)
                close()
                lineTo(8f, 12f)
                lineTo(8f, 14f)
                lineTo(16f, 14f)
                lineTo(16f, 12f)
                close()
                lineTo(8f, 16f)
                lineTo(8f, 18f)
                lineTo(13f, 18f)
                lineTo(13f, 16f)
                close()
            }
        }.build()
    }

    /** restore — Material Design Icons (IconVaultKit). */
    val Restore: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvRestore",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(13f, 3f)
                arcTo(9f, 9f, 0f, false, false, 4f, 12f)
                lineTo(1f, 12f)
                lineTo(4.89f, 15.89f)
                lineTo(4.96f, 16.03f)
                lineTo(9f, 12f)
                lineTo(6f, 12f)
                arcTo(7f, 7f, 0f, false, true, 13f, 5f)
                arcTo(7f, 7f, 0f, false, true, 20f, 12f)
                arcTo(7f, 7f, 0f, false, true, 13f, 19f)
                curveTo(11.07f, 19f, 9.32f, 18.21f, 8.06f, 16.94f)
                lineTo(6.64f, 18.36f)
                arcTo(8.9f, 8.9f, 0f, false, false, 13f, 21f)
                arcTo(9f, 9f, 0f, false, false, 22f, 12f)
                arcTo(9f, 9f, 0f, false, false, 13f, 3f)
            }
        }.build()
    }

    /** piggy-bank-outline — Material Design Icons (IconVaultKit). */
    val Savings: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvSavings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(15f, 10f)
                curveTo(15f, 9.45f, 15.45f, 9f, 16f, 9f)
                curveTo(16.55f, 9f, 17f, 9.45f, 17f, 10f)
                curveTo(17f, 10.55f, 16.55f, 11f, 16f, 11f)
                curveTo(15.45f, 11f, 15f, 10.55f, 15f, 10f)
                lineTo(8f, 9f)
                lineTo(13f, 9f)
                lineTo(13f, 7f)
                lineTo(8f, 7f)
                close()
                lineTo(22f, 7.5f)
                lineTo(22f, 14.47f)
                lineTo(19.18f, 15.41f)
                lineTo(17.5f, 21f)
                lineTo(12f, 21f)
                lineTo(12f, 19f)
                lineTo(10f, 19f)
                lineTo(10f, 21f)
                lineTo(4.5f, 21f)
                curveTo(4.5f, 21f, 2f, 12.54f, 2f, 9.5f)
                curveTo(2f, 6.46f, 4.46f, 4f, 7.5f, 4f)
                lineTo(12.5f, 4f)
                curveTo(13.41f, 2.79f, 14.86f, 2f, 16.5f, 2f)
                arcTo(1.498f, 1.498f, 0f, false, true, 17.88f, 4.08f)
                curveTo(17.74f, 4.42f, 17.62f, 4.81f, 17.56f, 5.23f)
                lineTo(19.83f, 7.5f)
                close()
                lineTo(20f, 9.5f)
                lineTo(19f, 9.5f)
                lineTo(15.5f, 6f)
                curveTo(15.5f, 5.35f, 15.59f, 4.71f, 15.76f, 4.09f)
                curveTo(14.79f, 4.34f, 14f, 5.06f, 13.67f, 6f)
                lineTo(7.5f, 6f)
                curveTo(5.57f, 6f, 4f, 7.57f, 4f, 9.5f)
                curveTo(4f, 11.38f, 5.22f, 16.15f, 6f, 19f)
                lineTo(8f, 19f)
                lineTo(8f, 17f)
                lineTo(14f, 17f)
                lineTo(14f, 19f)
                lineTo(16f, 19f)
                lineTo(17.56f, 13.85f)
                lineTo(20f, 13.03f)
                close()
            }
        }.build()
    }

    /** clock-outline — Material Design Icons (IconVaultKit). */
    val Schedule: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvSchedule",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 20f)
                arcTo(8f, 8f, 0f, false, false, 20f, 12f)
                arcTo(8f, 8f, 0f, false, false, 12f, 4f)
                arcTo(8f, 8f, 0f, false, false, 4f, 12f)
                arcTo(8f, 8f, 0f, false, false, 12f, 20f)
                lineTo(12f, 2f)
                arcTo(10f, 10f, 0f, false, true, 22f, 12f)
                arcTo(10f, 10f, 0f, false, true, 12f, 22f)
                curveTo(6.47f, 22f, 2f, 17.5f, 2f, 12f)
                arcTo(10f, 10f, 0f, false, true, 12f, 2f)
                lineTo(12.5f, 7f)
                lineTo(12.5f, 12.25f)
                lineTo(17f, 14.92f)
                lineTo(16.25f, 16.15f)
                lineTo(11f, 13f)
                lineTo(11f, 7f)
                close()
            }
        }.build()
    }

    /** magnify — Material Design Icons (IconVaultKit). */
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

    /** shield-outline — Material Design Icons (IconVaultKit). */
    val Security: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvSecurity",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(21f, 11f)
                curveTo(21f, 16.55f, 17.16f, 21.74f, 12f, 23f)
                curveTo(6.84f, 21.74f, 3f, 16.55f, 3f, 11f)
                lineTo(3f, 5f)
                lineTo(12f, 1f)
                lineTo(21f, 5f)
                close()
                lineTo(12f, 21f)
                curveTo(15.75f, 20f, 19f, 15.54f, 19f, 11.22f)
                lineTo(19f, 6.3f)
                lineTo(12f, 3.18f)
                lineTo(5f, 6.3f)
                lineTo(5f, 11.22f)
                curveTo(5f, 15.54f, 8.25f, 20f, 12f, 21f)
            }
        }.build()
    }

    /** cog-outline — Material Design Icons (IconVaultKit). */
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

    /** share-variant-outline — Material Design Icons (IconVaultKit). */
    val Share: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvShare",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18f, 16.08f)
                curveTo(17.24f, 16.08f, 16.56f, 16.38f, 16.04f, 16.85f)
                lineTo(8.91f, 12.7f)
                curveTo(8.96f, 12.47f, 9f, 12.24f, 9f, 12f)
                curveTo(9f, 11.76f, 8.96f, 11.53f, 8.91f, 11.3f)
                lineTo(15.96f, 7.19f)
                curveTo(16.5f, 7.69f, 17.21f, 8f, 18f, 8f)
                curveTo(19.66f, 8f, 21f, 6.66f, 21f, 5f)
                curveTo(21f, 3.34f, 19.66f, 2f, 18f, 2f)
                curveTo(16.34f, 2f, 15f, 3.34f, 15f, 5f)
                curveTo(15f, 5.24f, 15.04f, 5.47f, 15.09f, 5.7f)
                lineTo(8.04f, 9.81f)
                curveTo(7.5f, 9.31f, 6.79f, 9f, 6f, 9f)
                curveTo(4.34f, 9f, 3f, 10.34f, 3f, 12f)
                curveTo(3f, 13.66f, 4.34f, 15f, 6f, 15f)
                curveTo(6.79f, 15f, 7.5f, 14.69f, 8.04f, 14.19f)
                lineTo(15.16f, 18.34f)
                curveTo(15.11f, 18.55f, 15.08f, 18.77f, 15.08f, 19f)
                curveTo(15.08f, 20.61f, 16.39f, 21.91f, 18f, 21.91f)
                curveTo(19.61f, 21.91f, 20.92f, 20.61f, 20.92f, 19f)
                curveTo(20.92f, 17.39f, 19.61f, 16.08f, 18f, 16.08f)
                lineTo(18f, 4f)
                curveTo(18.55f, 4f, 19f, 4.45f, 19f, 5f)
                curveTo(19f, 5.55f, 18.55f, 6f, 18f, 6f)
                curveTo(17.45f, 6f, 17f, 5.55f, 17f, 5f)
                curveTo(17f, 4.45f, 17.45f, 4f, 18f, 4f)
                lineTo(6f, 13f)
                curveTo(5.45f, 13f, 5f, 12.55f, 5f, 12f)
                curveTo(5f, 11.45f, 5.45f, 11f, 6f, 11f)
                curveTo(6.55f, 11f, 7f, 11.45f, 7f, 12f)
                curveTo(7f, 12.55f, 6.55f, 13f, 6f, 13f)
                lineTo(18f, 20f)
                curveTo(17.45f, 20f, 17f, 19.55f, 17f, 19f)
                curveTo(17f, 18.45f, 17.45f, 18f, 18f, 18f)
                curveTo(18.55f, 18f, 19f, 18.45f, 19f, 19f)
                curveTo(19f, 19.55f, 18.55f, 20f, 18f, 20f)
            }
        }.build()
    }

    /** cart-outline — Material Design Icons (IconVaultKit). */
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

    /** store-outline — Material Design Icons (IconVaultKit). */
    val Store: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvStore",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18.36f, 9f)
                lineTo(18.96f, 12f)
                lineTo(5.04f, 12f)
                lineTo(5.64f, 9f)
                close()
                lineTo(20f, 4f)
                lineTo(4f, 4f)
                lineTo(4f, 6f)
                lineTo(20f, 6f)
                close()
                lineTo(20f, 7f)
                lineTo(4f, 7f)
                lineTo(3f, 12f)
                lineTo(3f, 14f)
                lineTo(4f, 14f)
                lineTo(4f, 20f)
                lineTo(14f, 20f)
                lineTo(14f, 14f)
                lineTo(18f, 14f)
                lineTo(18f, 20f)
                lineTo(20f, 20f)
                lineTo(20f, 14f)
                lineTo(21f, 14f)
                lineTo(21f, 12f)
                close()
                lineTo(6f, 18f)
                lineTo(6f, 14f)
                lineTo(12f, 14f)
                lineTo(12f, 18f)
                close()
            }
        }.build()
    }

    /** storefront-outline — Material Design Icons (IconVaultKit). */
    val Storefront: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvStorefront",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(5.06f, 3f)
                curveTo(4.63f, 3f, 4.22f, 3.14f, 3.84f, 3.42f)
                curveTo(3.46f, 3.7f, 3.24f, 4.06f, 3.14f, 4.5f)
                lineTo(2.11f, 8.91f)
                curveTo(1.86f, 10f, 2.06f, 10.95f, 2.72f, 11.77f)
                lineTo(3f, 12.05f)
                lineTo(3f, 19f)
                curveTo(3f, 19.5f, 3.2f, 20f, 3.61f, 20.39f)
                curveTo(4.02f, 20.78f, 4.5f, 21f, 5f, 21f)
                lineTo(19f, 21f)
                curveTo(19.5f, 21f, 20f, 20.8f, 20.39f, 20.39f)
                curveTo(20.78f, 19.98f, 21f, 19.5f, 21f, 19f)
                lineTo(21f, 12.05f)
                lineTo(21.28f, 11.77f)
                curveTo(21.94f, 10.95f, 22.14f, 10f, 21.89f, 8.91f)
                lineTo(20.86f, 4.5f)
                curveTo(20.73f, 4.06f, 20.5f, 3.7f, 20.13f, 3.42f)
                arcTo(1.88f, 1.88f, 0f, false, false, 18.94f, 3f)
                close()
                lineTo(18.89f, 4.97f)
                lineTo(19.97f, 9.38f)
                curveTo(20.06f, 9.81f, 19.97f, 10.2f, 19.69f, 10.55f)
                curveTo(19.44f, 10.86f, 19.13f, 11f, 18.75f, 11f)
                curveTo(18.44f, 11f, 18.17f, 10.9f, 17.95f, 10.66f)
                curveTo(17.73f, 10.43f, 17.61f, 10.16f, 17.58f, 9.84f)
                lineTo(16.97f, 5f)
                close()
                lineTo(5.06f, 5f)
                lineTo(7.03f, 5f)
                lineTo(6.42f, 9.84f)
                curveTo(6.3f, 10.63f, 5.91f, 11f, 5.25f, 11f)
                curveTo(4.84f, 11f, 4.53f, 10.86f, 4.31f, 10.55f)
                curveTo(4.03f, 10.2f, 3.94f, 9.81f, 4.03f, 9.38f)
                close()
                lineTo(9.05f, 5f)
                lineTo(11f, 5f)
                lineTo(11f, 9.7f)
                curveTo(11f, 10.05f, 10.89f, 10.35f, 10.64f, 10.62f)
                curveTo(10.39f, 10.88f, 10.08f, 11f, 9.7f, 11f)
                curveTo(9.36f, 11f, 9.07f, 10.88f, 8.84f, 10.59f)
                curveTo(8.61f, 10.3f, 8.5f, 10f, 8.5f, 9.66f)
                lineTo(8.5f, 9.5f)
                close()
                lineTo(13f, 5f)
                lineTo(14.95f, 5f)
                lineTo(15.5f, 9.5f)
                curveTo(15.58f, 9.92f, 15.5f, 10.27f, 15.21f, 10.57f)
                curveTo(14.95f, 10.87f, 14.61f, 11f, 14.2f, 11f)
                curveTo(13.89f, 11f, 13.61f, 10.88f, 13.36f, 10.62f)
                arcTo(1.3f, 1.3f, 0f, false, true, 13f, 9.7f)
                close()
                lineTo(7.45f, 12.05f)
                curveTo(8.08f, 12.67f, 8.86f, 13f, 9.8f, 13f)
                curveTo(10.64f, 13f, 11.38f, 12.67f, 12f, 12.05f)
                curveTo(12.69f, 12.67f, 13.45f, 13f, 14.3f, 13f)
                curveTo(15.17f, 13f, 15.92f, 12.67f, 16.55f, 12.05f)
                curveTo(17.11f, 12.67f, 17.86f, 13f, 18.8f, 13f)
                lineTo(19.03f, 13f)
                lineTo(19.03f, 19f)
                lineTo(5f, 19f)
                lineTo(5f, 13f)
                lineTo(5.25f, 13f)
                curveTo(6.16f, 13f, 6.89f, 12.67f, 7.45f, 12.05f)
            }
        }.build()
    }

    /** swap-horizontal — Material Design Icons (IconVaultKit). */
    val SwapHoriz: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvSwapHoriz",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(21f, 9f)
                lineTo(17f, 5f)
                lineTo(17f, 8f)
                lineTo(10f, 8f)
                lineTo(10f, 10f)
                lineTo(17f, 10f)
                lineTo(17f, 13f)
                lineTo(7f, 11f)
                lineTo(3f, 15f)
                lineTo(7f, 19f)
                lineTo(7f, 16f)
                lineTo(14f, 16f)
                lineTo(14f, 14f)
                lineTo(7f, 14f)
                close()
            }
        }.build()
    }

    /** alert-outline — Material Design Icons (IconVaultKit). */
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

    /** view-dashboard-outline — Material Design Icons (IconVaultKit). */
    val Workspaces: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvWorkspaces",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 5f)
                lineTo(19f, 7f)
                lineTo(15f, 7f)
                lineTo(15f, 5f)
                close()
                lineTo(9f, 5f)
                lineTo(9f, 11f)
                lineTo(5f, 11f)
                lineTo(5f, 5f)
                close()
                lineTo(19f, 13f)
                lineTo(19f, 19f)
                lineTo(15f, 19f)
                lineTo(15f, 13f)
                close()
                lineTo(9f, 17f)
                lineTo(9f, 19f)
                lineTo(5f, 19f)
                lineTo(5f, 17f)
                close()
                lineTo(21f, 3f)
                lineTo(13f, 3f)
                lineTo(13f, 9f)
                lineTo(21f, 9f)
                close()
                lineTo(11f, 3f)
                lineTo(3f, 3f)
                lineTo(3f, 13f)
                lineTo(11f, 13f)
                close()
                lineTo(21f, 11f)
                lineTo(13f, 11f)
                lineTo(13f, 21f)
                lineTo(21f, 21f)
                close()
                lineTo(11f, 15f)
                lineTo(3f, 15f)
                lineTo(3f, 21f)
                lineTo(11f, 21f)
                close()
            }
        }.build()
    }

    /** medal-outline — Material Design Icons (IconVaultKit). */
    val WorkspacePremium: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvWorkspacePremium",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(14.94f, 19.5f)
                lineTo(12f, 17.77f)
                lineTo(9.06f, 19.5f)
                lineTo(9.84f, 16.16f)
                lineTo(7.25f, 13.92f)
                lineTo(10.66f, 13.63f)
                lineTo(12f, 10.5f)
                lineTo(13.34f, 13.63f)
                lineTo(16.75f, 13.92f)
                lineTo(14.16f, 16.16f)
                lineTo(20f, 2f)
                lineTo(4f, 2f)
                lineTo(4f, 4f)
                lineTo(8.86f, 7.64f)
                arcTo(8f, 8f, 0f, true, false, 15.14f, 7.64f)
                lineTo(20f, 4f)
                lineTo(18f, 15f)
                arcTo(6f, 6f, 0f, true, true, 10.82f, 9.12f)
                arcTo(5.9f, 5.9f, 0f, false, true, 13.18f, 9.12f)
                arcTo(6f, 6f, 0f, false, true, 18f, 15f)
                lineTo(12.63f, 7f)
                lineTo(11.37f, 7f)
                lineTo(7.37f, 4f)
                lineTo(16.71f, 4f)
                close()
            }
        }.build()
    }

    /** arrow-left — Material Design Icons (IconVaultKit). */
    val ArrowBack: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvArrowBack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20f, 11f)
                lineTo(20f, 13f)
                lineTo(8f, 13f)
                lineTo(13.5f, 18.5f)
                lineTo(12.08f, 19.92f)
                lineTo(4.16f, 12f)
                lineTo(12.08f, 4.08f)
                lineTo(13.5f, 5.5f)
                lineTo(8f, 11f)
                close()
            }
        }.build()
    }

    /** arrow-right — Material Design Icons (IconVaultKit). */
    val ArrowForward: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvArrowForward",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 11f)
                lineTo(4f, 13f)
                lineTo(16f, 13f)
                lineTo(10.5f, 18.5f)
                lineTo(11.92f, 19.92f)
                lineTo(19.84f, 12f)
                lineTo(11.92f, 4.08f)
                lineTo(10.5f, 5.5f)
                lineTo(16f, 11f)
                close()
            }
        }.build()
    }

    /** chevron-right — Material Design Icons (IconVaultKit). */
    val ArrowForwardIos: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvArrowForwardIos",
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

    /** backspace-outline — Material Design Icons (IconVaultKit). */
    val Backspace: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvBackspace",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 15.59f)
                lineTo(17.59f, 17f)
                lineTo(14f, 13.41f)
                lineTo(10.41f, 17f)
                lineTo(9f, 15.59f)
                lineTo(12.59f, 12f)
                lineTo(9f, 8.41f)
                lineTo(10.41f, 7f)
                lineTo(14f, 10.59f)
                lineTo(17.59f, 7f)
                lineTo(19f, 8.41f)
                lineTo(15.41f, 12f)
                close()
                lineTo(22f, 3f)
                arcTo(2f, 2f, 0f, false, true, 24f, 5f)
                lineTo(24f, 19f)
                arcTo(2f, 2f, 0f, false, true, 22f, 21f)
                lineTo(7f, 21f)
                curveTo(6.31f, 21f, 5.77f, 20.64f, 5.41f, 20.11f)
                lineTo(0f, 12f)
                lineTo(5.41f, 3.88f)
                curveTo(5.77f, 3.35f, 6.31f, 3f, 7f, 3f)
                close()
                lineTo(22f, 5f)
                lineTo(7f, 5f)
                lineTo(2.28f, 12f)
                lineTo(7f, 19f)
                lineTo(22f, 19f)
                close()
            }
        }.build()
    }

    /** chat-outline — Material Design Icons (IconVaultKit). */
    val Chat: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvChat",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 3f)
                curveTo(6.5f, 3f, 2f, 6.58f, 2f, 11f)
                arcTo(7.22f, 7.22f, 0f, false, false, 4.75f, 16.5f)
                curveTo(4.75f, 17.1f, 4.33f, 18.67f, 2f, 21f)
                curveTo(4.37f, 20.89f, 6.64f, 20f, 8.47f, 18.5f)
                curveTo(9.61f, 18.83f, 10.81f, 19f, 12f, 19f)
                curveTo(17.5f, 19f, 22f, 15.42f, 22f, 11f)
                curveTo(22f, 6.58f, 17.5f, 3f, 12f, 3f)
                lineTo(12f, 17f)
                curveTo(7.58f, 17f, 4f, 14.31f, 4f, 11f)
                curveTo(4f, 7.69f, 7.58f, 5f, 12f, 5f)
                curveTo(16.42f, 5f, 20f, 7.69f, 20f, 11f)
                curveTo(20f, 14.31f, 16.42f, 17f, 12f, 17f)
            }
        }.build()
    }

    /** help-circle-outline — Material Design Icons (IconVaultKit). */
    val HelpOutline: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvHelpOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11f, 18f)
                lineTo(13f, 18f)
                lineTo(13f, 16f)
                lineTo(11f, 16f)
                close()
                lineTo(12f, 2f)
                arcTo(10f, 10f, 0f, false, false, 2f, 12f)
                arcTo(10f, 10f, 0f, false, false, 12f, 22f)
                arcTo(10f, 10f, 0f, false, false, 22f, 12f)
                arcTo(10f, 10f, 0f, false, false, 12f, 2f)
                lineTo(12f, 20f)
                curveTo(7.59f, 20f, 4f, 16.41f, 4f, 12f)
                curveTo(4f, 7.59f, 7.59f, 4f, 12f, 4f)
                curveTo(16.41f, 4f, 20f, 7.59f, 20f, 12f)
                curveTo(20f, 16.41f, 16.41f, 20f, 12f, 20f)
                lineTo(12f, 6f)
                arcTo(4f, 4f, 0f, false, false, 8f, 10f)
                lineTo(10f, 10f)
                arcTo(2f, 2f, 0f, false, true, 12f, 8f)
                arcTo(2f, 2f, 0f, false, true, 14f, 10f)
                curveTo(14f, 12f, 11f, 11.75f, 11f, 15f)
                lineTo(13f, 15f)
                curveTo(13f, 12.75f, 16f, 12.5f, 16f, 10f)
                arcTo(4f, 4f, 0f, false, false, 12f, 6f)
            }
        }.build()
    }

    /** send-outline — Material Design Icons (IconVaultKit). */
    val Send: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvSend",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 6.03f)
                lineTo(11.5f, 9.25f)
                lineTo(4f, 8.25f)
                close()
                lineTo(11.5f, 14.75f)
                lineTo(4f, 17.97f)
                lineTo(4f, 15.75f)
                close()
                lineTo(2f, 3f)
                lineTo(2f, 10f)
                lineTo(17f, 12f)
                lineTo(2f, 14f)
                lineTo(2f, 21f)
                lineTo(23f, 12f)
                close()
            }
        }.build()
    }

    /** trending-up — Material Design Icons (IconVaultKit). */
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

    /** cart-arrow-down — Material Design Icons (IconVaultKit). */
    val CartArrowDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvCartArrowDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(10f, 0f)
                lineTo(10f, 4f)
                lineTo(8f, 4f)
                lineTo(12f, 8f)
                lineTo(16f, 4f)
                lineTo(14f, 4f)
                lineTo(14f, 0f)
                lineTo(1f, 2f)
                lineTo(1f, 4f)
                lineTo(3f, 4f)
                lineTo(6.6f, 11.6f)
                lineTo(5.2f, 14f)
                curveTo(5.1f, 14.3f, 5f, 14.6f, 5f, 15f)
                curveTo(5f, 16.1f, 5.9f, 17f, 7f, 17f)
                lineTo(19f, 17f)
                lineTo(19f, 15f)
                lineTo(7.4f, 15f)
                curveTo(7.3f, 15f, 7.2f, 14.9f, 7.2f, 14.8f)
                lineTo(7.2f, 14.7f)
                lineTo(8.1f, 13f)
                lineTo(15.5f, 13f)
                curveTo(16.2f, 13f, 16.9f, 12.6f, 17.2f, 12f)
                lineTo(21.1f, 5f)
                lineTo(19.4f, 4f)
                lineTo(15.5f, 11f)
                lineTo(8.5f, 11f)
                lineTo(4.3f, 2f)
                lineTo(7f, 18f)
                curveTo(5.9f, 18f, 5f, 18.9f, 5f, 20f)
                curveTo(5f, 21.1f, 5.9f, 22f, 7f, 22f)
                curveTo(8.1f, 22f, 9f, 21.1f, 9f, 20f)
                curveTo(9f, 18.9f, 8.1f, 18f, 7f, 18f)
                lineTo(17f, 18f)
                curveTo(15.9f, 18f, 15f, 18.9f, 15f, 20f)
                curveTo(15f, 21.1f, 15.9f, 22f, 17f, 22f)
                curveTo(18.1f, 22f, 19f, 21.1f, 19f, 20f)
                curveTo(19f, 18.9f, 18.1f, 18f, 17f, 18f)
            }
        }.build()
    }

    /** calculator-variant-outline — Material Design Icons (IconVaultKit). */
    val Calculator: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvCalculator",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 3f)
                lineTo(5f, 3f)
                curveTo(3.9f, 3f, 3f, 3.9f, 3f, 5f)
                lineTo(3f, 19f)
                curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
                lineTo(19f, 21f)
                curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
                lineTo(21f, 5f)
                curveTo(21f, 3.9f, 20.1f, 3f, 19f, 3f)
                lineTo(19f, 19f)
                lineTo(5f, 19f)
                lineTo(5f, 5f)
                lineTo(19f, 5f)
                close()
                lineTo(6.2f, 7.7f)
                lineTo(11.2f, 7.7f)
                lineTo(11.2f, 9.2f)
                lineTo(6.2f, 9.2f)
                close()
                lineTo(13f, 15.8f)
                lineTo(18f, 15.8f)
                lineTo(18f, 17.3f)
                lineTo(13f, 17.3f)
                close()
                lineTo(13f, 13.2f)
                lineTo(18f, 13.2f)
                lineTo(18f, 14.7f)
                lineTo(13f, 14.7f)
                close()
                lineTo(8f, 18f)
                lineTo(9.5f, 18f)
                lineTo(9.5f, 16f)
                lineTo(11.5f, 16f)
                lineTo(11.5f, 14.5f)
                lineTo(9.5f, 14.5f)
                lineTo(9.5f, 12.5f)
                lineTo(8f, 12.5f)
                lineTo(8f, 14.5f)
                lineTo(6f, 14.5f)
                lineTo(6f, 16f)
                lineTo(8f, 16f)
                close()
                lineTo(14.1f, 10.9f)
                lineTo(15.5f, 9.5f)
                lineTo(16.9f, 10.9f)
                lineTo(18f, 9.9f)
                lineTo(16.6f, 8.5f)
                lineTo(18f, 7.1f)
                lineTo(16.9f, 6f)
                lineTo(15.5f, 7.4f)
                lineTo(14.1f, 6f)
                lineTo(13f, 7.1f)
                lineTo(14.4f, 8.5f)
                lineTo(13f, 9.9f)
                close()
            }
        }.build()
    }

    /** bank-outline — Material Design Icons (IconVaultKit). */
    val Bank: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvBank",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6.5f, 10f)
                lineTo(4.5f, 10f)
                lineTo(4.5f, 17f)
                lineTo(6.5f, 17f)
                close()
                lineTo(12.5f, 10f)
                lineTo(10.5f, 10f)
                lineTo(10.5f, 17f)
                lineTo(12.5f, 17f)
                close()
                lineTo(21f, 19f)
                lineTo(2f, 19f)
                lineTo(2f, 21f)
                lineTo(21f, 21f)
                close()
                lineTo(18.5f, 10f)
                lineTo(16.5f, 10f)
                lineTo(16.5f, 17f)
                lineTo(18.5f, 17f)
                close()
                lineTo(11.5f, 3.26f)
                lineTo(16.71f, 6f)
                lineTo(6.29f, 6f)
                close()
                lineTo(11.5f, 1f)
                lineTo(2f, 6f)
                lineTo(2f, 8f)
                lineTo(21f, 8f)
                lineTo(21f, 6f)
                close()
            }
        }.build()
    }

    /** check-decagram-outline — Material Design Icons (IconVaultKit). */
    val QualityBadge: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvQualityBadge",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(23f, 12f)
                lineTo(20.6f, 9.2f)
                lineTo(20.9f, 5.5f)
                lineTo(17.3f, 4.7f)
                lineTo(15.4f, 1.5f)
                lineTo(12f, 3f)
                lineTo(8.6f, 1.5f)
                lineTo(6.7f, 4.7f)
                lineTo(3.1f, 5.5f)
                lineTo(3.4f, 9.2f)
                lineTo(1f, 12f)
                lineTo(3.4f, 14.8f)
                lineTo(3.1f, 18.5f)
                lineTo(6.7f, 19.3f)
                lineTo(8.6f, 22.5f)
                lineTo(12f, 21f)
                lineTo(15.4f, 22.5f)
                lineTo(17.3f, 19.3f)
                lineTo(20.9f, 18.5f)
                lineTo(20.6f, 14.8f)
                close()
                lineTo(18.7f, 16.9f)
                lineTo(16f, 17.5f)
                lineTo(14.6f, 19.9f)
                lineTo(12f, 18.8f)
                lineTo(9.4f, 19.9f)
                lineTo(8f, 17.5f)
                lineTo(5.3f, 16.9f)
                lineTo(5.5f, 14.1f)
                lineTo(3.7f, 12f)
                lineTo(5.5f, 9.9f)
                lineTo(5.3f, 7.1f)
                lineTo(8f, 6.5f)
                lineTo(9.4f, 4.1f)
                lineTo(12f, 5.2f)
                lineTo(14.6f, 4.1f)
                lineTo(16f, 6.5f)
                lineTo(18.7f, 7.1f)
                lineTo(18.5f, 9.9f)
                lineTo(20.3f, 12f)
                lineTo(18.5f, 14.1f)
                close()
                lineTo(16.6f, 7.6f)
                lineTo(18f, 9f)
                lineTo(10f, 17f)
                lineTo(6f, 13f)
                lineTo(7.4f, 11.6f)
                lineTo(10f, 14.2f)
                close()
            }
        }.build()
    }

    /** hammer-wrench — Material Design Icons (IconVaultKit). */
    val HammerWrench: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvHammerWrench",
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

    /** warehouse — Material Design Icons (IconVaultKit). */
    val Warehouse: ImageVector by lazy {
        ImageVector.Builder(
            name = "IvWarehouse",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 19f)
                lineTo(8f, 19f)
                lineTo(8f, 21f)
                lineTo(6f, 21f)
                close()
                lineTo(12f, 3f)
                lineTo(2f, 8f)
                lineTo(2f, 21f)
                lineTo(4f, 21f)
                lineTo(4f, 13f)
                lineTo(20f, 13f)
                lineTo(20f, 21f)
                lineTo(22f, 21f)
                lineTo(22f, 8f)
                close()
                lineTo(8f, 11f)
                lineTo(4f, 11f)
                lineTo(4f, 9f)
                lineTo(8f, 9f)
                close()
                lineTo(14f, 11f)
                lineTo(10f, 11f)
                lineTo(10f, 9f)
                lineTo(14f, 9f)
                close()
                lineTo(20f, 11f)
                lineTo(16f, 11f)
                lineTo(16f, 9f)
                lineTo(20f, 9f)
                close()
                lineTo(6f, 15f)
                lineTo(8f, 15f)
                lineTo(8f, 17f)
                lineTo(6f, 17f)
                close()
                lineTo(10f, 15f)
                lineTo(12f, 15f)
                lineTo(12f, 17f)
                lineTo(10f, 17f)
                close()
                lineTo(10f, 19f)
                lineTo(12f, 19f)
                lineTo(12f, 21f)
                lineTo(10f, 21f)
                close()
                lineTo(14f, 19f)
                lineTo(16f, 19f)
                lineTo(16f, 21f)
                lineTo(14f, 21f)
                close()
            }
        }.build()
    }
}
