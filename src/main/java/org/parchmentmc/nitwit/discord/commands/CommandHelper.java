package org.parchmentmc.nitwit.discord.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.Nullable;
import org.parchmentmc.nitwit.discord.DiscordClient;

import java.awt.*;

public final class CommandHelper {
    private CommandHelper() { // Prevent instantiation
    }

    public static LiteralArgumentBuilder<MessageContext> literal(String literal) {
        return LiteralArgumentBuilder.literal(literal);
    }

    public static <T> RequiredArgumentBuilder<MessageContext, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public static Message createErrorMessage(Throwable throwable) {
        return new MessageBuilder().setEmbeds(createErrorEmbed(null, throwable)).build();
    }

    public static MessageEmbed createErrorEmbed(Throwable throwable) {
        return createErrorEmbed(null, throwable);
    }

    public static MessageEmbed createErrorEmbed(@Nullable String message, Throwable throwable) {
        String description = throwable.getMessage();
        if (message != null) {
            description = message + '\n' + description;
        }

        final EmbedBuilder builder = new EmbedBuilder()
                .setTitle("Uh oh, an error!")
                .setColor(Color.RED)
                .setDescription(description)
                .setFooter("Report this to the bot admins. Thank you!");

        if (DiscordClient.ACTIVE != null) {
            builder.setColor(DiscordClient.ACTIVE.config().cosmetic.colors.danger);
        }

        return builder.build();
    }

    public static Message createProcessingMessage() {
        return new MessageBuilder().setEmbeds(createProcessingEmbed()).build();
    }

    public static MessageEmbed createProcessingEmbed() {
        final EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.CYAN)
                .setDescription("Twiddling my thumbs and crunching the numbers, hang on for a moment...");

        if (DiscordClient.ACTIVE != null) {
            embed.setColor(DiscordClient.ACTIVE.config().cosmetic.colors.info);
        }

        return embed.build();
    }

    public static Emoji getIcon(DiscordClient client, boolean truth) {
        return truth ? client.config().cosmetic.emotes.check : client.config().cosmetic.emotes.cross;
    }
}
