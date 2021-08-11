package ru.ifmo.rain.regex;

import ru.ifmo.rain.regex.RegexAtom;

import java.text.ParseException;
import java.util.*;

public class RegexLexer {
    private final char[] content;
    private final Set<Character> stop;

    public int p = 0;
    public RegexAtom atom;

    public static final String DIGITS = "0123456789";
    public static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    public static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";


    public RegexLexer(String content, Set<Character> until) {
        this.content = content.toCharArray();
        this.stop = until;
    }

    public void nextAtom() throws ParseException {
        if (atom != null && atom.token == RegexToken.REGEX_END) {
            throw new ParseException("Attempt to get over the lexer end", p);
        }
        while (content[p] == ' ') { p++; }
        if (stop.contains(content[p])) { atom = new RegexAtom(RegexToken.REGEX_END); return; }
        switch (content[p]) {
            case '[': atom = new RegexAtom(parseCharset()); break;
            case '\\': atom = new RegexAtom(String.valueOf(parseEscape())); break;
            case '\'': atom = new RegexAtom(parseLiteral()); break;
            case '?': atom = new RegexAtom(RegexToken.QUESTION); break;
            case '*': atom = new RegexAtom(RegexToken.WILDCARD); break;
            case '+': atom = new RegexAtom(RegexToken.PLUS); break;
            case '|': atom = new RegexAtom(RegexToken.ALT); break;
            case '{': atom = new RegexAtom(parseAmount()); break;
            case '(': atom = new RegexAtom(RegexToken.REGEX); break;
            default: throw new ParseException("Unexpected symbol: " + content[p], p);
        }
        p++;
    }

    private char parseEscape() throws ParseException {
        assert content[p] == '\\';
        switch (content[++p]) {
            case 'n': return '\n';
            case 't': return '\t';
            case 'r': return '\r';
            default: throw new ParseException("Not an escape sequence: \\" + content[p], p);
        }
    }

    private String getNeighboursCategory() {
        if (Character.isLowerCase(content[p - 1]) && Character.isLowerCase(content[p + 1])) { return LOWER; }
        else if (Character.isUpperCase(content[p - 1]) && Character.isUpperCase(content[p + 1])) { return UPPER; }
        else if (Character.isDigit(content[p - 1]) && Character.isDigit(content[p + 1])) { return DIGITS; }
        else { return null; }
    }

    private Set<Character> parseCharset() throws ParseException {
        assert content[p] == '[';
        Set<Character> charset = new HashSet<>();
        while (content[++p] != ']') {
            if (content[p] == '\\') { charset.add(parseEscape()); }
            else if (content[p] == '-') {
                String category = getNeighboursCategory();
                if (category != null) {
                    int from = category.lastIndexOf(content[p - 1]);
                    int to = category.lastIndexOf(content[p + 1]);
                    for (char c : category.substring(from, to + 1).toCharArray()) { charset.add(c); }
                }
                else { charset.add('-'); }
            }
            else { charset.add(content[p]);}
        }
        return charset;
    }

    private String parseLiteral() throws ParseException {
        assert content[p] == '\'';
        int start = ++p;
        while (content[p] != '\'') { p++; }
        if (p == start) { throw new ParseException("Expected non empty literal", start); }
        return new String(content, start, p - start);
    }

    private int getInt() throws ParseException {
        int start = p;
        while (Character.isDigit(content[p])) { p++;}
        if (start == p) { throw new ParseException("Expected '{'_num_(','_num_)?'}'", start); }
        String str = new String(content, start, p - start);
        try { return Integer.parseInt(str); }
        catch (NumberFormatException e) { throw new ParseException("Failed to parse int: " + str, start); }
    }


    private List<Integer> parseAmount() throws ParseException {
        assert content[p++] == '{';
        List<Integer> v = new ArrayList<>();
        v.add(getInt());
        if (content[p] == ',') {
            if (Character.isDigit(content[++p])) { v.add(getInt()); }
            else { v.add(-1); }
        }
        if (content[p] != '}') { throw new ParseException("Expected '{'_num_(','_num_)?'}'", p); }
        if (v.size() == 2 && v.get(1) != -1 && v.get(0) < v.get(1)) {
            throw new IllegalArgumentException("Invalid value given: {" + v.get(0) + "," + v.get(1)+ "}");}
        return v;
    }
}
