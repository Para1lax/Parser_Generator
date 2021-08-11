package ru.ifmo.rain.shared;

import java.util.*;

public abstract class Grammar<T extends Enum<T>, A extends Attribute<T>> {
    public List<Product<T, A>> products;
    public Map<T, Set<Product<T, A>>> entries;

    public Set<T> terminals;
    public Map<T, Boolean> nonTerminals;

    public Map<T, Set<T>> first;
    public Map<T, Set<T>> follow;

    protected abstract void setProducts();

    public Grammar(EnumSet<T> terminals) {
        this.terminals = new HashSet<>(terminals);
        this.terminals.add(null);
        EnumSet<T> nonTerminals = EnumSet.complementOf(terminals);
        this.nonTerminals = new HashMap<>();
        for (T nonTerminal : nonTerminals) { this.nonTerminals.put(nonTerminal, false); }
        this.entries = new HashMap<>();
        this.first = new HashMap<>();
        this.follow = new HashMap<>();

        for(T nonTerm : nonTerminals) {
            entries.put(nonTerm, new HashSet<>());
            first.put(nonTerm, new HashSet<>());
            follow.put(nonTerm, new HashSet<>());
        }
        this.products = new ArrayList<>();
    }

    protected void create(Product<T, A> product, AttributeAction<T, A> action) {
        product.action = action;
        products.add(product);
        entries.get(product.left).add(product);
    }

    private Set<T> set(T x) {
        Set<T> result = new HashSet<>();
        result.add(x); return result;
    }

    protected void findFirst() {
        for (T term : terminals) { first.put(term, set(term)); }
        boolean update = true;
        while (update) {
            update = false;
            for (Product<T, A> product : products) {
                Map.Entry<Set<T>, Boolean> toAdd = first(product.right);
                if (first.get(product.left).addAll(toAdd.getKey())) { update = true; }
                if (toAdd.getValue()) { if (!nonTerminals.put(product.left, true)) update = true; }
                if (update) { break; }
            }
        }
    }

    public Map.Entry<Set<T>, Boolean> first(List<T> seq) {
        int index = 0;
        Set<T> seqFirst = new HashSet<>();
        for (; index < seq.size(); ++index) {
            T right = seq.get(index);
            seqFirst.addAll(first.get(right));
            if (!nonTerminals.getOrDefault(right, false)) { break; }
        }
        return Map.entry(seqFirst, index == seq.size());
    }

    protected void findFollow() {
        follow.put(products.get(0).left, set(null));
        follow.put(products.get(0).right.get(0), set(null));
        boolean update = true;
        while (update) {
            update = false;
            for (Product<T, A> product : products) {
                for (int index = 0; index < product.right.size(); ++index) {
                    T right = product.right.get(index);
                    if (nonTerminals.containsKey(right)) {
                        Map.Entry<Set<T>, Boolean> next =
                                first(product.right.subList(index + 1, product.right.size()));
                        if (follow.get(right).addAll(next.getKey())) { update = true; }
                        if (next.getValue()) {
                            Set<T> followLeft = follow.get(product.left);
                            if (follow.get(right).addAll(followLeft)) { update = true; }
                        }
                    }
                }
                if (update) { break; }
            }
        }
    }
}
