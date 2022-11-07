package com.virtuslab.gitmachete.backend.api;

import java.util.function.Function;

import io.vavr.collection.List;
import io.vavr.collection.Map;
import lombok.Getter;

@Getter
public class GitMachetePullRequests {
  private final List<PullRequest> pullRequests;
  private final Map<String, PullRequest> headNameToPullRequest;

  public final static GitMachetePullRequests EMPTY = new GitMachetePullRequests(List.empty());

  public GitMachetePullRequests(List<PullRequest> pullRequests) {
    this.pullRequests = pullRequests;
    this.headNameToPullRequest = pullRequests.toMap(PullRequest::getHeadRefName, Function.identity());
  }
}
