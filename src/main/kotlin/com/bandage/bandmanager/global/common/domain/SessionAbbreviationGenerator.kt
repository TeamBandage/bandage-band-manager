package com.bandage.bandmanager.global.common.domain

import com.bandage.bandmanager.global.error.errorcode.ErrorCode
import com.bandage.bandmanager.global.error.exception.BusinessException

/**
 * 세션 약어(short) 생성기(BD-229).
 *
 * 약어는 개별 세션의 성질이 아니라 **세션 목록의 성질**이다(한 트랙 / 한 합주 / 한 선곡 항목).
 * 같은 목록에 VOCAL 이 하나 더 추가되면 기존 VOCAL 의 정답이 V -> V1 로 바뀌므로,
 * 세션이 추가/수정될 때마다 목록 전체를 재생성해야 한다.
 *
 * 규칙
 *  1. label 은 영문 알파벳만 허용하고 대문자로 정규화한다.
 *  2. 기본 약어는 label 의 첫 글자.
 *  3. 같은 첫 글자를 가진 다른 label 이 있으면, 앞서 배정된 같은 첫 글자 label 들과
 *     **처음으로 달라지는 자리**의 글자를 첫 글자에 덧붙인다.
 *     (VOCAL -> V, VIOLIN -> VI, VIOLA -> VA, VIOLB -> VB)
 *  4. 같은 약어를 쓰는 항목이 2개 이상이면 등장 순서대로 1..N 을 접미한다.
 *  5. 길이는 3자(알파벳 2 + 숫자 1)를 지향하되, **유일성이 길이보다 우선**한다.
 *     동일 label 이 10개 이상이면 V10 처럼 3자를 넘을 수 있다.
 */
object SessionAbbreviationGenerator {
    private val ALPHABET_ONLY = Regex("^[A-Za-z]+$")

    /** label 을 검증한 뒤 대문자로 정규화한다. 알파벳 외 문자가 섞이면 예외. */
    fun normalizeLabel(label: String): String {
        val trimmed = label.trim()
        if (!ALPHABET_ONLY.matches(trimmed)) {
            throw BusinessException(ErrorCode.SESSION_LABEL_NOT_ALPHABETIC)
        }
        return trimmed.uppercase()
    }

    /**
     * 정규화된 label 목록에 대해 같은 순서의 약어 목록을 반환한다.
     * 입력은 [normalizeLabel] 을 통과한 대문자 문자열이어야 한다.
     */
    fun generate(labels: List<String>): List<String> {
        if (labels.isEmpty()) return emptyList()

        val baseByLabel = resolveBases(labels)
        val occurrences = labels.groupingBy { baseByLabel.getValue(it) }.eachCount()
        val counters = mutableMapOf<String, Int>()
        return labels.map { label ->
            val base = baseByLabel.getValue(label)
            if (occurrences.getValue(base) <= 1) {
                base
            } else {
                val order = counters.merge(base, 1, Int::plus)
                "$base$order"
            }
        }
    }

    /** label 목록을 검증·정규화한 뒤 (정규화된 label, 약어) 쌍을 순서대로 반환한다. */
    fun normalizeAndGenerate(labels: List<String>): List<Pair<String, String>> {
        val normalized = labels.map { normalizeLabel(it) }
        return normalized.zip(generate(normalized))
    }

    /**
     * label 별 기본 약어(숫자 접미 이전)를 계산한다.
     * 첫 글자별로 묶고, 목록 등장 순서를 유지한 distinct label 에 순서대로 약어를 배정한다.
     */
    private fun resolveBases(labels: List<String>): Map<String, String> {
        val groups = linkedMapOf<Char, MutableList<String>>()
        labels.forEach { label ->
            val bucket = groups.getOrPut(label[0]) { mutableListOf() }
            if (label !in bucket) bucket.add(label)
        }

        val baseByLabel = mutableMapOf<String, String>()
        groups.forEach { (initial, names) ->
            val used = mutableSetOf<String>()
            names.forEachIndexed { rank, name ->
                val base = if (rank == 0) initial.toString() else twoLetterBase(initial, name, names.take(rank), used)
                used.add(base)
                baseByLabel[name] = base
            }
        }
        return baseByLabel
    }

    /**
     * 앞선 동일 첫 글자 label 들과 처음으로 달라지는 자리의 글자를 골라 2글자 약어를 만든다.
     * 그런 자리가 없거나 이미 점유되었다면 미사용 2글자 조합을 앞에서부터 탐색하고,
     * 그마저 없으면 첫 글자로 폴백한다(이후 숫자 접미로 구분됨).
     */
    private fun twoLetterBase(
        initial: Char,
        name: String,
        previous: List<String>,
        used: Set<String>,
    ): String {
        val distinguishing =
            (1 until name.length)
                .firstOrNull { i -> previous.all { it.length <= i || it[i] != name[i] } }
                ?.let { "$initial${name[it]}" }
        if (distinguishing != null && distinguishing !in used) return distinguishing

        return (1 until name.length)
            .map { "$initial${name[it]}" }
            .firstOrNull { it !in used }
            ?: initial.toString()
    }
}
