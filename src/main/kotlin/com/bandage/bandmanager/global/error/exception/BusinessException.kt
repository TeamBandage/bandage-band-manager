package com.bandage.bandmanager.global.error.exception

import com.bandage.bandmanager.global.error.errorcode.ErrorCode

open class BusinessException(
    val errorCode: ErrorCode,
) : RuntimeException(errorCode.message)
