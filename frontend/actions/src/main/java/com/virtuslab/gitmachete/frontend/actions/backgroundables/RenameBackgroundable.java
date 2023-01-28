package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import static com.virtuslab.gitmachete.frontend.actions.common.BranchCreationUtils.waitForCreationOfLocalBranch;
import static com.virtuslab.gitmachete.frontend.common.WriteActionUtils.blockingRunWriteActionOnUIThread;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import java.nio.file.Path;
import java.util.Collections;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.branch.GitBrancher;
import git4idea.branch.GitNewBranchOptions;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitLineHandler;
import git4idea.repo.GitRepository;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.branchlayout.api.BranchLayout;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutWriter;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.frontend.file.MacheteFileWriter;
import com.virtuslab.gitmachete.frontend.ui.api.table.BaseEnhancedGraphTable;
import com.virtuslab.gitmachete.frontend.vfsutils.GitVfsUtils;
import com.virtuslab.qual.async.ContinuesInBackground;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

@ExtensionMethod(GitVfsUtils.class)
public class RenameBackgroundable extends Task.Backgroundable {

  protected final Project project;
  private final GitRepository gitRepository;
  private final BaseEnhancedGraphTable graphTable;
  private final BranchLayout branchLayout;
  private final IManagedBranchSnapshot currentBranchSnapshot;
  private final GitNewBranchOptions gitNewBranchOptions;

  public RenameBackgroundable(
      GitRepository gitRepository,
      BaseEnhancedGraphTable graphTable, BranchLayout branchLayout,
      IManagedBranchSnapshot currentBranchSnapshot,
      GitNewBranchOptions gitNewBranchOptions) {
    super(gitRepository.getProject(), getString("action.GitMachete.RenameBackgroundable.task-title"));
    this.project = gitRepository.getProject();
    this.gitRepository = gitRepository;
    this.graphTable = graphTable;
    this.branchLayout = branchLayout;
    this.currentBranchSnapshot = currentBranchSnapshot;
    this.gitNewBranchOptions = gitNewBranchOptions;
  }

  @Override
  @UIThreadUnsafe
  @ContinuesInBackground
  public void run(ProgressIndicator indicator) {
    graphTable.disableEnqueuingUpdates();

    Path macheteFilePath = gitRepository.getMacheteFilePath();

    val newBranchName = gitNewBranchOptions.getName();
    val newBranchLayout = branchLayout.rename(currentBranchSnapshot.getName(), newBranchName);
    val branchLayoutWriter = ApplicationManager.getApplication().getService(IBranchLayoutWriter.class);
    val gitBrancher = GitBrancher.getInstance(project);
    gitBrancher.renameBranch(currentBranchSnapshot.getName(), newBranchName, Collections.singletonList(gitRepository));

    blockingRunWriteActionOnUIThread(() -> {
      MacheteFileWriter.writeBranchLayout(
          macheteFilePath,
          branchLayoutWriter,
          newBranchLayout,
          /* backupOldLayout */ true,
          /* requestor */ this);

    });

    // `gitBrancher.renameBranch` continues asynchronously and doesn't allow for passing a callback to execute once complete.
    // (Stepping deeper is not an option since we would lose some important logic or become very dependent on the internals of git4idea).
    // Hence, we wait for the creation of the branch (with exponential backoff).
    waitForCreationOfLocalBranch(gitRepository, newBranchName);

    if (gitNewBranchOptions.shouldSetTracking()) {
      val remote = currentBranchSnapshot.getRemoteTrackingBranch();
      assert remote != null : "shouldSetTracking is true but the remote branch does not exist";
      handleSetTrackingBranch(remote.getRemoteName());
    }
  }

  @UIThreadUnsafe
  private void handleSetTrackingBranch(String remoteName) {
    val newBranchName = gitNewBranchOptions.getName();
    val renamedRemoteName = "${remoteName}/${newBranchName}";

    // setUpstream: git branch --set-upstream-to <upstreamBranchName> <branchName>
    val setUpstream = Git.getInstance().setUpstream(gitRepository, renamedRemoteName, newBranchName);
    var error = setUpstream.getErrorOutputAsJoinedString();

    if (!setUpstream.success()) {
      if (setUpstream.getErrorOutput().get(0)
          .equals("fatal: the requested upstream branch '${renamedRemoteName}' does not exist")) {
        val unsetUpstream = unsetUpstream(newBranchName);
        if (unsetUpstream.success()) {
          // This happens when one renames a branch with a remote branch set up,
          // and a local branch with the new name is created for the second and next time,
          // and the remote with the new name does not exist.
          return;
        } else if (unsetUpstream.getErrorOutput().get(0)
            .equals("fatal: Branch '${newBranchName}' has no upstream information")) {
          // This happens when one renames a branch with a remote branch set up,
          // and a local branch with the new name is created for the first time,
          // and the remote with the new name does not exist.
          return;
        }
        error = unsetUpstream.getErrorOutputAsJoinedString();
      }

      VcsNotifier.getInstance(project).notifyError(null,
          getString("action.GitMachete.RenameBackgroundable.rename-failed"),
          error);
    } else {
      // This happens when one renames a branch with a remote branch set up,
      // and the remote with the new name exist.
    }
  }

  @UIThreadUnsafe
  private GitCommandResult unsetUpstream(String branchName) {
    GitLineHandler h = new GitLineHandler(gitRepository.getProject(), gitRepository.getRoot(), GitCommand.BRANCH);
    h.setStdoutSuppressed(false);
    h.addParameters("--unset-upstream", branchName);
    return Git.getInstance().runCommand(h);
  }

  @Override
  @UIEffect
  public void onThrowable(Throwable e) {
    val exceptionMessage = e.getMessage();
    VcsNotifier.getInstance(project).notifyError(/* displayId */ null,
        /* title */ getString(
            "action.GitMachete.BaseSlideInBelowAction.notification.title.branch-layout-write-fail"),
        exceptionMessage == null ? "" : exceptionMessage);
  }

  @Override
  public void onFinished() {
    graphTable.enableEnqueuingUpdates();
  }

}
