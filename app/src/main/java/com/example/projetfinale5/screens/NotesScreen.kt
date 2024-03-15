package com.example.projetfinale5.screens


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.projetfinale5.model.Note
import com.example.projetfinale5.model.NotesViewModel
import androidx.compose.material3.DropdownMenuItem



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(notesViewModel: NotesViewModel, navController: NavHostController) {
    // État pour gérer la catégorie sélectionnée, initialisé à "Tous"
    var selectedCategory by remember { mutableStateOf("Tous") }
    val categories = listOf("Tous", "Personnel", "Professionnel", "Autre")

    // Récupère la liste des notes et applique le filtre de catégorie
    val allNotes by notesViewModel.notes.collectAsState()
    val filteredNotes = if (selectedCategory == "Tous") {
        allNotes
    } else {
        allNotes.filter { it.categories?.contains(selectedCategory) == true }
    }

    // Charge les notes depuis Firebase une seul fois au lancement du composant
    LaunchedEffect(Unit) {
        notesViewModel.loadNotes()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes Notes") },
                actions = {
                    // Composable pour le menu déroulant de sélection des catégories.
                    CategoryDropdownMenu(selectedCategory, categories) { newCategory ->
                        selectedCategory = newCategory
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("addEditNoteScreen/new") }) {
                Icon(imageVector = Icons.Filled.AddCircle, contentDescription = "Ajouter une note")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (filteredNotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucune note à afficher", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                // Composable qui affiche la liste des notes.
                NotesList(filteredNotes, navController, notesViewModel, Modifier.padding(innerPadding))
            }
        }
    }
}


@Composable
fun CategoryDropdownMenu(
    selectedCategory: String,
    categories: List<String>,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        Text(selectedCategory, modifier = Modifier
            .clickable { expanded = true } // Ouvre le menu lorsqu'on clique sur le texte.
            .padding(8.dp))
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false } // Ferme le menu lorsqu'on clique ailleurs.
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    onClick = {
                        onCategorySelected(category) // Mise à jour de la catégorie et fermeture du menu.
                        expanded = false
                    },
                    text = { Text(category) }
                )
            }
        }
    }
}


@Composable
fun NotesList(notes: List<Note>, navController: NavHostController, notesViewModel: NotesViewModel, modifier: Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(notes) { note ->
            // Pour chaque note, affiche une carte avec les détails de la note.
            NoteCard(note, navController, notesViewModel)
        }
    }
}

@Composable
fun NoteCard(note: Note, navController: NavHostController, notesViewModel: NotesViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { navController.navigate("noteDetail/${note.id}") },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                    note.categories?.joinToString(", ")?.let { categories ->
                        Text(text = categories, style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterVertically))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                // Affiche un extrait du contenu de la note.
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2
                )
            }
            IconButton(
                onClick = { note.id?.let { notesViewModel.deleteNote(it) } },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
            ) {
                Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
