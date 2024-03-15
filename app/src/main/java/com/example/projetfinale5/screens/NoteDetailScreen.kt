package com.example.projetfinale5.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.projetfinale5.model.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(notesViewModel: NotesViewModel, navController: NavHostController, noteId: String) {
    // Collecter la note actuelle en tant que State pour la rendre observable par Compose.
    val note by notesViewModel.note.collectAsState()
    val context = LocalContext.current

    notesViewModel.getNoteById(noteId)

    LaunchedEffect(noteId) {
        notesViewModel.getNoteById(noteId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détail de la note") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        note?.let { currentNote ->
            Surface(modifier = Modifier.padding(padding)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = currentNote.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Affichage de l'image, si présente
                    currentNote.imageUri?.let { uri ->
                        Image(
                            painter = rememberAsyncImagePainter(model = currentNote.imageUri),
                            contentDescription = "Image de la note",
                            modifier = Modifier.size(200.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Affichage du contenu de la note
                    Text(
                        text = currentNote.content,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    currentNote.fileUri?.let { fileUrl ->
                        ClickableText(
                            text = AnnotatedString("Ouvrir le fichier"),
                            onClick = {
                                //Ouvre navigateur pour lire pdf
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl))
                                context.startActivity(browserIntent)
                            }
                        )

                    Spacer(modifier = Modifier.height(64.dp))
                    }

                    // Affichage des catégories
                    CategoriesSection(categories = currentNote.categories)

                    Spacer(modifier = Modifier.height(48.dp))

                    // Boutons pour éditer et supprimer la note
                    EditAndDeleteButtons(noteId = noteId, navController = navController, notesViewModel = notesViewModel)
                }
            }
        } ?: Text("Note non trouvée", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun CategoriesSection(categories: List<String>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Catégories :", style = MaterialTheme.typography.bodyLarge)
        categories.forEach { category ->
            Text("- $category", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun EditAndDeleteButtons(noteId: String, navController: NavHostController, notesViewModel: NotesViewModel) {
    Row {
        // Bouton pour naviguer vers l'écran d'édition de la note.
        Button(
            onClick = { navController.navigate("addEditNoteScreen/$noteId") },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.Edit, contentDescription = "Éditer")
            Spacer(Modifier.width(4.dp))
            Text("Éditer")
        }
        Spacer(modifier = Modifier.width(8.dp))
        // Bouton pour supprimer la note et revenir à l'écran précédent.
        Button(
            onClick = {
                notesViewModel.deleteNote(noteId)
                navController.popBackStack()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = Color.Red
            ),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.Delete, contentDescription = "Supprimer")
            Spacer(Modifier.width(4.dp))
            Text("Supprimer")
        }
    }
}
