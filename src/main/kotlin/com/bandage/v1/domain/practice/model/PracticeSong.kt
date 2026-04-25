package com.bandage.v1.domain.practice.model

import com.bandage.v1.global.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(name = "p_practice_song")
@SQLRestriction("deleted_at IS NULL")
open class PracticeSong(
    title: String,
    artist: String,
    album: String,
    duration: Int,
    refLink: String? = null,
) : BaseEntity() {
    @Id
    @Column(name = "practice_song_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @Column(name = "title", nullable = false)
    var title: String = title
        protected set

    @Column(name = "artist", nullable = false)
    var artist: String = artist
        protected set

    @Column(name = "album", nullable = false)
    var album: String = album
        protected set

    @Column(name = "duration", nullable = false)
    var duration: Int = duration
        protected set

    @Column(name = "ref_link", nullable = true)
    var refLink: String? = refLink
        protected set

    companion object {
        fun create(
            title: String,
            artist: String,
            album: String,
            duration: Int,
            refLink: String? = null,
        ): PracticeSong =
            PracticeSong(
                title = title,
                artist = artist,
                album = album,
                duration = duration,
                refLink = refLink,
            )
    }

    fun updateRefLink(refLink: String) {
        this.refLink = refLink
    }

    fun deleteRefLink() {
        this.refLink = null
    }

    fun updateTitle(title: String) {
        this.title = title
    }

    fun updateArtist(artist: String) {
        this.artist = artist
    }

    fun updateAlbum(album: String) {
        this.album = album
    }

    fun updateDuration(duration: Int) {
        this.duration = duration
    }

    fun updateAll(
        title: String,
        artist: String,
        album: String,
        duration: Int,
        refLink: String?,
    ) {
        this.title = title
        this.artist = artist
        this.album = album
        this.duration = duration
        this.refLink = refLink
    }
}
