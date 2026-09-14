package com.missa.b360.ui.tasks

import androidx.compose.runtime.Composable
import com.missa.b360.R
import com.missa.b360.ui.components.PlaceholderScreen

@Composable
fun TasksScreen(onBack: () -> Unit) {
    PlaceholderScreen(titleRes = R.string.tasks_title, onBack = onBack)
}
