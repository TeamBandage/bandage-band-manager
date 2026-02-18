package com.bandage.v1.domain.practice.model

import com.bandage.v1.global.domain.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.util.*

@Entity
@Table(name = "p_practice")
class Practice(
    @Column(name = "title", nullable = false)
    var title: String,
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id")
    var song: PracticeSong,
    @OneToMany(mappedBy = "practice", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @Column(name = "participants")
    private var _participants: MutableList<PracticeParticipant> = mutableListOf(),
    @OneToMany(mappedBy = "practice", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @Column(name = "sessions")
    private var _sessions: MutableList<PracticeSession> = mutableListOf()
): BaseEntity() {
    @Id
    @Column(name = "practice_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null

    val participants: List<PracticeParticipant> get() = _participants.toList()
    val sessions: List<PracticeSession> get() = _sessions.toList()

    companion object {
        fun create(title: String, song: PracticeSong): Practice {
            return Practice(
                title = title,
                song = song)
        }
        fun createWithBasicSessions(title: String, song: PracticeSong): Practice
        = Practice(title = title, song = song).apply {
                listOf(SessionType.VOCAL, SessionType.GUITAR, SessionType.BASE, SessionType.DRUM)
                    .forEach { addDefaultSession(it) }
        }
    }
    fun updateTitle(newTitle: String) {
        this.title = newTitle
    }
    fun updateSong(song: PracticeSong) {
        this.song = song
    }
    fun addParticipant(member: UUID){
        if(!this._participants.any { it.member==member }) return
        var participant = PracticeParticipant(
            practice = this,
            member = member
        )
        this._participants.add(participant)
    }
    fun deleteParticipant(participant: PracticeParticipant){
        this._participants.remove(participant)
    }
    fun addDefaultSession(type: SessionType){
        this._sessions.add(PracticeSession.create(this, type.label, type))
    }
    fun addSession(session: PracticeSession){
        this._sessions.add(session)
    }
    fun deleteSession(session: PracticeSession){
        this._sessions.remove(session)
    }

}