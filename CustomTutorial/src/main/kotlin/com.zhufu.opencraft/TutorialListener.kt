package com.zhufu.opencraft

import com.zhufu.opencraft.TutorialManager.Tutorial
import com.zhufu.opencraft.TutorialManager.Tutorial.TutorialStep
import com.zhufu.opencraft.data.DualInventory
import com.zhufu.opencraft.data.Info
import com.zhufu.opencraft.events.PlayerTeleportedEvent
import com.zhufu.opencraft.ui.EditorUI
import com.zhufu.opencraft.util.Language
import com.zhufu.opencraft.util.toInfoMessage
import com.zhufu.opencraft.util.toTipMessage
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.title.Title
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import java.time.Duration

class TutorialListener : Listener {
    companion object {
        lateinit var instance: TutorialListener
    }

    init {
        instance = this
    }

    val inCreation = HashMap<Player, Tutorial>()
    val inRecording = HashMap<Player, TutorialStep>()
    val originProjectData = HashMap<Player, Tutorial>()
    val originData = HashMap<Player, TutorialStep>()
    fun creator(player: Player, project: Tutorial) {
        if (inCreation.containsKey(player))
            return
        val info = PlayerManager.findInfoByPlayer(player)
        if (info == null) {
            player.error(Language.getDefault("player.error.unknown"))
            return
        }
        info.status = Info.GameStatus.InTutorial
        info.inventory.create(DualInventory.RESET).load(inventoryOnly = true)
        originProjectData[player] = project.clone()
        inCreation[player] = project
        player.gameMode = GameMode.SPECTATOR
        player.closeInventory()
        player.showTitle(
            Title.title(
                "您已进入编辑模式".toInfoMessage(),
                "输入/ct 或使用鼠标点击以调出编辑模式菜单".toTipMessage(),
                Title.Times.times(
                    Duration.ofMillis(300),
                    Duration.ofSeconds(4),
                    Duration.ofMillis(150)
                )
            )
        )
    }

    fun recorder(player: Player, step: TutorialStep) {
        creator(player, step.project)
        if (inRecording.containsKey(player))
            return
        val info = PlayerManager.findInfoByPlayer(player)
        if (info == null) {
            player.error(Language.getDefault("player.error.unknown"))
            return
        }
        info.doNotTranslate = true
        originData[player] = step.clone()
        inRecording[player] = step
        player.info("您已进入记录模式")
        player.tip("您的聊天栏已被替换为记录模式指令输入器，输入\"help\"查看帮助")
    }

    fun removeRecorder(player: Player) {
        inRecording.remove(player)
        PlayerManager.findInfoByPlayer(player)?.doNotTranslate = false
    }

    fun removeCreator(player: Player) {
        val info = PlayerManager.findInfoByPlayer(player)
        if (info == null) {
            player.error(Language.getDefault("player.error.unknown"))
            return
        }
        val lastInventory = info.inventory.last
        info.status = if (lastInventory.name == "survivor") Info.GameStatus.Surviving else Info.GameStatus.InLobby
        lastInventory.load()

        removeRecorder(player)
        inCreation.remove(player)
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (event.to.block.type != Material.AIR && event.to.block.type.isSolid
            && event.player.gameMode == GameMode.SPECTATOR
            && inCreation.containsKey(event.player)
        ) {
            event.isCancelled = true
        } else if (!inCreation.containsKey(event.player)) {
            Bukkit.getScheduler().runTaskAsynchronously(TutorialManager.mPlugin!!) { _ ->
                for (it in TutorialManager.everything()) {
                    if (it.triggerOrNot(event.player)) {
                        it.play(event.player, true)
                        break
                    }
                }
            }
        }
    }

    @EventHandler
    fun onPlayerRegister(event: com.zhufu.opencraft.events.PlayerRegisterEvent) {
        Bukkit.getScheduler().runTaskAsynchronously(TutorialManager.mPlugin!!) { _ ->
            for (it in TutorialManager.everything()) {
                if (it.triggerMethod == Tutorial.TriggerMethod.REGISTER) {
                    it.play(event.info.player, true)
                    break
                }
            }
        }
    }

    @EventHandler
    fun onPlayerClick(event: PlayerInteractEvent) {
        val project = inCreation[event.player]
        if (project != null && !inRecording.containsKey(event.player)) {
            EditorUI(project).show(event.player)
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerTeleported(event: PlayerTeleportedEvent) {
        if (inCreation.containsKey(event.player)) {
            inCreation[event.player]!!
                .also {
                    it.isDraft = true
                    it.id = TutorialManager.getNewID()
                }
            event.player.info("修改已保存为草稿")
            inCreation.remove(event.player)
        }
    }

    @EventHandler
    fun onPlayerAsyncChat(event: AsyncChatEvent) {
        val step = inRecording[event.player]
        if (step != null) {
            event.isCancelled = true
            val command = (event.message() as TextComponent).content()
            CommandExecutor.onCommand(event.player, command, step)
        }
    }
}