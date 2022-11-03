package com.virtuslab.gitmachete.frontend.ui.providerservice;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.plugins.github.api.GHRepositoryCoordinates;
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor;
import org.jetbrains.plugins.github.api.data.pullrequest.GHPullRequestShort;
import org.jetbrains.plugins.github.authentication.GithubAuthenticationManager;
import org.jetbrains.plugins.github.pullrequest.data.GHPRListLoader;
import org.jetbrains.plugins.github.pullrequest.ui.toolwindow.GHPRListSearchValue;
import org.jetbrains.plugins.github.util.GithubUrlUtil;

@Service
public final class GHPRLoaderProvider {
  @Nullable
  private final GHPRListLoader ghprListLoader;

  public GHPRLoaderProvider(Project project) {
    this.ghprListLoader = createLoader(project);
  }

  private static @Nullable GHPRListLoader createLoader(Project project) {
    val githubAuthenticationManager = GithubAuthenticationManager.getInstance();
    val gitRepositoryProvider = project.getService(SelectedGitRepositoryProvider.class);

    val account = githubAuthenticationManager.getAccounts().iterator().next();
    val token = githubAuthenticationManager.getTokenForAccount$intellij_vcs_github(account);
    if (token == null) {
      return null;
    }
    GithubApiRequestExecutor requestExecutor = GithubApiRequestExecutor.Factory.Companion.getInstance().create(token);

    GitRepository selectedGitRepository = gitRepositoryProvider.getSelectedGitRepository();
    if (selectedGitRepository == null) {
      return null;
    }
    val gitRemote = selectedGitRepository.getRemotes().iterator().next();
    val url = gitRemote.getFirstUrl();
    if (url == null) {
      return null;
    }
    val repositoryPath = GithubUrlUtil.getUserAndRepositoryFromRemoteUrl(url);
    if (repositoryPath == null) {
      return null;
    }
    GHRepositoryCoordinates repository = new GHRepositoryCoordinates(account.getServer(), repositoryPath);

    GHPRListLoader loader = new GHPRListLoader(ProgressManager.getInstance(), requestExecutor, repository);
    loader.setSearchQuery(GHPRListSearchValue.Companion.getDEFAULT().toQuery());
    loader.loadMore(false);
    return loader;
  }

  public List<GHPullRequestShort> getLoadedData() {
    if (ghprListLoader != null) {
      return List.ofAll(ghprListLoader.getLoadedData());
    } else {
      return List.empty();
    }
  }

}
