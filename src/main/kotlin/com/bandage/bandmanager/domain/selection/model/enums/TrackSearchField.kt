package com.bandage.bandmanager.domain.selection.model.enums

/** 트랙 정보 검색 대상 필드(BD-228). 미지정 시 전체 필드를 OR 검색한다. */
enum class TrackSearchField {
    TITLE,
    ARTIST,
    ALBUM,
}
