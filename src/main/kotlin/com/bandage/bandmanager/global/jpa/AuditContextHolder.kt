package com.bandage.bandmanager.global.jpa

object AuditContextHolder {
    private val context = ThreadLocal<Long>()

    fun setAuditor(memberId: Long) = context.set(memberId)

    fun getAuditor(): Long? = context.get()

    fun clear() = context.remove()
}
