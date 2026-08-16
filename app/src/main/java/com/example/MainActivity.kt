package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.model.ProcessingState
import com.example.ui.components.ProcessingDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.NarratorViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: NarratorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DrawingNarratorApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun DrawingNarratorApp(viewModel: NarratorViewModel) {
    val processingState by viewModel.processingState.collectAsState()
    val analysisResult by viewModel.currentAnalysisResult.collectAsState()

    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    // When analysis succeeds, smoothly navigate to Results screen
    LaunchedEffect(analysisResult) {
        if (analysisResult != null) {
            currentScreen = Screen.RESULTS
        }
    }

    // Handle Error States
    LaunchedEffect(processingState) {
        if (processingState is ProcessingState.Error) {
            // Error handling
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState == Screen.RESULTS) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut()
                )
            } else {
                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            }
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            Screen.HOME -> {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToResults = { currentScreen = Screen.RESULTS }
                )
            }
            Screen.RESULTS -> {
                if (analysisResult != null) {
                    ResultsScreen(
                        result = analysisResult!!,
                        viewModel = viewModel,
                        onBackToHome = { currentScreen = Screen.HOME }
                    )
                } else {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToResults = { currentScreen = Screen.RESULTS }
                    )
                }
            }
        }
    }

    // Modal Processing State Overlay
    if (processingState is ProcessingState.InProgress) {
        ProcessingDialog(
            state = processingState as ProcessingState.InProgress,
            onCancel = { viewModel.cancelAnalysis() }
        )
    }
}

enum class Screen {
    HOME,
    RESULTS
}
