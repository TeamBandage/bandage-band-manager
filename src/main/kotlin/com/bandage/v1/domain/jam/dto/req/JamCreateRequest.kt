package com.bandage.v1.domain.jam.dto.req

import com.bandage.v1.domain.selection.dto.req.SessionDefDto
import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Min
import java.time.LocalDateTime

@Schema(description = "합주 생성 요청")
data class JamCreateRequest(
    @Schema(description = "합주 타이틀 (미입력 시 곡 제목 승계)", example = "TuNA 정기공연 1주차 합주")
    val title: String?,
    @field:Valid
    @Schema(description = "곡 정보")
    val track: TrackInfoRequest,
    @Schema(description = "합주 메모")
    val note: String? = null,
    @Schema(description = "합주 장소", example = "홍대 스튜디오")
    val venue: String?,
    @field:Future(message = "합주 시작 시간은 현재 이후여야 합니다.")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", shape = JsonFormat.Shape.STRING, timezone = "Asia/Seoul")
    @Schema(description = "합주 시작 시간 (현재 이후만 허용)", example = "2026-03-15 18:00")
    val startAt: LocalDateTime,
    @field:Min(1)
    @Schema(description = "합주 시간 (분)", example = "60")
    val durationMinutes: Int,
    @field:Valid
    @Schema(description = "세션 정의 목록")
    val sessions: List<SessionDefDto> = emptyList(),
)
