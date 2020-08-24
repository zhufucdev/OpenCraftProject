package citizens

import groovyjarjarantlr4.v4.runtime.misc.NotNull
import net.citizensnpcs.api.ai.speech.SpeechContext
import org.bukkit.entity.Entity
import ss.ResultConstructor

class SpeechConstructor implements ResultConstructor<SpeechConstructor, SpeechContext> {
    private ArrayList<Entity> recipients = new ArrayList<>()
    private String message
    private net.citizensnpcs.api.npc.NPC talker

    void from(@NotNull net.citizensnpcs.api.npc.NPC npc) {
        talker = npc
    }

    void message(@NotNull String msg) {
        message = msg
    }

    void to(@NotNull Entity entity) {
        recipients.add(entity)
    }

    @Override
    SpeechContext construct() {
        if (message == null) throw new IllegalArgumentException("Message is not declared.")
        SpeechContext context
        if (talker == null)
            context = new SpeechContext(message)
        else
            context = new SpeechContext(talker, message)
        recipients.each { context.addRecipient(it) }
        context
        return context
    }
}
