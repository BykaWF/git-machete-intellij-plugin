package com.virtuslab.gitmachete.frontend.actions.contextmenu;

import static com.virtuslab.gitmachete.frontend.resourcebundles.GitMacheteBundle.getString;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import lombok.val;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitmachete.frontend.actions.base.BaseSyncToParentByMergeAction;
import com.virtuslab.gitmachete.frontend.actions.dialogs.WarnAboutSyncToParentByMergeDialog;
import com.virtuslab.gitmachete.frontend.actions.expectedkeys.IExpectsKeySelectedBranchName;
import com.virtuslab.qual.async.ContinuesInBackground;

@CustomLog
public class SyncSelectedToParentByMergeAction extends BaseSyncToParentByMergeAction
    implements
      IExpectsKeySelectedBranchName {

  public static final String SHOW_MERGE_WARNING = "git-machete.merge.warning.show";

  @Override
  public @Nullable String getNameOfBranchUnderAction(AnActionEvent anActionEvent) {
    return getSelectedBranchName(anActionEvent);
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
    if (PropertiesComponent.getInstance(project).getBoolean(SHOW_MERGE_WARNING, /* defaultValue */ true)) {

      val dialogBuilder = MessageDialogBuilder.okCancel(
          getString("action.GitMachete.SyncSelectedToParentByMergeAction.warning-dialog.title"),
          getString("action.GitMachete.SyncSelectedToParentByMergeAction.warning-dialog.message.HTML"));

      dialogBuilder
          .icon(Messages.getWarningIcon())
          .doNotAsk(new WarnAboutSyncToParentByMergeDialog(project));

      val dialogResult = dialogBuilder.ask(project);

      if (!dialogResult) {
        return;
      }
    }

    super.actionPerformed(anActionEvent);
  }

  @Override
  public LambdaLogger log() {
    return LOG;
  }
}
