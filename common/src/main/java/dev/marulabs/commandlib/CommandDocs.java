package dev.marulabs.commandlib;

import dev.marulabs.commandlib.CommandTree.ArgumentEntry;
import dev.marulabs.commandlib.CommandTree.BranchEntry;
import dev.marulabs.commandlib.CommandTree.CommandEntry;
import dev.marulabs.commandlib.CommandTree.OptionEntry;
import dev.marulabs.commandlib.CommandTree.PermissionEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Documents commands as Markdown or JSON: their usages, descriptions, aliases, arguments, options and the permission
 * nodes CommandLib generates for them with the given prefix. The nodes are those that registration with the same
 * prefix registers and checks (see {@code docs/permissions.md}).
 *
 * <pre>{@code
 * CommandDocs docs = CommandDocs.of("myplugin.command", new SpawnCommand(), new ConfigCommand());
 * Files.writeString(Path.of("commands.md"), docs.markdown());
 * Files.writeString(Path.of("permissions.json"), docs.permissionsJson());
 * }</pre>
 *
 * <p>Each call reads the commands again, so it reflects changes made to them since. Descriptions set with a function
 * depend on the sender and are left out. Platform registrations return their documentation with
 * {@code CommandLib#docs()}.
 */
public final class CommandDocs {
    private static final Pattern COLOR_CODE = Pattern.compile("(?i)§[0-9a-fk-orx]");

    private final String permissionPrefix;
    private final List<CommonCommand<?, ?>> commands;

    private CommandDocs(String permissionPrefix, List<CommonCommand<?, ?>> commands) {
        this.permissionPrefix = permissionPrefix;
        this.commands = commands;
    }

    /**
     * Documents the given top-level commands with the permission prefix they are registered with, such as
     * {@code myplugin.command}.
     *
     * @throws IllegalArgumentException if the prefix is empty
     */
    public static CommandDocs of(@NotNull String permissionPrefix,
                                 @NotNull Collection<? extends CommonCommand<?, ?>> commands) {
        Objects.requireNonNull(permissionPrefix);
        if (permissionPrefix.isEmpty()) {
            throw new IllegalArgumentException("permissionPrefix must not be empty");
        }
        commands.forEach(Objects::requireNonNull);
        return new CommandDocs(permissionPrefix, List.copyOf(commands));
    }

    /**
     * Same as {@link #of(String, Collection)}.
     */
    public static CommandDocs of(@NotNull String permissionPrefix,
                                 @NotNull CommonCommand<?, ?> command,
                                 @NotNull CommonCommand<?, ?>... commands) {
        List<CommonCommand<?, ?>> list = new ArrayList<>();
        list.add(command);
        Collections.addAll(list, commands);
        return of(permissionPrefix, list);
    }

    public String permissionPrefix() {
        return permissionPrefix;
    }

    /**
     * Returns a Markdown section for every command and child command, depth first. Each section is headed
     * {@code ## /route} and lists the description, aliases, permission, usages, arguments, options and examples.
     */
    public String markdown() {
        List<String> sections = new ArrayList<>();
        entries().forEach(x -> addSections(sections, x));
        return String.join("", sections)
                     .stripTrailing()
                + "\n";
    }

    /**
     * Returns the commands as a JSON object: {@code permissionPrefix} and a {@code commands} array, each command with
     * its {@code route}, {@code aliases}, {@code description}, {@code permission}, {@code executable},
     * {@code usages}, {@code examples}, {@code options}, {@code branches} (argument branches with their {@code arguments} and
     * {@code children}) and {@code children}.
     */
    public String json() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("permissionPrefix", permissionPrefix);
        root.put("commands",
                 entries().stream()
                          .map(CommandDocs::commandJson)
                          .collect(Collectors.toList()));
        return Json.write(root);
    }

    /**
     * Returns a Markdown table of every permission node: the node, its default, the route it guards and its
     * description. A route needs the nodes of every step on it.
     */
    public String permissionsMarkdown() {
        StringBuilder out = new StringBuilder();
        out.append("| Permission | Default | Route | Description |\n");
        out.append("| --- | --- | --- | --- |\n");
        for (PermissionEntry permission : permissions()) {
            out.append("| ")
               .append(code(permission.node))
               .append(" | ")
               .append(code(permission.defaultPermission.name()))
               .append(" | ")
               .append(code(permission.route))
               .append(" | ")
               .append(cell(permission.description))
               .append(" |\n");
        }
        return out.toString();
    }

    /**
     * Returns every permission node as a JSON object: {@code permissionPrefix} and a {@code permissions} array of
     * {@code node}, {@code default} ({@code ALL}, {@code OP} or {@code NONE}), {@code description}, {@code route} and
     * {@code kind} ({@code command}, {@code argument} or {@code option}).
     */
    public String permissionsJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("permissionPrefix", permissionPrefix);
        List<Object> permissions = new ArrayList<>();
        for (PermissionEntry permission : permissions()) {
            Map<String, Object> json = permissionJson(permission);
            json.put("route", permission.route);
            json.put("kind", permission.kind);
            permissions.add(json);
        }
        root.put("permissions", permissions);
        return Json.write(root);
    }

    /**
     * Returns the commands as indented text, one line per command, option and argument branch, with each permission
     * node and its default. It is the format of the testing artifacts' {@code CommandTester#commandTree()}.
     */
    public String tree() {
        return CommandTreeRenderer.render(entries());
    }

    /**
     * One line per permission node: the node, its default and its route.
     */
    String permissionLines() {
        StringBuilder out = new StringBuilder();
        for (PermissionEntry permission : permissions()) {
            out.append(permission.node)
               .append(" [")
               .append(permission.defaultPermission)
               .append("] ")
               .append(permission.route)
               .append('\n');
        }
        return out.toString();
    }

    int permissionCount() {
        return permissions().size();
    }

    List<String> labels() {
        List<String> labels = new ArrayList<>();
        for (CommonCommand<?, ?> command : commands) {
            labels.add(command.name());
            labels.addAll(command.aliases());
        }
        return labels;
    }

    private List<CommandEntry> entries() {
        return CommandTree.of(commands, permissionPrefix);
    }

    private List<PermissionEntry> permissions() {
        return CommandTree.permissions(entries());
    }

    private static void addSections(List<String> sections, CommandEntry command) {
        sections.add(section(command));
        for (BranchEntry branch : command.branches) {
            branch.children.forEach(x -> addSections(sections, x));
        }
        command.children.forEach(x -> addSections(sections, x));
    }

    private static String section(CommandEntry command) {
        StringBuilder out = new StringBuilder();
        out.append("## ")
           .append(command.route)
           .append("\n\n");
        String description = plain(command.description);
        if (!description.isEmpty()) {
            out.append(description)
               .append("\n\n");
        }
        if (!command.aliases.isEmpty()) {
            out.append("- Aliases: ")
               .append(command.aliases.stream()
                                      .map(CommandDocs::code)
                                      .collect(Collectors.joining(", ")))
               .append('\n');
        }
        out.append("- Permission: ")
           .append(code(command.permission.node))
           .append(" (default ")
           .append(code(command.permission.defaultPermission.name()))
           .append(")\n\n");

        List<Usage> usages = usages(command);
        if (usages.isEmpty()) {
            out.append(code(command.route))
               .append(" shows the help of its subcommands.\n\n");
        } else {
            out.append("| Usage | Description | Permission |\n");
            out.append("| --- | --- | --- |\n");
            for (Usage usage : usages) {
                out.append("| ")
                   .append(code(usage.text))
                   .append(" | ")
                   .append(cell(plain(usage.description)))
                   .append(" | ")
                   .append(permissionCell(usage.permission))
                   .append(" |\n");
            }
            out.append('\n');
        }

        Set<String> argumentRows = new LinkedHashSet<>();
        for (BranchEntry branch : command.branches) {
            for (ArgumentEntry argument : branch.arguments) {
                argumentRows.add("| " + code(argument.name) + " | " + code(argument.type) + " | "
                        + String.join(", ", argument.notes()) + " |");
            }
        }
        if (!argumentRows.isEmpty()) {
            out.append("Arguments:\n\n");
            out.append("| Argument | Type | Notes |\n");
            out.append("| --- | --- | --- |\n");
            argumentRows.forEach(x -> out.append(x)
                                         .append('\n'));
            out.append('\n');
        }

        if (!command.options.isEmpty()) {
            out.append("Options:\n\n");
            out.append("| Option | Value | Notes | Description |\n");
            out.append("| --- | --- | --- | --- |\n");
            for (OptionEntry option : command.options) {
                out.append("| ")
                   .append(option.spellings()
                                 .stream()
                                 .map(CommandDocs::code)
                                 .collect(Collectors.joining(", ")))
                   .append(" | ")
                   .append(option.valueName == null ? "" : code("<" + option.valueName + ">"))
                   .append(" | ")
                   .append(String.join(", ", optionNotes(option)))
                   .append(" | ")
                   .append(cell(plain(option.description)))
                   .append(" |\n");
            }
            out.append('\n');
        }

        if (!command.examples.isEmpty()) {
            out.append("Examples:\n\n");
            command.examples.forEach(x -> out.append("- ")
                                             .append(code(x))
                                             .append('\n'));
            out.append('\n');
        }
        return out.toString();
    }

    /**
     * The inputs that run the command: the command alone when it has an executor, and each argument branch that runs
     * at its end or has no child commands.
     */
    private static List<Usage> usages(CommandEntry command) {
        List<Usage> usages = new ArrayList<>();
        if (command.executable) {
            usages.add(new Usage(command.usagePrefix(), command.description, command.permission));
        }
        for (BranchEntry branch : command.branches) {
            if (branch.executable || branch.children.isEmpty()) {
                usages.add(new Usage(command.usagePrefix() + " " + branch.tags(),
                                     branch.description,
                                     branch.permission));
            }
        }
        return usages;
    }

    private static Map<String, Object> commandJson(CommandEntry command) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("name", command.name);
        json.put("route", command.route);
        json.put("aliases", command.aliases);
        json.put("description", plainOrNull(command.description));
        json.put("permission", permissionJson(command.permission));
        json.put("executable", command.executable);
        json.put("usages",
                 usages(command).stream()
                                .map(x -> x.text)
                                .collect(Collectors.toList()));
        json.put("examples", command.examples);
        json.put("options",
                 command.options.stream()
                                .map(CommandDocs::optionJson)
                                .collect(Collectors.toList()));
        json.put("branches",
                 command.branches.stream()
                                 .map(CommandDocs::branchJson)
                                 .collect(Collectors.toList()));
        json.put("children",
                 command.children.stream()
                                 .map(CommandDocs::commandJson)
                                 .collect(Collectors.toList()));
        return json;
    }

    private static Map<String, Object> branchJson(BranchEntry branch) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("route", branch.route);
        List<Object> arguments = new ArrayList<>();
        for (ArgumentEntry argument : branch.arguments) {
            Map<String, Object> argumentJson = new LinkedHashMap<>();
            argumentJson.put("name", argument.name);
            argumentJson.put("type", argument.type);
            argumentJson.put("optional", argument.optional);
            argumentJson.put("variadic", argument.variadic());
            if (argument.variadic()) {
                argumentJson.put("maxCount", argument.maxCount);
            }
            arguments.add(argumentJson);
        }
        json.put("arguments", arguments);
        json.put("description", plainOrNull(branch.description));
        json.put("permission", permissionJson(branch.permission));
        json.put("executable", branch.executable);
        json.put("children",
                 branch.children.stream()
                                .map(CommandDocs::commandJson)
                                .collect(Collectors.toList()));
        return json;
    }

    private static Map<String, Object> optionJson(OptionEntry option) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("name", option.names.get(0));
        json.put("aliases", option.names.subList(1, option.names.size()));
        json.put("shortName", option.shortName == null ? null : String.valueOf(option.shortName));
        json.put("value", option.valueName);
        json.put("required", option.required);
        json.put("maxOccurrences", option.maxOccurrences);
        json.put("description", plain(option.description));
        if (option.permission != null) {
            json.put("permission", permissionJson(option.permission));
        }
        return json;
    }

    private static List<String> optionNotes(OptionEntry option) {
        List<String> notes = new ArrayList<>(option.notes());
        if (option.permission != null) {
            notes.add("permission " + permissionCell(option.permission));
        }
        return notes;
    }

    private static Map<String, Object> permissionJson(PermissionEntry permission) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("node", permission.node);
        json.put("default", permission.defaultPermission.name());
        json.put("description", permission.description);
        return json;
    }

    private static String permissionCell(PermissionEntry permission) {
        return code(permission.node) + " (" + code(permission.defaultPermission.name()) + ")";
    }

    private static String plain(@Nullable String text) {
        return text == null ? "" : COLOR_CODE.matcher(text)
                                             .replaceAll("");
    }

    @Nullable
    private static String plainOrNull(@Nullable String text) {
        return text == null ? null : plain(text);
    }

    private static String code(String text) {
        return "`" + text.replace("|", "\\|") + "`";
    }

    private static String cell(String text) {
        return text.replace("|", "\\|")
                   .replace("\r", "")
                   .replace("\n", "<br>");
    }

    private static final class Usage {
        private final String text;
        private final @Nullable String description;
        private final PermissionEntry permission;

        private Usage(String text, @Nullable String description, PermissionEntry permission) {
            this.text = text;
            this.description = description;
            this.permission = permission;
        }
    }
}
