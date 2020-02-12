const fs = require('filesystem');
const object = require('object');
const logger = require('logger');
const bukkit = require('bukkit');
const event = require('bukkitEvent');
const minecraft = require('minecraft');
bukkit.listen("ServerListPingEvent", function(e) {
    e.serverIcon = fs.of('logo.png');
    e.motd = minecraft.text.blue + "OpenCraft Test Server";
    return 0;
}, event.priority.highest);

