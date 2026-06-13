package com.bandage.bandmanager.domain.schedule.placement

import com.bandage.bandmanager.domain.schedule.placement.strategy.SoftScorer
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 합주 일정 자동 배치기 v1.
 *
 * 1) Greedy 초기해: 어려운(참여자 많은) 항목부터, 각 세션을 hard 제약 통과 후보 중 최고 점수 슬롯에 배치
 * 2) Hill Climbing: 배치된 블록을 더 높은 점수의 슬롯으로 이동(first-improvement), 개선 없으면 종료
 * 3) Explainability: 배치 사유(placementReasons)와 미배치 항목의 nearMisses 기록
 *
 * 후보는 dateGenerator × 시작슬롯으로 생성하고, hard 제약 위반은 점수 계산 전에 걸러 비용을 줄인다.
 */
@Component
class AutoPlacer(
    private val availabilityCalculator: AvailabilityCalculator,
) {
    fun propose(context: PlacementContext): Proposal {
        val initial = greedyInitial(context)
        return hillClimb(context, initial)
    }

    // ===== 1) Greedy =====

    private fun greedyInitial(context: PlacementContext): Proposal {
        val duration = context.strategy.blockShape.durationSlots
        val dates = context.strategy.dateGenerator.generate(context.windowFrom, context.windowTo)

        val placed = mutableListOf<ProposedBlock>()
        val unplaced = mutableListOf<UnplacedItem>()
        // 고정(anchored) 블록을 pending 으로 선점하여 새 배치가 겹치지 않도록 한다.
        val pending = context.anchored.toMutableList()

        // 참여자가 많아 배치가 어려운 항목부터, 그리고 세션 수만큼 demand 전개
        val demands =
            context.items
                .sortedByDescending { it.memberIds.size }
                .flatMap { item -> (1..item.sessionsNeeded.coerceAtLeast(1)).map { item } }

        demands.forEach { item ->
            val env = ScoringEnv(context.windowFrom, context.windowTo, pending.toList())
            val candidates = evaluateCandidates(context, item, duration, dates, pending.toList())
            val feasible = candidates.filter { context.strategy.passesHardConstraints(it, env) }
            val best = feasible.maxByOrNull { context.strategy.totalScore(it, env) }

            if (best != null) {
                val block = toProposedBlock(context, item, best, env)
                placed.add(block)
                pending.add(block.toPendingBlock())
            } else {
                unplaced.add(toUnplaced(item, candidates))
            }
        }

        return Proposal(blocks = placed, unplaced = unplaced, totalScore = placed.sumOf { it.score })
    }

    // ===== 2) Hill Climbing =====

    private fun hillClimb(
        context: PlacementContext,
        initial: Proposal,
    ): Proposal {
        if (initial.blocks.isEmpty()) return initial
        val duration = context.strategy.blockShape.durationSlots
        val dates = context.strategy.dateGenerator.generate(context.windowFrom, context.windowTo)
        val blocks = initial.blocks.toMutableList()

        var iteration = 0
        while (iteration < MAX_ITERATIONS) {
            iteration++
            val move = findImprovingMove(context, blocks, duration, dates) ?: break
            blocks[move.first] = move.second
        }
        return Proposal(blocks = blocks, unplaced = initial.unplaced, totalScore = blocks.sumOf { it.score })
    }

    private fun findImprovingMove(
        context: PlacementContext,
        blocks: List<ProposedBlock>,
        duration: Int,
        dates: List<LocalDate>,
    ): Pair<Int, ProposedBlock>? {
        for (i in blocks.indices) {
            val block = blocks[i]
            val others = blocks.filterIndexed { idx, _ -> idx != i }.map { it.toPendingBlock() }
            val env = ScoringEnv(context.windowFrom, context.windowTo, others)

            val item = PlaceableItem(block.trackId, block.memberIds, 1)
            val currentScore = currentScore(context, block, others, env)

            val candidates = evaluateCandidates(context, item, duration, dates, others)
            val best =
                candidates
                    .filter { context.strategy.passesHardConstraints(it, env) }
                    .maxByOrNull { context.strategy.totalScore(it, env) }
                    ?: continue

            val bestScore = context.strategy.totalScore(best, env)
            if (bestScore > currentScore + IMPROVEMENT_EPSILON) {
                return i to toProposedBlock(context, item, best, env)
            }
        }
        return null
    }

    private fun currentScore(
        context: PlacementContext,
        block: ProposedBlock,
        others: List<PendingBlock>,
        env: ScoringEnv,
    ): Double {
        val feas =
            availabilityCalculator.evaluate(
                context.availabilityContext,
                block.date,
                block.startSlot,
                block.durationSlots,
                block.memberIds.toList(),
                others,
            )
        val candidate = PlacementCandidate(block.date, block.startSlot, block.durationSlots, feas)
        return if (context.strategy.passesHardConstraints(candidate, env)) {
            context.strategy.totalScore(candidate, env)
        } else {
            Double.NEGATIVE_INFINITY
        }
    }

    // ===== 후보 생성/평가 (lazy → 필요한 만큼 평가) =====

    private fun evaluateCandidates(
        context: PlacementContext,
        item: PlaceableItem,
        duration: Int,
        dates: List<LocalDate>,
        pending: List<PendingBlock>,
    ): List<PlacementCandidate> {
        val memberIds = item.memberIds.toList()
        val maxStart = SLOTS_PER_DAY - duration
        if (maxStart < 0) return emptyList()
        val result = ArrayList<PlacementCandidate>(dates.size * (maxStart + 1))
        for (date in dates) {
            for (start in 0..maxStart) {
                val feas =
                    availabilityCalculator.evaluate(
                        context.availabilityContext,
                        date,
                        start,
                        duration,
                        memberIds,
                        pending,
                    )
                result.add(PlacementCandidate(date, start, duration, feas))
            }
        }
        return result
    }

    // ===== 3) Explainability =====

    private fun toProposedBlock(
        context: PlacementContext,
        item: PlaceableItem,
        candidate: PlacementCandidate,
        env: ScoringEnv,
    ): ProposedBlock {
        val score = context.strategy.totalScore(candidate, env)
        return ProposedBlock(
            trackId = item.trackId,
            date = candidate.date,
            startSlot = candidate.startSlot,
            durationSlots = candidate.durationSlots,
            memberIds = item.memberIds,
            score = score,
            availabilityRatio = candidate.feasibility.availabilityRatio,
            placementReasons = placementReasons(context, candidate, env),
        )
    }

    private fun placementReasons(
        context: PlacementContext,
        candidate: PlacementCandidate,
        env: ScoringEnv,
    ): List<String> {
        val reasons = mutableListOf<String>()
        val f = candidate.feasibility
        reasons.add("가용 ${f.availableMembers.size}/${f.totalMembers}명 (${(f.availabilityRatio * 100).toInt()}%)")
        // 점수 기여가 큰 SoftScorer 상위 2개를 사유로 제시
        context.strategy.softScorers
            .map { scorerLabel(it) to it.score(candidate, env) }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }
            .take(2)
            .forEach { reasons.add(it.first) }
        return reasons
    }

    private fun scorerLabel(scorer: SoftScorer): String =
        when (scorer) {
            is SoftScorer.AvailabilityScorer -> "가용성 높음"
            is SoftScorer.TimeOfDayScorer -> "선호 시간대"
            is SoftScorer.EarlyDateScorer -> "이른 날짜"
            is SoftScorer.CompactnessScorer -> "연속 배치"
        }

    private fun toUnplaced(
        item: PlaceableItem,
        candidates: List<PlacementCandidate>,
    ): UnplacedItem {
        val nearMisses =
            candidates
                .sortedByDescending { it.feasibility.availabilityRatio }
                .take(MAX_NEAR_MISSES)
                .map { c ->
                    NearMiss(
                        date = c.date,
                        startSlot = c.startSlot,
                        durationSlots = c.durationSlots,
                        availabilityRatio = c.feasibility.availabilityRatio,
                        reason = dominantConflictReason(c),
                    )
                }
        val reason =
            if (candidates.isEmpty()) {
                "후보 날짜가 없습니다(window/요일 전략 확인 필요)"
            } else {
                "모든 후보가 제약을 만족하지 못했습니다"
            }
        return UnplacedItem(item.trackId, item.memberIds, reason, nearMisses)
    }

    private fun dominantConflictReason(candidate: PlacementCandidate): String {
        val conflicts = candidate.feasibility.conflicts
        if (conflicts.isEmpty()) return "hard 제약 위반(근무시간/길이 등)"
        val dominant =
            conflicts
                .groupingBy { it.reason }
                .eachCount()
                .maxByOrNull { it.value }!!
                .key
        return when (dominant) {
            ConflictReason.UNAVAILABLE -> "일부 멤버 가용 시간 아님"
            ConflictReason.JAM_RESERVATION -> "다른 합주와 시간 충돌"
            ConflictReason.PENDING_BLOCK -> "같은 보드 내 블록과 시간 충돌"
        }
    }

    companion object {
        const val SLOTS_PER_DAY: Int = 48
        const val MAX_ITERATIONS: Int = 50
        const val MAX_NEAR_MISSES: Int = 3
        private const val IMPROVEMENT_EPSILON: Double = 1e-9
    }
}
