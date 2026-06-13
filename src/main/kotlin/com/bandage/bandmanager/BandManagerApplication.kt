package com.bandage.bandmanager

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@SpringBootApplication
class BandManagerApplication

fun main(args: Array<String>) {
    runApplication<BandManagerApplication>(*args)
}
