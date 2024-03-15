package com.example.projetfinale5

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.projetfinale5.ui.theme.ProjetFinalE5Theme
import com.example.projetfinale5.screens.*
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.projetfinale5.model.NotesViewModel

// FragmentActivity car utiliser pour la biométrie
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProjetFinalE5Theme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Détermine si l'utilisateur est authentifié en vérifiant l'état actuel de l'utilisateur Firebase.
    val isAuthenticated = remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }

    // Fournit une instance de NotesViewModel pour accéder aux données de l'application.
    val notesViewModel: NotesViewModel = viewModel()

    // Observe les modifications de l'entrée actuelle de la pile de retour pour obtenir la route actuelle.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Scaffold fournit une structure de base pour l'application avec un emplacement pour une BottomBar.
    Scaffold(
        bottomBar = {
            // Affiche la BottomBar si l'utilisateur est authentifié et n'est pas sur l'écran de connexion/inscription.
            if (isAuthenticated.value && currentRoute != "signUpInScreen") {
                BottomBar(navController = navController)
            }
        }
    ) { innerPadding ->
        //Définit la navigation
        NavHost(
            navController = navController,
            startDestination = if (isAuthenticated.value) "homeScreen" else "signUpInScreen",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("signUpInScreen") { SignUpInScreen(navController, isAuthenticated) }
            composable("homeScreen") { HomeScreen(navController) }
            composable("notesScreen") { NotesScreen(notesViewModel, navController) }
            composable(
                route = "noteDetail/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.StringType })
            ) { backStackEntry ->
                NoteDetailScreen(
                    notesViewModel = notesViewModel,
                    noteId = backStackEntry.arguments?.getString("noteId") ?: "",
                    navController = navController
                )
            }
            composable(
                route = "addEditNoteScreen/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.StringType })
            ) { backStackEntry ->
                AddEditNoteScreen(
                    notesViewModel = notesViewModel,
                    noteId = backStackEntry.arguments?.getString("noteId") ?: "new",
                    navController = navController
                )
            }
        }
    }
}

@Composable
fun BottomBar(navController: NavHostController) {
    val items = listOf("homeScreen", "notesScreen")
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    // État pour gérer la demande d'authentification biométrique
    var requestBiometricAuthForScreen by remember { mutableStateOf<String?>(null) }

    // LaunchedEffect écoute les changements de l'état requestBiometricAuthForScreen
    LaunchedEffect(requestBiometricAuthForScreen) {
        requestBiometricAuthForScreen?.let { screen ->
            if (context is FragmentActivity) {
                requestBiometricAuth(context) {
                    navController.navigate(screen) {
                        popUpTo(navController.graph.startDestinationRoute ?: "") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                requestBiometricAuthForScreen = null // Réinitialiser l'état après l'authentification
            }
        }
    }

    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    when (screen) {
                        "homeScreen" -> Icon(Icons.Filled.Home, contentDescription = null)
                        "notesScreen" -> Icon(Icons.Filled.List, contentDescription = null)
                    }
                },
                label = {
                    when (screen) {
                        "homeScreen" -> Text("Accueil")
                        "notesScreen" -> Text("Notes")
                    }
                },
                selected = currentRoute == screen,
                onClick = {
                    // Mettre à jour l'état pour déclencher LaunchedEffect et la logique d'authentification
                    if (screen == "notesScreen") {
                        requestBiometricAuthForScreen = screen
                    } else if (currentRoute != screen) {
                        navController.navigate(screen)
                    }
                }
            )
        }
    }
}


fun requestBiometricAuth(
    activity: FragmentActivity,
    onSuccess: () -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
        })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Authentification requise")
        .setSubtitle("Confirmez votre identité pour accéder aux notes")
        .setNegativeButtonText("Annuler")
        .build()

    biometricPrompt.authenticate(promptInfo)
}
