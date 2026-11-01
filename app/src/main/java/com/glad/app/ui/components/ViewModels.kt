package com.glad.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.glad.app.AppContainer
import com.glad.app.container

/** Creates (or reuses) a ViewModel built from the app container. */
@Composable
inline fun <reified VM : ViewModel> gladViewModel(
    key: String? = null,
    crossinline factory: (AppContainer) -> VM
): VM {
    val appContainer = LocalContext.current.container
    return viewModel(
        key = key,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = factory(appContainer) as T
        }
    )
}
