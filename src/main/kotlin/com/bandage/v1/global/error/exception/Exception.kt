package com.bandage.v1.global.error.exception

import com.bandage.v1.global.error.errorcode.ErrorCode

open class Exception(
    val errorCode: ErrorCode,
) : RuntimeException(errorCode.message)
