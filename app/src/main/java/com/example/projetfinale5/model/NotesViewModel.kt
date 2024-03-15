package com.example.projetfinale5.model

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll



class NotesViewModel : ViewModel() {
    // _notes est un MutableStateFlow qui contiendra la liste des notes. Ce StateFlow permet de notifier les observateurs de toute modification de la liste des notes.
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes

    // _note est un MutableStateFlow pour gérer la note actuellement sélectionnée ou en cours de visualisation.
    private val _note = MutableStateFlow<Note?>(null)
    // Version immuable de _note exposée aux observateurs.
    val note: StateFlow<Note?> = _note

    init {
        // Au lancement du ViewModel, charger toutes les notes de l'utilisateur.
        loadNotes()
    }

    fun getNoteById(noteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Récupération de la note depuis Firestore.
                val docSnapshot = FirebaseFirestore.getInstance()
                    .collection("notes").document(noteId)
                    .get().await()
                // Conversion du document Firestore en objet Note.
                val fetchedNote = docSnapshot.toObject<Note>()
                // Mise à jour de l'état avec la note récupérée.
                _note.value = fetchedNote
            } catch (e: Exception) {
                Log.e("NotesViewModel", "Error fetching note", e)
            }
        }
    }

    fun loadNotes() {
        viewModelScope.launch(Dispatchers.IO) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.w("NotesViewModel", "User ID is null, can't load notes.")
            return@launch
        }
        // Écoute des modifications sur la collection de notes pour l'utilisateur actuel.
        FirebaseFirestore.getInstance().collection("notes")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("NotesViewModel", "Listen failed.", e)
                    return@addSnapshotListener
                }
                // Transformation des documents Firestore en liste d'objets Note.
                val notesList = snapshot?.documents?.mapNotNull {
                    val note = it.toObject(Note::class.java)
                    note?.id = it.id
                    note
                } ?: emptyList()
                // Mise à jour de l'état avec la nouvelle liste de notes.
                _notes.value = notesList
                Log.d("NotesViewModel", "Notes loaded: ${_notes.value.size}")
            }
        }
    }


    fun addOrUpdateNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val noteId = note.id
                // Création d'un nouveau document pour une nouvelle note.
                val noteDocumentRef = if (noteId.isNullOrEmpty()) {
                    FirebaseFirestore.getInstance().collection("notes").document()
                } else {
                    // Référence à un document existant pour une note existante.
                    FirebaseFirestore.getInstance().collection("notes").document(noteId)
                }

                val updatedNote = note.apply {
                    this.id = noteDocumentRef.id // S'assurer que l'ID est correct pour les nouvelles notes
                }
                // Enregistrement de la note dans Firestore.
                noteDocumentRef.set(updatedNote).await()
                Log.d("NotesViewModel", "Note added/updated successfully")
            } catch (e: Exception) {
                Log.e("NotesViewModel", "Error adding/updating note", e)
            }
        }
    }


    fun deleteNote(noteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                try {
                    // Suppression de la note spécifiée par noteId.
                    FirebaseFirestore.getInstance().collection("notes")
                        .document(noteId)
                        .delete()
                        .await()
                    Log.d("NotesViewModel", "Note deleted successfully: $noteId")

                } catch (e: Exception) {
                    Log.e("NotesViewModel", "Error deleting note", e)
                }
            } else {
                Log.w("NotesViewModel", "User ID is null, can't delete note.")
            }
        }
    }


    fun uploadImageAndFile(
        imageUri: Uri?,
        fileUri: Uri?,
        onSuccess: (imageDownloadUrl: String?, fileDownloadUrl: String?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // Initialisation des variables pour les URLs de téléchargement
            var imageDownloadUrl: String? = null
            var fileDownloadUrl: String? = null

            // Upload de l'image si l'URI n'est pas null
            if (imageUri != null) {
                val imageRef = FirebaseStorage.getInstance().reference.child("images/${imageUri.lastPathSegment}")
                try {
                    val uploadResult = imageRef.putFile(imageUri).await()
                    imageDownloadUrl = uploadResult.metadata?.reference?.downloadUrl?.await().toString()
                } catch (e: Exception) {
                    Log.e("Upload", "Failed to upload image: ${e.message}")
                }
            }

            // Upload du fichier si l'URI n'est pas null
            if (fileUri != null) {
                val fileRef = FirebaseStorage.getInstance().reference.child("files/${fileUri.lastPathSegment}")
                try {
                    val uploadResult = fileRef.putFile(fileUri).await()
                    fileDownloadUrl = uploadResult.metadata?.reference?.downloadUrl?.await().toString()
                } catch (e: Exception) {
                    Log.e("Upload", "Failed to upload file: ${e.message}")
                }
            }

            // Exécution du callback onSuccess sur le thread principal
            withContext(Dispatchers.Main) {
                onSuccess(imageDownloadUrl, fileDownloadUrl)
            }
        }
    }
}