package com.bandage.bandmanager.domain.setlist.repository

import com.bandage.bandmanager.domain.setlist.model.SetlistTrack
import com.bandage.bandmanager.domain.setlist.model.SetlistTrackParticipant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SetlistTrackParticipantRepository : JpaRepository<SetlistTrackParticipant, UUID> {
    fun findAllByTrack(track: SetlistTrack): List<SetlistTrackParticipant>

    fun findAllByTrackIn(tracks: List<SetlistTrack>): List<SetlistTrackParticipant>

    /**
     * 셋리스트에 속한 모든 트랙의 참여자를 한 번에 조회한다.
     * SetlistTrack 엔티티를 먼저 로드하지 않아도 되며, track 을 fetch join 해
     * 참여자별 track.id 접근에서 N+1 이 발생하지 않게 한다.
     */
    @Query(
        """
        SELECT p FROM SetlistTrackParticipant p
        JOIN FETCH p.track t
        WHERE t.setlist.id = :setlistId
        """,
    )
    fun findAllBySetlistId(
        @Param("setlistId") setlistId: UUID,
    ): List<SetlistTrackParticipant>

    fun deleteAllByTrack(track: SetlistTrack)

    fun existsByTrackSetlistIdInAndMemberId(
        setlistIds: List<UUID>,
        memberId: Long,
    ): Boolean
}
