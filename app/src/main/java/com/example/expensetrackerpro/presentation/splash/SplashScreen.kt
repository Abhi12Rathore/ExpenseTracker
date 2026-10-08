package com.example.expensetrackerpro.presentation.splash

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.animation.OvershootInterpolator
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

// presentation/splash/SplashScreen.kt
@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate when ViewModel signals ready
    LaunchedEffect(uiState.isReady) {
        if (uiState.isReady) onTimeout()
    }

    SplashContent()
}

@Composable
fun SplashContent() {
    var startAnimation by remember { mutableStateOf(false) }

    // Animation Specs
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(1000)
    )
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = tween(1000, easing = { OvershootInterpolator(2f).getInterpolation(it) })
    )
    val offsetY by animateDpAsState(
        targetValue = if (startAnimation) 0.dp else 40.dp,
        animationSpec = tween(800, delayMillis = 200)
    )

    LaunchedEffect(Unit) { startAnimation = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFFFFFF)))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .offset(y = offsetY)
                .alpha(alpha)
        ) {
            // Logo Placeholder - Scaled and Faded
            Surface(
                // The .scale(scale) now correctly returns a Modifier
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale),
                color = Color.Transparent
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = Color(0xFF256324),
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Expense Tracker Pro",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = Color(0xFF256324)
            )

            Text(
                text = "TRACK • SAVE • GROW",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF256324).copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .size(28.dp),
            color = Color(0xFF256324),
            strokeWidth = 3.dp
        )
    }
}

// Helper to use Android Interpolators in Compose
fun androidx.core.animation.Interpolator.toEasing() = Easing { x -> getInterpolation(x) }