package org.example.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.example.project.features.login.presentation.screen.LoginScreen
import org.koin.compose.KoinContext

@Composable
@Preview
fun App() {
    MaterialTheme {
        KoinContext {
            LoginScreen(
                onNavigateToHome = { println("Navigate to Home") },
                onShowToast = { message -> println("Toast: $message") }
            )
        }
    }
}