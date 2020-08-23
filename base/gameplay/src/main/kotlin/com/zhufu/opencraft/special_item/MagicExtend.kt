package com.zhufu.opencraft.special_item

import com.zhufu.opencraft.*
import com.zhufu.opencraft.rpg.Role
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import kotlin.math.abs

class MagicList private constructor(private val owner: ServerPlayer) : MutableCollection<MagicBook.Magic> {
    private val mList = arrayListOf<MagicBook.Magic>()

    init {
        if (owner.role == Role.PRIEST) {
            mList.addAll(MagicBook.Magic.values().filter { it.isHealing })
        } else {
            owner.tag.getStringList("magic").forEach {
                mList.add(MagicBook.Magic.valueOf(it.toUpperCase()))
            }
        }
    }

    companion object {
        private val map = hashMapOf<ServerPlayer, MagicList>()
        fun of(info: ServerPlayer): MagicList {
            return map.getOrDefault(info, MagicList(info))
        }
    }

    private fun push() {
        owner.tag.set("magic", mList.map { it.name.toLowerCase() })
    }

    private fun clearTag() {
        owner.tag.set("magic", null)
    }

    override val size: Int
        get() = mList.size

    override fun contains(element: MagicBook.Magic): Boolean = mList.contains(element)

    override fun containsAll(elements: Collection<MagicBook.Magic>): Boolean = mList.containsAll(elements)

    override fun isEmpty(): Boolean = size > 0

    override fun add(element: MagicBook.Magic): Boolean {
        if (!mList.contains(element)) {
            mList.add(element)
            push()
            return true
        }
        return false
    }

    override fun addAll(elements: Collection<MagicBook.Magic>): Boolean {
        mList.addAll(elements.filter { !mList.contains(it) })
        push()
        return true
    }

    override fun clear() {
        mList.clear()
        clearTag()
    }

    override fun iterator(): MutableIterator<MagicBook.Magic> = mList.iterator()

    override fun remove(element: MagicBook.Magic): Boolean {
        mList.remove(element)
        if (isEmpty())
            clearTag()
        else
            push()
        return true
    }

    override fun removeAll(elements: Collection<MagicBook.Magic>): Boolean {
        mList.removeAll(elements)
        if (isEmpty())
            clearTag()
        else
            push()
        return true
    }

    override fun retainAll(elements: Collection<MagicBook.Magic>): Boolean {
        val r = mList.retainAll(elements)
        if (r) push()
        return r
    }
}

val ServerPlayer.magic: MagicList
    get() = MagicList.of(this)

val ServerPlayer.maxMP: Int
    get() =
        if (role == Role.MAGICIAN || role == Role.PRIEST)
            when (level) {
                1 -> 40
                2 -> 100
                3 -> 150
                else -> 180
            }
        else -1

val ServerPlayer.mpRecoverRate: Short
    get() = if (role == Role.MAGICIAN || role == Role.PRIEST)
        when (level) {
            1 -> 2
            2 -> 6
            3 -> 8
            else -> 8
        }
    else 0

var ServerPlayer.mp: Int
    get() = tag.getInt("mp")
    set(value) = tag.set("mp", value)

private val lastExp = hashMapOf<Player, Float>()
fun Info.showMP(playSound: Boolean = true) {
    val mp = this.mp
    val getter = Language.LangGetter(this)
    player.apply {
        val exp = mp / maxMP.toFloat()
        if (exp > 1) throw IllegalArgumentException("${name}'s MP is $mp, bigger than maximum $maxMP.")
        fun setExp() {
            sendExperienceChange(exp, mp)
            lastExp[this] = exp
        }

        sendActionBar(getter["rpg.wand.mp", mp].toInfoMessage())
        if (lastExp.containsKey(this)) {
            var x = lastExp[this]!!
            val delta = exp - x
            Bukkit.getScheduler().runTaskTimer(Base.pluginCore, { t: BukkitTask ->
                if (abs(x - exp) < 0.001) {
                    t.cancel()
                    setExp()
                    return@runTaskTimer
                }
                x += delta / 5
                sendExperienceChange(x, mp)
            }, 0, 1)
        } else {
            setExp()
        }

        if (playSound)
            playSound(eyeLocation, Sound.BLOCK_NOTE_BLOCK_CHIME, 1F, 0.6F)
    }
}