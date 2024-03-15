package com.example.projetfinale5.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.projetfinale5.R
import com.google.firebase.auth.FirebaseAuth

@Composable
fun HomeScreen(navController: NavHostController) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Bienvenue dans la Meilleure Application de Notes !",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Image(
                painter = painterResource(id = R.drawable.notebook),
                contentDescription = "Image d'un notebook",
                modifier = Modifier
                    .height(180.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Capturez vos pensées et gardez-les organisées en un seul endroit. Avec notre application, prenez des notes facilement et accédez-y où que vous soyez.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 8.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    // Déconnexion de l'utilisateur
                    FirebaseAuth.getInstance().signOut()
                    // Effacer l'historique de navigation et naviguer vers l'écran de connexion/inscription
                    navController.navigate("signUpInScreen") {
                        popUpTo(0) { inclusive = true } // Retourner à la racine du graphique de navigation
                        launchSingleTop = true // Empêche la création de plusieurs instances de l'écran de destination
                    }
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(imageVector = Icons.Filled.ExitToApp, contentDescription = "Déconnexion")
                Spacer(Modifier.width(8.dp))
                Text("Déconnexion")
            }
        }
    }
}
