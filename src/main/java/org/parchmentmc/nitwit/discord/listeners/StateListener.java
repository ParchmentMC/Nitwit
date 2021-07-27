package org.parchmentmc.nitwit.discord.listeners;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.Presence;
import org.jetbrains.annotations.NotNull;

public class StateListener extends ListenerAdapter {
    @Override
    public void onReady(@NotNull ReadyEvent event) {
        final Presence presence = event.getJDA().getPresence();
        presence.setPresence(OnlineStatus.ONLINE, Activity.watching("for PRs and commands"));
    }
}
