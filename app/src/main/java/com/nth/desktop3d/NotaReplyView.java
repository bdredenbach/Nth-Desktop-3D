package com.nth.desktop3d;

import android.content.*;
import android.graphics.Typeface;
import android.text.*;
import android.text.style.*;
import android.widget.*;
import java.util.regex.*;

/** Native selectable prose and scrollable code. Provider text is never interpreted as HTML. */
@android.annotation.SuppressLint("ViewConstructor")
final class NotaReplyView extends LinearLayout {
    TextView primary;
    NotaReplyView(Context c,String content){super(c);setOrientation(VERTICAL);for(NotaMarkdown.Block block:NotaMarkdown.blocks(content)){
        if(block.kind.equals("table")){HorizontalScrollView horizontal=new HorizontalScrollView(c);horizontal.setContentDescription("Comparison table; scroll horizontally for more columns");TableLayout table=new TableLayout(c);int rowIndex=0;for(String line:block.text.split("\n")){String trimmed=line.replaceFirst("^\\|","").replaceFirst("\\|$","");TableRow row=new TableRow(c);row.setBackground(NotaDesign.surface(c,rowIndex==0?0xFF303443:0xFF20242E,0));for(String cell:trimmed.split("\\|",-1)){TextView value=text(c,"",14,NotaDesign.TEXT);value.setText(inline(cell.trim()));value.setPadding(Ui.dp(c,12),Ui.dp(c,12),Ui.dp(c,12),Ui.dp(c,12));value.setTextIsSelectable(true);if(rowIndex==0)value.setTypeface(null,Typeface.BOLD);row.addView(value,new TableRow.LayoutParams(Ui.dp(c,168),-2));if(primary==null)primary=value;}table.addView(row);rowIndex++;}horizontal.addView(table);LayoutParams p=new LayoutParams(-1,-2);p.bottomMargin=Ui.dp(c,16);addView(horizontal,p);continue;}
        if(block.kind.equals("rule")){android.view.View line=new android.view.View(c);line.setBackgroundColor(NotaDesign.LINE);LayoutParams p=new LayoutParams(-1,Ui.dp(c,1));p.setMargins(0,Ui.dp(c,10),0,Ui.dp(c,14));addView(line,p);continue;}
        if(block.kind.equals("code")){LinearLayout box=Ui.column(c);box.setBackground(NotaDesign.surface(c,0xFF10131B,14));LinearLayout bar=Ui.row(c);TextView language=text(c,block.label.isEmpty()?"Code":block.label,12,NotaDesign.MUTED);language.setPadding(Ui.dp(c,12),0,0,0);bar.addView(language,new LayoutParams(0,-2,1));bar.addView(NotaDesign.action(c,"Copy code",()->copy(c,block.text)),new LayoutParams(-2,Ui.dp(c,44)));box.addView(bar);HorizontalScrollView scroll=new HorizontalScrollView(c);scroll.setFillViewport(true);TextView code=text(c,block.text,14,NotaDesign.TEXT);code.setTypeface(Typeface.MONOSPACE);code.setPadding(Ui.dp(c,12),0,Ui.dp(c,16),Ui.dp(c,14));code.setHorizontallyScrolling(true);code.setTextIsSelectable(true);scroll.addView(code);box.addView(scroll,new LayoutParams(-1,-2));LayoutParams p=new LayoutParams(-1,-2);p.bottomMargin=Ui.dp(c,14);addView(box,p);if(primary==null)primary=code;continue;}
        int size=block.kind.equals("heading")?(block.label.equals("1")?23:19):16;
        TextView text=text(c,"",size,NotaDesign.TEXT);text.setText(inline(block.text));text.setTextIsSelectable(true);text.setLineSpacing(Ui.dp(c,5),1.05f);
        if(block.kind.equals("heading"))text.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        if(block.kind.equals("quote")){text.setTextColor(NotaDesign.MUTED);text.setPadding(Ui.dp(c,12),Ui.dp(c,9),Ui.dp(c,12),Ui.dp(c,9));text.setBackground(NotaDesign.surface(c,0xFF202430,10));}
        LayoutParams p=new LayoutParams(-1,-2);p.bottomMargin=Ui.dp(c,block.kind.equals("list")?5:13);addView(text,p);if(primary==null)primary=text;
    }if(primary==null){primary=text(c,content,16,NotaDesign.TEXT);addView(primary);}}
    static TextView text(Context c,String value,int size,int color){TextView v=Ui.text(c,value,size,color);v.setShadowLayer(0,0,0,0);v.setIncludeFontPadding(false);return v;}
    static void copy(Context c,String value){ClipboardManager clipboard=(ClipboardManager)c.getSystemService(Context.CLIPBOARD_SERVICE);if(clipboard!=null)clipboard.setPrimaryClip(ClipData.newPlainText("Nth Nota AI reply",value));Toast.makeText(c,"Copied",Toast.LENGTH_SHORT).show();}
    static CharSequence inline(String value){
        SpannableStringBuilder result=new SpannableStringBuilder();Pattern tokens=Pattern.compile("`([^`\\n]+)`|\\*\\*([^*\\n]+)\\*\\*|__([^_\\n]+)__|\\*([^*\\n]+)\\*");Matcher matcher=tokens.matcher(value);int position=0;
        while(matcher.find()){result.append(value,position,matcher.start());int start=result.length();boolean code=matcher.group(1)!=null;String words=code?matcher.group(1):matcher.group(2)!=null?matcher.group(2):matcher.group(3)!=null?matcher.group(3):matcher.group(4);result.append(words);
            if(code){result.setSpan(new TypefaceSpan("monospace"),start,result.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);result.setSpan(new BackgroundColorSpan(0xFF343647),start,result.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);}else result.setSpan(new StyleSpan(matcher.group(4)!=null?Typeface.ITALIC:Typeface.BOLD),start,result.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);position=matcher.end();
        }result.append(value,position,value.length());return result;
    }
}
