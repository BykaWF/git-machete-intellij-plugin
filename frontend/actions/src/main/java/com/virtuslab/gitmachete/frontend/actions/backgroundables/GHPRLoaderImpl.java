package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.github.api.GHRepositoryCoordinates;
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor;
import org.jetbrains.plugins.github.api.data.pullrequest.GHPullRequest;
import org.jetbrains.plugins.github.api.data.pullrequest.GHPullRequestShort;
import org.jetbrains.plugins.github.authentication.GithubAuthenticationManager;
import org.jetbrains.plugins.github.pullrequest.data.GHListLoader;
import org.jetbrains.plugins.github.pullrequest.data.GHPRListLoader;
import org.jetbrains.plugins.github.pullrequest.data.service.GHPRDetailsService;
import org.jetbrains.plugins.github.pullrequest.data.service.GHPRDetailsServiceImpl;
import org.jetbrains.plugins.github.pullrequest.ui.toolwindow.GHPRListSearchValue;
import org.jetbrains.plugins.github.util.GithubUrlUtil;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.BranchLayoutEntry;
import com.virtuslab.branchlayout.api.BranchLayoutException;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutReader;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.frontend.ui.providerservice.GraphTableProvider;
import com.virtuslab.gitmachete.frontend.ui.providerservice.SelectedGitRepositoryProvider;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;

public class GHPRLoaderImpl implements GHPRLoader {
  private final Project project;
  private final Disposable disposable;
  private final ProgressIndicator indicator;

  public GHPRLoaderImpl(Project project, Disposable disposable, ProgressIndicator indicator) {
    this.project = project;
    this.disposable = disposable;
    this.indicator = indicator;
  }

  @Override
  public void run() {
    indicator.setFraction(0.0);
    val executor = getRequestExecutor();
    val coordinates = getRepositoryCoordinates(project);

    if (executor != null && coordinates != null) {
      GHPRListLoader ghprListLoader = new GHPRListLoader(ProgressManager.getInstance(), executor, coordinates);
      GHPRDetailsService ghprDetailsService = new GHPRDetailsServiceImpl(ProgressManager.getInstance(), executor, coordinates);
      Disposer.register(disposable, ghprListLoader);

      indicator.setFraction(0.1);
      val shortPrs = loadShortPRs(ghprListLoader);
      indicator.setFraction(0.2);
      val pullRequests = loadPRDetails(shortPrs, ghprDetailsService);
      indicator.setFraction(0.9);
      writeBranchLayout(pullRequests);
    }
  }

  private ArrayList<GHPullRequestShort> loadShortPRs(GHPRListLoader ghprListLoader) {
    ghprListLoader.setSearchQuery(GHPRListSearchValue.Companion.getDEFAULT().toQuery());
    ghprListLoader.addDataListener(disposable, new GHListLoader.ListDataListener() {
      @Override
      public void onAllDataRemoved() {}

      @Override
      public void onDataRemoved(@NotNull Object data) {}

      @Override
      public void onDataUpdated(int idx) {}

      @Override
      public void onDataAdded(int i) {
        if (ghprListLoader.canLoadMore()) {
          ghprListLoader.loadMore(false);
        } else {
          synchronized (ghprListLoader) {
            ghprListLoader.notify();
          }
        }
      }
    });
    ghprListLoader.loadMore(false);
    try {
      synchronized (ghprListLoader) {
        ghprListLoader.wait();
      }
    } catch (InterruptedException ignored) {}
    return ghprListLoader.getLoadedData();
  }

  private List<GHPullRequest> loadPRDetails(ArrayList<GHPullRequestShort> loadedData, GHPRDetailsService ghprDetailsService) {
    val progressIndicator = new EmptyProgressIndicator();
    return loadedData.stream()
        .map(x -> ghprDetailsService.loadDetails(progressIndicator, x)).map(x -> {
          try {
            return Option.of(x.get());
          } catch (InterruptedException | ExecutionException e) {
            return Option.<GHPullRequest>none();
          }
        }).filter(Option::isDefined).map(Option::get).collect(List.collector());

  }

  private void writeBranchLayout(List<GHPullRequest> pullRequests) {
    val repository = project.getService(SelectedGitRepositoryProvider.class).getSelectedGitRepository();

    Map<String, GHPullRequest> requestMap = pullRequests.toMap(GHPullRequest::getHeadRefName, Function.identity());
    val macheteFilePath = Option.of(repository).map(GitVfsUtils::getMacheteFilePath).getOrNull();
    if (macheteFilePath != null) {
      val branchLayoutReader = RuntimeBinding.instantiateSoleImplementingClass(IBranchLayoutReader.class);
      val branchLayoutWriter = RuntimeBinding.instantiateSoleImplementingClass(IBranchLayoutWriter.class);
      try {
        BranchLayout branchLayout = branchLayoutReader.read(macheteFilePath);
        val newBranchLayout = branchLayout.map(entry -> {
          String annotation = requestMap.get(entry.getName()).map(x -> "PR #" + x.getNumber()).getOrNull();
          if (annotation == null) {
            annotation = entry.getCustomAnnotation();
          }
          return new BranchLayoutEntry(entry.getName(), annotation, entry.getChildren());
        });

        branchLayoutWriter.write(macheteFilePath, newBranchLayout, false);

      } catch (BranchLayoutException ignored) {}

      project.getService(GraphTableProvider.class).getGraphTable().queueRepositoryUpdateAndModelRefresh();
    }

  }

  private static @Nullable GithubApiRequestExecutor getRequestExecutor() {
    val githubAuthenticationManager = GithubAuthenticationManager.getInstance();

    if (!githubAuthenticationManager.hasAccounts()) {
      return null;
    }
    val account = githubAuthenticationManager.getAccounts().iterator().next();
    val token = githubAuthenticationManager.getTokenForAccount$intellij_vcs_github(account);
    if (token == null) {
      return null;
    }
    return (GithubApiRequestExecutor) GithubApiRequestExecutor.Factory.getInstance().create(token);
  }

  private static @Nullable GHRepositoryCoordinates getRepositoryCoordinates(Project project) {
    val gitRepositoryProvider = project.getService(SelectedGitRepositoryProvider.class);
    val githubAuthenticationManager = GithubAuthenticationManager.getInstance();

    if (!githubAuthenticationManager.hasAccounts()) {
      return null;
    }
    val account = githubAuthenticationManager.getAccounts().iterator().next();

    val selectedGitRepository = gitRepositoryProvider.getSelectedGitRepository();
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
    return new GHRepositoryCoordinates(account.getServer(), repositoryPath);
  }
}
