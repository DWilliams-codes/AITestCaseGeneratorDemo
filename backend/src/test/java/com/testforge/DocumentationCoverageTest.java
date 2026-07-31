package com.testforge;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

class DocumentationCoverageTest {
  /** Ensures every declared Java method and constructor carries an intent-level Javadoc. */
  @Test
  void everyDeclaredJavaFunctionIsDocumented() throws Exception {
    List<String> missing = undocumentedMethods(javaSources());

    assertThat(missing).as("Java methods and constructors missing Javadocs").isEmpty();
  }

  /** Returns every maintained Java source file in the production and test source sets. */
  private List<Path> javaSources() throws IOException {
    List<Path> result = new ArrayList<>();
    for (Path root :
        List.of(Path.of("src/main/java"), Path.of("src/test/java"), Path.of("tools"))) {
      try (var files = Files.walk(root)) {
        files
            .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
            .forEach(result::add);
      }
    }
    return result.stream().sorted().toList();
  }

  /** Parses source declarations and reports the file, line, and name of undocumented methods. */
  private List<String> undocumentedMethods(List<Path> sources) throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertThat(compiler).as("Documentation checks require a full JDK").isNotNull();
    List<String> missing = new ArrayList<>();
    try (StandardJavaFileManager files =
        compiler.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8)) {
      JavacTask task =
          (JavacTask)
              compiler.getTask(
                  null,
                  files,
                  null,
                  List.of("-proc:none"),
                  null,
                  files.getJavaFileObjectsFromPaths(sources));
      DocTrees docs = DocTrees.instance(task);
      for (CompilationUnitTree unit : task.parse()) {
        collectUndocumentedMethods(unit, docs, missing);
      }
    }
    return missing;
  }

  /** Scans one compilation unit and records declarations without an attached Javadoc tree. */
  private void collectUndocumentedMethods(
      CompilationUnitTree unit, DocTrees docs, List<String> missing) {
    new TreePathScanner<Void, Void>() {
      /** Checks the current declaration before continuing into nested and anonymous types. */
      @Override
      public Void visitMethod(MethodTree method, Void unused) {
        if (docs.getDocCommentTree(getCurrentPath()) == null) {
          long position = docs.getSourcePositions().getStartPosition(unit, method);
          long line = position < 0 ? -1 : unit.getLineMap().getLineNumber(position);
          missing.add(unit.getSourceFile().getName() + ':' + line + " " + method.getName());
        }
        return super.visitMethod(method, unused);
      }
    }.scan(unit, null);
  }
}
