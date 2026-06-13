package com.bandage.bandmanager.domain.schedule.model

import com.bandage.bandmanager.domain.selection.model.PracticeWindow
import com.bandage.bandmanager.global.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.time.LocalDate
import java.util.UUID

/**
 * 시간표 시안. PRD-2 에서 스코프가 TrackSelection(meeting) → Performance 로 이전되었다.
 *
 * - performanceId: 보드가 속한 공연(필수)
 * - meetingId: 구(舊) 선곡회의 스코프. 마이그레이션/dual-write 기간 동안만 유지되는 nullable 필드(추후 drop)
 * - windowFrom/windowTo: 보드 레벨 연습 가능 날짜 범위. 미지정 시 Performance 기준으로 계산
 */
@Entity
@Table(name = "p_schedule_board")
@SQLRestriction("deleted_at IS NULL")
open class ScheduleBoard(
    performanceId: UUID,
    meetingId: UUID?,
    name: String,
    paletteSeed: Int?,
    constraints: ScheduleBoardConstraints,
    windowFrom: LocalDate?,
    windowTo: LocalDate?,
) : BaseEntity() {
    @Id
    @Column(name = "schedule_board_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @Column(name = "performance_id", nullable = false)
    val performanceId: UUID = performanceId

    @Column(name = "meeting_id", nullable = true)
    val meetingId: UUID? = meetingId

    @Column(name = "name", nullable = false, length = 50)
    var name: String = name
        protected set

    @Column(name = "palette_seed", nullable = true)
    var paletteSeed: Int? = paletteSeed
        protected set

    @Column(name = "confirmed", nullable = false)
    var confirmed: Boolean = false
        protected set

    @Embedded
    val constraints: ScheduleBoardConstraints = constraints

    @Column(name = "window_from", nullable = true)
    var windowFrom: LocalDate? = windowFrom
        protected set

    @Column(name = "window_to", nullable = true)
    var windowTo: LocalDate? = windowTo
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
        protected set

    companion object {
        fun create(
            performanceId: UUID,
            name: String,
            paletteSeed: Int? = null,
            constraints: ScheduleBoardConstraints = ScheduleBoardConstraints(),
            windowFrom: LocalDate? = null,
            windowTo: LocalDate? = null,
            meetingId: UUID? = null,
        ): ScheduleBoard =
            ScheduleBoard(
                performanceId = performanceId,
                meetingId = meetingId,
                name = name,
                paletteSeed = paletteSeed,
                constraints = constraints,
                windowFrom = windowFrom,
                windowTo = windowTo,
            )
    }

    /** 보드 레벨 연습 가능 날짜 범위. windowFrom/To 가 모두 설정된 경우에만 반환. */
    fun practiceWindowOrNull(): PracticeWindow? {
        val from = windowFrom
        val to = windowTo
        return if (from != null && to != null) PracticeWindow(from, to) else null
    }

    fun updateWindow(
        from: LocalDate?,
        to: LocalDate?,
    ) {
        this.windowFrom = from
        this.windowTo = to
    }

    fun rename(newName: String) {
        this.name = newName
    }

    fun updatePaletteSeed(seed: Int?) {
        this.paletteSeed = seed
    }

    fun confirm() {
        this.confirmed = true
    }

    fun unconfirm() {
        this.confirmed = false
    }
}
