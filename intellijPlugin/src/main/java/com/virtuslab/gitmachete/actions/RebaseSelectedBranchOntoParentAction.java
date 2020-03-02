package com.virtuslab.gitmachete.actions;

import static com.virtuslab.gitmachete.actions.DataKeyIDs.KEY_SELECTED_BRANCH;
import static com.virtuslab.gitmachete.actions.DataKeyIDs.KEY_SELECTED_BRANCH_NAME;
import static com.virtuslab.gitmachete.actions.DataKeyIDs.KEY_TABLE_MANAGER;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.virtuslab.gitmachete.gitmacheteapi.GitMacheteException;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteBranch;
import com.virtuslab.gitmachete.gitmacheteapi.IGitMacheteRepository;
import com.virtuslab.gitmachete.gitmacheteapi.IGitRebaseParameters;

import git4idea.GitUtil;
import git4idea.branch.GitRebaseParams;
import git4idea.config.GitVersion;
import git4idea.rebase.GitRebaseUtils;
import git4idea.repo.GitRepository;

public class RebaseSelectedBranchOntoParentAction extends AnAction {
	private static final Logger LOG = Logger.getInstance(RebaseSelectedBranchOntoParentAction.class);

	@Override
	public void update(@Nonnull AnActionEvent anActionEvent) {
		super.update(anActionEvent);
		// TODO (#79): prohibit rebase during rebase/merge/revert etc.
	}

	@Override
	public void actionPerformed(@Nonnull AnActionEvent anActionEvent) {
		Project project = anActionEvent.getProject();
		assert project != null;
		GitRepository repository = getRepository(project);
		GitVersion gitVersion = repository.getVcs().getVersion();

		IGitMacheteRepository gitMacheteRepository = anActionEvent.getData(KEY_TABLE_MANAGER).getRepository();

		IGitMacheteBranch branchToRebase = anActionEvent.getData(KEY_SELECTED_BRANCH);
		if (branchToRebase == null) {
			Optional<IGitMacheteBranch> branchToRebaseOptional = gitMacheteRepository
					.getBranchByName(anActionEvent.getData(KEY_SELECTED_BRANCH_NAME));
			if (branchToRebaseOptional.isEmpty()) {
				LOG.error("Can't get branch to rebase");
				return;
			}

			branchToRebase = branchToRebaseOptional.get();
		}

		Optional<IGitRebaseParameters> gitRebaseParameters = computeGitRebaseParameters(branchToRebase);

		if (gitRebaseParameters.isEmpty()) {
			LOG.error("Unable to get rebase parameters");
			return;
		}

		IGitRebaseParameters parameters = gitRebaseParameters.get();
		String currentBranch = parameters.getCurrentBranch().getName();
		String newBase = parameters.getNewBaseCommit().getHash();
		String forkPoint = parameters.getForkPointCommit().getHash();

		new Task.Backgroundable(project, "Rebasing") {
			@Override
			public void run(@Nonnull ProgressIndicator indicator) {
				GitRebaseParams params = new GitRebaseParams(gitVersion, currentBranch, newBase,
						/* upstream */ forkPoint, /* interactive */ true, /* preserveMerges */ false);
				GitRebaseUtils.rebase(project, List.of(repository), params, indicator);
			}

			/*
			 * TODO (#95): on success, refresh only sync statuses (not the whole repository). Keep in mind potential
			 * changes to commits (eg. commits may get squashed so the graph structure changes).
			 */
		}.queue();
	}

	@Nonnull
	private Optional<IGitRebaseParameters> computeGitRebaseParameters(IGitMacheteBranch gitMacheteCurrentBranch) {
		if (gitMacheteCurrentBranch == null)
			return Optional.empty();

		Optional<IGitRebaseParameters> gitRebaseParameters = Optional.empty();
		try {
			gitRebaseParameters = Optional.of(gitMacheteCurrentBranch.computeRebaseParameters());
		} catch (GitMacheteException e) {
			LOG.error("Unable to compute rebase parameters", e);
		}

		return gitRebaseParameters;
	}

	/**
	 * The visibility predicate
	 * {@link com.virtuslab.gitmachete.ui.GitMacheteContentProvider.GitMacheteVisibilityPredicate} performs
	 * {@link com.intellij.openapi.vcs.ProjectLevelVcsManager#checkVcsIsActive(String)} which is true when the specified
	 * VCS is used by at least one module in the project. Therefore it is guaranteed that while the Git Machete plugin
	 * tab is visible, a git repository exists.
	 */
	protected GitRepository getRepository(Project project) {
		// TODO (#64): handle multiple repositories
		Iterator<GitRepository> iterator = GitUtil.getRepositories(project).iterator();
		assert iterator.hasNext();
		return iterator.next();
	}
}
