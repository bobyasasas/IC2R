import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import javax.lang.model.element.Modifier;
import javax.tools.ToolProvider;

/** Parses declarations without resolving the obsolete Forge class path. */
public final class RegistryScanner {
    public static void main(String[] args) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var files = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            var task = (JavacTask) compiler.getTask(null, files, null,
                    List.of("-proc:none"), null, files.getJavaFileObjects(args));
            for (var unit : task.parse()) {
                new TreePathScanner<Void, Void>() {
                    @Override
                    public Void visitVariable(VariableTree node, Void unused) {
                        if (node.getModifiers().getFlags().contains(Modifier.STATIC)
                                && node.getInitializer() instanceof MethodInvocationTree call
                                && (call.getMethodSelect().toString().startsWith("register")
                                    || call.getMethodSelect().toString().equals("create"))) {
                            if (!(call.getArguments().getFirst() instanceof LiteralTree literal)
                                    || !(literal.getValue() instanceof String id)) {
                                throw new IllegalStateException("Nonliteral registration: " + node.getName());
                            }
                            var expression = call.getArguments().size() > 1
                                    ? call.getArguments().get(1).toString() : "";
                            System.out.println(String.join("\t", unit.getSourceFile().getName(),
                                    node.getName().toString(), call.getMethodSelect().toString(), id,
                                    Base64.getEncoder().encodeToString(expression.getBytes(StandardCharsets.UTF_8))));
                        }
                        return super.visitVariable(node, unused);
                    }
                }.scan(unit, null);
            }
        }
    }
}
