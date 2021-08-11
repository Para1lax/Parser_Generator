package ru.ifmo.rain.calc;

public enum CalcItem {
	T_0, T_1, T_2, T_3, 
	T_4, T_5, __augment__, T_6, 
	__skip__, T_7, T_8, T_9, 
	T_10, expr, term, factor, 
	atom, T_11;

	@Override
	public String toString() {
		switch (this) {
			case T_0: return "'-'";
			case T_7: return "'**'";
			case T_8: return "'|'";
			case T_1: return "'+'";
			case T_2: return "'*'";
			case T_9: return "'pi'";
			case T_3: return "')'";
			case T_4: return "'('";
			case T_5: return "'e'";
			case T_10: return "[ \\r\\n]+";
			case T_6: return "[0-9]+ ('.' [0-9]+)?";
			case T_11: return "'/'";
			default: return this.name();
		}
	}
}