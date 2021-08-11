package ru.ifmo.rain.shared;

import ru.ifmo.rain.codegen.GrammarParser;
import ru.ifmo.rain.regex.RegexNDFA;
import ru.ifmo.rain.regex.RegexParser;

import java.text.ParseException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class Lexer<T extends Enum<T>, A extends Attribute<T>> {
    protected Map<T, RegexParser> regexMap;
    public LexerDFA<T> automaton;
    public T skip;
    public char[] input;
    public int p;

    public Lexer() { this.regexMap = new HashMap<>(); }

    public A nextToken() {
        while (true) {
            LexerDFA.State<T> current = automaton.start;
            Map.Entry<LexerDFA.State<T>, Integer> lastTerminal = null;
            A attribute = null;
            int oldPosition = p;
            for (; p != input.length; ++p) {
                assert current != null;
                current = current.transitions.get(input[p]);
                if (current == null) { attribute = recall(lastTerminal, oldPosition); break; }
                else if (current.isTerminator()) { lastTerminal = Map.entry(current, p + 1); }
            }
            if (p == input.length) { attribute = recall(lastTerminal, oldPosition); }
            if (attribute != null && attribute.token == skip) { continue; }
            return attribute;
        }
    }

    private A recall(Map.Entry<LexerDFA.State<T>, Integer> last, int oldPosition) {
        if (last == null) { return null; }
        p = last.getValue();
        A attribute = makeAttribute(last.getKey().token);
        attribute.token = last.getKey().token;
        attribute.string = new String(input, oldPosition, p - oldPosition);
        return attribute;
    }

    public Deque<A> tokenise(String input) throws ParseException {
        this.input = input.toCharArray();
        this.p = 0;
        Deque<A> tokenStream = new ArrayDeque<>();
        do {
            A next = nextToken();
            if (next == null) {
                throw new ParseException("ERROR: Unexpected char: '" + this.input[p] + "' (" + p + ")", p);
            }
            tokenStream.offerLast(next);
        } while (p != this.input.length);
        tokenStream.offerLast(makeAttribute(null));
        return tokenStream;
    }

    public void setSkipToken(Grammar<T, A> grammar) {
        int last = grammar.products.size() - 1;
        T left = grammar.products.get(last).left;
        if (left.name().equals(GrammarParser.SKIP)) {
            this.skip = grammar.products.get(last).right.get(0);
        }
    }

    protected void build() {
        regexMap.values().forEach(p -> p.getAutomaton().terminators.forEach(t -> t.terminal = p.regex));
        RegexNDFA full = regexMap.values().stream().map(RegexParser::getAutomaton).reduce(RegexNDFA::join).orElseThrow();
        Map<String, T> tokenMap = regexMap.entrySet().stream().
                collect(Collectors.toMap(pair -> pair.getValue().regex, Map.Entry::getKey));
        this.automaton = new LexerDFA<>(full, tokenMap);
    }

    public abstract A makeAttribute(T token);
}
