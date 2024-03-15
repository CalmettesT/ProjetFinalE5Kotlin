package com.example.projetfinale5.model

data class Note(
    var id: String? = null,
    var title: String= "",
    var content: String= "",
    var userId: String? = null,
    var timestamp: com.google.firebase.Timestamp? = null,
    var imageUri: String? = null,
    var fileUri: String? = null,
    var categories: List<String> = emptyList()
)