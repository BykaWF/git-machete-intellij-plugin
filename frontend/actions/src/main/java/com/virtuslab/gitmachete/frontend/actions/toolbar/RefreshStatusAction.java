package com.virtuslab.gitmachete.frontend.actions.toolbar;

import java.util.List;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.jetbrains.plugins.github.api.data.pullrequest.GHPullRequestShort;

import com.virtuslab.gitmachete.frontend.actions.base.BaseProjectDependentAction;
import com.virtuslab.gitmachete.frontend.actions.common.GHPRLoader;

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
    List<GHPullRequestShort> pullRequestShorts = GHPRLoader.loadPRs(getProject(anActionEvent));
    log().info(pullRequestShorts.toString());
    getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
  }
}
