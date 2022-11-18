package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;

public final class GHPRLoaderBackgroundable extends Task.Backgroundable {
  private final Project project;
  private final Disposable disposable;

  public GHPRLoaderBackgroundable(Project project) {
    super(project, "Loading github PRs", true);
    this.project = project;
    this.disposable = Disposer.newDisposable();
  }

  @Override
  public void run(@NotNull ProgressIndicator indicator) {
    if (PluginManagerCore.isDisabled(PluginId.getId("org.jetbrains.plugins.github"))) {
      return;
    }
    try {
      GHPRLoader loader = new GHPRLoaderImpl(project, disposable, indicator);
      loader.run();
    } catch (NoClassDefFoundError ignored) {} //safeguard if someone enables GitHub plugin during runtime
  }

  @Override
  public void onFinished() {
    Disposer.dispose(disposable);
  }

}
