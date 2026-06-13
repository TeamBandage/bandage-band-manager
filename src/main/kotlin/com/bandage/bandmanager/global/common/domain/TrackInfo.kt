package com.bandage.bandmanager.global.common.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class TrackInfo(
    title: String,
    artist: String,
    album: String? = null,
    duration: Int? = null,
    reference: String? = null,
) {
    @Column(name = "title", nullable = false)
    var title: String = title
        protected set

    @Column(name = "artist", nullable = false)
    var artist: String = artist
        protected set

    @Column(name = "album", nullable = true)
    var album: String? = album
        protected set

    // 곡 길이(초 단위). 분/초(mm:ss) 표시 변환은 클라이언트 책임.
    @Column(name = "duration", nullable = true)
    var duration: Int? = duration
        protected set

    // 참고 링크(예: YouTube). 트랙 메모(note)와는 별개 필드.
    @Column(name = "reference", nullable = true)
    var reference: String? = reference
        protected set

    fun update(
        title: String?,
        artist: String?,
        album: String?,
        duration: Int?,
        reference: String?,
    ) {
        title?.let { this.title = it }
        artist?.let { this.artist = it }
        album?.let { this.album = it }
        duration?.let { this.duration = it }
        reference?.let { this.reference = it }
    }
}
