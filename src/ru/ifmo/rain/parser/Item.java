package ru.ifmo.rain.parser;

import ru.ifmo.rain.shared.Attribute;
import ru.ifmo.rain.shared.Grammar;
import ru.ifmo.rain.shared.Product;

import java.util.*;
import java.util.stream.Collectors;

class Item<T extends Enum<T>, A extends Attribute<T>> {
    public final Product<T, A> product;
    public final int pointer;
    public final boolean terminator;
    public final T next;
    public final boolean nextTerminal;
    public final Set<T> lookAhead;
    private final List<T> followers;
    private final Grammar<T, A> grammar;

    public Item(Product<T, A> product, int pointer, Set<T> lookAhead, Grammar<T, A> grammar) {
        this.product = product; this.pointer = pointer; this.grammar = grammar;
        this.terminator = pointer == product.right.size();
        this.next = (terminator)? null : product.right.get(pointer);
        this.nextTerminal = (!terminator) && grammar.terminals.contains(next);
        this.lookAhead = new HashSet<>(lookAhead);
        this.followers = terminator? null : new ArrayList<>(product.right.subList(pointer + 1, product.right.size()));
    }

    public Set<T> afterNext() {
        if (terminator) { return Collections.emptySet(); }
        Set<T> result = new HashSet<>();
        int lastIndex = followers.size();
        for (T ahead: lookAhead) {
            followers.add(ahead);
            result.addAll(grammar.first(followers).getKey());
            followers.remove(lastIndex);
        }
        return result;
    }

    public static <Token extends Enum<Token>>  Set<Token> set(Token x) {
        Set<Token> result = new HashSet<>();
        result.add(x); return result;
    }

    public Item<T, A> advance() {
        return new Item<>(product, pointer + 1, lookAhead, grammar);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item<?, ?> other = (Item<?, ?>) o;
        return pointer == other.pointer && Objects.equals(product, other.product);
    }

    @Override
    public int hashCode() { return Objects.hash(product, pointer, terminator); }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(product.left.toString() + " ->");
        int index = 0;
        for (; index < pointer; ++index) { str.append(" ").append(product.right.get(index).toString()); }
        str.append(" .");
        for (; index < product.right.size(); ++index) { str.append(" ").append(product.right.get(index).toString()); }
        str.append(" ,  ").append(lookAhead.stream().map(x -> (x == null)? "$" : x.toString())
                .collect(Collectors.joining(" / ")));
        return str.toString();
    }
}