package org.parchmentmc.nitwit.config;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.nio.file.Path;
import java.util.function.Function;

@ConfigSerializable
public class SecretsConfig {
    public static final SecretsConfig EMPTY = new SecretsConfig();

    @Setting("discord_token")
    @Comment("Discord bot token")
    @Nullable
    protected String discordToken;

    @Setting("gh_app_id")
    @Comment("The ID of the GitHub App")
    protected String ghAppId;
    @Setting("gh_private_key")
    @Comment("The location of the GH App private key")
    protected Path ghAppPrivateKey;

    SecretsConfig() {
    }

    public String discordToken() {
        final String token = get("SECRETS.DISCORD_TOKEN", discordToken, Function.identity());
        if (token == null) {
            throw new IllegalStateException("No configured discord token");
        }
        return token;
    }

    public String ghAppId() {
        final String appId = get("SECRETS.APP_ID", ghAppId, Function.identity());
        if (appId == null) {
            throw new IllegalStateException("No configured GitHub App ID");
        }
        return appId;
    }

    public Path ghAppPrivateKey() {
        final Path privateKey = get("SECRETS.PRIVATE_KEY", ghAppPrivateKey, SecretsConfig::createPath);
        if (privateKey == null) {
            throw new IllegalStateException("No configured GitHub App private key file");
        }
        return privateKey;
    }

    @Nullable
    static <T> T get(String key, @Nullable T defaultValue, Function<@Nullable String, @Nullable T> converter) {
        @Nullable T ret = converter.apply(System.getProperty(key));
        if (ret == null) {
            ret = converter.apply(System.getenv(key));
        }
        if (ret == null) {
            ret = defaultValue;
        }
        return ret;
    }

    @Nullable
    private static Path createPath(@Nullable String str) {
        if (str == null) {
            return null;
        }
        return Path.of(str);
    }
}
