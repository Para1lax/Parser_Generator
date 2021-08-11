package ru.ifmo.rain.shared;

@FunctionalInterface
public interface AttributeAction<T extends Enum<T>, A extends Attribute<T>> {
    void calculate(RuntimeProduct<T, A> product);
}
