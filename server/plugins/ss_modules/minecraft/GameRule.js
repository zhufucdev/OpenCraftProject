const object = require('object');
const javaPackage = 'org.bukkit.GameRule';
const Rule = object.fromJava(javaPackage);

function GameRule(name, world) {
    let wrapper;

    if (object.isOfClass(name, javaPackage)) {
        wrapper = name;
    } else if (typeof name === 'string') {
        wrapper = Rule.getByName(name)
    } else {
        throw "Parameter name must be a string."
    }
    if (!wrapper) throw 'No such game rule: ' + name;
    this.$wrapper = wrapper;

    const config = {
        name: {getter: () => wrapper.getName()},
        type: {getter: () => wrapper.getType().getSimpleName() === 'Integer' ? 'number' : 'boolean'}
    };
    if (world && world.MCType === "World") {
        config.value = {
            getter: () => world.$wrapper.getWorld().getGameRuleValue(wrapper),
            setter: (value) => wrapper.$wrapper.getWorld().setGameRule(wrapper, value)
        }
    }
    this.prototype = object.withProperties(config)
}

['announceAdvancements', 'commandBlockOutput', 'disableElytraMovementCheck', 'doDaylightCycle', 'doEntityDrops',
    'doFireTick', 'doLimitedCrafting', 'doMobLoot', 'doMobSpawning', 'doTileDrops', 'doWeatherCycle',
    'keepInventory', 'logAdminCommands', 'mobGriefing', 'naturalRegeneration', 'reducedDebugInfo', 'sendCommandFeedback',
    'showDeathMessages', 'spectatorsGenerateChunks', 'disableRaids', 'doInsomnia', 'doImmediateRespawn',
    'drowningDamage', 'fallDamage', 'fireDamage', 'doPatrolSpawning', 'doTraderSpawning', 'randomTickSpeed',
    'spawnRadius', 'maxEntityCramming', 'maxCommandChainLength'
].forEach((value => GameRule[value] = new GameRule(value)));

module.exports = GameRule;