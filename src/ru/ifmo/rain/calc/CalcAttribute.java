package ru.ifmo.rain.calc;

import java.lang.*;
import ru.ifmo.rain.shared.Attribute;

public class CalcAttribute extends Attribute<CalcItem> {
	public CalcAttribute(CalcItem token) { super(token); }

	double val = 0.0;
}