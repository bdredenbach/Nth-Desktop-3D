package com.nth.desktop3d;
import org.junit.Test;
import static org.junit.Assert.*;
public class IconLabelTest {
    @Test public void longNamesUseEightCharacters(){assertEquals("5e Compa...",IconLabel.display("5e Companion App"));assertEquals("Calculat...",IconLabel.display("Calculator"));}
    @Test public void shortNamesAreUnchanged(){assertEquals("Calendar",IconLabel.display("Calendar"));assertEquals("",IconLabel.display(null));}
    @Test public void newlineCannotCreateASecondLine(){assertEquals("5e Compa...",IconLabel.display("5e Companion\nApp"));assertEquals("Nth Note...",IconLabel.display("Nth\tNotes"));}
    @Test public void emojiSurrogatesAreNotSplit(){assertEquals("😀😀😀😀😀😀😀😀...",IconLabel.display("😀😀😀😀😀😀😀😀😀"));}
}
