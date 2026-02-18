package com.bandage.v1.domain.practice.model

import com.bandage.v1.global.domain.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
@Table(name = "p_practice")
class Practice(
    @Column(name = "title", nullable = false)
    var title: String,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id")
    var song: PracticeSong,
    @Column(name = "start_at", nullable = false)
    var startAt: LocalDateTime = defaultStartTime(),
    @Column(name = "duration_minutes", nullable = false)
    var durationMinutes: Int = 60,
    @Column(name = "venue", nullable = true)
    var venue: String? = null,
    @OneToMany(mappedBy = "practice", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    private var _participants: MutableList<PracticeParticipant> = mutableListOf(),
    @OneToMany(mappedBy = "practice", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    private var _sessions: MutableList<PracticeSession> = mutableListOf(),
) : BaseEntity() {
    @Id
    @Column(name = "practice_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null

    val participants: List<PracticeParticipant> get() = _participants.toList()
    val sessions: List<PracticeSession> get() = _sessions.toList()

    companion object {
        fun create(
            title: String,
            song: PracticeSong,
        ): Practice =
            Practice(
                title = title,
                song = song,
            )

        fun createWithBasicSessions(
            title: String,
            song: PracticeSong,
        ): Practice =
            Practice(title = title, song = song).apply {
                listOf(SessionType.VOCAL, SessionType.GUITAR, SessionType.BASE, SessionType.DRUM)
                    .forEach { addDefaultSession(it) }
            }

        private fun defaultStartTime(): LocalDateTime =
            LocalDateTime
                .now()
                .plusDays(1)
                .truncatedTo(ChronoUnit.HOURS)
    }

    fun updateTitle(newTitle: String) {
        this.title = newTitle
    }

    fun updateSong(song: PracticeSong) {
        this.song = song
    }

    fun updateStartAt(newStartAt: LocalDateTime) {
        this.startAt = newStartAt
    }

    fun updateDurationMinutes(newDurationMinutes: Int) {
        this.durationMinutes = newDurationMinutes
    }

    fun updateVenue(newVenue: String) {
        this.venue = newVenue
    }

    fun addParticipant(member: UUID) {
        if (!this._participants.any { it.member == member }) return
        var participant =
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
        this._sessions.add(PracticeSession.create(this, type.label, type))
    }

    fun addSession(session: PracticeSession) {
        this._sessions.add(session)
    }

    fun deleteSession(session: PracticeSession) {
        this._sessions.remove(session)
    }
}
