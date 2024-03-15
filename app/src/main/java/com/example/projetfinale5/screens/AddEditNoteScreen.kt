package com.example.projetfinale5.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewModelScope
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.projetfinale5.model.NotesViewModel
import com.example.projetfinale5.model.Note
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNoteScreen(
    navController: NavHostController,
    noteId: String?,
    notesViewModel: NotesViewModel = viewModel()
) {
    // Utilisation de rememberSaveable pour conserver l'état lors des recompositions et des changements de configuration.
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    // Utilisation de remember sans saveable car les URI ne doivent pas être conservées après la destruction de l'activité.
    var imageUri by remember { mutableStateOf<String?>(null) }
    var fileUri by remember { mutableStateOf<String?>(null) }
    var categories by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    // State pour gérer les messages d'erreur.
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val categoryOptions = listOf("Professionnel", "Personnel", "Autre")

    // Cette ligne va collecter les changements émis par note dans le ViewModel
    val currentNote by notesViewModel.note.collectAsState()

    // LaunchedEffect est utilisé ici pour réagir aux changements de noteId.
    LaunchedEffect(noteId) {
        if (noteId == "new") {
            // Réinitialisation de l'état pour une nouvelle note
            title = ""
            content = ""
            imageUri = null
            fileUri = null
            categories = emptyList()
        } else if (noteId != null) {
            // Chargement des détails de la note pour édition
            notesViewModel.getNoteById(noteId)
        }
    }

    // Pour réinitialiser les champs lorsqu'une nouvelle note est sélectionnée ou créée
    LaunchedEffect(currentNote) {
        if (currentNote != null && noteId != "new") {
            // Si currentNote est non-null et noteId n'est pas "new", mettez à jour les champs
            title = currentNote!!.title ?: ""
            content = currentNote!!.content ?: ""
            imageUri = currentNote!!.imageUri ?: null
            fileUri = currentNote!!.fileUri ?: null
            categories = currentNote!!.categories ?: emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                // Change le titre en fonction du contexte : ajout ou édition.
                title = { Text(if (noteId == "new") "Ajouter une note" else "Modifier la note") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        floatingActionButton = {
            // FloatingActionButton pour sauvegarder la note.
            FloatingActionButton(onClick = {

                if (title.isBlank() || content.isBlank()) {
                    errorMessage = "Le titre et le contenu ne peuvent pas être vides."
                } else {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid

                    if (userId != null) {
                        notesViewModel.viewModelScope.launch {
                            // Détermination des URIs d'image et de fichier à utiliser, soit des nouvelles soit celles existantes.
                            val updatedImageUri = imageUri ?: currentNote?.imageUri
                            val updatedFileUri = fileUri ?: currentNote?.fileUri

                            // Si de nouvelles URIs sont présentes, ou si les URIs existantes sont réutilisées, procéder à l'upload.
                            if (updatedImageUri != null || updatedFileUri != null) {
                                notesViewModel.uploadImageAndFile(
                                    imageUri = updatedImageUri?.let { Uri.parse(it) },
                                    fileUri = updatedFileUri?.let { Uri.parse(it) },
                                    // En cas de succès de l'upload, recevoir les URLs téléchargées.
                                    onSuccess = { imageDownloadUrl, fileDownloadUrl ->
                                        // Utilisez l'URL téléchargée si disponible, sinon réutilisez l'URL existante
                                        val finalImageUri = imageDownloadUrl ?: updatedImageUri
                                        val finalFileUri = fileDownloadUrl ?: updatedFileUri

                                        // Création de l'objet Note à sauvegarder ou à mettre à jour.
                                        val noteToSave = Note(
                                            id = if (noteId == "new") null else noteId,
                                            title = title,
                                            content = content,
                                            userId = userId,
                                            imageUri = finalImageUri,
                                            fileUri = finalFileUri,
                                            categories = categories
                                        )
                                        // Sauvegarde de la note dans le ViewModel.
                                        notesViewModel.addOrUpdateNote(noteToSave)
                                        // Retour à l'écran précédent.
                                        navController.popBackStack()
                                    }
                                )
                            } else {
                                // Aucune nouvelle image ou fichier, enregistrez la note avec les URIs existantes
                                val noteToSave = Note(
                                    id = if (noteId == "new") null else noteId,
                                    title = title,
                                    content = content,
                                    userId = userId,
                                    imageUri = updatedImageUri,
                                    fileUri = updatedFileUri,
                                    categories = categories
                                )
                                notesViewModel.addOrUpdateNote(noteToSave)
                                navController.popBackStack()
                            }
                        }
                    } else {
                        errorMessage = "Vous devez être connecté pour enregistrer une note."
                    }
                }
            }) {
                Icon(Icons.Filled.Done, contentDescription = "Sauvegarder")
            }

        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextFieldForNote(
                title = title,
                onTitleChange = { title = it },
                content = content,
                onContentChange = { content = it },
                errorMessage = errorMessage
            )
            CategorySelection(categoryOptions, categories) { selectedCategory ->
                categories = categories.toMutableList().apply {
                    if (contains(selectedCategory)) remove(selectedCategory) else add(selectedCategory)
                }
            }
            ImageAndFileSection(
                imageUri = imageUri,
                fileUri = fileUri,
                onImageUriChange = { newUri ->
                    imageUri = newUri
                },
                onFileUriChange = { newUri ->
                    fileUri = newUri
                }
            )
        }
    }
}

@Composable
fun TextFieldForNote(
    title: String,
    onTitleChange: (String) -> Unit,
    content: String,
    onContentChange: (String) -> Unit,
    errorMessage: String?
) {
    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        label = { Text("Titre") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = content,
        onValueChange = onContentChange,
        label = { Text("Contenu") },
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        keyboardActions = KeyboardActions(onDone = {})
    )
    if (errorMessage != null) {
        Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
    }
}



@Composable
fun CategorySelection(
    options: List<String>, // Liste des catégories disponibles à choisir.
    selectedCategories: List<String>, // Liste des catégories actuellement sélectionnées.
    onSelectionChange: (String) -> Unit // Fonction appelée chaque fois qu'une catégorie est sélectionnée ou désélectionnée. Unit = void
) {
    Column {
        options.forEach { category ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = selectedCategories.contains(category),
                    onCheckedChange = { _ -> onSelectionChange(category) }
                )
                Text(category)
            }
        }
    }
}

@Composable
fun ImageAndFileSection(
    imageUri: String?, // URI de l'image actuellement sélectionnée, si disponible.
    fileUri: String?, // URI du fichier actuellement sélectionné, si disponible.
    onImageUriChange: (String?) -> Unit, // Fonction appelée pour mettre à jour l'URI de l'image.
    onFileUriChange: (String?) -> Unit // Fonction appelée pour mettre à jour l'URI du fichier.
) {
    val context = LocalContext.current

    // Lanceur pour sélectionner une image, utilise rememberLauncherForActivityResult pour survivre aux recompositions.
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            // Action à réaliser une fois une image sélectionnée.
            val decodedUri = Uri.decode(uri.toString())
            Log.e("ImagePicker", "Decoded Image URI: $decodedUri")
            if (decodedUri.contains("image")) {
                onImageUriChange(uri.toString())
            }
        }
    }

    // Lanceur pour sélectionner un fichier PDF
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        // Log pour voir l'URI sélectionnée
        Log.d("FilePicker", "File URI: $uri")
        if (uri != null) {
            onFileUriChange(uri.toString())
        }
    }

    Row {
        Button(onClick = { imagePickerLauncher.launch("image/*") }) {
            Text(if (imageUri == null) "Ajouter une image" else "Changer l'image")
        }

        Spacer(modifier = Modifier.width(16.dp))

        Button(onClick = { filePickerLauncher.launch(arrayOf("application/pdf").toString()) }) {
            Text(if (fileUri == null) "Ajouter un fichier" else "Changer le fichier")
        }
    }
}
