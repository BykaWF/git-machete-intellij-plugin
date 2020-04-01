package com.virtuslab.gitmachete.backend.impl;

import java.util.Optional;

import io.vavr.collection.List;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.virtuslab.gitcore.api.IGitCoreLocalBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteNonRootBranch;
import com.virtuslab.gitmachete.backend.api.BaseGitMacheteRootBranch;
import com.virtuslab.gitmachete.backend.api.SyncToOriginStatus;

@Getter
@ToString
public class GitMacheteRootBranch extends BaseGitMacheteRootBranch {
  private final String name;
  private final List<GitMacheteNonRootBranch> downstreamBranches;
  private final GitMacheteCommit pointedCommit;
  private final SyncToOriginStatus syncToOriginStatus;
  private final IGitCoreLocalBranch coreLocalBranch;
  @Nullable
  private final String customAnnotation;

  @SuppressWarnings("argument.type.incompatible")
  public GitMacheteRootBranch(String name, List<GitMacheteNonRootBranch> downstreamBranches,
      GitMacheteCommit pointedCommit,
      SyncToOriginStatus syncToOriginStatus,
      IGitCoreLocalBranch coreLocalBranch, @Nullable String customAnnotation) {
    this.name = name;
    this.downstreamBranches = downstreamBranches;
    this.pointedCommit = pointedCommit;
    this.syncToOriginStatus = syncToOriginStatus;
    this.coreLocalBranch = coreLocalBranch;
    this.customAnnotation = customAnnotation;

    // This is a hack necessary to create an immutable cyclic structure (children pointing at the parent & parent
    // pointing at the children).
    // This is definitely not the cleanest solution, but still easier to manage and reason about than keeping the
    // upstream data somewhere outside of GitMacheteBranch (e.g. in GitMacheteRepository).
    for (GitMacheteNonRootBranch branch : downstreamBranches) {
      branch.setUpstreamBranch(this);
    }
  }

  @Override
  public List<BaseGitMacheteNonRootBranch> getDownstreamBranches() {
    return List.narrow(downstreamBranches);
  }

  @Override
  public Optional<String> getCustomAnnotation() {
    return Optional.ofNullable(customAnnotation);
  }
}
