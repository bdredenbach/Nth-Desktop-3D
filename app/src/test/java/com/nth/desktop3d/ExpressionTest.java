package com.nth.desktop3d;

import org.junit.Test;
import static org.junit.Assert.*;

public class ExpressionTest {
    @Test public void respectsPrecedenceAndBrackets(){assertEquals(14,Expression.evaluate("2 × (3 + 4)"),0);assertEquals(11,Expression.evaluate("3+4*2"),0);}
    @Test public void handlesUnaryMinusAndDecimal(){assertEquals(-1.5,Expression.evaluate("-(3 ÷ 2)"),0);}
    @Test(expected=IllegalArgumentException.class) public void rejectsDivisionByZero(){Expression.evaluate("1/0");}
    @Test(expected=IllegalArgumentException.class) public void rejectsPartialInput(){Expression.evaluate("2 +");}
    @Test(expected=IllegalArgumentException.class) public void rejectsCode(){Expression.evaluate("System.exit(0)");}
}
