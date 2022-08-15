package com.zhufu.opencraft.data

import org.bson.Document

class PlayerPreference(private val doc: Document) {
    var playerUtilitiesGesture: Boolean
        get() = doc.getBoolean("prefer.gesture", true)
        set(value) = doc.set("prefer.gesture", value)
    var showTranslations: Boolean
        get() = doc.getBoolean("prefer.translations", true)
        set(value) = doc.set("prefer.translations", value)
    var sendMessagesOnLogin: Boolean
        get() = doc.getBoolean("prefer.messages", true)
        set(value) = doc.set("prefer.messages", value)
}