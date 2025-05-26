package com.example.pedait

import com.google.firebase.firestore.GeoPoint

data class Course(
    var id: String? = null,
    var kodeMK: String? = null,
    var namaMK: String? = null,
    var location: GeoPoint? = null,

    ){
    constructor() : this( null, null, null, null)
}
