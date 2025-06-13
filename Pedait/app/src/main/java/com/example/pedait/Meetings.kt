package com.example.pedait

import com.google.firebase.Timestamp

data class Meetings(
    var datetime: Timestamp? = null,
    var topic: String? = null,
    var id: String? = null,
    var status: String? = null,

    ){
    constructor() : this( null, null, null, null)
}
