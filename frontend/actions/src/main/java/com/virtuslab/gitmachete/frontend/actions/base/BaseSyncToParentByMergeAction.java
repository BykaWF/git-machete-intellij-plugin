package com.virtuslab.gitmachete.frontend.actions.base;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;
import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;
import static org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory.GENERAL;

import java.util.Collections;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import git4idea.GitReference;
import git4idea.branch.GitBrancher;
import git4idea.repo.GitRepository;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;
import org.checkerframework.checker.tainting.qual.Untainted;

import com.virtuslab.gitmachete.backend.api.SyncToParentStatus;
import com.virtuslab.gitmachete.frontend.actions.common.MergeProps;
import com.virtuslab.gitmachete.frontend.defs.ActionPlaces;
import com.virtuslab.qual.async.ContinuesInBackground;
import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public abstract class BaseSyncToParentByMergeAction extends BaseGitMacheteRepositoryReadyAction
    implements
      IBranchNameProvider,
      ISyncToParentStatusDependentAction {

  @Override
  public @I18nFormat({}) String getActionNameForDisabledDescription() {
    return getString("action.GitMachete.BaseSyncToParentByMergeAction.description-action-name");
  }

  @Override
  public @Untainted @I18nFormat({GENERAL, GENERAL}) String getEnabledDescriptionFormat() {
    return getNonHtmlString("action.GitMachete.BaseSyncToParentByMergeAction.description");
  }

  @Override
  public List<SyncToParentStatus> getEligibleStatuses() {
    return List.of(SyncToParentStatus.OutOfSync);
  }

  @Override
  @UIEffect
  protected void onUpdate(AnActionEvent anActionEvent) {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
    super.onUpdate(anActionEvent);
    syncToParentStatusDependentActionUpdate(anActionEvent);
    val presentation = anActionEvent.getPresentation();
    val isCalledFromContextMenu = anActionEvent.getPlace().equals(ActionPlaces.CONTEXT_MENU);
    val branchName = getNameOfBranchUnderAction(anActionEvent);
    val branch = branchName != null
        ? getManagedBranchByName(anActionEvent, branchName)
        : null;
    val currentBranchNameIfManaged = getCurrentBranchNameIfManaged(anActionEvent);
    val isMergingIntoCurrent = branch != null && currentBranchNameIfManaged != null
        && currentBranchNameIfManaged.equals(branch.getName());
    if (isCalledFromContextMenu && isMergingIntoCurrent) {
      presentation.setText(getString("action.GitMachete.BaseSyncToParentByMergeAction.text"));
    }
  }

  @Override
  @ContinuesInBackground
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }

    val project = getProject(anActionEvent);
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val stayingBranchName = getNameOfBranchUnderAction(anActionEvent);
    if (gitRepository == null || stayingBranchName == null) {
      return;
    }

    val movingBranch = getManagedBranchByName(anActionEvent, stayingBranchName);
    if (movingBranch == null) {
      return;
    }
    // This is guaranteed by `syncToParentStatusDependentActionUpdate` invoked from `onUpdate`.
    assert movingBranch.isNonRoot() : "Branch that would be merged INTO is a root";

    val currentBranchName = Option.of(gitRepository.getCurrentBranch()).map(GitReference::getName).getOrNull();
    val nonRootMovingBranch = movingBranch.asNonRoot();
    val mergeProps = new MergeProps(
        /* movingBranchName */ nonRootMovingBranch,
        /* stayingBranchName */ nonRootMovingBranch.getParent());

    if (nonRootMovingBranch.getName().equals(currentBranchName)) {
      doMergeIntoCurrentBranch(gitRepository, mergeProps);
    } else {
      doMergeIntoNonCurrentBranch(gitRepository, mergeProps);
    }
  }

  @ContinuesInBackground
  @UIEffect
  public void doMergeIntoCurrentBranch(GitRepository gitRepository, MergeProps mergeProps) {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
    val stayingBranch = mergeProps.getStayingBranch().getName();
    val project = gitRepository.getProject();
    log().debug(() -> "Entering: project = ${project}, gitRepository = ${gitRepository}," +
        " stayingBranch = ${stayingBranch}");

    new Task.Backgroundable(project, getString("action.GitMachete.BaseSyncToParentByMergeAction.task-title.current")) {
      @Override
      @UIThreadUnsafe
      public void run(ProgressIndicator indicator) {
        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
          var sw = new java.io.StringWriter();
          var pw = new java.io.PrintWriter(sw);
          new Exception().printStackTrace(pw);
          String stackTrace = sw.toString();
          if (!stackTrace.contains("at com.virtuslab.gitmachete.frontend.actions.toolbar.DiscoverAction.actionPerformed")) {
            System.out.println("Expected non-EDT:");
            System.out.println(stackTrace);
            throw new RuntimeException("Expected EDT: " + stackTrace);
          }
        }
        log().info("Merging ${stayingBranch} into current branch");

        GitBrancher.getInstance(project)
            .merge(stayingBranch, GitBrancher.DeleteOnMergeOption.NOTHING, Collections.singletonList(gitRepository));
      }
    }.queue();

  }

  @ContinuesInBackground
  @UIEffect
  private void doMergeIntoNonCurrentBranch(GitRepository gitRepository, MergeProps mergeProps) {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      var sw = new java.io.StringWriter();
      var pw = new java.io.PrintWriter(sw);
      new Exception().printStackTrace(pw);
      String stackTrace = sw.toString();
      System.out.println("Expected EDT:");
      System.out.println(stackTrace);
      throw new RuntimeException("Expected EDT: " + stackTrace);
    }
    val stayingBranch = mergeProps.getStayingBranch().getName();
    val movingBranch = mergeProps.getMovingBranch().getName();
    log().debug(() -> "Entering: gitRepository = ${gitRepository}," +
        " stayingBranch = ${stayingBranch}, movingBranch = ${movingBranch}");

    val gitBrancher = GitBrancher.getInstance(gitRepository.getProject());
    val repositories = Collections.singletonList(gitRepository);

    Runnable callInAwtLater = () -> gitBrancher.merge(stayingBranch, GitBrancher.DeleteOnMergeOption.NOTHING, repositories);
    gitBrancher.checkout(/* reference */ movingBranch, /* detach */ false, repositories, callInAwtLater);
  }

}
