package com.bandage.v1.domain.practice.controller

import com.bandage.v1.domain.practice.dto.Song
import com.bandage.v1.domain.practice.dto.req.PracticeSongCreateFromSongRequest
import com.bandage.v1.domain.practice.dto.req.PracticeSongCreateRequest
import com.bandage.v1.domain.practice.dto.req.PracticeSongRefLinkUpsertRequest
import com.bandage.v1.domain.practice.dto.req.PracticeSongSearchQuery
import com.bandage.v1.domain.practice.dto.req.PracticeSongUpdateRequest
import com.bandage.v1.domain.practice.dto.res.PracticeSongResponse
import com.bandage.v1.domain.practice.service.PracticeService
import com.bandage.v1.global.common.constants.PathPrefix.PREFIX
import com.bandage.v1.global.common.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "practice-songs", description = "합주곡 API")
@RestController
@RequestMapping("$PREFIX/practice-songs")
class PracticeSongController(
    private val practiceService: PracticeService,
) {
    @GetMapping("/search")
    @Operation(
        summary = "합주곡 검색 API",
        description = "키워드로 외부 시스템에서 곡 정보를 검색합니다. 응답은 Song 객체로 반환되며 이 시스템에서 DB 엔티티로 관리되지 않습니다. (현재는 외부 연동 전 mock 응답)",
    )
    fun searchSongs(
        @Valid query: PracticeSongSearchQuery,
    ): ApiResponse<List<Song>> = ApiResponse.success(practiceService.searchSongs(query.keyword))

    @PostMapping
    @Operation(
        summary = "합주곡 생성 API (필드 입력)",
        description = "자작곡 등 외부 API 연동이 필요 없는 곡을 사용자가 각 필드를 직접 입력하여 합주곡으로 생성하고 합주에 1:1 바인딩합니다.",
    )
    fun createPracticeSong(
        @Valid @RequestBody request: PracticeSongCreateRequest,
    ): ApiResponse<PracticeSongResponse> =
        ApiResponse.success(
            practiceService.createPracticeSong(
                practiceId = request.practiceId,
                title = request.title,
                artist = request.artist,
                album = request.album,
                duration = request.duration,
                refLink = request.refLink,
            ),
        )

    @PostMapping("/from-song")
    @Operation(
        summary = "합주곡 생성 API (Song 객체)",
        description = "외부 시스템에서 받아온 Song 객체를 그대로 사용하여 합주곡을 생성하고 합주에 1:1 바인딩합니다.",
    )
    fun createPracticeSongFromSong(
        @Valid @RequestBody request: PracticeSongCreateFromSongRequest,
    ): ApiResponse<PracticeSongResponse> =
        ApiResponse.success(
            practiceService.createPracticeSong(request.practiceId, request.song),
        )

    @PatchMapping("/{songId}")
    @Operation(summary = "합주곡 부분 수정 API", description = "합주곡의 일부 정보만 수정합니다. 전달된 필드만 업데이트됩니다.")
    fun updatePracticeSong(
        @PathVariable songId: UUID,
        @Valid @RequestBody request: PracticeSongUpdateRequest,
    ): ApiResponse<PracticeSongResponse> = ApiResponse.success(practiceService.updatePracticeSong(songId, request))

    @PutMapping("/{songId}")
    @Operation(summary = "합주곡 Upsert API", description = "Song 객체를 사용하여 합주곡 정보를 전체 갱신합니다.")
    fun upsertPracticeSong(
        @PathVariable songId: UUID,
        @Valid @RequestBody song: Song,
    ): ApiResponse<PracticeSongResponse> = ApiResponse.success(practiceService.upsertPracticeSong(songId, song))

    @PutMapping("/{songId}/ref-link")
    @Operation(summary = "합주곡 참조 링크 upsert API", description = "합주곡 참조 링크를 등록하거나 수정합니다.")
    fun upsertRefLink(
        @PathVariable songId: UUID,
        @Valid @RequestBody request: PracticeSongRefLinkUpsertRequest,
    ): ApiResponse<Unit> {
        practiceService.upsertPracticeSongRefLink(songId, request)
        return ApiResponse.success()
    }

    @DeleteMapping("/{songId}/ref-link")
    @Operation(summary = "합주곡 참조 링크 삭제 API", description = "합주곡 참조 링크를 삭제합니다.")
    fun deleteRefLink(
        @PathVariable songId: UUID,
    ): ApiResponse<Unit> {
        practiceService.deletePracticeSongRefLink(songId)
        return ApiResponse.success()
    }
}
