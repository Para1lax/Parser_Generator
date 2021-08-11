package ru.ifmo.rain.codegen;

import java.text.ParseException;
import java.util.*;

/**
 * root -> ((_gr | _gr_import | _attr | _attr_import | _start | _skip | product) __newline__)+
 * _gr -> "@grammar"  __lower__
 * _gr_import -> "#grammar" __line__
 * _attr -> "@attr" code
 * _attr_import -> "#attr" __line__
 * _start -> "@start" non_term
 * _skip -> "@skip" "->" regex
 * product -> left "->" right (_newline_ code)?
 * left -> non_term
 * right -> (non_term | regex)+
 * non_term -> __snake_case__
 */
public class GrammarParser {
    private final GrammarLexer lexer;
    public static final String AUGMENT = "__augment__";
    public static final String SKIP = "__skip__";

    protected String name, start, skips;

    protected final List<String> attributes = new ArrayList<>();
    protected final Map<String, GrammarToken> units = new HashMap<>();
    protected final List<GrammarProduct> products = new ArrayList<>();

    protected final Set<String> grammarImports = new HashSet<>();
    protected final Set<String> attributeImports = new HashSet<>();

    protected GrammarParser(String grammar) throws ParseException {
        this.lexer = new GrammarLexer(grammar);
        lexer.nextLexeme(); enterRoot();
        this.addSpecialRule();
    }

    private void getNewline() throws ParseException {
        lexer.nextLexeme();
        if (lexer.token != GrammarToken.NEWLINE) {
            throw new ParseException("Expected newline [" + lexer.lines + "]", lexer.lines);
        }
        lexer.nextLexeme();
    }

    private void addSpecialRule() {
        if (units.containsKey(AUGMENT)) { throw new RuntimeException("'" + AUGMENT + "' is not available for use"); }
        if (units.containsKey(SKIP)) { throw new RuntimeException("'" + SKIP + "' is not available for use"); }
        if (start == null) { start = products.get(0).left; }
        units.put(AUGMENT, GrammarToken.SNAKE_CASE);
        List<Map.Entry<GrammarToken, String>> right = List.of(Map.entry(GrammarToken.SNAKE_CASE, start));
        GrammarProduct augment = new GrammarProduct(AUGMENT, right, "$it = $0;");
        products.add(0, augment);
        if (skips != null) {
            units.put(SKIP, GrammarToken.SNAKE_CASE);
            units.put(skips, GrammarToken.REGEX);
            right = List.of(Map.entry(GrammarToken.REGEX, skips));
            GrammarProduct skipper = new GrammarProduct(SKIP, right, null);
            products.add(skipper);
        }
    }

    private void enterRoot() throws ParseException {
        while (lexer.token != GrammarToken.END) {
            switch (lexer.token) {
                case NAME: enterName(); break;
                case GR_IMPORT: grammarImports.add(lexer.getLine()); break;
                case ATTR: enterAttr(); break;
                case ATTR_IMPORT: attributeImports.add(lexer.getLine()); break;
                case START: enterStart(); break;
                case SKIP: enterSkip(); break;
                case SNAKE_CASE: enterProduct(); break;
                case NEWLINE: lexer.nextLexeme(); break;
            }
        }
    }


    private void enterName() throws ParseException {
        assert lexer.token == GrammarToken.NAME;
        lexer.nextLexeme();
        if (lexer.token == GrammarToken.SNAKE_CASE) {
            if (name != null) {
                throw new RuntimeException("Grammar name has been already specified: " + name);
            }
            else { this.name = lexer.lexeme; }
            getNewline();
        }
        else { throw new ParseException("Expected grammar name", lexer.lines); }
    }

    private void enterAttr() throws ParseException {
        assert lexer.token == GrammarToken.ATTR;
        lexer.nextLexeme();
        if (lexer.token == GrammarToken.CODE) { attributes.add(lexer.lexeme); }
        getNewline();
    }

    private void enterStart() throws ParseException {
        assert lexer.token == GrammarToken.START;
        lexer.nextLexeme();
        if (lexer.token == GrammarToken.SNAKE_CASE) {
            if (start == null) { start = lexer.lexeme; getNewline(); }
            else { throw new RuntimeException("Start non-terminal has been already specified: " + start); }
        }
        else { throw new ParseException("Expected non-terminal name [" + lexer.lines + ']', lexer.p); }
    }

    private void enterSkip() throws ParseException {
        assert lexer.token == GrammarToken.SKIP;
        lexer.nextLexeme();
        if (lexer.token != GrammarToken.ARROW) {
            throw new ParseException("Expected arrow [" + lexer.lines + ']', lexer.p);
        }
        lexer.nextLexeme();
        if (lexer.token == GrammarToken.REGEX) {
            if (skips == null) { skips = lexer.lexeme; getNewline();}
            else { throw new RuntimeException("Skip-regex has been already specified: " + skips); }
        }
        else { throw new ParseException("@skip should be regex [" + lexer.lines + ']', lexer.p); }
    }

    private void enterProduct() throws ParseException {
        assert lexer.token == GrammarToken.SNAKE_CASE;
        String left = lexer.lexeme;
        units.put(left, GrammarToken.SNAKE_CASE);
        lexer.nextLexeme();
        if (lexer.token != GrammarToken.ARROW) {
            throw new ParseException("Expected arrow [" + lexer.lines + ']', lexer.p);
        }
        List<Map.Entry<GrammarToken, String>> right = new ArrayList<>();
        lexer.nextLexeme();
        while (lexer.token != GrammarToken.NEWLINE) {
            if (lexer.token == GrammarToken.SNAKE_CASE || lexer.token == GrammarToken.REGEX) {
                units.put(lexer.lexeme, lexer.token);
                right.add(Map.entry(lexer.token, lexer.lexeme));
            }
            else { throw new ParseException("Expected regex or non-terminal [" + lexer.lines +']', lexer.p); }
            lexer.nextLexeme();
        }
        lexer.nextLexeme();
        if (lexer.token == GrammarToken.CODE) {
            products.add(new GrammarProduct(left, right, lexer.lexeme));
            getNewline();
        }
        else { products.add(new GrammarProduct(left, right, null));}
    }
}
