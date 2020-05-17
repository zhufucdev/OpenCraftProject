'use javaPlugin';
const object = require('object');
const bukkit = object.fromJava('org.bukkit.Bukkit');
const HandlerList = object.fromJava('org.bukkit.event.HandlerList');
const logger = require('logger');
const pluginManager = bukkit.getPluginManager();
const bukkitEvent = require('bukkitEvent');
const createEventExecutor = object.fromJava('$opencraft.lang.Extension').createEventExecutor;

function searchForClass(name) {
    function searchRoot(root) {
        const list = object.javaPackages(root);
        for (let i in list) {
            if (list[i] === root) continue;
            const selection = list[i] + "." + name;
            try {
                if (object.fromJava(selection)) {
                    return selection
                }
            } catch (ignored) {
            }
            const research = searchRoot(list[i]);
            if (research) return research
        }
        return null
    }

    if (!name.includes('.')) {
        let search = searchRoot('org.bukkit.event');
        if (search) return search;
        search = searchRoot('com.zhufu.opencraft.events');
        return search;
    } else {
        name = name.replace('$opencraft', 'com.zhufu.opencraft.events')
        try {
            object.fromJava(name);
            return name
        } catch (ignored) {
            return null
        }
    }
}

let listeners = [];
const bukkitEventPackage = 'org.bukkit.event';
function suggest(element) {
    if (object.isOfClass(element, bukkitEventPackage + '.player.PlayerLoginEvent')) {
        logger.warn('Listening %s.player.PlayerLoginEvent is deprecated because it is called ' +
            'earlier than the OpenCraft Player Info is generated. ' +
            'Use PlayerJoinEvent instead.', bukkitEventPackage)//TODO: Improve.
    }
}

function Listener(wrap) {
    listeners.push(this);
    const events = [];
    /**
     * @return {string[]} name of events that this listener is currently listening.
     */
    this.events = () => Array.from(events);
    /**
     * Make the listener listen a specific event.
     * One listener can listen the same event for many times.
     * @param event {string} name of event to listen.
     * @example 'PlayerLoginEvent' for org.bukkit.event.player.PlayerLoginEvent,
     *  or '$opencraft.PlayerLoginEvent' for com.zhufu.opencraft.events.PlayerLoginEvent
     * @param onTriggered {function(Event)} Called when the event is called by server.
     * @param priority {bukkitEvent.priority} The priority of this listening.
     * @default bukkitEvent.priority.normal
     */
    this.listen = (event, onTriggered, priority) => {
        const e = object.fromJava(searchForClass(event));
        if (!priority) priority = bukkitEvent.priority.normal;
        if (e) {
            logger.info("Listening event " + e.javaClass.getName());
            suggest(e);
            pluginManager.registerEvent(
                e.javaClass,
                wrap,
                priority,
                createEventExecutor(
                    (l, e) => {
                        onTriggered(new bukkitEvent.Event(e))
                    }
                ),
                javaPlugin
            );
            events.push(event)
        } else throw "No such event: " + event;
    };
    /**
     * Cancel all the event listening of this listener.
     * @return {*}
     */
    this.unregister = () => {
        HandlerList.unregisterAll(wrap);
    }
}

const server = require('Server');
const newListener = object.fromJava('$opencraft.lang.Extension').newListener;
module.exports = object.withProperties({
    /**
     * Listen all events of given name.
     * @param event {string} name of event to listen.
     * @param trigger {function(Event)} called when the specific event is listened.
     * @param priority {bukkitEvent.priority} The priority of this listening.
     * @return {Listener} Listener listening this event which can be unregistered to cancel listening or listen other events.
     */
    listen: function (event, trigger, priority) {
        const n = newListener();
        const listener = new Listener(n);
        listener.listen(event, trigger, priority);
        return listener;
    },
    /**
     * @return {string} String representing the version of Bukkit server.
     */
    version: {
        getter: () => bukkit.getVersion()
    },
    /**
     * @return {OpenCraftServer} Object representing the server.
     * @see OpenCraftServer
     */
    server: {
        getter: () => server
    },
    Event: {
        getter: () => require('Event')
    }
});

module.onDisable = function () {
    logger.info('Unregister all listeners.');
    for (let i in listeners) {
        listeners[i].unregister();
    }
};