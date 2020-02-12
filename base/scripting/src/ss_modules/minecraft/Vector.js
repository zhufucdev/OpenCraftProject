const object = require('object');
const vector = object.fromJava('org.bukkit.util.Vector');
function Vector(x, y, z) {
    let wrapper;
    if (object.isOfClass(x, 'org.bukkit.util.Vector')) {
        wrapper = x;
    } else {
        wrapper = new vector(x, y, z);
    }
    this.$wrapper = wrapper;

    this.add = (vec) => wrapper.add(vec.$wrapper);
    this.subtract = (vec) => wrapper.subtract(vec.$wrapper);
    this.multiply = (vec) => wrapper.multiply(vec.$wrapper);
    this.divide = (vec) => wrapper.divide(vec.$wrapper);
    this.distance = (vec) => wrapper.distance(vec.$wrapper);
    this.squaredDistance = (vec) => wrapper.distanceSquared(vec.$wrapper);
    this.angle = (vec) => wrapper.angle(vec.$wrapper);
    this.midpoint = (vec) => new Vector(wrapper.getMidpoint(vec.$wrapper));
    this.crossProduct = (vec) => new Vector(wrapper.getCrossProduct(vec.$wrapper));
    this.inAABB = (min, max) => wrapper.isInAABB(min.$wrapper, max.$wrapper);
    this.inSphere = (origin, radius) => wrapper.isInSphere(origin.$wrapper, radius);
    this.rotate = (axis, angle, keepLength) => {
        if (axis.MCType === "Vector") {
            if (keepLength) {
                wrapper.rotateAroundAxis(axis.$wrapper, angle)
            } else {
                wrapper.rotateAroundNonUnitAxis(axis.$wrapper, angle)
            }
        } else {
            switch (axis) {
                case 'x': wrapper.rotateAroundX(angle);break;
                case 'y': wrapper.rotateAroundY(angle);break;
                case 'z': wrapper.rotateAroundZ(angle);break;
                default: throw "Unknown axis: " + axis + ". Use a Vector or 'x'|'y'|'z' instead."
            }
        }
    };

    this.prototype = object.withProperties({
        length: {getter: () => wrapper.length()},
        squaredLength: {getter: () => wrapper.lengthSquared()},
        x: {
            getter: () => wrapper.getX(),
            setter: (value) => wrapper.setX(value)
        },
        y: {
            getter: () => wrapper.getY(),
            setter: (value) => wrapper.setY(value)
        },
        z: {
            getter: () => wrapper.getZ(),
            setter: (value) => wrapper.setZ(value)
        }
    });

    this.MCType = "Vector";
}

module.exports = Vector;