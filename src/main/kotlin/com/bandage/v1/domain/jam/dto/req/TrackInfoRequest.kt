package com.bandage.v1.domain.jam.dto.req

import com.bandage.v1.global.common.domain.TrackInfo
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "곡 정보")
data class TrackInfoRequest(
    @field:NotBlank
    @Schema(description = "곡 제목", example = "Stairway to Heaven")
    val title: String,
    @field:NotBlank
    @Schema(description = "아티스트", example = "Led Zeppelin")
    val artist: String,
    @Schema(description = "앨범", example = "Led Zeppelin IV")
    val album: String? = null,
    @Schema(description = "곡 길이(초 단위). 분/초(mm:ss) 표시 변환은 클라이언트에서 처리", example = "482")
    val duration: Int? = null,
    @Schema(description = "참고 링크(예: YouTube)")
    val reference: String? = null,
) {
    fun toEntity(): TrackInfo =
        TrackInfo(
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            reference = reference,
        )
}
