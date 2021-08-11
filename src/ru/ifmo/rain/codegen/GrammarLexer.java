package ru.ifmo.rain.codegen;

import ru.ifmo.rain.regex.RegexLexer;
import ru.ifmo.rain.regex.RegexToken;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GrammarLexer {
    private final char[] content;
    protected int p = 0, lines = 0;
    protected GrammarToken token;
    protected String lexeme;

    private static Set<Character> regexEnd = new HashSet<>();

    static {
        for (char c : "_abcdefghijklmnopqrstuvwxyz$\r\n".toCharArray()) { regexEnd.add(c); }
    }

    private static Map<String, GrammarToken> keyWords = Map.ofEntries(
            Map.entry("@grammar", GrammarToken.NAME),
            Map.entry("#grammar", GrammarToken.GR_IMPORT),
            Map.entry("@attr", GrammarToken.ATTR),
            Map.entry("#attr", GrammarToken.ATTR_IMPORT),
            Map.entry("@start", GrammarToken.START),
            Map.entry("@skip", GrammarToken.SKIP)
    );

    protected GrammarLexer(String content) {
        this.content = (content + '$').toCharArray();
    }

    public void nextLexeme() throws ParseException {
        token = null; lexeme = null;
        while (Character.isWhitespace(content[p])) {
            if (checkWindowsNewline() || checkLinuxNewline()) {
                token = GrammarToken.NEWLINE; lines++; return;
            }
            else { p++; }
        }
        int start = p;
        switch (content[start]) {
            case '[':
            case '(':
            case '\'':
            case '\\': parseRegex(); return;
            case '@':
            case '#': parseKeyWord(); return;
            case '{': parseCodeBlock(); return;
            case '-': parseArrow(); return;
            case '\n': {
                token = GrammarToken.NEWLINE;
                lines++; return;
            }
            case '$': token = GrammarToken.END; return;
        }
        if (content[start] == '_' || Character.isLowerCase(content[start])) {
            token = GrammarToken.SNAKE_CASE; ++p;
            while (content[p] == '_' || Character.isLowerCase(content[p]) || Character.isDigit(content[p])) { p++; }
            lexeme = new String(content, start, p - start);
        }
    }

    private boolean checkWindowsNewline() {
        if (content[p] == '\n') { p++; return true; }
        else { return false; }
    }

    private boolean checkLinuxNewline() {
        if (content[p] == '\r' && content[p + 1] == '\n') { p += 2; return true; }
        else { return false; }
    }

    public String getLine() {
        int start = p;
        while (!(checkWindowsNewline() || checkLinuxNewline())) { p++; }
        lexeme = new String(content, start, p - start).strip();
        token = GrammarToken.NEWLINE;
        return lexeme;
    }

    private void parseKeyWord() throws ParseException {
        assert content[p] == '@' || content[p] == '#';
        int start = p++;
        while (Character.isLowerCase(content[p])) { p++; }
        String keyWord = new String(content, start, p - start);
        token = keyWords.get(keyWord);
        if (token == null) { throw new ParseException("Unknown key word: " + keyWord + " [" + lines + ']', p); }
    }

    private void parseCodeBlock() throws ParseException {
        assert content[p] == '{';
        int scope = 1, start = ++p, startLine = lines;
        while(p < content.length && scope > 0) {
            if (content[++p] == '\n') { lines++;}
            if (content[p] == '}') { scope--; }
            if (content[p] == '{') { scope++; }
        }
        if (p == content.length) { throw new ParseException("Failed to finish code block [" + startLine + ']', p); }
        this.token = GrammarToken.CODE;
        this.lexeme = new String(content, start, p++ - start).strip();
    }

    private void parseArrow() throws ParseException {
        assert content[p] == '-';
        if (content[++p] == '>') { p++; token = GrammarToken.ARROW; }
        else { throw new ParseException("Expected arrow, got -" + content[p] + " [" + lines + ']', p); }
    }

    private void parseRegex() throws ParseException {
        int start = p, length = regexLength(p, false);
        lexeme = new String(content, start, length).strip();
        token = GrammarToken.REGEX;
        this.p += length;
    }

    private int regexLength(int start, boolean internal) throws ParseException {
        String rest = new String(content, start, content.length - start);
        Set<Character> end = internal? Set.of(')'): regexEnd;
        RegexLexer regexLexer = new RegexLexer(rest, end);
        regexLexer.nextAtom();
        while (regexLexer.atom.token != RegexToken.REGEX_END) {
            if (regexLexer.atom.token == RegexToken.REGEX) {
                regexLexer.p += regexLength(start + regexLexer.p, true) + 1;
            }
            regexLexer.nextAtom();
        }
        return regexLexer.p;
    }
}
