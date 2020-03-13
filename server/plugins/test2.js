const bukkit = require('bukkit');
const logger = require('logger');
bukkit.listen('PlayerJoinEvent', function (e) {
    logger.info("Player %s has logged in.", e.player.internetIP);

});