package com.virtuslab.gitmachete.backend.api;

import lombok.Data;

@Data
public class PullRequest {
  private final Integer number;
  private final String title;
  private final PullRequestState state;
}
