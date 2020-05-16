const object = require('object');
const manager = object.fromJava('com.zhufu.opencraft.PlayerManager');
const UUID = require('util/UUID');
const entitify = require('Entitify');
const format = require('util/Format');
const Pattern = require('MessagePattern');

function Player(id) {
    const Location = require('World').Location;

    function update() {
        let impl;
        if (object.isOfClass(id, 'org.bukkit.entity.Player')) {
            impl = manager.findInfoByPlayer(id);
        } else if (typeof id === 'string') {
            impl = manager.findInfoByName(id);
            if (impl === null) {
                impl = manager.findOfflineInfoByName(id);
            }
        } else if (id.type === "UUID") {
            impl = manager.findInfoByPlayer(id.$wrapper);
            if (impl === null) {
                impl = manager.findOfflineInfoByPlayer(id.$wrapper);
            }
        }
        if (impl === null)
            throw format('Identity %s is not mapped with any player.', id)
        return impl;
    }

    function updatePlayer() {
        const updated = update();
        if (typeof updated.getPlayer !== "function")
            throw format('Could not access player client. Perhaps it is not online.')
        return updated.getPlayer();
    }

    function checkMCType(obj, shouldBe) {
        if (obj.MCType !== shouldBe) throw format('Parameter %s is not a Minecraft %s object.', obj, shouldBe);
    }

    this.prototype = object.withProperties(entitify(() => updatePlayer(), {
        nickname: {getter: () => update().getNickname(), setter: (value) => update().setNickname(value)},
        playerListName: {
            getter: () => updatePlayer().getPlayerListName(),
            setter: (value) => updatePlayer().setPlayerListName(value)
        },
        playerListHeader: {
            getter: () => updatePlayer().getPlayerListHeader(),
            setter: (value) => updatePlayer().setPlayerListHeader(value)
        },
        playerListFooter: {
            getter: () => updatePlayer().getPlayerListFooter(),
            setter: (value) => updatePlayer().setPlayerListFooter(value)
        },
        compassTarget: {
            getter: () => new Location(updatePlayer().getCompassTarget()),
            setter: (value) => {
                checkMCType(value, 'Location');
                updatePlayer().setCompassTarget(value.$wrapper);
            }
        },
        internetIP: {getter: () => updatePlayer().getAddress().getHostString()},
        isOnline: {getter: () => updatePlayer().isOnline()},
        eyeLocation: {getter: () => new Location(updatePlayer().getEyeLocation())},
        chat: (msg) => updatePlayer().chat(msg),
        message: (message) => {
            const updated = update();
            const p = new Pattern(message, updated);
            if (p.isRaw) {
                updated.getPlayer().sendRawMessage(p.getString());
            } else {
                updated.getPlayer().sendMessage(p.getString());
            }
        },
    }));
}

module.exports = Player;
module.shareContext = false;