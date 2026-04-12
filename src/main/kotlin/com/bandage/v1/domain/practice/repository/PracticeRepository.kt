package com.bandage.v1.domain.practice.repository

import com.bandage.v1.domain.practice.model.Practice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PracticeRepository : JpaRepository<Practice, UUID>
