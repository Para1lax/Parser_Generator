package ru.ifmo.rain.parser;

import ru.ifmo.rain.calc.CalcAttribute;
import ru.ifmo.rain.calc.CalcGrammar;
import ru.ifmo.rain.calc.CalcLexer;
import ru.ifmo.rain.shared.Attribute;
import ru.ifmo.rain.shared.Grammar;
import ru.ifmo.rain.shared.Lexer;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.*;

public class Parser<T extends Enum<T>, A extends Attribute<T>> {
    protected Stack<Automaton.State<T, A>> states;
    protected Stack<A> viablePrefix;
    protected final Automaton<T, A> automaton;
    protected final Lexer<T, A> lexer;
    protected Deque<A> tokenStream;
    protected boolean accepted;

    public Parser(Class<? extends Grammar<T, A>> grammar, Class<? extends Lexer<T, A>> lexer) {
        try {
            Grammar<T, A> gram = grammar.getDeclaredConstructor().newInstance();
            this.lexer = lexer.getDeclaredConstructor().newInstance();
            this.lexer.setSkipToken(gram);
            this.automaton = new Automaton<>(gram);
        } catch (InstantiationException | InvocationTargetException |
                NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to create instances: " + e.getMessage());
        }
        Item<T, A> terminator = new Item<>(automaton.grammar.products.get(0),
                1, Item.set(null), automaton.grammar);
        for (Automaton.State<T, A> state : automaton.canonicalCollection.values()) {
            for (Item<T, A> item : state.items.keySet()) {
                if (!item.terminator && item.nextTerminal) {
                    Automaton.State<T, A> next = state.move.get(item.next);
                    Action prev = state.action.put(item.next, () -> {
                        states.push(next); viablePrefix.push(tokenStream.pollFirst());
                    });
                    checkShiftReduce(prev, state.id, item.next);
                }
                else if (item.terminator) {
                    Reduce<T, A> reduce = new Reduce<>(item.product, this);
                    if (item.equals(terminator)) { reduce.accept = true; }
                    for (T ahead : item.lookAhead) {
                        Action prev = state.action.put(ahead, reduce);
                        if (prev != null) { checkConflict(prev, reduce, state, item); }
                    }
                }
            }
        }
    }

    public A makeTree(String input) throws ParseException {
        states = new Stack<>();
        viablePrefix = new Stack<>();
        tokenStream = lexer.tokenise(input);
        states.push(automaton.initial);
        accepted = false;
        while (!accepted) {
            A lookAhead = tokenStream.peekFirst();
            Automaton.State<T, A> current = states.peek();
            assert lookAhead != null;
            Action action = current.action.get(lookAhead.token);
            if (action != null) { action.perform(); }
            else {
                int id = states.peek().id;
                assert tokenStream.peekFirst() != null;
                T token = tokenStream.peekFirst().token;
                throw new RuntimeException("@ No action on state " + id + " by " + token + " (" + lexer.p + ")");
            }
        }
        return viablePrefix.pop();
    }


    private void checkShiftReduce(Action prev, int id, T next) {
        if (prev instanceof Reduce) {
            throw new RuntimeException("@ Shift-Reduce conflict appears on state " + id + " via " + next);
        }
    }

    private void checkConflict(Action prev, Reduce<T, A> reduce, Automaton.State<T, A> state, Item<T, A> item) {
        if (!(prev instanceof Reduce)) {
            throw new RuntimeException("@ Shift-Reduce conflict on state " + state.id + " via " + item.next);
        }
        else if (!reduce.equals(prev)) {
            throw new RuntimeException(
                    "@ Reduce-Reduce conflict on state " + state.id + " by " + item.next +
                        System.lineSeparator() + ((Reduce<?, ?>) prev).product +
                        System.lineSeparator() + reduce.product + System.lineSeparator());
        }
    }

    public static void main(String[] args) throws ParseException {
        CalcAttribute root = new Parser<>(CalcGrammar.class, CalcLexer.class).
                makeTree("2**8 - 1 * 36 + |28-5|");
    }
}