package com.virtuslab.gitmachete.backend.integration;

import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_FOR_OVERRIDDEN_FORK_POINT;
import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_FOR_YELLOW_EDGES;
import static com.virtuslab.gitmachete.testcommon.SetupScripts.SETUP_WITH_SINGLE_REMOTE;
import static com.virtuslab.gitmachete.testcommon.TestFileUtils.cleanUpDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.FileInputStream;

import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.virtuslab.binding.RuntimeBinding;
import com.virtuslab.branchlayout.api.readwrite.IBranchLayoutReader;
import com.virtuslab.gitmachete.backend.api.IManagedBranchSnapshot;
import com.virtuslab.gitmachete.backend.impl.GitMacheteRepositoryCache;
import com.virtuslab.gitmachete.testcommon.GitRepositoryBackedIntegrationTestSuiteInitializer;

public class ParentInferenceIntegrationTestSuite {

  private final GitMacheteRepositoryCache gitMacheteRepositoryCache = new GitMacheteRepositoryCache();

  public static String[][] getTestData() {
    return new String[][]{
        // script name, for branch, expected parent
        {SETUP_FOR_OVERRIDDEN_FORK_POINT, "allow-ownership-link", "develop"},
        {SETUP_FOR_YELLOW_EDGES, "allow-ownership-link", "develop"},
        {SETUP_FOR_YELLOW_EDGES, "drop-constraint", "call-ws"},
        {SETUP_WITH_SINGLE_REMOTE, "drop-constraint", "call-ws"},
    };
  }

  @SneakyThrows
  @ParameterizedTest
  @MethodSource("getTestData")
  public void parentIsCorrectlyInferred(String scriptName, String forBranch, String expectedParent) {
    val it = new GitRepositoryBackedIntegrationTestSuiteInitializer(scriptName);

    val branchLayoutReader = RuntimeBinding.instantiateSoleImplementingClass(IBranchLayoutReader.class);
    val branchLayout = branchLayoutReader.read(new FileInputStream(it.mainGitDirectoryPath.resolve("machete").toFile()));

    val gitMacheteRepository = gitMacheteRepositoryCache.getInstance(it.rootDirectoryPath, it.mainGitDirectoryPath,
        it.worktreeGitDirectoryPath);
    val gitMacheteRepositorySnapshot = gitMacheteRepository.createSnapshotForLayout(branchLayout);

    val managedBranchNames = gitMacheteRepositorySnapshot.getManagedBranches().map(IManagedBranchSnapshot::getName).toSet();
    val result = gitMacheteRepository.inferParentForLocalBranch(managedBranchNames, forBranch);
    assertNotNull(result);
    assertEquals(expectedParent, result.getName());

    cleanUpDir(it.parentDirectoryPath);
  }

}
