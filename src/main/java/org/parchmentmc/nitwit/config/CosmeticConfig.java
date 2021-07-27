package org.parchmentmc.nitwit.config;

import net.dv8tion.jda.api.entities.Emoji;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.awt.*;

@ConfigSerializable
public class CosmeticConfig {

    @Setting("emotes")
    public EmoteConfig emotes = new EmoteConfig();

    @Setting("colors")
    public ColorsConfig colors = new ColorsConfig();

    CosmeticConfig() {
    }

    @ConfigSerializable
    public static class ColorsConfig {

        @Setting("success")
        public Color success = new Color(63, 185, 80);
        @Setting("neutral")
        public Color neutral = new Color(139, 148, 158);
        @Setting("danger")
        public Color danger = new Color(248, 81, 73);
        @Setting("merged")
        public Color merged = new Color(163, 113, 247);

        @Setting("info")
        public Color info = new Color(56, 137, 196);

        @Setting("approved")
        public Color approved = new Color(46, 160, 67);
        @Setting("changes")
        public Color changes = new Color(218, 54, 51);

        ColorsConfig() {
        }
    }

    @ConfigSerializable
    public static class EmoteConfig {

        @Setting("check")
        @Comment("'check' emote, used for successful PR checks")
        public Emoji check = Emoji.fromMarkdown("<:check:866105676987891763>");
        @Setting("pending")
        @Comment("'pending' emote, used for pending PR checks")
        public Emoji pending = Emoji.fromMarkdown("<:dot:866105677230505984>");
        @Setting("cross")
        @Comment("'cross' emote, used for failed PR checks")
        public Emoji cross = Emoji.fromMarkdown("<:cross:866105677143605268>");

        @Setting("pr_open")
        @Comment("Emote for open, non-draft PRs")
        public Emoji prOpen = Emoji.fromMarkdown("<:propen:866107061523447878>");
        @Setting("pr_draft")
        @Comment("Emote for open, draft PRs")
        public Emoji prDraft = Emoji.fromMarkdown("<:prdraft:866107061145829376>");
        @Setting("pr_merged")
        @Comment("Emote for closed, merged PRs")
        public Emoji prMerged = Emoji.fromMarkdown("<:prmerged:866107061354889266>");
        @Setting("pr_closed")
        @Comment("Emote for closed, unmerged PRs")
        public Emoji prClosed = Emoji.fromMarkdown("<:prclosed:866107061058011136>");

        @Setting("review_approve")
        @Comment("Emote for approving PR reviews")
        public Emoji reviewApprove = Emoji.fromMarkdown("<:reviewapprove:868800495001686028>");
        @Setting("review_neutral")
        @Comment("Emote for neutral PR reviews")
        public Emoji reviewNeutral = Emoji.fromMarkdown("<:reviewneutral:868800495370776616>");
        @Setting("review_changes")
        @Comment("Emote for change requesting PR reviews")
        public Emoji reviewChanges = Emoji.fromMarkdown("<:reviewchange:868800495014252575>");

        EmoteConfig() {
        }
    }
}
