package com.demo.creditlimit.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.demo.creditlimit.ui.home.HomeScreen
import com.demo.creditlimit.ui.kyc.BasicInfoScreen
import com.demo.creditlimit.ui.kyc.BindCardScreen
import com.demo.creditlimit.ui.kyc.EmergencyContactScreen
import com.demo.creditlimit.ui.kyc.OcrScreen
import com.demo.creditlimit.ui.kyc.SupplementaryInfoScreen
import com.demo.creditlimit.ui.login.LoginScreen
import com.demo.creditlimit.ui.result.CreditResultScreen

private fun enterTransition(): EnterTransition =
    slideInHorizontally(initialOffsetX = { it }) + fadeIn()

private fun exitTransition(): ExitTransition =
    slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut()

private fun popEnterTransition(): EnterTransition =
    slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn()

private fun popExitTransition(): ExitTransition =
    slideOutHorizontally(targetOffsetX = { it }) + fadeOut()

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = { enterTransition() },
        exitTransition = { exitTransition() },
        popEnterTransition = { popEnterTransition() },
        popExitTransition = { popExitTransition() }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        composable(Screen.KycBasicInfo.route) {
            BasicInfoScreen(navController = navController)
        }
        composable(Screen.KycEmergencyContact.route) {
            EmergencyContactScreen(navController = navController)
        }
        composable(Screen.KycSupplementaryInfo.route) {
            SupplementaryInfoScreen(navController = navController)
        }
        composable(Screen.KycBindCard.route) {
            BindCardScreen(navController = navController)
        }
        composable(Screen.KycOcr.route) {
            OcrScreen(navController = navController)
        }
        composable(Screen.CreditResult.route) {
            CreditResultScreen(navController = navController)
        }
    }
}
