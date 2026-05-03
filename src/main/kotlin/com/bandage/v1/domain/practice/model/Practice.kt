package com.bandage.v1.domain.practice.model

import com.bandage.v1.domain.practice.model.enums.SessionType
import com.bandage.v1.global.common.domain.BaseEntity
import com.bandage.v1.global.common.domain.TimeInfoUnit
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
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
    song: PracticeSong,
    timeInfo: TimeInfoUnit,
) : BaseEntity() {
    @Id
    @Column(name = "practice_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @Column(name = "title", nullable = false)
    var title: String = title
        protected set

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id")
    var song: PracticeSong = song
        protected set

    @Embedded
    var timeInfo: TimeInfoUnit = timeInfo
        protected set

    @OneToMany(mappedBy = "practice", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    private var _participants: MutableList<PracticeParticipant> = mutableListOf()

    @OneToMany(mappedBy = "practice", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    private var _sessions: MutableList<PracticeSession> = mutableListOf()

    val participants: List<PracticeParticipant> get() = _participants.toList()
    val sessions: List<PracticeSession> get() = _sessions.toList()

    companion object {
        fun create(
            title: String,
            song: PracticeSong,
            startAt: LocalDateTime,
            durationMinutes: Int,
            venue: String?,
        ): Practice =
            Practice(
                title = title,
                song = song,
                timeInfo = TimeInfoUnit(startAt = startAt, durationMinutes = durationMinutes, venue = venue),
            )

        fun createWithBasicSessions(
            title: String,
            song: PracticeSong,
            startAt: LocalDateTime,
            venue: String?,
        ): Practice =
            Practice(
                title = title,
                song = song,
                timeInfo = TimeInfoUnit(startAt = startAt, venue = venue),
            ).apply {
                listOf(SessionType.VOCAL, SessionType.GUITAR, SessionType.BASS, SessionType.DRUM)
                    .forEach { addDefaultSession(it) }
            }
    }

    fun updateTitle(newTitle: String) {
        this.title = newTitle
    }

    fun updateSong(song: PracticeSong) {
        this.song = song
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

    fun addParticipant(member: Long) {
        if (this._participants.any { it.member == member }) return
        val participant =
            PracticeParticipant(
                practice = this,
                member = member,
            )
        this._participants.add(participant)
    }

    fun deleteParticipant(participant: PracticeParticipant) {
        this._participants.remove(participant)
    }

    fun addDefaultSession(type: SessionType) {
        this._sessions.add(
            PracticeSession.create(
                practice = this,
                label = type.label,
                type = type,
                participant = null,
            ),
        )
    }

    fun addSession(session: PracticeSession) {
        this._sessions.add(session)
    }

    fun deleteSession(session: PracticeSession) {
        this._sessions.remove(session)
    }
}
