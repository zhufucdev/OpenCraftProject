const object = require('object');
const Permission = object.fromJava('com.zhufu.opencraft.WorldManager').WorldPermissions;

module.exports = object.withProperties({
    public: {getter: () => Permission.PUBLIC},
    protected: {getter: () => Permission.PROTECTED},
    private: {getter: () => Permission.PRIVATE}
});