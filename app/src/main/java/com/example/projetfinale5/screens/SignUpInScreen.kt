package com.example.projetfinale5.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SignUpInScreen(navController: NavHostController, isAuthenticated: MutableState<Boolean>) {
    val auth = FirebaseAuth.getInstance()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isSigningUp by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isSigningUp) {
            Text(text = "Inscription", style = MaterialTheme.typography.headlineMedium)
        } else {
            Text(text = "Connexion", style = MaterialTheme.typography.headlineMedium)
        }

        errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("E-mail") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mot de passe") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {})
        )

        if (isSigningUp) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirmez le mot de passe") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {})
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                errorMessage = null // Réinitialiser le message d'erreur
                when {
                    username.isBlank() -> errorMessage = "L'e-mail ne peut pas être vide."
                    password.length < 6 -> errorMessage = "Le mot de passe doit contenir au moins 6 caractères."
                    isSigningUp && password != confirmPassword -> errorMessage = "Les mots de passe ne correspondent pas."
                    isSigningUp -> {
                        // Logique d'inscription
                        auth.createUserWithEmailAndPassword(username, password).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                isAuthenticated.value = true
                                navController.navigate("notesScreen") {
                                    popUpTo("signInScreen") { inclusive = true }
                                }
                            } else {
                                errorMessage = task.exception?.localizedMessage ?: "Erreur d'inscription"
                            }
                        }
                    }
                    else -> {
                        // Logique de connexion
                        auth.signInWithEmailAndPassword(username, password).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                isAuthenticated.value = true
                                navController.navigate("notesScreen") {
                                    popUpTo("signInScreen") { inclusive = true }
                                }
                            } else {
                                errorMessage = task.exception?.localizedMessage ?: "Erreur de connexion"
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSigningUp) "S'inscrire" else "Se connecter")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = {
                isSigningUp = !isSigningUp
                errorMessage = null // Réinitialiser le message d'erreur lors du changement de mode
            }
        ) {
            Text(if (isSigningUp) "Avez-vous déjà un compte ? Se connecter" else "Créer un compte")
        }
    }
}

