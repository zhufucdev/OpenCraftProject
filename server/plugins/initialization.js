const fs = require('filesystem');
const object = require('object');
const logger = require('logger');
const bukkit = require('bukkit');
const minecraft = require('minecraft');
const thread = require('thread');

bukkit.listen("ServerListPingEvent", function (e) {
    e.serverIcon = fs.of('logo.png');
    e.motd = minecraft.text.blue + "OpenCraft Test Server";
}, bukkit.Event.priority.highest);