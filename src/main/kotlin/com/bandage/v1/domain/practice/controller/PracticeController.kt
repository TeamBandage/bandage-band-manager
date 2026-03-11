package com.bandage.v1.domain.practice.controller

import com.bandage.v1.global.common.constants.PathPrefix.PREFIX
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "practices", description = "합주 API")
@RestController
@RequestMapping("$PREFIX/practices")
class PracticeController {
    // TODO: 합주 생성 API
    // TODO: 합주 조회 API
    // TODO: 합주 세션 생성 API
    // TODO: 합주 세션 삭제 API
    // TODO: 합주 멤버 추가
    // TODO: 합주 세션 추가
    // TODO: 합주 세션 멤버 지정 API (본인)
    // TODO: 합주 세션 멤버 지정 취소 API (본인)
    // TODO: 합주 일정 변경 API
    // TODO: 합주 장소 변경 API
    // TODO: 합주 삭제 API
}
