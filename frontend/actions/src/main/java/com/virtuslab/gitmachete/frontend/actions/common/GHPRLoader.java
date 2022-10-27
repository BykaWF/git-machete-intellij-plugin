package com.virtuslab.gitmachete.frontend.actions.common;

import com.intellij.openapi.project.Project;
import io.vavr.collection.List;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.jetbrains.plugins.github.api.GHRepositoryCoordinates;
import org.jetbrains.plugins.github.api.GithubServerPath;
import org.jetbrains.plugins.github.api.data.pullrequest.GHPullRequestShort;
import org.jetbrains.plugins.github.pullrequest.data.GHPRDataContextRepository;
import org.jetbrains.plugins.github.util.GithubUrlUtil;

import com.virtuslab.gitmachete.frontend.ui.providerservice.SelectedGitRepositoryProvider;

import java.lang.reflect.Method;

@SuppressWarnings({"nullness:argument", "nullness:dereference.of.nullable"})
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
      setQuery(loader);
      while (loader.canLoadMore()) {
        loader.loadMore(false);
      }
      return List.ofAll(loader.getLoadedData());
    } catch (AssertionError e) {
      return List.empty();
    }
  }

  @SneakyThrows
  private static void setQuery(Object loader) {
        Class<?> c = Class.forName("org.jetbrains.plugins.github.pullrequest.ui.toolwindow.GHPRListSearchValue");
        Object companion = c.getField("Companion").get(null);
        Method getDefault = companion.getClass().getMethod("getDEFAULT");
        Object ghprListSearchValue = getDefault.invoke(companion);
        Method toQuery = ghprListSearchValue.getClass().getMethod("toQuery");
        Object query = toQuery.invoke(ghprListSearchValue);
        Method searchQuery = loader.getClass().getMethod("setSearchQuery");
        searchQuery.invoke(loader, query);
  }
}
