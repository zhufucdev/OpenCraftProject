package com.zhufu.opencraft

fun Long.toGP() = GeneralPrise(this)
fun Int.toGP() = GeneralPrise(this.toLong())

fun Int.toUP() = UndefinedPrise(this)