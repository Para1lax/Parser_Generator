package ru.ifmo.rain.shared;

import ru.ifmo.rain.regex.RegexNDFA;
import ru.ifmo.rain.regex.RegexParser;

import java.util.*;
import java.util.stream.Collectors;

public class LexerDFA<Token extends Enum<Token>> {
    public static class State<T extends Enum<T>> {
        public Set<RegexNDFA.State> equivalent;
        public Map<Character, State<T>> transitions;
        public Set<Character> terminals;
        public T token = null;

        public State() {
            this.equivalent = new HashSet<>();
            this.transitions = new HashMap<>();
            this.terminals = new HashSet<>();
        }

        public State(Set<RegexNDFA.State> equivalent, Map<String, T> tokenMap) {
            this.equivalent = equivalent;
            this.transitions = new HashMap<>();
            this.collectTerminals(tokenMap);
        }

        public void collectTerminals(Map<String, T> tokenMap) {
            this.terminals = equivalent.stream().map(state -> {
                if (state.terminal != null) { this.token = tokenMap.get(state.terminal); }
                return state.transitions.keySet();
            }).flatMap(Collection::stream).collect(Collectors.toSet());
            this.terminals.remove(null);
        }

        public boolean isTerminator() { return token != null; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            State<?> other = (State<?>) o;
            return isTerminator() == other.isTerminator() && Objects.equals(equivalent, other.equivalent);
        }

        @Override
        public int hashCode() { return Objects.hash(token, equivalent); }
    }

    public final State<Token> start;
    public final Map<State<Token>, State<Token>> states;

    public LexerDFA(RegexNDFA automaton, Map<String, Token> tokenMap) {
        this.start = new State<>(automaton.closure(Set.of(automaton.start)), tokenMap);
        this.states = new HashMap<>();
        states.put(start, start);
        Queue<State<Token>> statesQueue = new ArrayDeque<>();
        statesQueue.offer(start);
        while (!statesQueue.isEmpty()) {
            State<Token> current = statesQueue.poll();
            for (Character term : current.terminals) {
                State<Token> candidate = new State<>();
                current.equivalent.forEach(inner -> candidate.equivalent
                        .addAll(inner.transitions.getOrDefault(term, Collections.emptySet())));
                candidate.equivalent = automaton.closure(candidate.equivalent);
                candidate.collectTerminals(tokenMap);
                State<Token> similarState = states.get(candidate);
                if (similarState != null) { current.transitions.put(term, similarState); }
                else {
                    current.transitions.put(term, candidate);
                    states.put(candidate, candidate);
                    statesQueue.offer(candidate);
                }
            }
        }
    }
}