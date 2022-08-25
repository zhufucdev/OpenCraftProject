import com.zhufu.opencraft.data.Database
import org.bson.Document
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.configuration.file.YamlConfiguration
import ss.Logger

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import static com.mongodb.client.model.Filters.eq
import static com.mongodb.client.model.Filters.eq

static void importTags() {
    for (def tag : (new File("plugins", "tag")).listFiles()) {
        if (tag.isHidden()) return
        def uuid = UUID.fromString(tag.name.substring(0, tag.name.length() - 4))
        def playerName = Bukkit.getOfflinePlayer(uuid).name
        Logger.info("Importing tag for $playerName.")
        try {
            def yml = YamlConfiguration.loadConfiguration(tag)
            def doc = Database.INSTANCE.tag(uuid, true)
            doc.append("lang", yml.getString("lang"))
                    .append("currency", yml.getLong("currency"))
                    .append("territoryID", yml.getInt("territoryID"))
                    .append("survivalSpawn", new Document(yml.getLocation("surviveSpawn").serialize()))
                    .append("isSurvivor", true)
                    .append("isSurveyPassed", yml.getBoolean("isSurveyPassed", false))
                    .append("isTTShown", yml.getBoolean("isTTShown", false))
                    .append("gameTime", yml.getLong("gameTime"))
                    .append("damage", yml.getDouble("damage"))
                    .append("password",
                            MessageDigest.getInstance("SHA-256")
                                    .digest(
                                            yml.getString("password")
                                                    .getBytes(StandardCharsets.UTF_8)
                                    )
                    )
            Database.INSTANCE.tag(uuid, doc)
        } catch (Exception e) {
            e.printStackTrace()
        }
    }
}

static void importInventories() {
    for (def inventory : (new File("plugins", "inventories")).listFiles()) {
        if (inventory.isHidden()) return
        def uuid = UUID.fromString(inventory.name)
        def yml = YamlConfiguration.loadConfiguration(new File(inventory, "survivor.yml"))
        def playerName = Bukkit.getOfflinePlayer(uuid).name
        Logger.info("Importing inventory for $playerName.")
        try {
            def invDoc = new Document()
            for (def slot : yml.getConfigurationSection("inventory").getKeys(false)) {
                invDoc.append(slot, yml.getItemStack("inventory.$slot").serializeAsBytes())
            }
            def doc = new Document()
                    .append("_id", "survivor")
                    .append("exp", yml.getInt("exp"))
                    .append("flySpeed", yml.getDouble("flySpeed"))
                    .append("foodLevel", yml.getInt("foodLevel"))
                    .append("gameMode", GameMode.valueOf(yml.getString("gameMode")))
                    .append("health", yml.getDouble("health"))
                    .append("inventory", invDoc)
                    .append("location", new Document(yml.getLocation("location").serialize()))
                    .append("potion", [])
                    .append("walkSpeed", yml.getDouble("walkSpeed"))

            def collection = Database.INSTANCE.inventory(uuid)
            if (collection.find(eq("survivor")).first() == null) {
                collection.insertOne(doc)
            } else {
                collection.replaceOne(eq("survivor"), doc)
            }
        } catch (Exception e) {
            e.printStackTrace()
        }
    }
}

static void importFriendships() {
    for (def friendship : (new File("plugins", "friendData")).listFiles()) {
        if (friendship.isHidden()) return
        def yml = YamlConfiguration.loadConfiguration(friendship)
        def uuid = UUID.fromString(yml.getString("id"))
        Logger.info("Importing friendship $uuid.")
        try {
            def doc = Database.INSTANCE.friendship(uuid)

            def extraDoc = new Document()
            def extra = yml.getConfigurationSection("extra")
            if (extra != null) {
                if (extra.contains("sharedInventory")) {
                    def inv = extra.getConfigurationSection("sharedInventory")
                    def invDoc = new Document()
                    for (def slot : inv.getKeys(false)) {
                        invDoc.append(slot, inv.getItemStack(slot).serializeAsBytes())
                    }
                    extraDoc.append("sharedInventory", invDoc)
                }
                if (extra.contains("sharedCheckpoints")) {
                    def ckp = extra.getConfigurationSection("sharedCheckpoints")
                    def ckpDoc = new Document()
                    for (def name : ckp.getKeys(false)) {
                        ckpDoc.append(name, new Document(ckp.getLocation(name).serialize()))
                    }
                    extraDoc.append("sharedCheckpoints", ckpDoc)
                }
                extraDoc.append("transfer", extra.getInt("transfer", 0))
                        .append("shareLocation", extra.getBoolean("shareLocation", false))
            }

            doc.append("a", UUID.fromString(yml.getString("a")))
                    .append("b", UUID.fromString(yml.getString("b")))
                    .append("startAt", yml.getLong("startAt"))
                    .append("extra", extraDoc)
            Database.INSTANCE.friendship(uuid, doc)
        } catch (Exception e) {
            e.printStackTrace()
        }
    }
}

static void importCheckpoints() {
    for (def tag : (new File("plugins", "tag")).listFiles()) {
        if (tag.isHidden()) return
        def uuid = UUID.fromString(tag.name.substring(0, tag.name.length() - 4))
        def playerName = Bukkit.getOfflinePlayer(uuid).name
        Logger.info("Importing checkpoints for $playerName.")
        try {
            def yml = YamlConfiguration.loadConfiguration(tag).getConfigurationSection("checkpoints")
            def collection = Database.INSTANCE.checkpoint(uuid)
            for (def name : yml.getKeys(false)) {
                def doc = new Document(yml.getLocation(name).serialize())
                        .append("name", name)
                collection.insertOne(doc)
            }
        } catch (Exception e) {
            e.printStackTrace()
        }
    }
}

importTags()
importFriendships()
importCheckpoints()