package zhufucdev.gun

import bukkit.ExtendedBlock
import bukkit.Server
import com.zhufu.opencraft.ExtendsKt
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector

class Bullet {
    protected Float mAcceleration
    protected Vector calcA

    Vector getAcceleration() {
        if (calcA != null) return calcA
        return (ExtendsKt.unitVector(initialVelocity) * mAcceleration).add(gravity).tap { calcA = it }
    }

    protected Vector initialV, velocity

    Vector getInitialVelocity() {
        return initialV.clone()
    }

    Vector getVelocity() {
        // v = v0 + at
        return initialV.clone().add(((ExtendsKt.unitVector(initialVelocity) * mAcceleration).add(gravity)) * t)
    }
    protected Location mL0, mLocation

    Location getInitialLocation() {
        return mL0
    }

    Location getLocation() {
        return mLocation
    }

    private Player mShooter

    Player getShooter() {
        return mShooter
    }
    private Double mMass

    double getMass() {
        return mMass
    }

    private World mWorld

    World getWorld() {
        return mWorld
    }

    /**
     * Constructs a bullet shot.
     * @param acceleration The horizontal acceleration(block per square tick) of this bullet. Often negative.
     * @param initialVelocity The initial velocity of this bullet.
     * @param initialLocation The location where this bullet appears.
     * @param shooter Shooter of this bullet.
     */
    Bullet(Float acceleration, Vector initialVelocity, double mass, Location initialLocation, Player shooter) {
        this.mAcceleration = acceleration
        this.initialV = initialVelocity
        this.mMass = mass
        this.mShooter = shooter
        this.mL0 = initialLocation
        this.mWorld = initialLocation.world
    }

    protected BukkitTask shootingTask
    protected long t = 0

    long getShootTime() {
        return t
    }

    void shoot() {
        if (shootingTask != null) {
            shootingTask.cancel()
        }

        playShootSound()

        Closure<Location> damageable = {
            double T = 0
            double delta = 1, max = -1, TMax = 0
            while (locateAt(T).block.type == Material.AIR) {
                def T2 = T + delta, d = locateAt(T).distance(locateAt(T2))
                if (max < 0 || d > max) {
                    TMax = T
                    max = d
                }
                T = T2
            }
            if (max == -1) {
                return locateAt(T)
            }
            while (locateAt(TMax).distance(locateAt(TMax + delta)) > 1) {
                delta *= 0.618
            }
            T = 0
            Location r
            while ((r = locateAt(T)).block.type == Material.AIR) {
                T += delta
            }
            return r
        }

        t = 0
        shootingTask = Server.eachTick {
            t++
            mLocation = locateAt(t)
            spawnParticles()

            if (!shootingTask.cancelled) {
                // If the bullet hit ground
                if (location.block.type != Material.AIR) {
                    shootingTask.cancel()
                    def blockShot = damageable()
                    if (ExtendedBlock.getAt(blockShot) == null) {
                        ExtendedBlock.place(blockShot, new DamageableBlock(), shooter)
                    }
                    def block = ExtendedBlock.getAt(blockShot)
                    if (block instanceof DamageableBlock) {
                        // Ek = 1/2mv^2
                        block.damage(0.5 * mass * (velocity.length() * 20)**2)
                    }
                }
            }
        }
    }

    protected void spawnParticles() {
        mWorld.spawnParticle(Particle.NOTE, location, 1)
    }

    protected void playShootSound() {
        mWorld.playSound(mL0, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0f)
    }

    @Override
    String toString() {
        return "Bullet{acceleration = $mAcceleration, v0 = $initialVelocity, shooter = ${shooter.name}}"
    }

    static Vector getGravity() {
        return new Vector(0.0, -0.49, 0.0)
    }

    protected Location locateAt(double t) {
        // x = v0t + 1/2at^2
        def l = mL0.clone().add((initialVelocity * t).add(acceleration.clone() * (t**2 / 2)))
        if (!l.chunkLoaded) {
            l.chunk.load(true)
        }
        return l
    }
}
