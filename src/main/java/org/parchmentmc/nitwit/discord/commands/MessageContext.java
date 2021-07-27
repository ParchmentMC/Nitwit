package org.parchmentmc.nitwit.discord.commands;

import net.dv8tion.jda.api.entities.*;
import org.parchmentmc.nitwit.discord.DiscordClient;

import javax.annotation.Nullable;

public class MessageContext {
    private final DiscordClient client;
    private final MessageChannel channel;
    private final User user;
    @Nullable
    private final Member member;
    private final Message message;

    public MessageContext(DiscordClient client, Message message, MessageChannel channel, User user, @Nullable Member member) {
        this.client = client;
        this.message = message;
        this.channel = channel;
        this.user = user;
        this.member = member;
    }

    public MessageContext(DiscordClient client, Message message, MessageChannel channel, Member member) {
        this(client, message, channel, member.getUser(), member);
    }

    public DiscordClient getClient() {
        return client;
    }

    public Message getMessage() {
        return message;
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public User getUser() {
        return user;
    }

    @Nullable
    public Member getMember() {
        return member;
    }

    public boolean isFromType(ChannelType type) {
        return channel.getType() == type;
    }
}
