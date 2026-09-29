import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts a Mojang ProGuard mapping file (client.txt) into a tiny v2 mapping file so that
 * tiny-remapper can apply it.
 *
 * <p>ProGuard lines look like:
 * <pre>
 *   net.minecraft.network.chat.Component -&gt; sw:
 *       net.minecraft.network.chat.Style getStyle() -&gt; a
 *       net.minecraft.network.chat.ComponentContents getContents() -&gt; b
 * </pre>
 *
 * <p>The output puts the <b>obfuscated</b> namespace first and writes member descriptors in
 * obfuscated names, because tiny-remapper resolves a member mapping by looking the call site up
 * using the descriptor of the namespace it reads. Namespace 0 therefore has to be the namespace
 * the input classes are actually in.
 *
 * <pre>
 *   tiny 2 0 obfuscated mojang
 *   c   sw   net/minecraft/network/chat/Component
 *       m   ()Lsx;   b   getStyle
 *       m   ()Lsx;   b   getContents
 * </pre>
 */
public final class ProguardToTiny {
    private static final Map<String, String> PRIMITIVES = new HashMap<>();

    static {
        PRIMITIVES.put("byte", "B");
        PRIMITIVES.put("char", "C");
        PRIMITIVES.put("double", "D");
        PRIMITIVES.put("float", "F");
        PRIMITIVES.put("int", "I");
        PRIMITIVES.put("long", "J");
        PRIMITIVES.put("short", "S");
        PRIMITIVES.put("boolean", "Z");
        PRIMITIVES.put("void", "V");
    }

    /** mojang internal name -&gt; obfuscated internal name, for every class the file mentions. */
    private static final Map<String, String> CLASS_NAMES = new HashMap<>();

    public static void main(String[] args) throws IOException {
        Path in = Path.of(args[0]);
        Path out = Path.of(args[1]);

        collectClassNames(in);

        try (BufferedReader reader = Files.newBufferedReader(in, StandardCharsets.UTF_8);
             PrintWriter writer = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {

            writer.println("tiny\t2\t0\tobfuscated\tmojang");

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }

                if (!Character.isWhitespace(line.charAt(0))) {
                    int arrow = line.indexOf(" -> ");

                    if (arrow < 0) {
                        continue;
                    }

                    String mojang = internalName(line.substring(0, arrow).trim());
                    String obf = internalName(stripTrailingColon(line.substring(arrow + 4).trim()));

                    writer.println("c\t" + obf + "\t" + mojang);
                } else {
                    String member = stripLineNumbers(line.trim());
                    int arrow = member.indexOf(" -> ");

                    if (arrow < 0) {
                        continue;
                    }

                    String left = member.substring(0, arrow).trim();
                    String obfName = member.substring(arrow + 4).trim();

                    if (left.endsWith(")")) {
                        writeMethod(writer, left, obfName);
                    } else {
                        writeField(writer, left, obfName);
                    }
                }
            }
        }
    }

    /** First pass: class names are needed to rewrite member types, and they may appear later. */
    private static void collectClassNames(Path in) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(in, StandardCharsets.UTF_8)) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#' || Character.isWhitespace(line.charAt(0))) {
                    continue;
                }

                int arrow = line.indexOf(" -> ");

                if (arrow < 0) {
                    continue;
                }

                CLASS_NAMES.put(internalName(line.substring(0, arrow).trim()),
                        internalName(stripTrailingColon(line.substring(arrow + 4).trim())));
            }
        }
    }

    private static void writeMethod(PrintWriter writer, String left, String obfName) {
        int paren = left.indexOf('(');

        if (paren < 0) {
            return;
        }

        String name = left.substring(0, paren).trim();
        int space = name.lastIndexOf(' ');

        if (space < 0) {
            return;
        }

        String returnType = name.substring(0, space).trim();
        name = name.substring(space + 1).trim();

        List<String> params = new ArrayList<>();
        String raw = left.substring(paren + 1, left.length() - 1).trim();

        if (!raw.isEmpty()) {
            for (String part : raw.split(",")) {
                params.add(descriptor(part.trim()));
            }
        }

        StringBuilder desc = new StringBuilder("(");

        for (String param : params) {
            desc.append(param);
        }

        desc.append(')').append(descriptor(returnType));

        writer.println("m\t" + desc + "\t" + obfName + "\t" + name);
    }

    private static void writeField(PrintWriter writer, String left, String obfName) {
        int space = left.indexOf(' ');

        if (space < 0) {
            return;
        }

        String type = left.substring(0, space).trim();
        String name = left.substring(space + 1).trim();

        writer.println("f\t" + descriptor(type) + "\t" + obfName + "\t" + name);
    }

    /** Optional ProGuard line-number prefix, e.g. {@code 51:51:} or {@code 9:9:10:10:}. */
    private static final java.util.regex.Pattern LINE_NUMBERS =
            java.util.regex.Pattern.compile("^(?:\\d+:\\d*:)+");

    /** Drops the optional {@code 10:10:} ProGuard line-number prefix. */
    private static String stripLineNumbers(String line) {
        return LINE_NUMBERS.matcher(line).replaceFirst("").trim();
    }

    private static String stripTrailingColon(String name) {
        return name.endsWith(":") ? name.substring(0, name.length() - 1).trim() : name;
    }

    private static String internalName(String name) {
        return name.replace('.', '/');
    }

    /** Rewrites a ProGuard type into an obfuscated JVM descriptor. */
    private static String descriptor(String type) {
        int dimensions = 0;

        while (type.endsWith("[]")) {
            dimensions++;
            type = type.substring(0, type.length() - 2).trim();
        }

        String base = PRIMITIVES.get(type);

        if (base == null) {
            String internal = internalName(type);
            base = "L" + CLASS_NAMES.getOrDefault(internal, internal) + ";";
        }

        return "[".repeat(dimensions) + base;
    }
}
