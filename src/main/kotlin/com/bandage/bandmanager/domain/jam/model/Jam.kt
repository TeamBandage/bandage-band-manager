package com.bandage.bandmanager.domain.jam.model

import com.bandage.bandmanager.global.common.domain.BaseEntity
import com.bandage.bandmanager.global.common.domain.SessionDef
import com.bandage.bandmanager.global.common.domain.TimeInfoUnit
import com.bandage.bandmanager.global.common.domain.TrackInfo
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.CascadeType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "p_jam")
@SQLRestriction("deleted_at IS NULL")
open class Jam(
    title: String,
    trackInfo: TrackInfo,
    timeInfo: TimeInfoUnit,
    note: String? = null,
    setlistId: UUID? = null,
) : BaseEntity() {
    @Id
    @Column(name = "jam_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @Column(name = "title", nullable = false)
    var title: String = title
        protected set

    // 곡 정보. Jam.title(합주명)과의 컬럼 충돌을 피하기 위해 track_* 로 매핑.
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "title", column = Column(name = "track_title", nullable = false)),
        AttributeOverride(name = "artist", column = Column(name = "track_artist", nullable = false)),
        AttributeOverride(name = "album", column = Column(name = "track_album", nullable = true)),
        AttributeOverride(name = "duration", column = Column(name = "track_duration", nullable = true)),
        AttributeOverride(name = "reference", column = Column(name = "track_reference", nullable = true)),
    )
    var trackInfo: TrackInfo = trackInfo
        protected set

    @Embedded
    var timeInfo: TimeInfoUnit = timeInfo
        protected set

    @Column(name = "note", nullable = true, length = 1000)
    var note: String? = note
        protected set

    // Setlist 경유 생성 시 출처 추적용. 독립 생성 시 null.
    @Column(name = "setlist_id", nullable = true)
    var setlistId: UUID? = setlistId
        protected set

    @OneToMany(mappedBy = "jam", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    private var _participants: MutableList<JamParticipant> = mutableListOf()

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "p_jam_session",
        joinColumns = [JoinColumn(name = "jam_id")],
    )
    private var _sessions: MutableList<SessionDef> = mutableListOf()

    val participants: List<JamParticipant> get() = _participants.toList()
    val sessions: List<SessionDef> get() = _sessions.toList()

    companion object {
        fun create(
            title: String,
            trackInfo: TrackInfo,
            startAt: LocalDateTime,
            durationMinutes: Int,
            venue: String?,
            note: String? = null,
            setlistId: UUID? = null,
            sessions: List<SessionDef> = emptyList(),
        ): Jam =
            Jam(
                title = title,
                trackInfo = trackInfo,
                timeInfo = TimeInfoUnit(startAt = startAt, durationMinutes = durationMinutes, venue = venue),
                note = note,
                setlistId = setlistId,
            ).apply {
                _sessions.addAll(sessions)
            }
    }

    fun updateTitle(newTitle: String) {
        this.title = newTitle
    }

    fun updateTrackInfo(
        title: String?,
        artist: String?,
        album: String?,
        duration: Int?,
        reference: String?,
    ) {
        trackInfo.update(title, artist, album, duration, reference)
    }

    fun updateNote(note: String?) {
        this.note = note
    }

    fun updateTimeInfo(
        startAt: LocalDateTime,
        durationMinutes: Int,
    ) {
        timeInfo.updateTimeInfo(startAt, durationMinutes)
    }

    fun updateVenue(newVenue: String) {
        timeInfo.updateVenue(newVenue)
    }

    fun replaceSessions(newSessions: List<SessionDef>) {
        this._sessions.clear()
        this._sessions.addAll(newSessions)
    }

    fun addSession(def: SessionDef) {
        require(_sessions.none { it.sessionId == def.sessionId }) { "이미 존재하는 세션입니다: ${def.sessionId}" }
        _sessions.add(def)
    }

    fun updateSession(
        sessionId: String,
        label: String,
        short: String,
    ) {
        val idx = _sessions.indexOfFirst { it.sessionId == sessionId }
        require(idx >= 0) { "존재하지 않는 세션입니다: $sessionId" }
        val custom = _sessions[idx].custom
        _sessions[idx] = SessionDef(sessionId = sessionId, label = label, short = short, custom = custom)
    }

    fun removeSession(sessionId: String) {
        _sessions.removeIf { it.sessionId == sessionId }
    }

    fun addParticipant(
        sessionId: String,
        member: Long,
    ): JamParticipant? {
        if (this._participants.any { it.sessionId == sessionId && it.member == member }) return null
        val participant =
            JamParticipant(
                jam = this,
                sessionId = sessionId,
                member = member,
            )
        this._participants.add(participant)
        return participant
    }

    fun deleteParticipant(participant: JamParticipant) {
        this._participants.remove(participant)
    }
}
