const object = require('object');

/**
 * Make an object have the properties of entity. Please put the result into {@link #object.withProperties}
 * @param javaEntity {org.bukkit.entity.Entitify} Any object extended on org.bukkit.entity.Entitiy
 * @param target {Object} The object to have entity properties.
 * @function
 */
function Entitify(javaEntity, target) {
    function e() {
        function test() {
            if (typeof javaEntity === 'function')
                return javaEntity()
            else
                return javaEntity
        }

        const r = test();
        if (!object.isOfClass(r, 'org.bukkit.entity.Entity'))
            throw 'Parameter is not extended on org.bukkit.entity.Entity';
        return r
    }

    const addition = {
        uuid: {getter: () => new UUID(e().getUniqueId())},
        name: {getter: () => e().getName()},
        location: {
            getter: () => new Location(e().getLocation()),
            setter: (value) => {
                const entity = e();
                entity.teleport(require('World').locationLike(value).$wrapper, entity.getLocation().getWorld().getName())
            }
        }
    }

    for (let key in addition) {
        target[key] = addition[key]
    }
    return target;
}

module.exports = Entitify;