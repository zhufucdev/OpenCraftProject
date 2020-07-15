package bukkit

import com.zhufu.opencraft.Scripting
import com.zhufu.opencraft.ServerReloadEvent
import com.zhufu.opencraft.Status
import com.zhufu.opencraft.events.SSLoadCompleteEvent
import com.zhufu.opencraft.events.SSReloadEvent
import groovyjarjarantlr4.v4.runtime.misc.NotNull
import groovyjarjarantlr4.v4.runtime.misc.Nullable
import opencraft.Global
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import ss.Logger

import java.nio.file.Paths

abstract class ExtendedBlock {
    void onCreate(@Nullable ConfigurationSection savedData, @Nullable Player creator) {}

    void tick() {}

    void onInteracted(@NotNull Player player, @NotNull Action action, @NotNull BlockFace face) {}

    void saveStatus(@NotNull ConfigurationSection configuration) {}

    void onBroken(Player player) {
        onDestroy()
    }

    void onDestroy() {}

    protected final dropItem(ItemStack what) {
        final Location location = this.location
        location.world.dropItemNaturally(location, what)
    }

    @NotNull
    Location getLocation() {
        def f1 = existingBlocks.find { _, b -> b == this }
        if (f1 != null) return f1.key
        f1 = blocksCreating[this]
        return f1
    }

    void setLocation(@NotNull Location l) {
        def oldLocation = location
        def oldType = oldLocation.block.type
        oldLocation.block.type = Material.AIR
        existingBlocks.remove(oldLocation)
        def existing = existingBlocks[l]
        if (existing != null) {
            existing.onDestroy()
        }
        existingBlocks[l] = this
        l.block.type = oldType
    }

    @NotNull
    Material getType() {
        return location.block.type
    }

    void setType(@NotNull Material type) {
        location.block.type = type
    }

    private static Map<Location, ExtendedBlock> existingBlocks = new HashMap<>()
    private static Map<ExtendedBlock, Location> blocksCreating = new HashMap<>()
    private static File root = Paths.get("plugins", "extendedBlocks").toFile()
    private static Listener mListener = new Listener() {}

    static void place(Location where, ExtendedBlock block, Player player) {
        def existing = existingBlocks[where]
        if (existing != null) {
            existing.onDestroy()
        }
        blocksCreating[block] = where
        block.onCreate(null, player)
        existingBlocks[where] = block
        blocksCreating.remove(block)
    }

    private static boolean initialized = false
    static void init() {
        if (initialized) return

        def loadFromDisk = {
            if (root.exists() && root.isDirectory()) {
                // Load and recover status of blocks stored on the disk
                root.listFiles().each {
                    if (!it.isFile() || it.isHidden()) return
                    def yaml = YamlConfiguration.loadConfiguration(it)
                    def name = yaml.getString("name")
                    def constructor = Content.definedBlocks.find { it.name == name }
                    if (constructor == null) {
                        throw new IllegalStateException("Extended block $name has its configuration file at ${it.path} " +
                                "but doesn't have a registered ExtendedBlockConstructor to match.")
                    }
                    def c = constructor.type.getConstructor()
                    if (c == null) {
                        throw new IllegalStateException("Extended block $name is registered but it's type doesn't have " +
                                "at least one constructor to create its instance.")
                    }
                    def instance = c.newInstance(yaml.getConfigurationSection("data"))
                    def location = yaml.getLocation("location")
                    blocksCreating[instance] = location
                    instance.onCreate(yaml.getConfigurationSection("data"), null)
                    blocksCreating.remove(instance)
                    existingBlocks[location] = instance
                }
                if (Global.isDebug())
                    Logger.info("${existingBlocks.size()} were loaded.")
            }
        }
        if (Scripting.status != Status.LOADED)
            Server.listenEvent(SSLoadCompleteEvent.class, loadFromDisk)
        else
            loadFromDisk()

        Server.eachTick {
            existingBlocks.each { _, b ->
                b.tick()
            }
        }

        Server.listenEvent(PlayerInteractEvent.class, mListener, EventPriority.NORMAL) {
            if (action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK) {
                def block = existingBlocks[clickedBlock.location]
                if (block != null) block.onInteracted(player, action, blockFace)
            }
        }
        Server.listenEvent(BlockBreakEvent.class, mListener, EventPriority.NORMAL) {
            def existing = existingBlocks[block.location]
            if (existing != null) {
                dropItems = false
                existing.onBroken(player)
                existingBlocks.remove(block.location)
            }
        }
        def save = {
            def index = 0
            existingBlocks.forEach { l, b ->
                def yaml = new YamlConfiguration()
                yaml.set("location", l)
                def data = new YamlConfiguration()
                b.saveStatus(data)
                if (data.getKeys(false).size() > 0)
                    yaml.set("data", data)
                def constructor = Content.definedBlocks.find { it.type == b.class }
                if (constructor == null) throw new IllegalStateException("Extended block of ${b.class.simpleName} is not defined.")
                yaml.set("name", constructor.name)

                def file = new File(root, "${index}.yml")
                if (!file.exists()) {
                    file.parentFile.mkdirs()
                    file.createNewFile()
                }
                yaml.save(file)

                index++
            }
            def list = root.listFiles(new FileFilter() {
                @Override
                boolean accept(File file) {
                    return !file.isHidden() && file.isFile()
                }
            })
            // Remove old files
            if (list.size() > index) {
                list.each {
                    def dot = it.name.lastIndexOf('.')
                    if (dot == -1) return
                    def name = it.name.substring(0, dot)
                    if (!name.isInteger() || name.toInteger() >= index) {
                        it.delete()
                    }
                }
            }

            if (Global.isDebug())
                Logger.info("$index were saved.")
        }
        Server.listenEvent(SSReloadEvent.class, save)
        Server.listenEvent(ServerReloadEvent.class, save)

        initialized = true
    }
}
