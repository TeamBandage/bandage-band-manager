package com.bandage.v1.domain.practice.model

import com.bandage.v1.global.common.domain.BaseEntity
import com.bandage.v1.global.common.domain.SessionDef
import com.bandage.v1.global.common.domain.TimeInfoUnit
import com.bandage.v1.global.common.domain.TrackInfo
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
@Table(name = "p_practice")
@SQLRestriction("deleted_at IS NULL")
open class Practice(
    title: String,
    trackInfo: TrackInfo,
    timeInfo: TimeInfoUnit,
    note: String? = null,
    setlistId: UUID? = null,
) : BaseEntity() {
    @Id
    @Column(name = "practice_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @Column(name = "title", nullable = false)
    var title: String = title
        protected set

    // 곡 정보. Practice.title(합주명)과의 컬럼 충돌을 피하기 위해 track_* 로 매핑.
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

    @OneToMany(mappedBy = "practice", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    private var _participants: MutableList<PracticeParticipant> = mutableListOf()

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "p_practice_session",
        joinColumns = [JoinColumn(name = "practice_id")],
    )
    private var _sessions: MutableList<SessionDef> = mutableListOf()

    val participants: List<PracticeParticipant> get() = _participants.toList()
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
        ): Practice =
            Practice(
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

    fun addParticipant(
        sessionId: String,
        member: Long,
    ): PracticeParticipant? {
        if (this._participants.any { it.sessionId == sessionId && it.member == member }) return null
        val participant =
            PracticeParticipant(
                practice = this,
                sessionId = sessionId,
                member = member,
            )
        this._participants.add(participant)
        return participant
    }

    fun deleteParticipant(participant: PracticeParticipant) {
        this._participants.remove(participant)
    }
}
