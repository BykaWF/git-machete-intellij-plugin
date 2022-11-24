package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;

import com.virtuslab.gitmachete.frontend.actions.base.BaseProjectDependentAction;
import com.virtuslab.gitmachete.frontend.actions.github.GHPRLoaderBackgroundable;

@CustomLog
public class RefreshStatusAction extends BaseProjectDependentAction {

  @Override
  public LambdaLogger log() {
    return LOG;
  }

  @Override
  @UIEffect
  public void actionPerformed(AnActionEvent anActionEvent) {
    FileDocumentManager.getInstance().saveAllDocuments();
    new GHPRLoaderBackgroundable(getProject(anActionEvent)).queue();
    getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
  }
}
