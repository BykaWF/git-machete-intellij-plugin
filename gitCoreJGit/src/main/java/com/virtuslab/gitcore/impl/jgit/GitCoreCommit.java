package com.virtuslab.gitcore.impl.jgit;

import java.time.Instant;

import lombok.Getter;
import org.eclipse.jgit.revwalk.RevCommit;

import com.virtuslab.gitcore.api.BaseGitCoreCommit;
import com.virtuslab.gitcore.api.BaseGitCoreCommitHash;

@Getter
public class GitCoreCommit extends BaseGitCoreCommit {
  private final RevCommit jgitCommit;
  private final String message;
  private final GitCorePersonIdentity author;
  private final GitCorePersonIdentity committer;
  private final Instant commitTime;
  private final BaseGitCoreCommitHash hash;

  public GitCoreCommit(RevCommit commit) {
    this.jgitCommit = commit;
    this.message = jgitCommit.getFullMessage();
    this.author = new GitCorePersonIdentity(jgitCommit.getAuthorIdent());
    this.committer = new GitCorePersonIdentity(jgitCommit.getCommitterIdent());
    this.commitTime = Instant.ofEpochSecond(jgitCommit.getCommitTime());
    this.hash = GitCoreCommitHash.of(jgitCommit);
  }

  @Override
  @SuppressWarnings("index:argument.type.incompatible")
  public String toString() {
    return jgitCommit.getId().getName().substring(0, 7) + ": " + jgitCommit.getShortMessage();
  }
}
