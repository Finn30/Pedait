package com.example.pedait

data class Course(
    var id: String? = null,
    var kodeMK: String? = null,
    var namaMK: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,

    ){
    constructor() : this( null, null, null, null)
}
