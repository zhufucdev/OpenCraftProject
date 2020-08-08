package citizens


import groovyjarjarantlr4.v4.runtime.misc.NotNull
import net.citizensnpcs.api.ai.tree.BehaviorGoalAdapter
import net.citizensnpcs.api.ai.tree.BehaviorStatus
import ss.ResultConstructor

class BehaviorConstructor implements ResultConstructor<BehaviorConstructor, BehaviorGoalAdapter> {
    private Closure<?> reset
    private Closure<BehaviorStatus> run
    private Closure<Boolean> shouldExecute
    private int mPriority

    int getPriority() { return mPriority }

    void rest(@NotNull Closure<?> closure) {
        reset = closure
    }

    void run(@NotNull Closure<BehaviorStatus> closure) {
        run = closure
    }

    void shouldRun(@NotNull Closure<Boolean> closure) {
        shouldExecute = closure
    }

    void priority(@NotNull int priority) {
        mPriority = priority
    }

    @Override
    BehaviorGoalAdapter construct() {
        return new BehaviorGoalAdapter() {
            @Override
            void reset() {
                if (reset != null)
                    reset.call()
            }

            @Override
            BehaviorStatus run() {
                if (run != null)
                    return run.call()
                return BehaviorStatus.RESET_AND_REMOVE
            }

            @Override
            boolean shouldExecute() {
                if (shouldExecute != null)
                    return shouldExecute.call()
                return false
            }
        }
    }
}
