package com.example.deciflux

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.deciflux.ui.main.MainScreen
import com.example.deciflux.ui.onboarding.InitialSetupScreen

private const val PREFS_NAME = "deciflux_preferences"
private const val KEY_SETUP_COMPLETED = "initial_setup_completed"

@Composable
fun MainNavigation() {
  val context = LocalContext.current
  val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
  val isSetupCompleted = remember { prefs.getBoolean(KEY_SETUP_COMPLETED, false) }

  val initialKey = if (isSetupCompleted) Main else Onboarding
  val backStack = rememberNavBackStack(initialKey)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Onboarding> {
          InitialSetupScreen(
            onComplete = {
              prefs.edit().putBoolean(KEY_SETUP_COMPLETED, true).apply()
              backStack.clear()
              backStack.add(Main)
            }
          )
        }
        entry<Main> {
          MainScreen(
            onOpenSetup = {
              backStack.add(Onboarding)
            }
          )
        }
      },
  )
}
