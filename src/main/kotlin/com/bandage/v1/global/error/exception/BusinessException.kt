package com.bandage.v1.global.error.exception

import com.bandage.v1.global.error.errorcode.ErrorCode

open class BusinessException(
    val errorCode: ErrorCode,
) : RuntimeException(errorCode.message)
