package com.bandage.bandmanager.domain.selection.dto.req

import com.bandage.bandmanager.domain.selection.model.enums.RecruitStatus
import com.bandage.bandmanager.domain.selection.model.enums.TrackSearchField
import com.bandage.bandmanager.domain.selection.repository.TrackSelectionItemFilter
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.util.UUID

@Schema(description = "선곡 항목 목록 조회 쿼리. 4개 필터(status/appliedByMe/memberName/keyword)는 서로 독립적이며 AND 로 결합된다.")
data class TrackSelectionItemPagingQuery(
    val lastId: UUID?,
    @field:Min(1)
    @field:Max(200)
    val pageSize: Int = 50,
    @Schema(
        description =
            "모집 상태 필터. 여러 개 지정 시 OR(합집합). 상태끼리 겹칠 수 있다" +
                "(예: ASSIGN_COMPLETED 항목은 APPLY_COMPLETED 조건도 만족). 미지정 시 전체 조회.",
        example = "OPEN",
    )
    val status: List<RecruitStatus>? = null,
    @Schema(description = "true: 내가 지원한 항목만, false: 내가 지원하지 않은 항목만, 미지정: 전체", example = "true")
    val appliedByMe: Boolean? = null,
    @field:Size(max = 50)
    @Schema(description = "참여자(지원자) 이름 부분 검색. 대소문자를 무시한다.", example = "홍길")
    val memberName: String? = null,
    @field:Size(max = 100)
    @Schema(description = "트랙 정보 검색어. 대소문자를 무시한 부분 일치이며 searchFields 와 함께 사용한다.", example = "stairway")
    val keyword: String? = null,
    @Schema(description = "keyword 검색 대상 필드. 미지정 시 TITLE/ARTIST/ALBUM 전체를 OR 검색한다.", example = "TITLE")
    val searchFields: List<TrackSearchField>? = null,
) {
    fun toFilter(): TrackSelectionItemFilter =
        TrackSelectionItemFilter(
            status = status,
            appliedByMe = appliedByMe,
            memberName = memberName,
            keyword = keyword,
            searchFields = searchFields,
        )
}
