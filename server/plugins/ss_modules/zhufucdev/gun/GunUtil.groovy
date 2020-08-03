package zhufucdev.gun

class GunUtil {
    static float getAirAcceleration(float radius, double mass) {
        return (0.1 * Math.PI * radius.toDouble()**2 / mass).toFloat()
    }
}
