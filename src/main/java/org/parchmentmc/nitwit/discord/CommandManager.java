package org.parchmentmc.nitwit.discord;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.parchmentmc.nitwit.config.Configuration;
import org.parchmentmc.nitwit.discord.commands.MessageContext;

import static net.dv8tion.jda.api.utils.MarkdownUtil.monospace;
import static org.parchmentmc.nitwit.discord.commands.CommandHelper.createErrorEmbed;

public class CommandManager extends ListenerAdapter {
    private final DiscordClient client;
    private final CommandDispatcher<MessageContext> dispatcher = new CommandDispatcher<>();

    public CommandManager(DiscordClient client) {
        this.client = client;
    }

    public DiscordClient getClient() {
        return client;
    }

    public Configuration config() {
        return client.config();
    }

    public CommandDispatcher<MessageContext> getDispatcher() {
        return dispatcher;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        // Ignore commands from DMs if settings say so
        if (event.isFromType(ChannelType.PRIVATE) && config().discord.ignoreDMs) {
            return;
        }

        // Ignore webhook messages, messages from bot users, and system messages from Discord
        if (event.isWebhookMessage() || event.getMessage().getAuthor().isSystem() || event.getMessage().getAuthor().isBot()) {
            return;
        }

        final String prefix = config().discord.commandPrefix;
        final String altPrefix = config().discord.alternativePrefix;
        final Message message = event.getMessage();
        final String content = message.getContentRaw();

        String command = null;

        // Check if it matches the main prefix
        if (content.regionMatches(false, 0, prefix, 0, prefix.length())) {
            command = content.substring(prefix.length());
        }

        // If the main prefix did not match, check if it matches the alternative prefix
        if (command != null && altPrefix != null
                && content.regionMatches(false, 0, prefix, 0, altPrefix.length())) {
            command = content.substring(altPrefix.length());
        }

        // If none of the prefixes match, return
        if (command == null) {
            return;
        }

        // If we're in commander-only mode and there is a commander
        final long commanderID = config().discord.commanderID;
        if (config().discord.commanderOnlyMode && commanderID != 0 && commanderID == event.getAuthor().getIdLong()) {
            message.reply("Hrm! I'm only listening to <@" + commanderID + ">.")
                    .queue();
            return;
        }

        final MessageContext context = new MessageContext(client, message, event.getChannel(), event.getAuthor(), event.getMember());
        final ParseResults<MessageContext> results = dispatcher.parse(command, context);

        // If the command does not fully parse, then return
        if (results.getReader().getRemainingLength() > 0) {
            return;
        }

        try {
            dispatcher.execute(results);
        } catch (CommandSyntaxException e) {
            message.replyEmbeds(createErrorEmbed("Error while executing a command! (" + monospace(command) + ")", e))
                    .queue();
            System.err.println("Error while executing command: " + command);
            e.printStackTrace();
        }
    }
}
