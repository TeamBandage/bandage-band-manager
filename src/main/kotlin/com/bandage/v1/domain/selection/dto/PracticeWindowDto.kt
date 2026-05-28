package com.bandage.v1.domain.selection.dto

import com.bandage.v1.domain.selection.model.PracticeWindow
import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

@Schema(description = "선곡 합주 가능 기간")
data class PracticeWindowDto(
    @field:NotNull
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @Schema(description = "기간 시작일", example = "2026-05-02")
    val from: LocalDate,
    @field:NotNull
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @Schema(description = "기간 종료일 (포함)", example = "2026-05-15")
    val to: LocalDate,
) {
    fun toEntity(): PracticeWindow = PracticeWindow(from = from, to = to)

    companion object {
        fun of(window: PracticeWindow): PracticeWindowDto = PracticeWindowDto(from = window.from, to = window.to)
    }
}
