package com.virtuslab.gitmachete.gitmacheteapi;

public class GitMacheteException extends Exception {
	public GitMacheteException() {
		super();
	}

	public GitMacheteException(String message) {
		super(message);
	}

	public GitMacheteException(Throwable e) {
		super(e);
	}

	public GitMacheteException(String message, Throwable e) {
		super(message, e);
	}
}
