import bukkit.Server
import citizens.NPC
import com.zhufu.opencraft.Info
import com.zhufu.opencraft.ItemPrisePair
import com.zhufu.opencraft.Language
import com.zhufu.opencraft.Scripting
import com.zhufu.opencraft.TextUtil
import com.zhufu.opencraft.ui.EquipUpgradeUI
import com.zhufu.opencraft.ui.GoodSelectionUI
import com.zhufu.opencraft.ui.ServerTradeUI
import com.zhufu.opencraft.ui.TaskSelectUI
import net.citizensnpcs.api.ai.tree.BehaviorStatus
import net.citizensnpcs.api.event.NPCRightClickEvent
import opencraft.Global
import opencraft.Lang
import opencraft.PlayerManager
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.HumanEntity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Vector

NPC.define {
    name '装备'
    type EntityType.PLAYER
    spawnAt Global.arg("survivalCenter", Location.class)
    rightClick {
        (new EquipUpgradeUI(Scripting.plugin, PlayerManager.getInfo(clicker))).show(clicker)
    }
    behave {
        Entity lastSpoken
        priority 1
        run {
            def nearbyPlayer = livingNPC().location.getNearbyEntitiesByType(HumanEntity.class, 4).find { it != livingNPC() && !it.hasMetadata("NPC") }
            if (nearbyPlayer == null) return BehaviorStatus.FAILURE

            getNPC().faceLocation(nearbyPlayer.location)
            if (lastSpoken != nearbyPlayer) {
                speak {
                    message 'LOL'
                    to nearbyPlayer
                }
                lastSpoken = nearbyPlayer
            }
            return BehaviorStatus.SUCCESS
        }

        shouldRun { true }
    }
}

NPC.define {
    name '任务'
    type EntityType.PLAYER
    spawnAt Global.arg("survivalCenter", Location.class).clone().add(new Vector(2, 0, 0))
    rightClick {
        (new TaskSelectUI(PlayerManager.getInfo(clicker), Scripting.plugin).show(clicker))
    }
    behave {
        Entity lastSpoken
        priority 1
        run {
            def nearbyPlayer = livingNPC().location.getNearbyEntitiesByType(HumanEntity.class, 4).find { it != livingNPC() && !it.hasMetadata("NPC") }
            if (nearbyPlayer == null) return BehaviorStatus.FAILURE

            getNPC().faceLocation(nearbyPlayer.location)
            if (lastSpoken != nearbyPlayer) {
                speak {
                    message '欢迎回来，我的朋友'
                    to nearbyPlayer
                }
                lastSpoken = nearbyPlayer
            }
            return BehaviorStatus.SUCCESS
        }

        shouldRun { true }
    }
}

static GoodSelectionUI goodSelectionUI(Info owner, Plugin plugin) {
    def goods = [
            new ItemPrisePair(new ItemStack(Material.BIRCH_PLANKS), null),
            new ItemPrisePair(
                    new ItemStack(Material.POTION).tap {
                        def meta = it.itemMeta as PotionMeta
                        meta.addCustomEffect(new PotionEffect(PotionEffectType.HEAL, 10, 2), false)
                        it.itemMeta = meta
                    },
                    null
            )
    ]
    return new GoodSelectionUI(goods, new Language.LangGetter(owner), plugin)
};

NPC.define {
    name '服务器商人'
    type EntityType.PLAYER
    spawnAt Global.arg("survivalCenter", Location.class).clone().add(new Vector(4, 0, 0))
    rightClick {
        def info = PlayerManager.getInfo(clicker)
        if (info == null) {
            clicker.sendMessage(TextUtil.INSTANCE.error(Lang.getDefault("player.error.unknown")))
            return
        }
        (new ServerTradeUI(info, goodSelectionUI(info, Scripting.plugin), Scripting.plugin)).show(clicker)
    }
}