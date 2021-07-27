package org.parchmentmc.nitwit.discord;

import com.apollographql.apollo.ApolloClient;
import net.dv8tion.jda.api.JDA;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.nitwit.config.Configuration;
import org.parchmentmc.nitwit.util.LambdaUtil;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.reference.ValueReference;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class DiscordClient {
    /**
     * Avoid if possible.
     */
    @Nullable
    public static DiscordClient ACTIVE = null;

    private final CompletableFuture<JDA> jdaFuture;
    private JDA jda = null;
    private final CompletableFuture<ApolloClient> apolloFuture;
    private ApolloClient apollo = null;
    private final ValueReference<Configuration, CommentedConfigurationNode> configuration;
    private final CommandManager commandManager;

    public DiscordClient(CompletableFuture<JDA> jdaFuture, CompletableFuture<ApolloClient> apolloFuture,
                         ValueReference<Configuration, CommentedConfigurationNode> configuration) {
        this.jdaFuture = jdaFuture;
        this.apolloFuture = apolloFuture;
        this.configuration = configuration;
        this.commandManager = new CommandManager(this);
        jdaFuture.thenAccept(jda -> jda.addEventListener(commandManager));
        ACTIVE = this;
    }

    public JDA getDiscord() {
        if (jda == null) {
            if (!jdaFuture.isDone()) {
                throw new IllegalStateException("Cannot access JDA while loading is not finished");
            }
            jda = LambdaUtil.uncheck(jdaFuture::get);
        }

        return jda;
    }

    public ApolloClient getGraphQLClient() {
        if (apollo == null) {
            if (!apolloFuture.isDone()) {
                throw new IllegalStateException("Cannot access Apollo Client while loading is not finished");
            }
            apollo = LambdaUtil.uncheck(apolloFuture::get);
        }

        return apollo;
    }

    public Configuration config() {
        return Objects.requireNonNull(configuration.get());
    }

    public CommandManager getCommands() {
        return commandManager;
    }
}
