package com.zhufu.opencraft

class PlayerPreference(private val parent: ServerPlayer) {
    private val tag by lazy { parent.tag }
    var playerUtilitiesGesture: Boolean
        get() = tag.getBoolean("prefer.gesture", true)
        set(value) = tag.set("prefer.gesture", value)
    var showTranslations: Boolean
        get() = tag.getBoolean("prefer.translations", true)
        set(value) = tag.set("prefer.translations", value)
    var sendMessagesOnLogin: Boolean
        get() = tag.getBoolean("prefer.messages", true)
        set(value) = tag.set("prefer.messages", value)
}