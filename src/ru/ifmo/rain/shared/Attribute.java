package ru.ifmo.rain.shared;

import java.util.Objects;

public abstract class Attribute<T extends Enum<T>> {
    public RuntimeProduct<T, ? extends Attribute<T>> tree;
    public T token;
    public String string;

    public Attribute(T token) { this.token = token; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attribute<?> that = (Attribute<?>) o;
        return Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() { return Objects.hash(token); }
}
