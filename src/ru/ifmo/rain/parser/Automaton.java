package ru.ifmo.rain.parser;

import ru.ifmo.rain.shared.Attribute;
import ru.ifmo.rain.shared.Grammar;
import ru.ifmo.rain.shared.Product;

import java.util.*;

class Automaton<T extends Enum<T>, A extends Attribute<T>> {
    public static class State<T extends Enum<T>, A extends Attribute<T>> {
        public final Map<Item<T, A>, Item<T, A>> items;
        public final Map<T, State<T, A>> move;
        public final Map<T, Action> action;
        public final Set<T> current;
        public final int id;
        public boolean terminator;

        public State(Set<Item<T, A>> items, int id) {
            this.items = new HashMap<>();
            for (Item<T, A> item: items) { this.items.put(item, item); }
            this.id = id;
            this.move = new HashMap<>();
            this.action = new HashMap<>();
            this.current = new HashSet<>();
            this.terminator = false;
            for (Item<T, A> item : items) {
                if (!item.terminator) { current.add(item.next); }
                else { this.terminator = true; }
            }
        }

        public boolean merge(State<T, A> other) {
            boolean modified = false;
            for (Item<T, A> item : items.keySet()) {
                Set<T> otherLookAhead = other.items.get(item).lookAhead;
                if (item.lookAhead.addAll(otherLookAhead)) { modified = true; }
            }
            return modified;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            State<?, ?> lrState = (State<?, ?>) o;
            return Objects.equals(items, lrState.items);
        }

        @Override
        public int hashCode() { return Objects.hash(items); }

        @Override
        public String toString() { return String.valueOf(id); }
    }

    public final Grammar<T, A> grammar;
    public final State<T, A> initial;
    public final Map<State<T, A>, State<T, A>> canonicalCollection;

    public Automaton(Grammar<T, A> grammar) {
        this.grammar = grammar;
        this.canonicalCollection = new HashMap<>();
        Item<T, A> initialItem = new Item<>(grammar.products.get(0), 0, Item.set(null), grammar);
        this.initial = new State<>(closure(Set.of(initialItem)), 0);
        canonicalCollection.put(initial, initial);
        boolean update = true;
        while (update) {
            update = false;
            attempt:
            for (State<T, A> state : canonicalCollection.values()) {
                int id = canonicalCollection.size();
                for (T x : state.current) {
                    update = newState(state, x , new State<>(move(state.items.keySet(), x), id));
                    if (update) { break attempt; }
                }
            }
        }
    }

    private boolean newState(State<T, A> state, T x, State<T, A> candidate) {
        State<T, A> similar = canonicalCollection.get(candidate);
        boolean updated;
        if (similar != null) {
            state.move.put(x, similar);
            updated = similar.merge(candidate);
        }
        else {
            canonicalCollection.put(candidate, candidate);
            state.move.put(x, candidate);
            updated = true;
        }
        return updated;
    }

    private boolean newItem(Map<Item<T, A>, Item<T, A>> result, Item<T, A> candidate) {
        Item<T, A> similar = result.get(candidate);
        boolean updated;
        if (similar != null) { updated = similar.lookAhead.addAll(candidate.lookAhead); }
        else { result.put(candidate, candidate); updated = true; }
        return updated;
    }

    private Set<Item<T, A>> closure(Set<Item<T, A>> in) {
        Map<Item<T, A>, Item<T, A>> result = new HashMap<>();
        for (Item<T, A> item : in) { result.put(item, item); }
        boolean update = true;
        while (update) {
            update = false;
            attempt:
            for (Item<T, A> item : result.keySet()) {
                if (item.terminator || grammar.terminals.contains(item.next)) { continue; }
                for (Product<T, A> product : grammar.entries.get(item.next)) {
                    update = newItem(result, new Item<>(product, 0, item.afterNext(), grammar));
                    if (update) { break attempt; }
                }
            }
        }
        return result.keySet();
    }

    private Set<Item<T, A>> move(Set<Item<T, A>> items, T x) {
        Set<Item<T, A>> result = new HashSet<>();
        for (Item<T, A> item : items) {
            if (!item.terminator && item.next == x) { result.add(item.advance()); }
        }
        return closure(result);
    }
}