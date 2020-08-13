package com.zhufu.opencraft.task

import com.zhufu.opencraft.OfflineInfo
import com.zhufu.opencraft.PlayerManager
import com.zhufu.opencraft.bigger
import com.zhufu.opencraft.isUndead
import com.zhufu.opencraft.task.chunk_generator.FlatGenerator
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.generator.ChunkGenerator
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class LinearTask(override val owner: OfflineInfo, val level: Int) : ScheduledSpawnTask() {
    private val difficulty = (3 * sqrt(level.toDouble()) - 2.0 / level).roundToInt()

    override fun canStart(): Boolean = players.any { it.uniqueId == owner.uuid }

    override val worldGenerator: ChunkGenerator = FlatGenerator()
    private lateinit var world: World
    override fun onInit(world: World) {
        super.onInit(world)
        this.world = world
        world.worldBorder.apply {
            center = Location(world, 0.0, 0.0, 0.0)
            size = 16.0 * sizeChunks
        }
    }

    private val originalLocation = hashMapOf<Player, Location>()
    override fun onStart() {
        super.onStart()
        players.forEach {
            originalLocation[it] = it.location
            it.teleportAsync(world.spawnLocation)
        }
    }

    private var beaten = 0
    val target get() = difficulty.toDouble().pow(2).toInt() bigger 15

    @EventHandler
    fun onMonsterBeaten(event: EntityDeathEvent) {
        val entity = event.entity
        if (entity.world != world) return

        event.droppedExp = 0
        if (entity !is Player) {
            beaten++
            completeDegree = beaten.toFloat() / target
            if (completeDegree >= 1)
                complete()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        if (!players.contains(event.player)) return
        event.respawnLocation = Location(world, 0.0, 11.0, 0.0)
    }

    override fun onStop() {
        super.onStop()
        originalLocation.forEach { (t, u) ->
            t.teleport(u)
        }
        originalLocation.clear()
    }

    override fun quit(player: Player) {
        super.quit(player)
        originalLocation[player]?.let { player.teleport(it) }
    }

    override val typedRate: Map<EntityType, Double>
        get() = when (difficulty) {
            in 1..4 ->
                mapOf(EntityType.ZOMBIE to 0.7, EntityType.SKELETON to 0.3)
            in 4..8 ->
                mapOf(
                    EntityType.ZOMBIE to 0.3,
                    EntityType.SKELETON to 0.3,
                    EntityType.HUSK to 0.1
                )
            in 8..10 ->
                mapOf(
                    EntityType.ZOMBIE to 0.2,
                    EntityType.SKELETON to 0.3,
                    EntityType.CREEPER to 0.1,
                    EntityType.STRAY to 0.2,
                    EntityType.HUSK to 0.2
                )
            in 10..14 ->
                mapOf(
                    EntityType.ZOMBIE to 0.1,
                    EntityType.SKELETON to 0.2,
                    EntityType.CREEPER to 0.07,
                    EntityType.STRAY to 0.13,
                    EntityType.HUSK to 0.2,
                    EntityType.BLAZE to 0.15,
                    EntityType.GHAST to 0.15
                )
            else ->
                mapOf(
                    EntityType.ZOMBIE to 0.02,
                    EntityType.SKELETON to 0.1,
                    EntityType.CREEPER to 0.05,
                    EntityType.STRAY to 0.07,
                    EntityType.HUSK to 0.1,
                    EntityType.BLAZE to 0.15,
                    EntityType.GHAST to 0.15,
                    EntityType.SHULKER to 0.08,
                    EntityType.VINDICATOR to 0.15,
                    EntityType.EVOKER to 0.1
                )
        }
    override val amountPerMin: Int
        get() = difficulty
    override val chunks: List<Chunk>
        get() {
            val r = arrayListOf<Chunk>()
            val boundary = sizeChunks / 2
            for (x in -boundary..boundary) {
                for (z in -boundary..boundary) {
                    r.add(world.getChunkAt(x, z))
                }
            }
            return r
        }

    override fun spawnHeight(x: Int, z: Int): Int = 11
    override fun onSpawn(entity: Entity) {
        if (entity.type.isUndead) {
            (entity as LivingEntity).equipment?.helmet = ItemStack(Material.IRON_HELMET)
        }
        if (difficulty >= 13) {
            (entity as LivingEntity).addPotionEffect(
                PotionEffect(
                    PotionEffectType.INCREASE_DAMAGE,
                    Int.MAX_VALUE,
                    1
                )
            )
        }
    }

    override fun onSave() {
        super.onSave()
        data.set("owner", owner.uuid.toString())
        data.set("level", level)
        data.set("completeDegree", completeDegree)
    }


    var completeDegree: Float = 0F
        private set

    companion object {
        @JvmStatic
        fun deserialize(data: ConfigurationSection): LinearTask {
            return LinearTask(
                PlayerManager.findOfflineInfoByPlayer(UUID.fromString(data.getString("owner")))!!,
                data.getInt("level", 1)
            ).apply {
                completeDegree = data.getDouble("completeDegree").toFloat()
            }
        }

        private const val sizeChunks = 5
    }
}