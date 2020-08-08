import bukkit.Server
import citizens.NPC
import com.zhufu.opencraft.Base
import com.zhufu.opencraft.Scripting
import com.zhufu.opencraft.ui.EquipUpgradeUI
import net.citizensnpcs.api.ai.tree.BehaviorStatus
import net.citizensnpcs.api.event.NPCRightClickEvent
import opencraft.Global
import opencraft.PlayerManager
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Item
def npc = NPC.define {
    name '装备'
    type EntityType.PLAYER
    spawnAt Global.arg("survivalCenter", Location.class)
    behaveAll {
        or({
            livingNPC().location.getNearbyEntitiesByType(Item.class, 5).size() == 0
        }, behave {
            Entity lastSpoken
            run {
                def nearbyPlayer = livingNPC().location.getNearbyEntitiesByType(HumanEntity.class, 4).find { it != livingNPC() }
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
        }, behave {
            run {
                def item = livingNPC().location.getNearbyEntitiesByType(Item.class, 5)
                if (item.size() <= 0) return BehaviorStatus.FAILURE
                item = item.first()
                def thrower = PlayerManager.getInfo(item.thrower)
                def stack = item.itemStack
                item.remove()

                def ui = new EquipUpgradeUI(Base.INSTANCE.pluginCore, thrower)

                BehaviorStatus.SUCCESS
            }
        })
    }
}

Server.listenEvent(NPCRightClickEvent.class) {
    if (getNPC() != npc.getNPC()) return
    (new EquipUpgradeUI(Scripting.plugin, PlayerManager.getInfo(clicker))).show(clicker)
}