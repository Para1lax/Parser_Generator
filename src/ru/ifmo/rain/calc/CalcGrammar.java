package ru.ifmo.rain.calc;

import java.lang.*;
import java.util.*;
import ru.ifmo.rain.shared.*;

public class CalcGrammar extends Grammar<CalcItem, CalcAttribute> {
	public CalcGrammar() {
		super(
			EnumSet.of(CalcItem.T_0, CalcItem.T_7, CalcItem.T_8, 
			CalcItem.T_1, CalcItem.T_2, CalcItem.T_9, CalcItem.T_3, 
			CalcItem.T_4, CalcItem.T_5, CalcItem.T_10, CalcItem.T_6, 
			CalcItem.T_11)
		);
		this.setProducts();
		this.findFirst();
		this.findFollow();
	}

	@Override
	protected void setProducts() {
		create(new Product<>(CalcItem.__augment__,
				CalcItem.expr
		), p -> {
			p.left = p.right.get(0);
		});

		create(new Product<>(CalcItem.expr,
				CalcItem.expr, CalcItem.T_1, CalcItem.term
		), p -> {
			p.left.val = p.right.get(0).val + p.right.get(2).val;
		});

		create(new Product<>(CalcItem.expr,
				CalcItem.expr, CalcItem.T_0, CalcItem.term
		), p -> {
			p.left.val = p.right.get(0).val - p.right.get(2).val;
		});

		create(new Product<>(CalcItem.expr,
				CalcItem.term
		), p -> {
			p.left.val = p.right.get(0).val;
		});

		create(new Product<>(CalcItem.term,
				CalcItem.term, CalcItem.T_2, CalcItem.factor
		), p -> {
			p.left.val = p.right.get(0).val * p.right.get(2).val;
		});

		create(new Product<>(CalcItem.term,
				CalcItem.term, CalcItem.T_11, CalcItem.factor
		), p -> {
			if (p.right.get(2).val == 0.0) {
				throw new RuntimeException("Division by zero");
			}
			else {
				p.left.val = p.right.get(0).val / p.right.get(2).val;
			}
		});

		create(new Product<>(CalcItem.term,
				CalcItem.factor
		), p -> {
			p.left.val = p.right.get(0).val;
		});

		create(new Product<>(CalcItem.factor,
				CalcItem.factor, CalcItem.T_7, CalcItem.atom
		), p -> {
			p.left.val = Math.pow(p.right.get(0).val, p.right.get(2).val);
		});

		create(new Product<>(CalcItem.factor,
				CalcItem.atom
		), p -> {
			p.left.val = p.right.get(0).val;
		});

		create(new Product<>(CalcItem.atom,
				CalcItem.T_4, CalcItem.expr, CalcItem.T_3
		), p -> {
			p.left.val = p.right.get(0).val;
		});

		create(new Product<>(CalcItem.atom,
				CalcItem.T_8, CalcItem.expr, CalcItem.T_8
		), p -> {
			p.left.val = Math.abs(p.right.get(1).val);
		});

		create(new Product<>(CalcItem.atom,
				CalcItem.T_9
		), p -> {
			p.left.val = Math.PI;
		});

		create(new Product<>(CalcItem.atom,
				CalcItem.T_5
		), p -> {
			p.left.val = Math.E;
		});

		create(new Product<>(CalcItem.atom,
				CalcItem.T_6
		), p -> {
			p.left.val = Double.parseDouble(p.right.get(0).string);
		});

		create(new Product<>(CalcItem.__skip__,
				CalcItem.T_10
		), p -> {});
	}
}