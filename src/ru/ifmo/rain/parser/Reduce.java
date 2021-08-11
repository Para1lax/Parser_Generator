package ru.ifmo.rain.parser;

import ru.ifmo.rain.shared.Attribute;
import ru.ifmo.rain.shared.Product;
import ru.ifmo.rain.shared.RuntimeProduct;

import java.util.Objects;

class Reduce<T extends Enum<T>, A extends Attribute<T>> implements Action {
    public RuntimeProduct<T, A> runtimeProduct;
    public final Product<T, A> product;
    private final Parser<T, A> parser;
    public boolean accept = false;

    public Reduce(Product<T, A> product, Parser<T, A> parser) {
        this.parser = parser; this.product = product;
    }

    @Override
    public void perform() {
        this.runtimeProduct = new RuntimeProduct<>(product, parser.lexer.makeAttribute(product.left));
        this.runtimeProduct.left.tree = runtimeProduct;
        for (int i = runtimeProduct.last; i >= 0; --i) {
            runtimeProduct.right.set(i, parser.viablePrefix.pop());
            parser.states.pop();
        }
        runtimeProduct.action.calculate(runtimeProduct);
        Automaton.State<T, A> current = parser.states.peek();
        parser.viablePrefix.push(runtimeProduct.left);
        parser.states.push(current.move.get(product.left));
        parser.accepted = this.accept;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reduce<?, ?> that = (Reduce<?, ?>) o;
        return Objects.equals(product, that.product);
    }

    @Override
    public int hashCode() { return Objects.hash(product); }
}
