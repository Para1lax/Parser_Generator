package ru.ifmo.rain.calc;

import ru.ifmo.rain.shared.Lexer;
import java.text.ParseException;
import ru.ifmo.rain.regex.RegexParser;

public class CalcLexer extends Lexer<CalcItem, CalcAttribute> {
	public CalcLexer() throws ParseException {
		regexMap.put(CalcItem.T_0, new RegexParser("'-'"));
		regexMap.put(CalcItem.T_7, new RegexParser("'**'"));
		regexMap.put(CalcItem.T_8, new RegexParser("'|'"));
		regexMap.put(CalcItem.T_1, new RegexParser("'+'"));
		regexMap.put(CalcItem.T_2, new RegexParser("'*'"));
		regexMap.put(CalcItem.T_9, new RegexParser("'pi'"));
		regexMap.put(CalcItem.T_3, new RegexParser("')'"));
		regexMap.put(CalcItem.T_4, new RegexParser("'('"));
		regexMap.put(CalcItem.T_5, new RegexParser("'e'"));
		regexMap.put(CalcItem.T_10, new RegexParser("[ \r\n]+"));
		regexMap.put(CalcItem.T_6, new RegexParser("[0-9]+ ('.' [0-9]+)?"));
		regexMap.put(CalcItem.T_11, new RegexParser("'/'"));
		this.build();
	}

	@Override
	public CalcAttribute makeAttribute(CalcItem token) {
		return new CalcAttribute(token);
	}
}