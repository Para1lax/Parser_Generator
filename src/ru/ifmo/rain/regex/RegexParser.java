package ru.ifmo.rain.regex;

import java.text.ParseException;
import java.util.Set;
import java.util.Stack;

/**
 *  regex -> alt
 *  alt -> cat ("|" cat)*  ; FIRST = (, [, ', \
 *  cat -> main+  ; FIRST = (, [, ', \
 *  main -> core ("*" | "+" | "?" | amount)  ; FIRST = (, [, ', \
 *  atom -> "(" regex ")" | charset | literal  ; FIRST = (, [, ', \
 *  amount -> "{" (_num_ | (_num_',') | (_num_','_num_)) "}"  ; FIRST = {
 *  charset -> "[" (_char_ | range | escape)* "]"  ; FIRST = [
 *  literal -> "'" _any_* "'" | escape  ; FIRST = '
 *  range -> (_lower_ "-" _lower_) | (_upper_ "-" _upper_) | (_digit_ "-" _digit_)
 *  escape -> "\" _letter_
 */
public class RegexParser {
    public final String regex;
    private final RegexLexer lexer;

    private final Stack<RegexNDFA> builder = new Stack<>();

    public RegexParser(String regex, Set<Character> until) throws ParseException {
        this.lexer = new RegexLexer(regex, until);
        lexer.nextAtom(); enterAlt();
        this.regex = regex.substring(0, lexer.p).strip();
        assert builder.size() == 1;
    }

    public RegexParser(String regex) throws ParseException {
        this.regex = regex;
        this.lexer = new RegexLexer(regex + '$', Set.of('$'));
        lexer.nextAtom(); enterAlt();
        assert builder.size() == 1;
    }

    public RegexNDFA getAutomaton() { return builder.peek(); }

    private void enterRegex() throws ParseException {
        RegexParser internalRegex = new RegexParser(this.regex.substring(lexer.p), Set.of(')'));
        builder.push(internalRegex.getAutomaton());
        lexer.atom.regex = internalRegex;
        this.lexer.p += internalRegex.lexer.p + 1;
    }

    private void enterAlt() throws ParseException {
        enterCat();
        while (lexer.atom.token != RegexToken.REGEX_END) {
            if (lexer.atom.token == RegexToken.ALT) {
                RegexNDFA prev = builder.pop();
                lexer.nextAtom(); enterCat();
                builder.push(RegexNDFA.alter(prev, builder.pop()));
            }
            else { throw new ParseException("Expected '|'", lexer.p); }
        }
    }

    private void enterCat() throws ParseException {
        enterMain();
        while (lexer.atom.token != RegexToken.REGEX_END && lexer.atom.token != RegexToken.ALT) {
            RegexNDFA prev = builder.pop(); enterMain();
            builder.push(RegexNDFA.concat(prev, builder.pop()));
        }
    }

    private void enterMain() throws ParseException {
        enterAtom(); lexer.nextAtom();
        if (lexer.atom.isModifier()) {
            RegexNDFA base = builder.pop();
            switch (lexer.atom.token) {
                case QUESTION: builder.push(RegexNDFA.question(base)); break;
                case WILDCARD: builder.push(RegexNDFA.wildcard(base)); break;
                case PLUS: builder.push(RegexNDFA.plus(base)); break;
                case AMOUNT: builder.push(RegexNDFA.amount(base, lexer.atom.amount)); break;
                default: throw new IllegalStateException("Unexpected token " + lexer.atom.token);
            }
            lexer.nextAtom();
        }
    }

    private void enterAtom() throws ParseException {
        switch (lexer.atom.token) {
            case REGEX: enterRegex(); break;
            case LITERAL: builder.push(RegexNDFA.sequential(lexer.atom.literal)); break;
            case CHARSET: builder.push(RegexNDFA.parallel(lexer.atom.charset)); break;
            default: throw new ParseException("Expected base atom, got " + lexer.atom.token, lexer.p);
        }
    }

    public static void main(String[] args) throws ParseException {
        RegexParser p = new RegexParser("([0-9]+('.'[0-9]+)?) | [a-f0-9]*");
        System.out.println(p);
    }
}
