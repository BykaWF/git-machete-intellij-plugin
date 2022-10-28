package com.virtuslab.gitmachete.frontend.ui.providerservice;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import io.vavr.collection.List;
import lombok.val;
import org.jetbrains.plugins.github.api.GHRepositoryCoordinates;
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor;
import org.jetbrains.plugins.github.api.GithubServerPath;
import org.jetbrains.plugins.github.api.data.pullrequest.GHPullRequestShort;
import org.jetbrains.plugins.github.authentication.GithubAuthenticationManager;
import org.jetbrains.plugins.github.pullrequest.data.GHPRListLoader;
import org.jetbrains.plugins.github.pullrequest.ui.toolwindow.GHPRListSearchValue;
import org.jetbrains.plugins.github.util.GithubUrlUtil;

@SuppressWarnings({"nullness:argument", "nullness:dereference.of.nullable"})
@Service
public class GHPRLoaderProvider {
  private GHPRListLoader ghprListLoader;
  private Project project;

  public GHPRLoaderProvider(Project project) {
    this.project = project;
  }

  public void init() {
    if (ghprListLoader == null) {
      try {
        val githubAuthenticationManager = GithubAuthenticationManager.getInstance();
        val gitRepositoryProvider = project.getService(SelectedGitRepositoryProvider.class);

        val remotes = gitRepositoryProvider.getSelectedGitRepository().getRemotes();
        val url = remotes.iterator().next().getFirstUrl();
        val repositoryPath = GithubUrlUtil.getUserAndRepositoryFromRemoteUrl(url);
        GHRepositoryCoordinates repository = new GHRepositoryCoordinates(GithubServerPath.DEFAULT_SERVER, repositoryPath);

        val account = githubAuthenticationManager.getDefaultAccount(project);
        val token = githubAuthenticationManager.getTokenForAccount$intellij_vcs_github(account);
        GithubApiRequestExecutor requestExecutor = GithubApiRequestExecutor.Factory.Companion.getInstance().create(token);

        ghprListLoader = new GHPRListLoader(ProgressManager.getInstance(), requestExecutor, repository);
        ghprListLoader.setSearchQuery(GHPRListSearchValue.Companion.getDEFAULT().toQuery());
        ghprListLoader.loadMore(false);
      } catch (NullPointerException ignored) {

      }
    }
  }

  public List<GHPullRequestShort> getLoadedData() {
    return List.ofAll(ghprListLoader.getLoadedData());
  }

}
