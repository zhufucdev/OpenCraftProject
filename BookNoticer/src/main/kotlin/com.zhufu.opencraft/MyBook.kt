package com.zhufu.opencraft

import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.zhufu.opencraft.util.toComponent
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import java.io.StringWriter


class MyBook : ItemStack {
    private val meta = this.itemMeta as BookMeta
    private var content: String = ""

    constructor(reader: JsonReader) : super(Material.WRITTEN_BOOK) {
        val jsonObject = JsonParser.parseReader(reader).asJsonObject
        if (jsonObject.has("title")) {
            meta.title = jsonObject["title"].asString
        }
        if (jsonObject.has("author")) {
            meta.author = jsonObject["author"].asString
        }
        if (jsonObject.has("content")) {
            content = jsonObject["content"].asString
            meta.addPages(*separateIntoPages(content, 13))
        }
    }

    private constructor(title: String, author: String, content: String) : super(Material.WRITTEN_BOOK) {
        meta.title = title
        meta.author = author
        meta.addPages(*separateIntoPages(content, 13))
        val sb = StringBuilder()
        meta.pages().forEach { sb.append(it) }
        this.content = sb.toString()
    }

    private constructor(title: String, author: String, content: List<Component>) : super(Material.WRITTEN_BOOK) {
        meta.title = title
        meta.author = author
        val sb = StringBuilder()
        content.forEach { sb.append(it) }
        this.content = sb.toString()
        meta.addPages(*content.toTypedArray())
    }

    private fun separateIntoPages(content: String, linePerPage: Int): Array<Component> {
        val r = ArrayList<Component>()
        var sb = StringBuilder()
        var i = 0
        var lines = 0
        val charPerLine = 24
        content.forEach {
            when {
                it == '\n' -> {
                    i = charPerLine + 1
                }

                it <= 'Z' -> i++
                else -> i += 2
            }
            sb.append(it)
            if (i >= charPerLine) {
                i = 0
                lines++
                if (lines > linePerPage) {
                    r.add(sb.toString().toComponent())
                    sb = StringBuilder()
                    lines = 0
                }
            }
        }
        if (r.isEmpty())
            return arrayOf(sb.toString().toComponent())

        return r.toTypedArray()
    }

    fun show(player: Player, open: Boolean = false) {
        this.itemMeta = meta
        val sw = StringWriter()
        val writer = JsonWriter(sw)
        this.toJson(writer).flush()

        val itemHeld = player.inventory.itemInMainHand.clone()
        player.inventory.setItem(0, this)
        if (!open) return

        player.openBook(this)
    }

    fun toJson(writer: JsonWriter): JsonWriter {
        writer
            .beginObject()
            .name("title").value(meta.title)
            .name("author").value(meta.author)
            .name("content").value(content)
            .endObject()
        return writer
    }

    class BookBuilder {
        private var title: String = ""
        private var author: String = ""
        private var content: String = ""
        private var pages: List<Component> = listOf()

        fun setTitle(title: String): BookBuilder {
            this.title = title
            return this
        }

        fun setAuthor(author: String): BookBuilder {
            this.author = author
            return this
        }

        fun setContent(content: String): BookBuilder {
            this.content = content
            return this
        }

        fun setPageContent(content: List<Component>): BookBuilder {
            this.pages = content
            return this
        }

        fun build() = if (content.isNotEmpty()) MyBook(title, author, content) else MyBook(title, author, pages)
    }
}