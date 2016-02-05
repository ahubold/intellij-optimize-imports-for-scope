package de.hubold.intellij.optimize;

import com.intellij.psi.search.SearchScope;
import org.jetbrains.annotations.Nullable;

public interface OptimizeImportsOptions {

  @Nullable
  SearchScope getSearchScope();

  @Nullable
  String getFileTypeMask();

}
