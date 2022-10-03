package com.virtuslab.gitmachete.frontend.actions.navigation;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getNonHtmlString;

import com.intellij.openapi.actionSystem.AnActionEvent;
import lombok.experimental.ExtensionMethod;
import lombok.val;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.tainting.qual.Untainted;

import com.virtuslab.gitmachete.frontend.actions.base.BaseCheckoutAction;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;
import com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle;

@ExtensionMethod(GitMacheteBundle.class)
public class CheckoutParentAction extends BaseCheckoutAction
    implements
      IExpectsKeySelectedBranchName {

  @Override
  protected @Untainted String getNonExistentBranchMessage(AnActionEvent anActionEvent) {
    val currentBranchName = getCurrentBranchNameIfManaged(anActionEvent);
    return currentBranchName != null
        ? getNonHtmlString("action.GitMachete.CheckoutParentAction.undefined.branch-name").format(currentBranchName)
        : getNonHtmlString("action.GitMachete.BaseCheckoutAction.undefined.current-branch");
  }

  @Override
  protected @Nullable String getTargetBranchName(AnActionEvent anActionEvent) {
    val gitRepository = getSelectedGitRepository(anActionEvent);
    val currentBranchName = getCurrentBranchNameIfManaged(anActionEvent);
    val currentBranch = getManagedBranchByName(anActionEvent, currentBranchName);
    if (currentBranch != null && currentBranch.isNonRoot()) {
      val nonRootBranch = currentBranch.asNonRoot();
      return nonRootBranch.getParent().getName();
    }
    return null;
  }
}