package com.nth.desktop3d;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Offline command interpretation is separate from cloud prose; network replies never execute. */
public final class SettingsCommands {
    public static final class Command {
        public final String id;public final Integer value;public final String page;
        Command(String id,Integer value,String page){this.id=id;this.value=value;this.page=page;}
    }
    private static boolean has(String text,String... words){for(String word:words)if(text.contains(word))return true;return false;}
    public static Command parse(String input){
        String s=input.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+"," ").replaceAll("[.!?]+$","");
        String[] words={"zero","one","two","three","four","five","six","seven","eight","nine","ten"};for(int i=0;i<words.length;i++)s=s.replaceAll("\\b"+words[i]+"\\b",String.valueOf(i));
        if(s.matches("(please )?(undo|undo that|undo last change|revert last change)"))return new Command("undo",null,null);
        if(s.matches(".*\\b(find|open|go to|where)\\b.*")||(s.contains("show ")&&s.contains("settings"))){
            String page=has(s,"folder")?"folders":has(s,"dock")?"dock":has(s,"drawer","all apps")?"drawer":has(s,"grid","home screen","desktop")?"home":has(s,"glass","transparent","opacity","appearance","look")?"look":has(s,"gesture")?"gestures":has(s,"feed","discover")?"feed":has(s,"backup","restore")?"backup":has(s,"card")?"cards":has(s,"scene","tilt","motion")?"motion":has(s,"search","nota")?"search":null;
            if(page!=null)return new Command(null,null,page);
        }
        String card=has(s,"media card","music card")?"card_media":has(s,"weather card")?"card_weather":has(s,"calendar card")?"card_calendar":has(s,"photo card","photos card")?"card_photos":has(s,"reading card")?"card_reading":has(s,"notes card")?"card_notes":null;
        String id=card!=null?card:has(s,"row height","row spacing")?"grid_row_dp":has(s,"column","grid")?"grid_columns":has(s,"icon size","icons larger","icons bigger","icons smaller")?"icon_size":has(s,"transparency","transparent")?"transparency":has(s,"feather","fade width")?"glass_feather":has(s,"opacity strength","opacity")?"glass_strength":has(s,"adaptive glass","fading menus","adaptive menus")?"adaptive_glass":has(s,"reduced motion","reduce motion")?"reduced_motion":has(s,"tilt","parallax")?"tilt":has(s,"labels","label")?"desktop_labels":has(s,"dock")?"show_dock":null;
        if(id==null||s.matches("^(how|why|what|explain)\\b.*"))return null;
        boolean action=has(s,"set ","change ","switch ","make ","turn ","enable ","disable ","hide ","show ","increase ","decrease ","reduce ");
        if(!action)return null;
        if(id.startsWith("card_")||has(id,"tilt","labels","show_dock","reduced_motion","adaptive_glass")){
            boolean off=has(s,"off","disable","hide","without");boolean on=has(s,"on","enable","show","reduce motion");if(!off&&!on)return null;return new Command(id,off?0:1,null);
        }
        Matcher number=Pattern.compile("(?<![\\d.])-?\\d+(?![\\d.])").matcher(s);if(number.find()){try{return new Command(id,Integer.parseInt(number.group()),null);}catch(NumberFormatException invalid){return null;}}
        if(id.equals("icon_size")&&has(s,"larger","bigger","increase"))return new Command(id,Integer.MIN_VALUE+1,null);
        if(id.equals("icon_size")&&has(s,"smaller","decrease"))return new Command(id,Integer.MIN_VALUE+2,null);
        return null;
    }
}
