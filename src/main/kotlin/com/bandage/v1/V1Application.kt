package com.bandage.v1

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@SpringBootApplication
class V1Application

fun main(args: Array<String>) {
    runApplication<V1Application>(*args)
}
