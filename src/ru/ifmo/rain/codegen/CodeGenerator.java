package ru.ifmo.rain.codegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeGenerator {
    private final GrammarParser grammar;
    private final String packageName;
    private final Path dir;

    private final String tokenClass, attrClass;
    private final String lexClass, grClass;

    private static final String LINE = System.lineSeparator();
    private static final int TOKENS_IN_ROW = 4;

    private static final Set<String> LEXER_IMPORTS = Set.of(
            "ru.ifmo.rain.regex.RegexParser",
            "ru.ifmo.rain.shared.Lexer",
            "java.text.ParseException"
    );

    private final Map<String, String> tokenMap = new HashMap<>();
    private static final Pattern tabs = Pattern.compile("\t+");

    public CodeGenerator(String grammar, String packageName, Path dir) throws IOException, ParseException {
        this.grammar = new GrammarParser(grammar);
        this.packageName = packageName + '.' + this.grammar.name;
        this.dir = Path.of(dir.toString(), this.grammar.name);
        if (!Files.exists(this.dir)) { Files.createDirectory(this.dir); }
        char[] name = this.grammar.name.toCharArray();
        name[0] = Character.toUpperCase(name[0]);
        String camelName = new String(name);
        tokenClass = camelName + "Item";
        attrClass = camelName + "Attribute";
        lexClass = camelName + "Lexer";
        grClass = camelName + "Grammar";

        this.tokenGen(mkFile(tokenClass));
        this.attributeGen(mkFile(attrClass));
        this.lexerGen(mkFile(lexClass));
        this.grammarGen(mkFile(grClass));
    }

    private Path mkFile(String className) throws IOException {
        Path newFile = Path.of(dir.toString(), className + ".java");
        Files.deleteIfExists(newFile);
        Files.createFile(newFile);
        return newFile;
    }

    private void tokenGen(Path path) throws IOException {
        StringBuilder enums = new StringBuilder();
        enums.append("package ").append(packageName).append(";").append(LINE.repeat(2));
        enums.append("public enum ").append(tokenClass).append(" {");
        int counter = 0, terminals = 0;
        for (Map.Entry<String, GrammarToken> unit : grammar.units.entrySet()) {
            if (counter++ % TOKENS_IN_ROW == 0) { enums.append(LINE).append("\t"); }
            String tokenName = unit.getKey();
            if (unit.getValue() == GrammarToken.REGEX) {
                tokenName = "T_" + terminals++;
                tokenMap.put(unit.getKey(), tokenName);
            }
            enums.append(tokenName).append(", ");
        }
        int lastIndex = enums.length() - 2;
        enums.replace(lastIndex, enums.length(), ";" + LINE.repeat(2));

        enums.append("\t").append("@Override").append(LINE);
        enums.append("\t").append("public String toString() {").append(LINE);
        enums.append("\t".repeat(2)).append("switch (this) {").append(LINE);
        for (Map.Entry<String, String> pair : tokenMap.entrySet()) {
            enums.append("\t".repeat(3)).append("case ").append(pair.getValue()).append(": return \"").
                    append(pair.getKey().replace("\\", "\\\\")).append("\";").append(LINE);
        }
        enums.append("\t".repeat(3)).append("default: return this.name();").append(LINE);
        enums.append("\t".repeat(2)).append("}").append(LINE);
        enums.append("\t").append("}").append(LINE).append("}");
        Files.writeString(path, enums);
    }

    private void grammarGen(Path path) throws IOException {
        StringBuilder grammar = new StringBuilder();
        grammar.append("package ").append(packageName).append(";").append(LINE.repeat(2));
        Set<String> imports = new HashSet<>(this.grammar.grammarImports);
        imports.add("ru.ifmo.rain.shared.*");
        imports.add("java.util.*");
        for (String importName: imports) {
            grammar.append("import ").append(importName).append(";").append(LINE);
        }
        grammar.append(LINE).append("public class ").append(grClass).append(" extends Grammar<").
                append(tokenClass).append(", ").append(attrClass).append("> {").append(LINE);

        grammar.append("\tpublic ").append(grClass).append("() {").append(LINE);
        grammar.append("\t".repeat(2)).append(this.callSuper());
        grammar.append("\t".repeat(2)).append("this.setProducts();").append(LINE);
        grammar.append("\t".repeat(2)).append("this.findFirst();").append(LINE);
        grammar.append("\t".repeat(2)).append("this.findFollow();").append(LINE);
        grammar.append("\t}").append(LINE.repeat(2));

        grammar.append("\t@Override").append(LINE);
        grammar.append("\tprotected void setProducts() {");
        grammar.append(this.createRules()).append("\t}").append(LINE).append("}");
        Files.writeString(path, grammar);
    }

    private String callSuper() {
        StringBuilder call = new StringBuilder();
        call.append("super(").append(LINE).append("\t".repeat(3)).append("EnumSet.of(");
        int counter = 1;
        for (Map.Entry<String, String> pair : tokenMap.entrySet()) {
            if (counter++ % TOKENS_IN_ROW == 0) { call.append(LINE).append("\t".repeat(3)); }
            call.append(tokenClass).append(".").append(pair.getValue()).append(", ");
        }
        int lastIndex = call.length() - 2;
        call.replace(lastIndex, call.length(), ")" + LINE + "\t\t);");
        return call.append(LINE).toString();
    }

    private String createRules() {
        StringBuilder rules = new StringBuilder();
        for (GrammarProduct rule : grammar.products) {
            rules.append(LINE).append("\t".repeat(2)).append("create(new Product<>(").append(tokenClass).
                    append(".").append(rule.left).append(",").append(LINE).append("\t".repeat(4));
            for (Map.Entry<GrammarToken, String> unit : rule.right) {
                String tokenName = unit.getValue();
                if (unit.getKey() == GrammarToken.REGEX) { tokenName = tokenMap.get(unit.getValue()); }
                rules.append(tokenClass).append(".").append(tokenName).append(", ");
            }
            int lastIndex = rules.length() - 2;
            rules.replace(lastIndex, rules.length(), LINE + "\t".repeat(2) + "), p -> {");
            if (rule.action == null) { rules.append("});").append(LINE); }
            else {
                String adapted = rule.action.strip().replace("$it", "p.left");
                for (int index = 0; index < rule.right.size(); ++index) {
                    adapted = adapted.replace("$" + index, "p.right.get(" + index + ")");
                }
                Matcher actionMatcher = tabs.matcher(adapted.replaceAll(" {4}", "\t"));
                StringBuffer actionBuilder = new StringBuffer();
                while (actionMatcher.find()) {
                    int tabsLength = actionMatcher.end() - actionMatcher.start();
                    actionMatcher.appendReplacement(actionBuilder, "\t".repeat(2 + tabsLength));;
                }
                actionMatcher.appendTail(actionBuilder);
                rules.append(LINE).append("\t".repeat(3)).append(actionBuilder.toString()).
                    append(LINE).append("\t".repeat(2)).append("});").append(LINE);
            }
        }
        return rules.toString();
    }

    private void attributeGen(Path path) throws IOException {
        StringBuilder attribute = new StringBuilder();
        attribute.append("package ").append(packageName).append(";").append(LINE.repeat(2));
        Set<String> imports = new HashSet<>(this.grammar.attributeImports);
        imports.add("ru.ifmo.rain.shared.Attribute");
        for (String importName : imports) {
            attribute.append("import ").append(importName).append(";").append(LINE);
        }
        attribute.append(LINE).append("public class ").append(attrClass).
                append(" extends Attribute<").append(tokenClass).append("> {").append(LINE);
        attribute.append("\t").append("public ").append(attrClass).append("(").
                append(tokenClass).append(" token) { super(token); }").append(LINE);

        for (String attr : grammar.attributes) { attribute.append(LINE).append("\t").append(attr).append(LINE); }
        Files.writeString(path, attribute.append("}"));
    }

    private void lexerGen(Path path) throws IOException {
        StringBuilder lex = new StringBuilder();
        lex.append("package ").append(packageName).append(";").append(LINE.repeat(2));
        for (String importName: LEXER_IMPORTS) {
            lex.append("import ").append(importName).append(";").append(LINE);
        }
        lex.append(LINE).append("public class ").append(lexClass).append(" extends Lexer<").
                append(tokenClass).append(", ").append(attrClass).append("> {").append(LINE);
        lex.append("\t").append("public ").append(lexClass).append("() throws ParseException {");
        for (Map.Entry<String, String> pair: tokenMap.entrySet()) {
            String tokenName = tokenClass + "." + pair.getValue();
            lex.append(LINE).append("\t".repeat(2)).append("regexMap.put(").append(tokenName)
                    .append(", new RegexParser(\"").append(pair.getKey()).append("\"));");
        }
        lex.append(LINE).append("\t".repeat(2)).append("this.build();").append(LINE);
        lex.append("\t}").append(LINE.repeat(2));
        lex.append("\t").append("@Override").append(LINE);
        lex.append("\t").append("public ").append(attrClass).append(" makeAttribute(").
                append(tokenClass).append(" token) {").append(LINE);
        lex.append("\t".repeat(2)).append("return new ").append(attrClass).
                append("(token);").append(LINE).append("\t}").append(LINE).append("}");
        Files.writeString(path, lex);
    }

    public static void main(String[] args) {
        try {
            Path grammarFile = Path.of(args[0]);
            String grammar = Files.readString(grammarFile);
            new CodeGenerator(grammar, args[1], grammarFile.getParent());
        }
        catch (ParseException | IOException e) { System.out.println(e.getMessage()); }
    }
}
