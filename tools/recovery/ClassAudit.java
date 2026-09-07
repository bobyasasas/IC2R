import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.*;

/** Compare code and signatures without source lines, local names, frames or pool ordering. */
public class ClassAudit {
    public static void main(String[] args) throws Exception {
        Map<String,String> names = new HashMap<>();
        if (!args[0].equals("-")) {
            for (String line : Files.readAllLines(Path.of(args[0]))) {
                String[] p = line.split("\t");
                if (p.length == 2) names.put(p[0], p[1]);
            }
        }
        Remapper remapper = new Remapper() {
            public String mapMethodName(String owner, String name, String desc) { return names.getOrDefault(name, name); }
            public String mapFieldName(String owner, String name, String desc) { return names.getOrDefault(name, name); }
            public String mapInvokeDynamicMethodName(String name, String desc) { return names.getOrDefault(name, name); }
        };
        Path input = Path.of(args[1]), output = Path.of(args[2]);
        Map<String,byte[]> classes = new TreeMap<>();
        if (Files.isDirectory(input)) {
            try (var paths = Files.walk(input)) {
                for (Path p : paths.filter(p -> p.toString().endsWith(".class")).toList())
                    classes.put(input.relativize(p).toString(), Files.readAllBytes(p));
            }
        } else {
            try (ZipFile zip = new ZipFile(input.toFile())) {
                for (ZipEntry e : Collections.list(zip.entries()))
                    if (e.getName().endsWith(".class")) classes.put(e.getName(), zip.getInputStream(e).readAllBytes());
            }
        }
        Files.createDirectories(output);
        try (ZipOutputStream remapped = new ZipOutputStream(Files.newOutputStream(output.resolve("classes.jar")))) {
            for (var entry : classes.entrySet()) {
                ClassReader reader = new ClassReader(entry.getValue());
                ClassWriter writer = new ClassWriter(0);
                reader.accept(new ClassRemapper(writer, remapper), 0);
                byte[] mapped = writer.toByteArray();
                remapped.putNextEntry(new ZipEntry(entry.getKey()));
                remapped.write(mapped);
                remapped.closeEntry();
                ClassNode node = new ClassNode();
                new ClassReader(mapped).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                node.fields.sort(Comparator.comparing(f -> f.name + f.desc));
                node.methods.sort(Comparator.comparing(m -> m.name + m.desc));
                node.innerClasses.sort(Comparator.comparing(i -> i.name));
                if (node.nestMembers != null) Collections.sort(node.nestMembers);
                for (MethodNode m : node.methods) { m.maxStack = 0; m.maxLocals = 0; }
                Path dest = output.resolve(entry.getKey().replace(".class", ".asm"));
                if (!dest.toAbsolutePath().normalize().startsWith(output.toAbsolutePath().normalize()))
                    throw new IOException("Unsafe class entry: " + entry.getKey());
                Files.createDirectories(dest.getParent());
                try (PrintWriter api = new PrintWriter(Files.newBufferedWriter(Path.of(dest.toString().replace(".asm", ".api"))))) {
                    api.println(node.access + " " + node.name + " " + node.superName + " " + node.signature + " " + node.interfaces);
                    for (FieldNode f : node.fields) api.println("FIELD " + f.access + " " + f.name + " " + f.desc + " " + f.signature + " " + f.value);
                    for (MethodNode m : node.methods) api.println("METHOD " + m.access + " " + m.name + " " + m.desc + " " + m.signature + " " + m.exceptions);
                }
                try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(dest))) {
                    node.accept(new TraceClassVisitor(out));
                }
            }
        }
        System.out.println("Audited " + classes.size() + " classes into " + output);
    }
}
