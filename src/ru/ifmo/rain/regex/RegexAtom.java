package ru.ifmo.rain.regex;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class RegexAtom {
    public RegexToken token;
    public String literal = null;
    public Set<Character> charset = null;
    public List<Integer> amount = null;
    public RegexParser regex = null;

    public static EnumSet<RegexToken> MODIFIERS =
            EnumSet.of(RegexToken.PLUS, RegexToken.QUESTION, RegexToken.WILDCARD, RegexToken.AMOUNT);

    public RegexAtom(RegexToken token) {
        this.token = token;
    }

    public RegexAtom(String literal) {
        this.literal = literal;
        this.token = RegexToken.LITERAL;
    }

    public RegexAtom(Set<Character> charset) {
        this.charset = charset;
        this.token = RegexToken.CHARSET;
    }

    public RegexAtom(List<Integer> amount) {
        this.amount = amount;
        this.token = RegexToken.AMOUNT;
    }

    public boolean isModifier() { return MODIFIERS.contains(token); }
}
