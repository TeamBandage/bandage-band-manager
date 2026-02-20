package com.bandage.v1.global.common.exception.exception

import com.bandage.v1.global.common.exception.errorcode.ErrorCode

open class Exception(
    val errorCode: ErrorCode,
) : RuntimeException(errorCode.message)
