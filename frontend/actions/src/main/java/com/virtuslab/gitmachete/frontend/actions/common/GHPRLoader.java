package com.virtuslab.gitmachete.frontend.actions.common;

import io.vavr.collection.List;

import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.jetbrains.plugins.github.api.GHRepositoryCoordinates;
import org.jetbrains.plugins.github.api.GithubServerPath;
import org.jetbrains.plugins.github.api.data.pullrequest.GHPullRequestShort;
import org.jetbrains.plugins.github.pullrequest.data.GHPRDataContextRepository;
import org.jetbrains.plugins.github.util.GithubUrlUtil;

import com.virtuslab.gitmachete.frontend.ui.providerservice.SelectedGitRepositoryProvider;

@UtilityClass
public class GHPRLoader {

  public static List<GHPullRequestShort> loadPRs(Project project) {
    try {
      val repository = GHPRDataContextRepository.Companion.getInstance(project);
      val gitRepository = project.getService(SelectedGitRepositoryProvider.class)
          .getSelectedGitRepository();
      assert gitRepository != null : "Git repository is null";
      val remotes = gitRepository.getRemotes();
      val remote = remotes.iterator().next();
      val url = remote.getFirstUrl();
      assert url != null : "Url is null";
      val repositoryPath = GithubUrlUtil.getUserAndRepositoryFromRemoteUrl(url);
      assert repositoryPath != null : "Repository path is null";
      GHRepositoryCoordinates coordinates = new GHRepositoryCoordinates(GithubServerPath.DEFAULT_SERVER, repositoryPath);
      val dataContext = repository.findContext(coordinates);
      assert dataContext != null : "Data context is null";
      val loader = dataContext.getListLoader();
      while (loader.canLoadMore()) {
        loader.loadMore(false);
      }
      return List.ofAll(loader.getLoadedData());
    } catch (AssertionError e) {
      return List.empty();
    }
  }
}
