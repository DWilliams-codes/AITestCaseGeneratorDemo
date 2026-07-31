import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePathScanner;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Adds concise intent-level Javadocs to Java declarations that do not already document themselves. */
public final class AddJavaComments {
  private record Insertion(int offset, String text) {}

  /** Prevents instantiation because this class is a command-line source maintenance utility. */
  private AddJavaComments() {}

  /** Documents every Java source file beneath the directories supplied on the command line. */
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      throw new IllegalArgumentException("Provide at least one Java source directory.");
    }
    for (String argument : args) {
      for (Path source : javaSources(Path.of(argument))) {
        document(source);
      }
    }
  }

  /** Returns Java source files below a directory in stable path order. */
  private static List<Path> javaSources(Path root) throws IOException {
    try (var paths = Files.walk(root)) {
      return paths
          .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
          .sorted()
          .toList();
    }
  }

  /** Parses one compilation unit and inserts missing comments without changing executable code. */
  private static void document(Path sourcePath) throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw new IllegalStateException("A full JDK is required to document Java sources.");
    }
    String source = Files.readString(sourcePath, StandardCharsets.UTF_8);
    List<Insertion> insertions = new ArrayList<>();
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
                  files.getJavaFileObjects(sourcePath.toFile()));
      DocTrees docs = DocTrees.instance(task);
      SourcePositions positions = docs.getSourcePositions();
      for (CompilationUnitTree unit : task.parse()) {
        collectInsertions(source, unit, docs, positions, insertions);
      }
    }
    if (insertions.isEmpty()) {
      return;
    }
    String newline = source.contains("\r\n") ? "\r\n" : "\n";
    StringBuilder updated = new StringBuilder(source);
    insertions.stream()
        .sorted(Comparator.comparingInt(Insertion::offset).reversed())
        .forEach(insertion -> updated.insert(insertion.offset(), insertion.text() + newline));
    Files.writeString(sourcePath, updated, StandardCharsets.UTF_8);
  }

  /** Finds undocumented methods and constructors while retaining their enclosing type context. */
  private static void collectInsertions(
      String source,
      CompilationUnitTree unit,
      DocTrees docs,
      SourcePositions positions,
      List<Insertion> insertions) {
    new TreePathScanner<Void, Void>() {
      private final Deque<String> types = new ArrayDeque<>();

      /** Tracks the nearest named class so generated comments can describe constructor intent. */
      @Override
      public Void visitClass(ClassTree tree, Void unused) {
        String name = tree.getSimpleName().toString();
        types.push(name.isBlank() ? "anonymous type" : name);
        try {
          return super.visitClass(tree, unused);
        } finally {
          types.pop();
        }
      }

      /** Records a Javadoc insertion for each declaration that lacks a documentation comment. */
      @Override
      public Void visitMethod(MethodTree tree, Void unused) {
        if (docs.getDocCommentTree(getCurrentPath()) == null) {
          long rawPosition = positions.getStartPosition(unit, tree);
          if (rawPosition >= 0 && rawPosition <= Integer.MAX_VALUE) {
            int position = (int) rawPosition;
            int lineStart = lineStart(source, position);
            String indentation = source.substring(lineStart, position);
            if (!indentation.isBlank()) {
              indentation = leadingWhitespace(indentation);
            }
            String type = types.isEmpty() ? "the enclosing type" : types.peek();
            String comment =
                indentation + "/** " + describe(tree, type) + " */";
            insertions.add(new Insertion(lineStart, comment));
          }
        }
        return super.visitMethod(tree, unused);
      }
    }.scan(unit, null);
  }

  /** Returns the character offset at which the declaration's source line begins. */
  private static int lineStart(String source, int position) {
    int newline = source.lastIndexOf('\n', Math.max(0, position - 1));
    return newline < 0 ? 0 : newline + 1;
  }

  /** Extracts indentation while ignoring annotations or modifiers sharing the same source line. */
  private static String leadingWhitespace(String value) {
    int index = 0;
    while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
      index++;
    }
    return value.substring(0, index);
  }

  /** Produces a short, declaration-specific explanation suitable for maintained application code. */
  private static String describe(MethodTree method, String type) {
    String name = method.getName().toString();
    if ("<init>".equals(name)) {
      if (method.getParameters().isEmpty() && type.endsWith("Entity")) {
        return "Creates an empty " + type + " instance for the persistence framework.";
      }
      if (type.endsWith("Dtos") || type.endsWith("Properties")) {
        return "Prevents instantiation because " + type + " is a static utility namespace.";
      }
      if (method.getParameters().isEmpty() && method.getBody() != null) {
        return "Creates an empty " + type + " instance for framework-managed construction.";
      }
      return "Initializes " + type + " with its required collaborators and domain state.";
    }
    String lifecycle = lifecycleDescription(method);
    if (lifecycle != null) {
      return lifecycle;
    }
    if (hasAnnotation(method, "Test") || hasAnnotation(method, "ParameterizedTest")) {
      return "Covers the " + humanize(name) + " scenario.";
    }
    if ("main".equals(name)) {
      return "Starts the TestForge Spring Boot application.";
    }
    if (hasAnnotation(method, "Bean")) {
      return "Creates the Spring-managed " + humanize(name) + " component.";
    }
    if (name.startsWith("get") && name.length() > 3) {
      return "Returns the current " + humanize(name.substring(3)) + " value.";
    }
    if (name.startsWith("set") && name.length() > 3) {
      return "Updates the " + humanize(name.substring(3)) + " value.";
    }
    if (name.startsWith("is") && name.length() > 2) {
      return "Reports whether " + humanize(name.substring(2)) + ".";
    }
    if (name.startsWith("has") && name.length() > 3) {
      return "Reports whether the result " + humanize(name.substring(3)) + ".";
    }
    if (name.startsWith("find")) {
      return "Finds " + humanize(name.substring(4)) + " for the supplied criteria.";
    }
    if (name.startsWith("count")) {
      return "Counts " + humanize(name.substring(5)) + " matching the supplied criteria.";
    }
    if (name.startsWith("exists")) {
      return "Determines whether " + humanize(name.substring(6)) + ".";
    }
    if (name.startsWith("delete")) {
      return "Deletes " + humanize(name.substring(6)) + " from persistent storage.";
    }
    if (name.startsWith("save")) {
      return "Persists " + humanize(name.substring(4)) + " and returns its stored representation.";
    }
    if (name.startsWith("to") && name.length() > 2) {
      return "Maps the source data to " + humanize(name.substring(2)) + ".";
    }
    if (name.startsWith("from") && name.length() > 4) {
      return "Builds the result from " + humanize(name.substring(4)) + ".";
    }
    if (type.endsWith("Controller")) {
      return "Handles the authenticated HTTP request to " + humanize(name) + ".";
    }
    String action = actionDescription(name, type);
    if (action != null) {
      return action;
    }
    return "Executes the " + humanize(name) + " operation for " + type + ".";
  }

  /** Maps common application method names to explanations that communicate domain intent. */
  private static String actionDescription(String name, String type) {
    return switch (name) {
      case "create" ->
          type.endsWith("Entity")
              ? "Creates a new " + type + " initialized from the supplied domain values."
              : "Creates and persists a new domain resource from validated input.";
      case "update" ->
          type.endsWith("Entity")
              ? "Updates the entity's mutable domain state and modification timestamp."
              : "Applies a validated update while preserving concurrency guarantees.";
      case "archive" ->
          type.endsWith("Entity")
              ? "Marks the entity as archived and records its modification time."
              : "Archives the owned resource and records the state transition.";
      case "list" -> "Lists resources visible to the current owner using the requested page.";
      case "get" -> "Returns the owned resource identified by the request.";
      case "generate" -> "Generates structured manual test coverage from the requirement input.";
      case "validate" -> "Validates generated content before any result is persisted.";
      case "register" -> "Registers a new user and creates an authenticated session.";
      case "login" -> "Authenticates supplied credentials and creates a rotating session.";
      case "refresh" -> "Rotates a valid refresh token and returns a renewed session.";
      case "logout" -> "Revokes the active refresh token and clears the browser session.";
      case "approve" -> "Approves the selected test case and records the review decision.";
      case "reject" -> "Rejects the selected test case and records the review decision.";
      case "clean" -> "Normalizes optional text before it is compared or persisted.";
      case "next" -> "Allocates the next globally unique work-item number from the database.";
      case "requireOwned" ->
          "Loads the requested resource and verifies that it belongs to the current owner.";
      case "assertVersion" ->
          "Rejects stale updates by comparing the submitted and persisted entity versions.";
      case "markGenerated" ->
          "Moves the requirement into its generated or clarification-needed state.";
      case "sessionResponse" ->
          "Builds an authentication response and attaches the protected refresh cookie.";
      case "refreshCookie" -> "Builds the configured protected refresh-token cookie.";
      case "expiredRefreshCookie" -> "Builds a cookie that removes the browser refresh token.";
      case "providerName" -> "Returns the stable provider identifier stored with generation runs.";
      case "modelName" -> "Returns the model or engine identifier stored with generation runs.";
      case "doFilterInternal" ->
          "Propagates a safe correlation identifier through the current HTTP request.";
      default -> prefixDescription(name);
    };
  }

  /** Describes families of helper methods whose leading verb already communicates their role. */
  private static String prefixDescription(String name) {
    String[] verbs = {
      "assert", "build", "check", "compute", "detect", "estimate", "export", "handle",
      "hash", "infer", "map", "normalize", "parse", "read", "record", "require", "resolve",
      "serialize", "verify", "write"
    };
    for (String verb : verbs) {
      if (name.startsWith(verb) && name.length() > verb.length()) {
        return capitalize(verb)
            + "s "
            + humanize(name.substring(verb.length()))
            + " for the current operation.";
      }
    }
    return null;
  }

  /** Returns a concise explanation for JUnit lifecycle callbacks, or null for ordinary methods. */
  private static String lifecycleDescription(MethodTree method) {
    if (hasAnnotation(method, "BeforeEach")) {
      return "Rebuilds isolated fixtures before each test scenario.";
    }
    if (hasAnnotation(method, "AfterEach")) {
      return "Releases per-scenario fixtures after each test completes.";
    }
    if (hasAnnotation(method, "BeforeAll")) {
      return "Initializes resources shared by all scenarios in the test class.";
    }
    if (hasAnnotation(method, "AfterAll")) {
      return "Releases resources shared by all scenarios in the test class.";
    }
    return null;
  }

  /** Reports whether a declaration carries the requested annotation by simple or qualified name. */
  private static boolean hasAnnotation(MethodTree method, String requestedName) {
    for (AnnotationTree annotation : method.getModifiers().getAnnotations()) {
      String name = annotation.getAnnotationType().toString();
      if (name.equals(requestedName) || name.endsWith('.' + requestedName)) {
        return true;
      }
    }
    return false;
  }

  /** Converts a Java identifier into a readable lower-case phrase. */
  private static String humanize(String identifier) {
    String spaced =
        identifier
            .replace('_', ' ')
            .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
            .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
            .strip()
            .toLowerCase(Locale.ROOT);
    return spaced.isBlank() ? "the requested operation" : spaced;
  }

  /** Capitalizes a generated sentence fragment without changing the remaining characters. */
  private static String capitalize(String value) {
    return Character.toUpperCase(value.charAt(0)) + value.substring(1);
  }
}
