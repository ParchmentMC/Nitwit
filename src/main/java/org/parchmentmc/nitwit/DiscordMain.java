package org.parchmentmc.nitwit;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy;
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory;
import com.github.api.type.CustomType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import okhttp3.Cache;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.time.StopWatch;
import org.kohsuke.github.GHAppInstallation;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.parchmentmc.nitwit.config.*;
import org.parchmentmc.nitwit.config.serializers.ColorSerializer;
import org.parchmentmc.nitwit.config.serializers.DurationSerializer;
import org.parchmentmc.nitwit.config.serializers.EmojiSerializer;
import org.parchmentmc.nitwit.discord.DiscordClient;
import org.parchmentmc.nitwit.discord.commands.impl.ListPullRequestsCommand;
import org.parchmentmc.nitwit.discord.commands.impl.QueryPullRequestCommand;
import org.parchmentmc.nitwit.discord.listeners.StateListener;
import org.parchmentmc.nitwit.discord.pagination.PaginationButtonListener;
import org.parchmentmc.nitwit.util.AuthenticationHelper;
import org.parchmentmc.nitwit.util.AuthenticationInterceptor;
import org.parchmentmc.nitwit.util.graphql.DateTimeAdapter;
import org.parchmentmc.nitwit.util.graphql.URIAdapter;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.reference.ConfigurationReference;
import org.spongepowered.configurate.reference.ValueReference;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.awt.*;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.parchmentmc.nitwit.util.LambdaUtil.rethrowConsumer;
import static org.parchmentmc.nitwit.util.LambdaUtil.rethrowFunction;

public class DiscordMain {
    static final TypeSerializerCollection ADDED_SERIALIZERS = TypeSerializerCollection.defaults()
            .childBuilder()
            .register(Duration.class, new DurationSerializer(false))
            .register(Emoji.class, new EmojiSerializer())
            .register(Color.class, new ColorSerializer())
            .build();

    public static void main(String[] args) throws Exception {
        final StopWatch loading = StopWatch.createStarted();

        final ValueReference<Configuration, CommentedConfigurationNode> configReference;
        final Configuration config;
        { // Normal configuration
            final HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                    .emitComments(true)
                    .prettyPrinting(true)
                    .defaultOptions(ConfigurationOptions.defaults().serializers(ADDED_SERIALIZERS))
                    .path(Path.of("nitwit.conf"))
                    .build();
            final ConfigurationReference<CommentedConfigurationNode> configRef = loader.loadToReference();
            configReference = configRef.referenceTo(Configuration.class);
            config = configReference.get();
        }

        final SecretsConfig secrets;
        { // Secrets
            final HoconConfigurationLoader loader = HoconConfigurationLoader.builder()
                    .emitComments(true)
                    .prettyPrinting(true)
                    .path(Path.of("secrets.conf"))
                    .build();
            final CommentedConfigurationNode node = loader.load();
            secrets = node.get(SecretsConfig.class);
        }

        if (secrets == null) {
            throw new IllegalStateException("Failed to load secrets configuration");
        }
        if (config == null) {
            throw new IllegalStateException("Failed to load configuration");
        }

        MessageAction.setDefaultMentions(Collections.emptySet());
        MessageAction.setDefaultMentionRepliedUser(false);
        MessageAction.setDefaultFailOnInvalidReply(false);

        final CompletableFuture<OkHttpClient> okHttpClient = CompletableFuture.supplyAsync(() -> {
            final Dispatcher dispatcher = new Dispatcher();
            dispatcher.setMaxRequestsPerHost(25);

            final Path okHttpCacheDirectory = config.cache.okhttpDirectory;
            final Cache okHttpCache = okHttpCacheDirectory != null
                    ? new Cache(okHttpCacheDirectory.toFile(),
                    config.cache.okhttpSize * 1024L)
                    : null;

            return new OkHttpClient.Builder()
                    .cache(okHttpCache)
                    .connectionPool(new ConnectionPool(5, 30, TimeUnit.SECONDS))
                    .dispatcher(dispatcher)
                    .build();
        });

        final CompletableFuture<ApolloClient> apolloClient = okHttpClient.thenApplyAsync(rethrowFunction(client -> {
            final Path privateKey = secrets.ghAppPrivateKey();
            final String appId = secrets.ghAppId();
            final String jwt = AuthenticationHelper.createJWT(privateKey, appId, 600000);

            final GitHub jwtGH = new GitHubBuilder().withJwtToken(jwt).build();

            final GHAppInstallation installation = jwtGH.getApp().getInstallationByOrganization(config.github.organization);
            final GHAppInstallationToken installationToken = installation.createToken().create();

            return ApolloClient.builder()
                    .serverUrl("https://api.github.com/graphql")
                    .okHttpClient(client.newBuilder()
                            .addInterceptor(AuthenticationInterceptor.bearer(installationToken.getToken()))
                            .build())
                    .normalizedCache(new LruNormalizedCacheFactory(EvictionPolicy.builder()
                            .maxSizeBytes(config.cache.apolloSize)
                            .build()))
                    .addCustomTypeAdapter(CustomType.DATETIME, new DateTimeAdapter(ZoneOffset.UTC))
                    .addCustomTypeAdapter(CustomType.URI, new URIAdapter())
                    .build();
        }));

        final CompletableFuture<JDA> jdaDiscord = okHttpClient.thenApplyAsync(rethrowFunction(client ->
                JDABuilder.createDefault(secrets.discordToken())
                        .setHttpClient(client)
                        .setStatus(OnlineStatus.DO_NOT_DISTURB)
                        .addEventListeners(new StateListener())
                        .setActivity(Activity.playing("the ready game"))
                        .build()));

        final DiscordClient client = new DiscordClient(jdaDiscord, apolloClient, configReference);
        ListPullRequestsCommand.register(client.getCommands().getDispatcher());
        QueryPullRequestCommand.register(client.getCommands().getDispatcher());

        jdaDiscord.thenAcceptAsync(jda -> jda.addEventListener(new PaginationButtonListener(config.discord.buttonTimeLimit)
                .add(ListPullRequestsCommand.ButtonListener.PULL_REQUEST_LISTING_ID,
                        new ListPullRequestsCommand.ButtonListener(client))));

        loading.split();
        System.out.println("Client initialization took " + loading.toSplitString());
        CompletableFuture.allOf(jdaDiscord.thenAccept(rethrowConsumer(s -> s.awaitStatus(JDA.Status.CONNECTED))), apolloClient).join();
        loading.stop();
        System.out.println("Full loading took " + loading);
    }
}
