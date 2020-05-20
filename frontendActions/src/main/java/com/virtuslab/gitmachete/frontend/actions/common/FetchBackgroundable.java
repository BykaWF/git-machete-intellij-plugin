package com.virtuslab.gitmachete.frontend.actions.common;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;

public class FetchBackgroundable extends Task.Backgroundable {

  private final Project project;
  private final GitRepository gitRepository;
  private final GitRemote remote;
  private final String refspec;

  public FetchBackgroundable(Project project,
      GitRepository gitRepository,
      String refspec,
      GitRemote remote,
      final String taskTitle) {
    super(project, taskTitle, /* canBeCancelled */ true);
    this.project = project;
    this.gitRepository = gitRepository;
    this.remote = remote;
    this.refspec = refspec;
  }

  @Override
  public void run(ProgressIndicator indicator) {
    var fetchSupport = GitFetchSupportImpl.fetchSupport(project);
    var fetchResult = fetchSupport.fetch(gitRepository, remote, refspec);
    try {
      fetchResult.ourThrowExceptionIfFailed();
    } catch (VcsException e) {
      fetchResult.showNotificationIfFailed("Fetch of refspec ${refspec} failed");
    }
  }

  @Override
  public void onSuccess() {
    VcsNotifier.getInstance(project).notifySuccess("Fetch of refspec ${refspec} succeeded");
  }
}