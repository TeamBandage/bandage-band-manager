package com.bandage.bandmanager.global.error.exception

import com.bandage.bandmanager.global.error.errorcode.ErrorCode

open class Exception(
    val errorCode: ErrorCode,
) : RuntimeException(errorCode.message)
