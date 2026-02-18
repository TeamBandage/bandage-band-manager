package com.bandage.v1.domain.practice.model

enum class SessionType(
    val label: String
) {
    VOCAL("Vocal"),
    CHORUS("Chorus"),
    GUITAR("Guitar"),
    BASE("Base"),
    DRUM("Drum"),
    PERCUSSION("Percussion"),
    SYNTH("Synth"),
    ETC("Session");
}