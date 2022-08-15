package com.zhufu.opencraft.data

import org.bson.Document

interface DatabaseRecord <T> {
    fun toDocument(): Document
    val parent: RecordHolder<T>?
}

interface RecordHolder <T> {
    fun update(record: T)
}