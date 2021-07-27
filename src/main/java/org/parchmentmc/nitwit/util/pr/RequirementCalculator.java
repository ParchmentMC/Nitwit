package org.parchmentmc.nitwit.util.pr;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.github.api.fragment.DetailedPullRequestInfo;
import com.github.api.type.MergeableState;
import com.github.api.type.PullRequestReviewState;
import com.github.api.type.RepositoryPermission;
import com.github.api.type.StatusState;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observables.GroupedObservable;
import org.jetbrains.annotations.Nullable;
import org.parchmentmc.nitwit.graphql.GetCollaboratorsQuery;
import org.parchmentmc.nitwit.graphql.GetPullRequestQuery;
import org.parchmentmc.nitwit.graphql.GetTeamMembersQuery;
import org.parchmentmc.nitwit.util.graphql.GraphQLHelper;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import static org.parchmentmc.nitwit.util.graphql.RxJavaHelper.npe;
import static org.parchmentmc.nitwit.util.graphql.RxJavaHelper.nullableMapS;

public class RequirementCalculator {
    public static Single<PullRequestState> queryState(ApolloClient client, String repositoryOwner,
                                                      String repositoryName, int prNumber) {
        final Single<GetPullRequestQuery.PullRequest> pullRequestSingle = GraphQLHelper.queryRxJava(client,
                new GetPullRequestQuery(repositoryOwner, repositoryName, prNumber))
                .singleOrError()
                .flatMap(nullableMapS(Response<GetPullRequestQuery.Data>::getData,
                        npe("Pull request query returned no data.")))
                .flatMap(nullableMapS(GetPullRequestQuery.Data::repository,
                        npe("Could not find repository **%s/%s**.", repositoryOwner, repositoryName)))
                .flatMap(nullableMapS(GetPullRequestQuery.Repository::pullRequest,
                        npe("Could not find pull request #**%s**.", prNumber)));

        final Single<DetailedPullRequestInfo> detailedPRInfoSingle = pullRequestSingle
                .map(GetPullRequestQuery.PullRequest::fragments)
                .map(GetPullRequestQuery.PullRequest.Fragments::detailedPullRequestInfo);

        //

        final Single<MergeableState> mergeableStateSingle = detailedPRInfoSingle.map(DetailedPullRequestInfo::mergeable);

        final Single<StatusState> statusStateSingle = detailedPRInfoSingle
                .map(DetailedPullRequestInfo::commits)
                .flatMap(nullableMapS(DetailedPullRequestInfo.Commits::nodes,
                        npe("Commit list for PR #**%s* is null.", prNumber)))
                .flattenStreamAsObservable(Collection::stream)
                .take(1)
                .singleOrError()
                .map(DetailedPullRequestInfo.Node::commit)
                .flatMap(nullableMapS(DetailedPullRequestInfo.Commit::statusCheckRollup,
                        npe("Status check rollup for the latest commit for PR #**%s** is null", prNumber)))
                .map(DetailedPullRequestInfo.StatusCheckRollup::state);

        final Observable<GroupedObservable<Boolean, DetailedPullRequestInfo.ReviewThreadNode>> reviewThreads = detailedPRInfoSingle
                .map(DetailedPullRequestInfo::reviewThreads)
                .flatMap(nullableMapS(DetailedPullRequestInfo.ReviewThreads::reviewThreadNodes,
                        npe("Review thread list for PR #**%s* is null.", prNumber)))
                .flattenStreamAsObservable(Collection::stream)
                .groupBy(DetailedPullRequestInfo.ReviewThreadNode::isResolved);

        final Single<Long> resolvedThreads = reviewThreads
                .filter(GroupedObservable::getKey)
                .count();
        final Single<Long> unresolvedThreads = reviewThreads
                .filter(s -> !s.getKey())
                .count();

        final Single<List<Review>> latestReviewsSingle = detailedPRInfoSingle
                .flatMap(nullableMapS(DetailedPullRequestInfo::latestReviews,
                        npe("Latest reviews list for PR #**%s* is null.", prNumber)))
                .flatMap(nullableMapS(DetailedPullRequestInfo.LatestReviews::latestReviewNodes,
                        npe("Latest reviews (nodes) list for PR #**%s* is null.", prNumber)))
                .flattenStreamAsObservable(Collection::stream)
                .map(s -> new Review(s.state(), s.submittedAt(), s.author(), s.url()))
                .toList();

        return Single.zip(pullRequestSingle, mergeableStateSingle, statusStateSingle, resolvedThreads,
                unresolvedThreads, latestReviewsSingle, PullRequestState::new);
    }

    @Nullable
    public static Review getTimeBaseReview(List<Review> reviews, Predicate<String> hasWriteAccess, Predicate<String> isDesignatedReviewer) {
        final ArrayList<Review> reviewsList = new ArrayList<>(reviews);

        reviewsList.sort(Comparator.comparing(Review::submitTime));

        // If a designated reviewer has approved
        boolean approvedByDesignatedReviewer = false;
        // The amount of approving reviews
        int approvals = 0;
        // The review which the waiting period is based off of
        Review timeBaseReview = null;

        for (Review review : reviewsList) {
            final PullRequestReviewState state = review.state();
            final String login = review.userLogin();

            // We only care about approving and change requesting user-authored reviews
            if (login == null || (state != PullRequestReviewState.APPROVED
                    && state != PullRequestReviewState.CHANGES_REQUESTED)) {
                continue;
            }

            switch (state) {
                case APPROVED -> {
                    approvals++;
                    if (isDesignatedReviewer.test(login)) {
                        // Designated review has approved of it
                        approvedByDesignatedReviewer = true;
                    }
                    // If there hasn't been a base review already found, and this is the second or higher review, and
                    // it has been approved (previously or this PR) by a designated reviewer, then set this as the
                    // time base review
                    if (timeBaseReview == null && approvals >= 2 && approvedByDesignatedReviewer) {
                        timeBaseReview = review;
                    }
                }
                case CHANGES_REQUESTED -> {
                    if (hasWriteAccess.test(login) || isDesignatedReviewer.test(login)) {
                        // Collaborator or designated reviewer has blocked it
                        return null;
                    }
                }
            }
        }

        return timeBaseReview;
    }

    public static Single<List<String>> getTeamMembers(ApolloClient client, String organization, String teamSlug) {
        return GraphQLHelper.queryRxJava(client, new GetTeamMembersQuery(organization, teamSlug))
                .singleOrError()
                .flatMap(nullableMapS(Response<GetTeamMembersQuery.Data>::getData,
                        npe("Organization team members query returned no data.", organization)))
                .flatMap(nullableMapS(GetTeamMembersQuery.Data::organization,
                        npe("No organization named **%s** found.", organization)))
                .flatMap(nullableMapS(GetTeamMembersQuery.Organization::team,
                        npe("No team named **%s** found in the *%s* organization.", teamSlug, organization)))
                .map(GetTeamMembersQuery.Team::members)
                .flatMap(nullableMapS(GetTeamMembersQuery.Members::memberNodes,
                        npe("Members list for team **%s/%s** is null.", organization, teamSlug)))
                .flattenStreamAsObservable(Collection::stream)
                .map(GetTeamMembersQuery.MemberNode::login)
                .toList();
    }

    public static Single<List<String>> getCollaborators(ApolloClient client, String repositoryOwner,
                                                        String repositoryName,
                                                        @javax.annotation.Nullable RepositoryPermission minimumPermission) {
        return GraphQLHelper.queryRxJava(client, new GetCollaboratorsQuery(repositoryOwner, repositoryName))
                .singleOrError()
                .flatMap(nullableMapS(Response<GetCollaboratorsQuery.Data>::getData,
                        npe("Repository collaborators query returned no data.")))
                .flatMap(nullableMapS(GetCollaboratorsQuery.Data::repository,
                        npe("No repository at **%s/%s** found.", repositoryOwner, repositoryName)))
                .flatMap(nullableMapS(GetCollaboratorsQuery.Repository::collaborators,
                        npe("Collaborators list for **%s/%s** is null.", repositoryOwner, repositoryName)))
                .flatMap(nullableMapS(GetCollaboratorsQuery.Collaborators::collaboratorEdges,
                        npe("Collaborators list (edges) for **%s/%s** is null.", repositoryOwner, repositoryName)))
                .flattenStreamAsObservable(Collection::stream)
                .filter(s -> minimumPermission != null && containsPermission(s.permission(), minimumPermission))
                .map(GetCollaboratorsQuery.CollaboratorEdge::collaboratorNode)
                .map(GetCollaboratorsQuery.CollaboratorNode::login)
                .toList();
    }

    static boolean containsPermission(RepositoryPermission user, RepositoryPermission minimum) {
        return user != RepositoryPermission.$UNKNOWN && minimum != RepositoryPermission.$UNKNOWN &&
                user.ordinal() <= minimum.ordinal();
    }

    public record PullRequestState(GetPullRequestQuery.PullRequest pullRequest, MergeableState mergeableState,
                                   StatusState commitStatus, long resolvedThreads, long unresolvedThreads,
                                   List<Review> latestReviews) {
        public List<Review> latestReviewsWithState(PullRequestReviewState state) {
            return latestReviews().stream().filter(s -> s.state() == state).toList();
        }
    }

    public record Review(PullRequestReviewState state, OffsetDateTime submitTime, String userLogin, URI url) {
        public Review(PullRequestReviewState state, OffsetDateTime submitTime, DetailedPullRequestInfo.Author author, URI url) {
            this(state, submitTime, author instanceof DetailedPullRequestInfo.AsUser user ? user.login() : null, url);
        }
    }

    public enum ReviewState {
        UNDER_REVIEW,
        WAITING,
        READY_FOR_MERGE
    }
}
