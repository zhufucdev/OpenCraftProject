package citizens

import net.citizensnpcs.api.ai.speech.SpeechContext
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import ss.Constructor
import groovyjarjarantlr4.v4.runtime.misc.NotNull
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.ai.Goal
import net.citizensnpcs.api.ai.tree.Behavior
import net.citizensnpcs.api.event.DespawnReason
import net.citizensnpcs.api.event.SpawnReason
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.api.trait.Trait
import org.bukkit.Location
import org.bukkit.entity.EntityType

class NPCConstructor implements Constructor<NPCConstructor> {
    private String name
    private Location spawnLocation
    private EntityType npcType
    private ArrayList<Trait> traits = new ArrayList<>()
    private HashMap<Goal, Integer> goals = new HashMap<>()
    private HashMap<Behavior, Integer> behaviors = new HashMap<>()
    private boolean mcAI = false, mProtected = true

    void name(@NotNull String name) {
        this.name = name
    }

    void spawnAt(@NotNull Location location) {
        spawnLocation = location
    }

    void type(@NotNull EntityType type) {
        npcType = type
    }

    void behave(@NotNull Trait t) {
        traits.add(t)
    }

    void minecraftAI(boolean use) {
        mcAI = use
    }

    void protect(boolean use) {
        mProtected = use
    }

    void behave(@NotNull Goal goal, int priority) {
        goals[goal] = priority
    }

    void behave(@NotNull Behavior behavior, int priority) {
        behaviors[behavior] = priority
    }

    void behave(@NotNull @DelegatesTo(BehaviorConstructor.class) Closure closure) {
        def constructor = new BehaviorConstructor()
        constructor.with(closure)
        def c = constructor.construct()
        goals[c] = constructor.priority
    }

    void behaveAll(@NotNull @DelegatesTo(BehaviorTreeConstructor.class) Closure closure) {
        def constructor = new BehaviorTreeConstructor()
        constructor.with(closure)
        goals[constructor.construct()] = 1
    }

    // Post-spawn methods
    private NPC mNPC
    @NotNull NPC getNPC() {
        if (mNPC == null) throw new IllegalAccessError("This constructor has not been applied yet.")
        return mNPC
    }

    @NotNull LivingEntity livingNPC() {
        return getNPC().entity as LivingEntity
    }

    void speak(@NotNull @DelegatesTo(SpeechConstructor.class) Closure closure) {
        def constructor = new SpeechConstructor()
        constructor.with(closure)
        getNPC().defaultSpeechController.speak(constructor.construct())
    }

    @Override
    void apply() {
        if (npcType == null) throw new IllegalArgumentException("Type not declared.")
        if (name == null) throw new IllegalArgumentException("Name not declared.")

        spawnLocation.chunk.with {
            forceLoaded = true
            load(true)
        }

        mNPC = CitizensAPI.getNamedNPCRegistry("temp").createNPC(npcType, name)
        mNPC.with {
            useMinecraftAI = mcAI
            setProtected(mProtected)
            traits.each { addTrait(it) }
            defaultGoalController.with {
                goals.forEach { g, p ->
                    addGoal(g, p)
                }
                behaviors.forEach { b, p ->
                    addBehavior(b, p)
                }
            }
            spawn(spawnLocation, SpawnReason.PLUGIN)
        }
    }


    @Override
    void unapply() {
        if (mNPC == null) return
        mNPC.despawn()
        mNPC.destroy()
    }
}
