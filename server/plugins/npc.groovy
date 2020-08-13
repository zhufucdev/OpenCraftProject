import bukkit.Server
import citizens.NPC
import com.zhufu.opencraft.Scripting
import com.zhufu.opencraft.ui.EquipUpgradeUI
import com.zhufu.opencraft.ui.TaskSelectUI
import net.citizensnpcs.api.ai.tree.BehaviorStatus
import net.citizensnpcs.api.event.NPCRightClickEvent
import opencraft.Global
import opencraft.PlayerManager
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.HumanEntity
import org.bukkit.util.Vector

def equip = NPC.define {
    name '装备'
    type EntityType.PLAYER
    spawnAt Global.arg("survivalCenter", Location.class)
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

def task = NPC.define {
    name '任务'
    type EntityType.PLAYER
    spawnAt Global.arg("survivalCenter", Location.class).clone().add(new Vector(2, 0, 0))
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

Server.listenEvent(NPCRightClickEvent.class) {
    if (getNPC() == equip.getNPC())
        (new EquipUpgradeUI(Scripting.plugin, PlayerManager.getInfo(clicker))).show(clicker)
    else if (getNPC() == task.getNPC())
        (new TaskSelectUI(PlayerManager.getInfo(clicker), Scripting.plugin).show(clicker))
}