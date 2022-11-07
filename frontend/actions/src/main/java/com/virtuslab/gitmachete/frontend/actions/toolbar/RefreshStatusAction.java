package com.virtuslab.gitmachete.frontend.actions.toolbar;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import io.vavr.collection.List;
import kr.pe.kwonnam.slf4jlambda.LambdaLogger;
import lombok.CustomLog;
import org.checkerframework.checker.guieffect.qual.UIEffect;
import org.jetbrains.plugins.github.api.data.pullrequest.GHPullRequest;

import com.virtuslab.gitmachete.frontend.actions.base.BaseProjectDependentAction;
import com.virtuslab.gitmachete.frontend.ui.providerservice.GHPRLoaderProvider;

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
    GHPRLoaderProvider service = getProject(anActionEvent).getService(GHPRLoaderProvider.class);
    List<GHPullRequest> ghPullRequests = service.getGhPullRequests();
    log().info(ghPullRequests.mkString());
    getGraphTable(anActionEvent).queueRepositoryUpdateAndModelRefresh();
  }
}
