package com.nth.desktop3d;

/** Small deterministic arithmetic parser: no code evaluation or network access. */
public final class Expression {
    private final String text;
    private int cursor;
    private Expression(String text) { this.text = text.replace('×', '*').replace('÷', '/').replace('−', '-'); }
    public static double evaluate(String text) {
        if (text.length() > 200) throw new IllegalArgumentException("Expression is too long");
        Expression p = new Expression(text);
        double value = p.sum();
        p.spaces();
        if (p.cursor != p.text.length() || !Double.isFinite(value)) throw new IllegalArgumentException("Check the expression");
        return value;
    }
    private void spaces() { while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) cursor++; }
    private boolean take(char c) { spaces(); if (cursor < text.length() && text.charAt(cursor) == c) { cursor++; return true; } return false; }
    private double sum() { double v = product(); while (true) { if (take('+')) v += product(); else if (take('-')) v -= product(); else return v; } }
    private double product() { double v = atom(); while (true) { if (take('*')) v *= atom(); else if (take('/')) { double n = atom(); if (n == 0) throw new IllegalArgumentException("Cannot divide by zero"); v /= n; } else return v; } }
    private double atom() {
        if (take('+')) return atom();
        if (take('-')) return -atom();
        if (take('(')) { double v = sum(); if (!take(')')) throw new IllegalArgumentException("Close the bracket"); return v; }
        spaces(); int start = cursor;
        while (cursor < text.length() && (Character.isDigit(text.charAt(cursor)) || text.charAt(cursor) == '.')) cursor++;
        if (start == cursor) throw new IllegalArgumentException("Enter a number");
        try { return Double.parseDouble(text.substring(start, cursor)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Check the number"); }
    }
}
