package com.bandage.bandmanager.domain.performance.dto.res

import com.bandage.bandmanager.domain.setlist.dto.res.SetlistParticipantResponse
import com.bandage.bandmanager.domain.setlist.dto.res.SetlistResponse
import com.bandage.bandmanager.domain.setlist.dto.res.SetlistTrackResponse
import com.bandage.bandmanager.domain.setlist.model.Setlist
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공연에 참여하는 셋리스트 1건과 그 트랙·참여자 전체")
data class PerformanceSetlistTracksResponse(
    @Schema(description = "셋리스트 정보")
    val setlist: SetlistResponse,
    @Schema(description = "셋리스트에 속한 트랙 전체")
    val tracks: List<SetlistTrackResponse>,
    @Schema(description = "셋리스트 참여자 전체 (해당 셋리스트의 매니저 + 트랙 배정자)")
    val participants: List<SetlistParticipantResponse>,
) {
    companion object {
        fun of(
            setlist: Setlist,
            tracks: List<SetlistTrackResponse>,
            participants: List<SetlistParticipantResponse>,
        ): PerformanceSetlistTracksResponse =
            PerformanceSetlistTracksResponse(
                setlist = SetlistResponse.of(setlist),
                tracks = tracks,
                participants = participants,
            )
    }
}
