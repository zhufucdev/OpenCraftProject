package citizens

import groovyjarjarantlr4.v4.runtime.misc.NotNull
import net.citizensnpcs.api.ai.tree.*
import ss.ResultConstructor

class BehaviorTreeConstructor implements ResultConstructor<BehaviorTreeConstructor, Sequence> {
    private ArrayList<Behavior> behaviors = new ArrayList<>()
    private boolean retries = false

    static Behavior behave(@NotNull @DelegatesTo(BehaviorConstructor.class) Closure closure) {
        def constructor = new BehaviorConstructor()
        constructor.with(closure)
        return constructor.construct()
    }

    void when(@NotNull Closure<Boolean> condition, @NotNull Behavior then) {
        behaviors.add(IfElse.create(condition, then, Empty.INSTANCE))
    }

    void or(@NotNull Closure<Boolean> condition, @NotNull Behavior ifAction, @NotNull Behavior elseAction) {
        behaviors.add(IfElse.create(condition, ifAction, elseAction))
    }

    void repeat(@NotNull Closure<Boolean> condition, @NotNull Behavior loop) {
        behaviors.add(Loop.createWithCondition(loop, condition))
    }

    void append(@NotNull Behavior behavior) {
        behaviors.add(behavior)
    }

    void retry(boolean retries = true) {
        this.retries = retries
    }

    @Override
    Sequence construct() {
        if (retries)
            return Sequence.createRetryingSequence(behaviors)
        else
            return Sequence.createSequence(behaviors)
    }
}
