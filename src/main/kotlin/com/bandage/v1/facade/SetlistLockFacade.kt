package com.bandage.v1.facade

import com.bandage.v1.domain.practice.model.PracticeSong
import com.bandage.v1.domain.practice.repository.PracticeSongRepository
import com.bandage.v1.domain.setlist.dto.res.SetlistLockDiff
import com.bandage.v1.domain.setlist.dto.res.SetlistLockResponse
import com.bandage.v1.domain.setlist.dto.res.SetlistLockSongMapping
import com.bandage.v1.domain.setlist.model.SetlistMeetingItem
import com.bandage.v1.domain.setlist.repository.SetlistMeetingItemRepository
import com.bandage.v1.domain.setlist.repository.SetlistMeetingRepository
import com.bandage.v1.global.error.errorcode.ErrorCode
import com.bandage.v1.global.error.exception.BusinessException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SetlistLockFacade(
    private val meetingRepository: SetlistMeetingRepository,
    private val itemRepository: SetlistMeetingItemRepository,
    private val practiceSongRepository: PracticeSongRepository,
) {
    @Transactional
    fun lockMeeting(
        meetingId: UUID,
        memberId: Long,
    ): SetlistLockResponse {
        val meeting =
            meetingRepository.findByIdOrNull(meetingId)
                ?: throw BusinessException(ErrorCode.SETLIST_MEETING_NOT_FOUND)
        if (meeting.managerId != memberId) throw BusinessException(ErrorCode.SETLIST_MEETING_NOT_MANAGER)
        if (meeting.isLocked) throw BusinessException(ErrorCode.SETLIST_MEETING_LOCKED)

        val items = itemRepository.findAllByMeeting(meeting)
        val added = mutableListOf<SetlistLockSongMapping>()
        val updated = mutableListOf<SetlistLockSongMapping>()

        items.forEach { item ->
            val existingId = item.practiceSongId
            if (existingId == null) {
                val created = practiceSongRepository.save(toPracticeSong(item))
                item.assignPracticeSongId(created.id)
                added += SetlistLockSongMapping(setlistMeetingItemId = item.id, practiceSongId = created.id)
            } else {
                val song = practiceSongRepository.findByIdOrNull(existingId)
                if (song == null) {
                    val created = practiceSongRepository.save(toPracticeSong(item))
                    item.assignPracticeSongId(created.id)
                    added += SetlistLockSongMapping(setlistMeetingItemId = item.id, practiceSongId = created.id)
                } else if (isChanged(song, item)) {
                    song.updateAll(
                        title = item.title,
                        artist = item.artist,
                        album = item.album.orEmpty(),
                        duration = parseDurationSeconds(item.duration),
                        refLink = song.refLink,
                    )
                    updated += SetlistLockSongMapping(setlistMeetingItemId = item.id, practiceSongId = song.id)
                }
            }
        }

        meeting.lock()
        val songs = items.map { SetlistLockSongMapping(setlistMeetingItemId = it.id, practiceSongId = it.practiceSongId) }
        val practiceSongMap = items.mapNotNull { it.practiceSongId?.let { sid -> it.id to sid } }.toMap()

        return SetlistLockResponse(
            lockedAt = meeting.lockedAt!!,
            songs = songs,
            practiceSongMap = practiceSongMap,
            diff = SetlistLockDiff(added = added, updated = updated),
        )
    }

    private fun toPracticeSong(item: SetlistMeetingItem): PracticeSong =
        PracticeSong.create(
            title = item.title,
            artist = item.artist,
            album = item.album.orEmpty(),
            duration = parseDurationSeconds(item.duration),
        )

    private fun isChanged(
        song: PracticeSong,
        item: SetlistMeetingItem,
    ): Boolean =
        song.title != item.title ||
            song.artist != item.artist ||
            song.album != item.album.orEmpty() ||
            song.duration != parseDurationSeconds(item.duration)

    /**
     * SetlistMeetingItem.duration 은 String? (FE 가 "MM:SS", "M:SS", 또는 초 단위 정수 문자열을 보냄).
     * PracticeSong.duration 은 초 단위 Int. 파싱 실패 / null 인 경우 0 반환 — 매니저가 사후 보정.
     */
    private fun parseDurationSeconds(raw: String?): Int {
        if (raw.isNullOrBlank()) return 0
        val trimmed = raw.trim()
        if (":" in trimmed) {
            val parts = trimmed.split(":")
            if (parts.size == 2) {
                val mm = parts[0].toIntOrNull() ?: return 0
                val ss = parts[1].toIntOrNull() ?: return 0
                return mm * 60 + ss
            }
        }
        return trimmed.toIntOrNull() ?: 0
    }
}
