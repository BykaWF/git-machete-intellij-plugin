package com.virtuslab.gitmachete.gitmacheteapi;

public class GitImplementationException extends GitMacheteJGitException {
	public GitImplementationException() {
		super();
	}

	public GitImplementationException(String message) {
		super(message);
	}

	public GitImplementationException(Throwable e) {
		super(e);
	}

	public GitImplementationException(String message, Throwable e) {
		super(message, e);
	}
}
