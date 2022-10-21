package com.virtuslab.gitmachete.frontend.actions.common;

import java.util.List;

import com.intellij.openapi.project.Project;
import lombok.val;
import org.jetbrains.plugins.github.api.GHRepositoryCoordinates;
import org.jetbrains.plugins.github.api.GithubServerPath;
import org.jetbrains.plugins.github.api.data.pullrequest.GHPullRequestShort;
import org.jetbrains.plugins.github.pullrequest.data.GHPRDataContextRepository;
import org.jetbrains.plugins.github.util.GithubUrlUtil;

import com.virtuslab.gitmachete.frontend.ui.providerservice.SelectedGitRepositoryProvider;

public class GHPRLoader {

  public static List<GHPullRequestShort> loadPRs(Project project) {
    try {
      val repository = GHPRDataContextRepository.Companion.getInstance(project);
      val gitRepository = project.getService(SelectedGitRepositoryProvider.class)
          .getSelectedGitRepository();
      assert gitRepository != null;
      val remotes = gitRepository.getRemotes();
      val remote = remotes.iterator().next();
      val url = remote.getFirstUrl();
      assert url != null;
      val repositoryPath = GithubUrlUtil.getUserAndRepositoryFromRemoteUrl(url);
      assert repositoryPath != null;
      GHRepositoryCoordinates coordinates = new GHRepositoryCoordinates(GithubServerPath.DEFAULT_SERVER, repositoryPath);
      val dataContext = repository.findContext(coordinates);
      assert dataContext != null;
      val loader = dataContext.getListLoader();
      while (loader.canLoadMore())
        loader.loadMore(false);
      return loader.getLoadedData();
    } catch (AssertionError e) {
      return List.of();
    }
  }
}
