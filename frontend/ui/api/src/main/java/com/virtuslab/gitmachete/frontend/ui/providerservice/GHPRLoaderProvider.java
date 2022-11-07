package com.virtuslab.gitmachete.frontend.ui.providerservice;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import io.vavr.collection.List;
import lombok.Getter;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.github.api.GHRepositoryCoordinates;
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor;
import org.jetbrains.plugins.github.api.data.pullrequest.GHPullRequest;
import org.jetbrains.plugins.github.authentication.GithubAuthenticationManager;
import org.jetbrains.plugins.github.pullrequest.data.GHListLoader;
import org.jetbrains.plugins.github.pullrequest.data.GHPRListLoader;
import org.jetbrains.plugins.github.pullrequest.data.service.GHPRDetailsService;
import org.jetbrains.plugins.github.pullrequest.data.service.GHPRDetailsServiceImpl;
import org.jetbrains.plugins.github.pullrequest.ui.toolwindow.GHPRListSearchValue;
import org.jetbrains.plugins.github.util.GithubUrlUtil;

@Service
public final class GHPRLoaderProvider implements Disposable {
  @Nullable
  private final GHPRListLoader ghprListLoader;
  @Nullable
  private final GHPRDetailsService ghprDetailsService;
  @Getter
  private List<GHPullRequest> ghPullRequests;

  public GHPRLoaderProvider(Project project) {
    val pair = createLoaders(project);
    this.ghprListLoader = pair.first;
    this.ghprDetailsService = pair.second;
    this.ghPullRequests = List.empty();

    if (ghprListLoader != null && ghprDetailsService != null) {
      ghprListLoader.setSearchQuery(GHPRListSearchValue.Companion.getDEFAULT().toQuery());
      ghprListLoader.loadMore(false);
      ghprListLoader.addDataListener(this, new GHListLoader.ListDataListener() {
        @Override
        public void onDataUpdated(int i) {}

        @Override
        public void onDataRemoved(@NotNull Object o) {}

        @Override
        public void onAllDataRemoved() {}

        @Override
        public void onDataAdded(int i) {
          if (ghprListLoader.canLoadMore()) {
            ghprListLoader.loadMore(false);
          } else {
            createData();
          }
        }
      });
    }
  }

  public void reload() {
    if (ghprListLoader != null) {
      ghprListLoader.loadMore(true);
    }
  }

  @Override
  public void dispose() {

  }

  private void createData() {
    if (ghprListLoader != null && ghprDetailsService != null) {
      val progressIndicator = new EmptyProgressIndicator();
      val details = ghprDetailsService;
      ghPullRequests = ghprListLoader.getLoadedData().stream()
          .map(x -> details.loadDetails(progressIndicator, x)).map(x -> {
            try {
              return Optional.ofNullable(x.get());
            } catch (InterruptedException | ExecutionException e) {
              return Optional.<GHPullRequest>empty();
            }
          }).filter(Optional::isPresent).map(Optional::get).collect(List.collector());
    }
  }

  private static Pair<GHPRListLoader, GHPRDetailsService> createLoaders(Project project) {
    val githubAuthenticationManager = GithubAuthenticationManager.getInstance();
    val gitRepositoryProvider = project.getService(SelectedGitRepositoryProvider.class);

    val account = githubAuthenticationManager.getAccounts().iterator().next();
    val token = githubAuthenticationManager.getTokenForAccount$intellij_vcs_github(account);
    if (token == null) {
      return Pair.empty();
    }
    GithubApiRequestExecutor requestExecutor = GithubApiRequestExecutor.Factory.Companion.getInstance().create(token);

    val selectedGitRepository = gitRepositoryProvider.getSelectedGitRepository();
    if (selectedGitRepository == null) {
      return Pair.empty();
    }
    val gitRemote = selectedGitRepository.getRemotes().iterator().next();
    val url = gitRemote.getFirstUrl();
    if (url == null) {
      return Pair.empty();
    }
    val repositoryPath = GithubUrlUtil.getUserAndRepositoryFromRemoteUrl(url);
    if (repositoryPath == null) {
      return Pair.empty();
    }
    val repository = new GHRepositoryCoordinates(account.getServer(), repositoryPath);

    GHPRListLoader loader = new GHPRListLoader(ProgressManager.getInstance(), requestExecutor, repository);
    GHPRDetailsService detailsService = new GHPRDetailsServiceImpl(ProgressManager.getInstance(), requestExecutor, repository);

    return Pair.pair(loader, detailsService);
  }
}
