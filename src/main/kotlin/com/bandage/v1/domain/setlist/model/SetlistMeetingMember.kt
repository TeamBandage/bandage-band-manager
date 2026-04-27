package com.bandage.v1.domain.setlist.model

import com.bandage.v1.global.common.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.SQLRestriction
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(
    name = "p_setlist_meeting_member",
    uniqueConstraints = [UniqueConstraint(name = "uk_setlist_meeting_member", columnNames = ["meeting_id", "member_id"])],
)
@SQLRestriction("deleted_at IS NULL")
open class SetlistMeetingMember(
    meeting: SetlistMeeting,
    memberId: Long,
) : BaseEntity() {
    @Id
    @Column(name = "meeting_member_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    lateinit var id: UUID
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    val meeting: SetlistMeeting = meeting

    @Column(name = "member_id", nullable = false)
    val memberId: Long = memberId

    companion object {
        fun create(
            meeting: SetlistMeeting,
            memberId: Long,
        ): SetlistMeetingMember = SetlistMeetingMember(meeting = meeting, memberId = memberId)
    }
}
