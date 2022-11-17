package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import git4idea.repo.GitRepository;
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

@Service
public final class GHPRLoader implements Disposable {
  private final Project project;
  @Nullable
  private final GithubApiRequestExecutor executor;
  @Nullable
  private final GHRepositoryCoordinates coordinates;
  @Nullable
  private final GitRepository repository;

  public GHPRLoader(Project project) {
    this.project = project;
    this.executor = getRequestExecutor();
    this.coordinates = getRepositoryCoordinates(project);
    this.repository = project.getService(SelectedGitRepositoryProvider.class).getSelectedGitRepository();
  }

  @Override
  public void dispose() {

  }

  public void updateCustomAnnotations() {
    if (executor != null && coordinates != null) {
      val ghprListLoader = new GHPRListLoader(ProgressManager.getInstance(), executor, coordinates);

      ghprListLoader.setSearchQuery(GHPRListSearchValue.Companion.getDEFAULT().toQuery());
      ghprListLoader.addDataListener(this, new GHListLoader.ListDataListener() {
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
            val pullRequests = loadDetails(ghprListLoader.getLoadedData());
            writeBranchLayout(pullRequests);
          }
        }
      });
      ghprListLoader.loadMore(false);
    }

  }

  private List<GHPullRequest> loadDetails(ArrayList<GHPullRequestShort> loadedData) {
    Objects.requireNonNull(executor);
    Objects.requireNonNull(coordinates);

    val ghprDetailsService = new GHPRDetailsServiceImpl(ProgressManager.getInstance(), executor, coordinates);

    val progressIndicator = new EmptyProgressIndicator();
    return loadedData.stream()
        .map(x -> ghprDetailsService.loadDetails(progressIndicator, x)).map(x -> {
          try {
            return Optional.ofNullable(x.get());
          } catch (InterruptedException | ExecutionException e) {
            return Optional.<GHPullRequest>empty();
          }
        }).filter(Optional::isPresent).map(Optional::get).collect(List.collector());

  }

  private void writeBranchLayout(List<GHPullRequest> pullRequests) {
    Objects.requireNonNull(repository);

    Map<String, GHPullRequest> requestMap = pullRequests.toMap(GHPullRequest::getHeadRefName, Function.identity());
    val macheteFilePath = Option.of(repository).map(GitVfsUtils::getMacheteFilePath).getOrNull();
    if (macheteFilePath != null) {
      val branchLayoutReader = RuntimeBinding.instantiateSoleImplementingClass(IBranchLayoutReader.class);
      val branchLayoutWriter = RuntimeBinding.instantiateSoleImplementingClass(IBranchLayoutWriter.class);
      try {
        BranchLayout branchLayout = branchLayoutReader.read(macheteFilePath);
        val newBranchLayout = branchLayout.map(entry -> {
          val name = entry.getName();
          String annotation = requestMap.get(name).map(x -> "PR #" + x.getNumber()).getOrNull();
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
