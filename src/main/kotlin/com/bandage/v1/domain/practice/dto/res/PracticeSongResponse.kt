package com.bandage.v1.domain.practice.dto.res

import com.bandage.v1.domain.practice.model.PracticeSong
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "합주곡 응답")
data class PracticeSongResponse(
    @Schema(description = "합주곡 고유 식별자 (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val songId: UUID,
    @Schema(description = "곡 제목", example = "Vicarious")
    val title: String,
    @Schema(description = "아티스트", example = "Tool")
    val artist: String,
    @Schema(description = "앨범", example = "10,000 Days")
    val album: String,
    @Schema(description = "곡 길이 (초)", example = "426")
    val duration: Int,
    @Schema(description = "참조 링크", example = "https://www.youtube.com/watch?v=example")
    val refLink: String?,
) {
    companion object {
        fun of(song: PracticeSong): PracticeSongResponse =
            PracticeSongResponse(
                songId = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                duration = song.duration,
                refLink = song.refLink,
            )
    }
}
