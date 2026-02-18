package com.bandage.v1.domain.performance.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.util.UUID

@Entity
@Table(name = "p_setlist_item")
class SetlistItem() {
    @Id
    @Column(name = "setlist_item_id")
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    var id: UUID? = null
}