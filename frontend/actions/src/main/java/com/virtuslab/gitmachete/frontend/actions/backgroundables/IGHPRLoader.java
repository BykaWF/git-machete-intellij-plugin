package com.virtuslab.gitmachete.frontend.actions.backgroundables;

import com.virtuslab.qual.guieffect.UIThreadUnsafe;

public interface IGHPRLoader {

  @UIThreadUnsafe
  void run();
}
