package com.czagrzebski.printhelm.web.ai;

import com.czagrzebski.printhelm.model.ApiProposedAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Per-chat-request state shared between the chat service and the tool instances.
 * Spring AI executes tools on a different thread than the request thread when
 * streaming, so this must be an explicit shared object rather than a ThreadLocal.
 */
public class ChatRequestContext {

    private final List<ApiProposedAction> actions = Collections.synchronizedList(new ArrayList<>());
    private final Consumer<String> statusListener;

    public ChatRequestContext() {
        this(status -> { });
    }

    public ChatRequestContext(Consumer<String> statusListener) {
        this.statusListener = statusListener;
    }

    public void addAction(ApiProposedAction action) {
        actions.add(action);
    }

    public List<ApiProposedAction> getActions() {
        synchronized (actions) {
            return new ArrayList<>(actions);
        }
    }

    /** Report a short human-readable label of what the assistant is currently doing. */
    public void status(String label) {
        try {
            statusListener.accept(label);
        } catch (Exception ignored) {
            // status updates are best-effort; never fail the tool call over them
        }
    }
}
