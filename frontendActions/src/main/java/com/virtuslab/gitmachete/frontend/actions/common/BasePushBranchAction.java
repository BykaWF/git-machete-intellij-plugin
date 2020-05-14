package com.virtuslab.gitmachete.frontend.actions.common;

import java.util.Collections;

import com.intellij.dvcs.push.ui.VcsPushDialog;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import git4idea.GitLocalBranch;
import git4idea.push.GitPushSource;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.backend.api.SyncToRemoteStatus;
import com.virtuslab.gitmachete.frontend.datakeys.DataKeys;
import com.virtuslab.logger.IPrefixedLambdaLogger;
import com.virtuslab.logger.PrefixedLambdaLoggerFactory;

/**
 * Expects DataKeys:
 * <ul>
 *  <li>{@link DataKeys#KEY_GIT_MACHETE_REPOSITORY}</li>
 *  <li>{@link CommonDataKeys#PROJECT}</li>
 * </ul>
 */
public abstract class BasePushBranchAction extends GitMacheteRepositoryReadyAction {
  private static final IPrefixedLambdaLogger LOG = PrefixedLambdaLoggerFactory.getLogger("frontendActions");

  protected final List<SyncToRemoteStatus.Relation> PUSH_ELIGIBLE_STATUSES = List.of(
      SyncToRemoteStatus.Relation.AheadOfRemote,
      SyncToRemoteStatus.Relation.DivergedFromAndNewerThanRemote,
      SyncToRemoteStatus.Relation.DivergedFromAndOlderThanRemote,
      SyncToRemoteStatus.Relation.Untracked);

  @UIEffect
  protected void doPush(Project project, GitRepository preselectedRepository, String branchName) {
    @Nullable
    GitLocalBranch localBranch = preselectedRepository.getBranches().findLocalBranch(branchName);

    if (localBranch != null) {
      java.util.List<GitRepository> selectedRepositories = Collections.singletonList(preselectedRepository);
      // Presented dialog shows commits for branches belonging to allRepositories, preselectedRepositories and currentRepo.
      // The second and the third one have higher priority of loading its commits.
      // From our perspective, we always have single (pre-selected) repository so we do not care about the priority.
      new VcsPushDialog(project,
          /* allRepositories */ selectedRepositories,
          /* preselectedRepositories */ selectedRepositories,
          /* currentRepo */ null,
          GitPushSource.create(localBranch)).show();
    } else {
      LOG.warn("Skipping the action because provided branch ${branchName} was not found in repository");
    }
  }
}
