package org.parchmentmc.nitwit.discord.commands.impl;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.github.api.fragment.PullRequestInfo;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.utils.MarkdownUtil;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.parchmentmc.nitwit.discord.DiscordClient;
import org.parchmentmc.nitwit.discord.commands.CommandHelper;
import org.parchmentmc.nitwit.discord.commands.MessageContext;
import org.parchmentmc.nitwit.discord.pagination.PaginationButtonListener;
import org.parchmentmc.nitwit.discord.pagination.PaginationHelper;
import org.parchmentmc.nitwit.discord.pagination.PaginationInfo;
import org.parchmentmc.nitwit.graphql.PullRequestsQuery;
import org.parchmentmc.nitwit.util.graphql.GraphQLHelper;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.parchmentmc.nitwit.discord.pagination.PaginationHelper.PageDirection.BACKWARDS;
import static org.parchmentmc.nitwit.discord.pagination.PaginationHelper.PageDirection.FORWARDS;
import static org.parchmentmc.nitwit.util.graphql.RxJavaHelper.npe;
import static org.parchmentmc.nitwit.util.graphql.RxJavaHelper.nullableMapS;

public class ListPullRequestsCommand {
    public static final String PULL_REQUEST_LISTING_ID = "pr_listing";
    public static final String PULL_REQUEST_LISTING_PREVIOUS = "prev";
    public static final String PULL_REQUEST_LISTING_NEXT = "next";

    public static void register(CommandDispatcher<MessageContext> dispatcher) {
        dispatcher.register(CommandHelper.literal("list")
                .executes(ListPullRequestsCommand::doList)
        );
    }

    private static int doList(CommandContext<MessageContext> ctx) {
        final MessageContext msgCtx = ctx.getSource();
        final DiscordClient client = msgCtx.getClient();
        final Message message = msgCtx.getMessage();
        final String authorID = msgCtx.getUser().getId();

        final ApolloClient apollo = client.getGraphQLClient();
        final String owner = client.config().github.organization;
        final String repo = client.config().github.repositoryName;

        query(apollo, owner, repo, List.of(client.config().github.mappingsLabel), null, null)
                .singleOrError()
                .flatMap(nullableMapS(Response<PullRequestsQuery.Data>::getData,
                        npe("Pull requests query returned no data.")))
                .flatMap(response -> createMessage(response, owner, repo, authorID, null))
                .onErrorReturn(CommandHelper::createErrorMessage)
                .blockingSubscribe(msg -> message.reply(msg).queue());

        return Command.SINGLE_SUCCESS;
    }

    static Observable<Response<PullRequestsQuery.Data>> query(ApolloClient client, String owner, String repo,
                                                              List<String> labels, @Nullable String beforeCursor,
                                                              @Nullable String afterCursor) {
        final PullRequestsQuery.Builder queryBuilder = PullRequestsQuery.builder().owner(owner).repo(repo).first(6).includeDetails(false);
        if (!labels.isEmpty()) {
            queryBuilder.labels(labels);
        }
        if (beforeCursor != null) {
            queryBuilder.before(beforeCursor);
        } else if (afterCursor != null) {
            queryBuilder.after(afterCursor);
        }

        return GraphQLHelper.queryRxJava(client, queryBuilder.build());
    }

    static Single<Message> createMessage(PullRequestsQuery.Data data, String owner, String repo,
                                         String userID, PaginationHelper.@Nullable PageDirection direction) {
        final Single<PullRequestsQuery.PullRequests> pullRequests = Single.just(data)
                .flatMap(nullableMapS(PullRequestsQuery.Data::repository,
                        npe("Could not find repository **%s/%s**.", owner, repo)))
                .map(PullRequestsQuery.Repository::pullRequests);

        final Single<List<String>> entriesSingle = pullRequests
                .flatMap(nullableMapS(PullRequestsQuery.PullRequests::edges,
                        npe("Pull requests list for **%s/%s** is null.", owner, repo)))
                .flattenStreamAsObservable(Collection::stream)
                .mapOptional(s -> Optional.ofNullable(s.node()))
                .map(PullRequestsQuery.Node::fragments)
                .map(PullRequestsQuery.Node.Fragments::pullRequestInfo)
                .sorted(Comparator.comparingInt(PullRequestInfo::number))
                .take(8)
                .map(pr -> "_PR_ #**%s** _by_ %s\n - [%s](%s)".formatted(
                        pr.number(),
                        getAuthorName(pr),
                        MarkdownUtil.monospace(pr.title()),
                        pr.url()))
                .toList();

        final Single<PaginationInfo> paginationInfoSingle = pullRequests.map(s -> new PaginationInfo(s.totalCount(),
                s.pageInfo().hasPreviousPage(), s.pageInfo().startCursor(), s.pageInfo().endCursor(),
                s.pageInfo().hasNextPage()));


        return Single.zip(paginationInfoSingle, entriesSingle,
                (paginationInfo, entries) -> PaginationHelper.createPaginationMessage(userID,
                        embed -> embed.setTitle("Pull Requests for " + owner + "/" + repo), PULL_REQUEST_LISTING_PREVIOUS,
                        PULL_REQUEST_LISTING_NEXT, entries, PULL_REQUEST_LISTING_ID, direction, paginationInfo));
    }

    private static String getAuthorName(PullRequestInfo pr) {
        final PullRequestInfo.@Nullable Author author = pr.author();
        if (author != null) {
            return "[@%s](%s)".formatted(author.login(), author.url());
        }
        return "**unknown**";
    }

    public static class ButtonListener implements PaginationButtonListener.CustomButtonListener {
        public static final String PULL_REQUEST_LISTING_ID = "pr_listing";

        private final DiscordClient client;

        public ButtonListener(DiscordClient client) {
            this.client = client;
        }

        @Override
        public void onButtonClick(ButtonClickEvent event, PaginationHelper.PaginationButtonInfo info) {
            final String userID = info.userID();
            final String[] tokens = info.tokens();

            final ApolloClient apollo = client.getGraphQLClient();
            final String owner = client.config().github.organization;
            final String repo = client.config().github.repositoryName;

            if (tokens.length >= 2) {
                String cursor = tokens[1];
                switch (tokens[0]) {
                    case PULL_REQUEST_LISTING_NEXT -> query(apollo, owner, repo,
                            List.of(client.config().github.mappingsLabel), null, cursor)
                            .singleOrError()
                            .flatMap(nullableMapS(Response<PullRequestsQuery.Data>::getData,
                                    npe("Pull requests query returned no data.")))
                            .flatMap(response -> createMessage(response, owner, repo, userID, FORWARDS))
                            .onErrorReturn(CommandHelper::createErrorMessage)
                            .blockingSubscribe(msg -> event.editMessage(msg).queue());

                    case PULL_REQUEST_LISTING_PREVIOUS -> query(apollo, owner, repo,
                            List.of(client.config().github.mappingsLabel), cursor, null)
                            .singleOrError()
                            .flatMap(nullableMapS(Response<PullRequestsQuery.Data>::getData,
                                    npe("Pull requests query returned no data.")))
                            .flatMap(response -> createMessage(response, owner, repo, userID, BACKWARDS))
                            .onErrorReturn(CommandHelper::createErrorMessage)
                            .blockingSubscribe(msg -> event.editMessage(msg).queue());
                }
            }
        }
    }
}
