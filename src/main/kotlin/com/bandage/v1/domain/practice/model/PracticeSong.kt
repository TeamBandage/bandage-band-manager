package com.bandage.v1.domain.practice.model

import com.bandage.v1.global.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.util.*

@Entity
@Table(name = "p_practice_song")
class PracticeSong(
    @Column(name = "title", nullable = false)
    var title: String,
    @Column(name = "artist", nullable = false)
    var artist: String,
    @Column(name = "album", nullable = false)
    var album: String,
    @Column(name = "duration", nullable = false)
    var duration: Int,
    @Column(name = "ref_link", nullable = true)
    var refLink: String? = null
): BaseEntity() {
    @Id
    @Column(name = "practice_song_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null

    companion object {
        fun create(title: String, artist: String, album: String, duration: Int): PracticeSong{
            return PracticeSong(
                title = title,
                artist = artist,
                album = album,
                duration = duration
            )
        }
    }

    fun updateRefLink(refLink: String){
        this.refLink  = refLink
    }
}