package ru.ifmo.rain.shared;

import java.util.ArrayList;
import java.util.List;

public class RuntimeProduct<T extends Enum<T>, A extends Attribute<T>> {
    public A left;
    public List<A> right;
    public int last;
    public AttributeAction<T, A> action;

    public RuntimeProduct(Product<T, A> origin, A left) {
        this.left = left;
        this.right = new ArrayList<>();
        for (int i = 0; i < origin.right.size(); ++i) { this.right.add(null); }
        this.last = origin.right.size() - 1;
        this.action = origin.action;
    }
}