package com.virtuslab.gitcore.api;

import java.util.Date;

public abstract class BaseGitCoreCommit {
  public abstract String getMessage();

  public abstract IGitCorePersonIdentity getAuthor();

  public abstract IGitCorePersonIdentity getCommitter();

  public abstract Date getCommitTime();

  public abstract IGitCoreCommitHash getHash();

  public final boolean equals(Object other) {
    if (this == other) {
      return true;
    } else if (!(other instanceof BaseGitCoreCommit)) {
      return false;
    } else {
      return getHash().getHashString().equals(((BaseGitCoreCommit) other).getHash().getHashString());
    }
  }
}