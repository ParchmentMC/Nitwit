package org.parchmentmc.nitwit;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import org.kohsuke.github.*;
import org.kohsuke.github.extras.okhttp3.OkHttpConnector;
import org.parchmentmc.nitwit.util.AuthenticationHelper;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class RESTMain {
    static final String TOKEN = "<TOKEN>";
    static final String APP_ID = "<APP ID>";
    static final Path PRIVATE_KEY = Path.of("<PRIVATE KEY>");

    static final Cache OKHTTP_CACHE = new Cache(new File(".okcache"), 10 * 1024 * 1024);

    public static void main(String[] args) throws Exception {
        final OkHttpClient httpClient = new OkHttpClient.Builder()
                .cache(OKHTTP_CACHE)
                .build();
        final OkHttpConnector connector = new OkHttpConnector(httpClient);

        final String jwt = AuthenticationHelper.createJWT(PRIVATE_KEY, APP_ID, 600000);

        final GitHub jwtGH = new GitHubBuilder().withConnector(connector).withJwtToken(jwt).build();

        final GHAppInstallation installation = jwtGH.getApp().getInstallationByRepository("sciwhiz12", "sciwhiz12");
        final GHAppInstallationToken appInstallationToken = installation.createToken().create();

        final GitHub installationGH = new GitHubBuilder()
                .withConnector(connector)
                .withAppInstallationToken(appInstallationToken.getToken())
                .build();

        final GitHub tokenGH = new GitHubBuilder()
                .withConnector(connector)
                .withOAuthToken(TOKEN)
                .build();

        final GHOrganization parchmentMC = tokenGH.getOrganization("ParchmentMC");
        final GHTeam reviewersTeam = parchmentMC.getTeamBySlug("mapping-reviewers");
        final GHRepository repository = tokenGH.getRepository("ParchmentMC/Parchment");

        for (GHPullRequest pr : repository.queryPullRequests()
                .state(GHIssueState.OPEN)
                .sort(GHPullRequestQueryBuilder.Sort.CREATED)
                .direction(GHDirection.DESC)
                .list()
                .toArray()) {
            System.out.printf("#%s: '%s' by %s%n", pr.getNumber(), pr.getTitle(), pr.getUser().getLogin());

            Set<Long> successful = new HashSet<>();
            for (GHPullRequestReview review : pr.listReviews().toArray()) {
                final GHUser reviewer = review.getUser();
                System.out.printf("    review by %s: %s%n", reviewer.getLogin(), review.getState());
                if (reviewer.isMemberOf(reviewersTeam)) {
                    Long id = reviewer.getId();
                    if (review.getState() == GHPullRequestReviewState.APPROVED) {
                        successful.add(id);
                    } else if (review.getState() != GHPullRequestReviewState.COMMENTED) {
                        successful.remove(id);
                    }
                }
            }

            System.out.printf("    approvals from reviewers team: %s%n", successful.size());
        }
    }
}
