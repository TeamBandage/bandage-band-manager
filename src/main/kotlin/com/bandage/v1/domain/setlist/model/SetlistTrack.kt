package com.bandage.v1.domain.setlist.model

import com.bandage.v1.global.common.domain.BaseEntity
import com.bandage.v1.global.common.domain.SessionDef
import com.bandage.v1.global.common.domain.TrackInfo
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(name = "p_setlist_track")
@SQLRestriction("deleted_at IS NULL")
open class SetlistTrack(
    setlist: Setlist,
    trackInfo: TrackInfo,
    note: String?,
) : BaseEntity() {
    @Id
    @Column(name = "setlist_track_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setlist_id", nullable = false)
    val setlist: Setlist = setlist

    @Embedded
    var trackInfo: TrackInfo = trackInfo
        protected set

    @Column(name = "note", nullable = true, length = 1000)
    var note: String? = note
        protected set

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "p_setlist_track_session",
        joinColumns = [JoinColumn(name = "setlist_track_id")],
    )
    private var _sessions: MutableList<SessionDef> = mutableListOf()

    val sessions: List<SessionDef> get() = _sessions.toList()

    companion object {
        fun create(
            setlist: Setlist,
            trackInfo: TrackInfo,
            note: String?,
            sessions: List<SessionDef>,
        ): SetlistTrack =
            SetlistTrack(
                setlist = setlist,
                trackInfo = trackInfo,
                note = note,
            ).apply {
                _sessions.addAll(sessions)
            }
    }

    fun updateMeta(
        title: String?,
        artist: String?,
        album: String?,
        duration: Int?,
        reference: String?,
        note: String?,
    ) {
        trackInfo.update(title, artist, album, duration, reference)
        note?.let { this.note = it }
    }

    fun replaceSessions(newSessions: List<SessionDef>) {
        this._sessions.clear()
        this._sessions.addAll(newSessions)
    }
}
