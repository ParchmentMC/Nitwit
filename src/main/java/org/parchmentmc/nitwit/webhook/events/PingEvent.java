package org.parchmentmc.nitwit.webhook.events;

import org.jetbrains.annotations.Nullable;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.parchmentmc.nitwit.webhook.WebhookEventHandler;

import java.util.UUID;

public record PingEvent(String zen, int hook_id, /* GHHook hook, */
                        GHRepository repository,
                        @Nullable GHOrganization organization, GHUser sender) implements WebhookEvent {
    public static final String EVENT_NAME = "ping";

    public static class Handler extends WebhookEventHandler<PingEvent> {
        public Handler() {
            super(EVENT_NAME, PingEvent.class);
        }

        @Override
        public void handleEvent(UUID deliveryID, PingEvent event) {
            System.out.println("Received ping event! ID: " + deliveryID);
        }
    }
}
