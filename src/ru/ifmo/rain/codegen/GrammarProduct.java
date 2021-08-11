package ru.ifmo.rain.codegen;

import java.util.List;
import java.util.Map;

public class GrammarProduct {
    protected final String left;
    protected final List<Map.Entry<GrammarToken, String>> right;
    protected final String action;

    protected GrammarProduct(String left, List<Map.Entry<GrammarToken, String>> right, String action) {
        this.left = left;
        this.right = right;
        this.action = action;
    }

    protected String adapt() {
        String adapted = action.strip().replace("$it", "p.left");
        for (int index = 0; index < right.size(); ++index) {
            adapted = adapted.replace("$" + index, "p.right.get(" + index + ")");
        }
        return adapted;
    }
}
