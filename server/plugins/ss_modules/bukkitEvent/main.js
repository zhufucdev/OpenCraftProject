'use requireJava, object';
const EventPriority = requireJava('org.bukkit.event.EventPriority');
const Event = require('Event');
module.exports = {
    priority: object({
        highest: {
            getter: () => EventPriority.HIGHEST
        },
        high: {
            getter: () => EventPriority.HIGH
        },
        normal: {
            getter: () => EventPriority.NORMAL
        },
        low: {
            getter: () => EventPriority.LOW
        },
        lowest: {
            getter: () => EventPriority.LOWEST
        }
    }),
    Event: Event
};