package org.parchmentmc.nitwit.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.nio.file.Path;

@ConfigSerializable
public class Configuration {
    public static final Configuration EMPTY = new Configuration();

    @Setting("github")
    @Required
    public GitHubConfig github = new GitHubConfig();
    @Setting("discord")
    public DiscordConfig discord = new DiscordConfig();
    @Setting("cosmetic")
    public CosmeticConfig cosmetic = new CosmeticConfig();
    @Setting("cache")
    public CacheConfig cache = new CacheConfig();

    Configuration() {
    }

    @ConfigSerializable
    public static class CacheConfig {

        @Setting("okttp.dir")
        @Comment("The cache directory for OkHttp")
        public Path okhttpDirectory = null;
        @Setting("okttp.size")
        @Comment("The max size of the OkHttp cache directory, in kibibytes")
        public int okhttpSize = 10 * 1024;

        @Setting("apollo.size")
        @Comment("The max size of the Apollo GraphQL in-memory cache, in kibibytes")
        public int apolloSize = 32 * 1024;

        CacheConfig() {
        }
    }
}
