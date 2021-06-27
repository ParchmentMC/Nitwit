package org.parchmentmc.nitwit.webhook;

import org.parchmentmc.nitwit.webhook.events.WebhookEvent;

import java.util.UUID;

public abstract class WebhookEventHandler<T extends WebhookEvent> {
    private final String eventName;
    private final Class<T> eventClass;

    public WebhookEventHandler(String eventName, Class<T> eventClass) {
        this.eventName = eventName;
        this.eventClass = eventClass;
    }

    public String getEventName() {
        return eventName;
    }

    public Class<T> getEventClass() {
        return eventClass;
    }

    public abstract void handleEvent(UUID deliveryID, T event);
}
