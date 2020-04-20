package com.virtuslab.gitmachete.frontend.graph.print;

import com.intellij.vcs.log.graph.PrintElement;
import com.intellij.vcs.log.graph.api.elements.GraphElement;
import com.intellij.vcs.log.graph.api.printer.PrintElementManager;
import com.intellij.vcs.log.graph.impl.print.elements.PrintElementWithGraphElement;
import org.checkerframework.checker.index.qual.NonNegative;

public class RightEdgePrintElement extends PrintElementWithGraphElement implements PrintElement {
  protected RightEdgePrintElement(@NonNegative int rowIndex,
      @NonNegative int positionInCurrentRow,
      GraphElement graphElement,
      PrintElementManager printElementManager) {
    super(rowIndex, positionInCurrentRow, graphElement, printElementManager);
  }
}