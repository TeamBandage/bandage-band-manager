package com.bandage.bandmanager.domain.performance.repository

import com.bandage.bandmanager.domain.performance.model.PerformancePoster
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PerformancePosterRepository :
    JpaRepository<PerformancePoster, UUID>,
    PerformancePosterRepositoryCustom
