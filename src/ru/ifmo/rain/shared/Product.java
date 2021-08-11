package ru.ifmo.rain.shared;

import java.util.*;

public class Product<T extends Enum<T>, A extends Attribute<T>> {
    public final T left;
    public final List<T> right;
    public final boolean epsilon;
    public AttributeAction<T, A> action;

    @SafeVarargs
    public Product(T left, T... right) {
        this.left = left;
        this.right = new ArrayList<>();
        if (right.length == 0) { this.epsilon = true; }
        else { this.epsilon = false; Collections.addAll(this.right, right); }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product<?, ?> product = (Product<?, ?>) o;
        return left == product.left && Objects.equals(right, product.right) && epsilon == product.epsilon;
    }

    @Override
    public int hashCode() { return Objects.hash(left, right, epsilon); }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(left.toString() + " ->");
        for (T token : right) { str.append(" ").append(token.toString()); }
        return str.toString();
    }
}