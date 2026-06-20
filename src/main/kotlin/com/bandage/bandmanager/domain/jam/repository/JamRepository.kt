package com.bandage.bandmanager.domain.jam.repository

import com.bandage.bandmanager.domain.jam.model.Jam
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface JamRepository :
    JpaRepository<Jam, UUID>,
    JamRepositoryCustom {
    // 임박 합주 알림 스케줄러용: [from, to) 구간에 시작하는 합주 조회
    @Query("SELECT j FROM Jam j WHERE j.timeInfo.startAt >= :from AND j.timeInfo.startAt < :to")
    fun findUpcomingBetween(
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): List<Jam>
}
