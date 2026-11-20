package com.nth.desktop3d;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

public class NotaMarkdownTest {
    @Test public void formatsAnAnswerWithoutLosingItsListsOrQuotes(){List<NotaMarkdown.Block> blocks=NotaMarkdown.blocks("## A plan\n\nA **small** start.\n\n- First step\n2. Second step\n\n> A useful thought");assertEquals(5,blocks.size());assertEquals("heading",blocks.get(0).kind);assertEquals("A **small** start.",blocks.get(1).text);assertEquals("• First step",blocks.get(2).text);assertEquals("2. Second step",blocks.get(3).text);assertEquals("quote",blocks.get(4).kind);}
    @Test public void copyableCodeKeepsWhitespaceAndMarkdownCharacters(){List<NotaMarkdown.Block> blocks=NotaMarkdown.blocks("```java\n\n  # literal\n\n**still code**\n```\nAfter");assertEquals("code",blocks.get(0).kind);assertEquals("java",blocks.get(0).label);assertEquals("\n  # literal\n\n**still code**",blocks.get(0).text);assertEquals("After",blocks.get(1).text);}
    @Test public void unfinishedCodeIsReadable(){List<NotaMarkdown.Block> blocks=NotaMarkdown.blocks("Before\n~~~text\n<not html>\nnext line");assertEquals(2,blocks.size());assertEquals("<not html>\nnext line",blocks.get(1).text);}
    @Test public void comparisonTablesKeepEveryCellAndFollowingProse(){List<NotaMarkdown.Block> blocks=NotaMarkdown.blocks("| Option | Cost |\n| --- | :---: |\n| A | Free |\n| B | Paid |\n\nChoose A.");assertEquals(2,blocks.size());assertEquals("table",blocks.get(0).kind);assertTrue(blocks.get(0).text.contains("| B | Paid |"));assertFalse(blocks.get(0).text.contains(":---:"));assertEquals("Choose A.",blocks.get(1).text);}
    @Test public void ordinaryPipesAndLiteralHtmlArePreserved(){List<NotaMarkdown.Block> blocks=NotaMarkdown.blocks("A | B\n<script>alert(1)</script>\n![image](https://example.com/a.png)");assertEquals(1,blocks.size());assertEquals("text",blocks.get(0).kind);assertTrue(blocks.get(0).text.contains("<script>"));assertTrue(blocks.get(0).text.contains("![image]"));}
}
