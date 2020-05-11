package com.virtuslab.gitmachete.uitest

import java.io.File
import java.net.URI

import com.virtuslab.ideprobe.dependencies._
import com.virtuslab.ideprobe.ide.intellij.{ DriverConfig, IntelliJFactory }
import com.virtuslab.ideprobe.protocol.IdeMessage.Level.Info
import com.virtuslab.ideprobe.{ IntegrationTestSuite, IntelliJFixture, WorkspaceConfig, WorkspaceTemplate }
import org.junit.{ Assert, Test }

class OpenTabActionUiTest extends IntegrationTestSuite {
  val currentDir: File = new File(System.getProperty("user.dir"))
  val rootDir: File = currentDir.getParentFile
  val distDir: File = new File(s"${rootDir.getAbsolutePath}/build/distributions")
  val latestFileInDistDir: File = distDir.listFiles().maxBy(_.lastModified)
  val pluginUri: URI = new URI(latestFileInDistDir.getAbsolutePath)

  private def testForIntelliJVersion(version: String): Unit = {
    // Apparently, testing our plugin in headless mode does NOT make sense:
    // `com.intellij.openapi.wm.ToolWindowManager.getToolWindow` always returns null in headless mode,
    // which for us means that we can't get the access to the VCS Tool Window, let alone Git Machete tab.
    // Hence, `.withDisplay()` is necessary.
    IntelliJFixture(
      workspaceTemplate = WorkspaceTemplate.from(WorkspaceConfig.Default(Resource.File(rootDir.getParentFile.toPath.resolve("machete-sandbox")))),
      version = IntelliJVersion(version),
      intelliJFactory = IntelliJFactory.Default.withConfig(DriverConfig(vmOptions = Seq("-Xmx1G"))),
      plugins = Seq(Plugin.Direct(pluginUri)),
    ).withDisplay().run { ij =>
      ij.probe.openProject(ij.workspace)
      ij.probe.invokeAction("GitMachete.OpenTabAction")
      ij.probe.awaitIdle()
      Assert.assertTrue(ij.probe.messages.exists(m => m.level == Info && m.content.endsWith("Opened Git Machete tab: Git Machete")))
      Assert.assertArrayEquals(ij.probe.errors.toArray[Object], Array.empty[Object])
    }
  }

  @Test def test_2019_3(): Unit = testForIntelliJVersion("193.7288.26")

  @Test def test_2020_1(): Unit = testForIntelliJVersion("201.6668.121")

}