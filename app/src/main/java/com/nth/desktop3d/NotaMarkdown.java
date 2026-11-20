package com.nth.desktop3d;

import java.util.ArrayList;
import java.util.List;

/** Small, literal-first Markdown block parser. No HTML, remote images or executable content. */
final class NotaMarkdown {
    static final class Block {
        final String kind,text,label;
        Block(String kind,String text,String label){this.kind=kind;this.text=text;this.label=label;}
    }
    static List<Block> blocks(String input){
        List<Block> blocks=new ArrayList<>();StringBuilder paragraph=new StringBuilder(),code=new StringBuilder();
        boolean fenced=false,codeLine=false;String marker="",language="";
        String[] lines=input.replace("\r\n","\n").replace('\r','\n').split("\n",-1);
        for(int index=0;index<lines.length;index++){String line=lines[index];
            String trim=line.trim();
            if(!fenced&&(trim.startsWith("```")||trim.startsWith("~~~"))){flush(blocks,paragraph);marker=trim.substring(0,3);language=trim.substring(3).trim();fenced=true;codeLine=false;continue;}
            if(fenced){if(trim.startsWith(marker)&&trim.substring(3).trim().isEmpty()){blocks.add(new Block("code",code.toString(),language));code.setLength(0);fenced=false;}else{if(codeLine)code.append('\n');code.append(line);codeLine=true;}continue;}
            if(trim.contains("|")&&index+1<lines.length&&lines[index+1].trim().matches("\\|?\\s*:?-{3,}:?\\s*(\\|\\s*:?-{3,}:?\\s*)+\\|?")){
                flush(blocks,paragraph);StringBuilder table=new StringBuilder(trim);index++;while(index+1<lines.length&&!lines[index+1].trim().isEmpty()&&lines[index+1].contains("|"))table.append('\n').append(lines[++index].trim());blocks.add(new Block("table",table.toString(),""));continue;
            }
            if(trim.isEmpty()){flush(blocks,paragraph);continue;}
            if(trim.matches("#{1,6} .+")){flush(blocks,paragraph);int level=trim.indexOf(' ');blocks.add(new Block("heading",trim.substring(level+1),String.valueOf(level)));continue;}
            if(trim.startsWith("> ")){flush(blocks,paragraph);blocks.add(new Block("quote",trim.substring(2),""));continue;}
            if(trim.matches("[-*_]{3,}")){flush(blocks,paragraph);blocks.add(new Block("rule","",""));continue;}
            if(trim.matches("[-*+] .+")||trim.matches("\\d+[.)] .+")){flush(blocks,paragraph);blocks.add(new Block("list",trim.replaceFirst("^[-*+] ","• "),""));continue;}
            if(paragraph.length()>0)paragraph.append('\n');paragraph.append(line);
        }
        if(fenced)blocks.add(new Block("code",code.toString(),language));flush(blocks,paragraph);return blocks;
    }
    private static void flush(List<Block> blocks,StringBuilder paragraph){if(paragraph.length()>0){blocks.add(new Block("text",paragraph.toString(),""));paragraph.setLength(0);}}
}
