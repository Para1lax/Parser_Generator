package ru.ifmo.rain.regex;

import java.util.*;

public class RegexNDFA {
    public static class State {
        public Map<Character, Set<RegexNDFA.State>> transitions;
        public String terminal = null;

        public State() { this.transitions = new HashMap<>(); }

        public void link(Character c, RegexNDFA.State state) {
            transitions.computeIfAbsent(c, x -> new HashSet<>()).add(state);
        }
    }

    public RegexNDFA.State start;
    public Set<RegexNDFA.State> terminators;
    public Set<RegexNDFA.State> states;

    public static RegexNDFA epsilon() {
        RegexNDFA automaton = new RegexNDFA();
        automaton.start.link(null, automaton.newState(true));
        return automaton;
    }

    public static RegexNDFA sequential(String value) {
        RegexNDFA automaton = new RegexNDFA();
        RegexNDFA.State current = automaton.start;
        for (char c : value.toCharArray()) {
            RegexNDFA.State next = automaton.newState(false);
            current.link(c, next);
            current = next;
        }
        automaton.terminators.add(current);
        return automaton;
    }

    public static RegexNDFA parallel(Set<Character> charset) {
        RegexNDFA automaton = new RegexNDFA();
        RegexNDFA.State last = automaton.newState(true);
        for (char c : charset) {
            RegexNDFA.State next = automaton.newState(false);
            automaton.start.link(c, next);
            next.link(null, last);
        }
        return automaton;
    }

    public static RegexNDFA concat(RegexNDFA first, RegexNDFA second) {
        RegexNDFA automaton = new RegexNDFA(first, second);
        automaton.start.link(null, first.start);
        RegexNDFA.State last = automaton.newState(true);
        first.terminators.forEach(terminator -> terminator.link(null, second.start));
        second.terminators.forEach(terminator -> terminator.link(null, last));
        return automaton;
    }

    public static RegexNDFA alter(RegexNDFA first, RegexNDFA second) {
        RegexNDFA automaton = new RegexNDFA(first, second);
        RegexNDFA.State last = automaton.newState(true);
        automaton.start.link(null, first.start);
        automaton.start.link(null, second.start);
        second.terminators.forEach(terminator -> terminator.link(null, last));
        first.terminators.forEach(terminator -> terminator.link(null, last));
        return automaton;
    }

    public static RegexNDFA join(RegexNDFA first, RegexNDFA second) {
        RegexNDFA automaton = new RegexNDFA(first, second);
        automaton.start.link(null, first.start);
        automaton.start.link(null, second.start);
        automaton.terminators.addAll(first.terminators);
        automaton.terminators.addAll(second.terminators);
        return automaton;
    }

    public static RegexNDFA wildcard(RegexNDFA automaton) {
        RegexNDFA klini = new RegexNDFA(automaton);
        klini.start.link(null, automaton.start);
        RegexNDFA.State next = klini.newState(true);
        automaton.terminators.forEach(terminator -> terminator.link(null, next));
        next.link(null, klini.start);
        klini.start.link(null, next);
        return klini;
    }

    public static RegexNDFA question(RegexNDFA automaton) {
        RegexNDFA optional = new RegexNDFA(automaton);
        optional.start.link(null, automaton.start);
        RegexNDFA.State next = optional.newState(true);
        optional.start.link(null, next);
        automaton.terminators.forEach(terminator -> terminator.link(null, next));
        return optional;
    }

    public static RegexNDFA plus(RegexNDFA automaton) {
        RegexNDFA multi = new RegexNDFA(automaton);
        RegexNDFA.State next = multi.newState(true);
        multi.start.link(null, automaton.start);
        automaton.terminators.forEach(terminator -> terminator.link(null, next));
        next.link(null, multi.start);
        return multi;
    }

    public static RegexNDFA amount(RegexNDFA automaton, List<Integer> v) {
        RegexNDFA counter = epsilon();
        for (int it = 0; it < v.get(0); ++it) {
            counter = concat(counter, (RegexNDFA) automaton.clone());
        }
        if (v.size() == 1) { return counter; }
        else if (v.get(1) == -1) { return concat(counter, wildcard(automaton)); }
        else {
            Set<RegexNDFA.State> terminators = counter.terminators;
            for (int it = v.get(0); it < v.get(1); ++it) {
                RegexNDFA clone = (RegexNDFA) automaton.clone();
                counter = concat(counter, clone);
                terminators.addAll(counter.terminators);
            }
            counter.terminators.addAll(terminators);
            return counter;
        }
    }

    private RegexNDFA(RegexNDFA... others) {
        this.terminators = new HashSet<>();
        this.states = new HashSet<>();
        this.start = newState(false);
        for (RegexNDFA automaton: others) { states.addAll(automaton.states); }
    }

    private RegexNDFA(RegexNDFA.State start, Set<State> states, Set<RegexNDFA.State> terminators) {
        this.start = start;
        this.states = states;
        this.terminators = terminators;
    }

    private State newState(boolean isTerminator) {
        State state = new State();
        this.states.add(state);
        if (isTerminator) { terminators.add(state); }
        return state;
    }

    public Set<RegexNDFA.State> closure(Set<RegexNDFA.State> states) {
        Set<RegexNDFA.State> closure = new HashSet<>(states);
        Queue<RegexNDFA.State> q = new ArrayDeque<>(closure);
        while (!q.isEmpty()) {
            RegexNDFA.State next = q.poll();
            for (RegexNDFA.State eps : next.transitions.getOrDefault(null, Collections.emptySet())) {
                if (closure.add(eps)) { q.offer(eps); }
            }
        }
        return closure;
    }

    @Override
    public Object clone() {
        Map<State, State> bijection = new HashMap<>();
        Set<State> clonedTerminators = new HashSet<>();
        for(State state: states) { bijection.put(state, new State()); }
        for(State state: states) {
            State cloned = bijection.get(state);
            for (Map.Entry<Character, Set<State>> move: state.transitions.entrySet()) {
                Set<State> clonedSet = new HashSet<>();
                for (State to: move.getValue()) { clonedSet.add(bijection.get(to)); }
                cloned.transitions.put(move.getKey(), clonedSet);
            }
            if (terminators.contains(state)) { clonedTerminators.add(cloned); }
        }
        return new RegexNDFA(bijection.get(start), new HashSet<>(bijection.values()), clonedTerminators);
    }
}
