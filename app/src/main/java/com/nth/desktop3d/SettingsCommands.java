package com.nth.desktop3d;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Offline commands select explicit settings; cloud prose never executes. */
public final class SettingsCommands {
    public static final class Command {
        public final String id;public final Integer value;public final String page;
        Command(String id,Integer value,String page){this.id=id;this.value=value;this.page=page;}
    }
    private static boolean has(String text,String... words){for(String word:words)if(text.contains(word))return true;return false;}
    private static Command page(String page){return new Command(null,null,page);}
    public static Command parse(String input){
        String s=input.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+"," ").replaceAll("[.!?]+$","");
        String[] words={"zero","one","two","three","four","five","six","seven","eight","nine","ten"};for(int i=0;i<words.length;i++)s=s.replaceAll("\\b"+words[i]+"\\b",String.valueOf(i));
        if(s.matches("(please )?(undo|undo that|undo last change|revert last change)"))return new Command("undo",null,null);
        if(s.matches("^(how|why|what|explain)\\b.*"))return null;
        boolean action=has(s,"set ","change ","switch ","make ","turn ","enable ","disable ","hide ","show ","increase ","decrease ","reduce ");
        boolean navigate=s.matches(".*\\b(find|open|go to|where)\\b.*")||(s.contains("show ")&&s.contains("settings"));
        boolean folder=s.contains("folder"),drawer=has(s,"drawer","all apps");
        if(s.matches("(please )?(add|create|new)( (a|an|another|new))*( desktop| home screen)"))return new Command("add_desktop",null,null);
        if(s.matches("(please )?(remove|delete)( (this|the|current|empty))*( desktop| home screen)"))return new Command("remove_desktop",null,null);
        if((action||navigate)&&has(s,"animation","opening effect")&&folder)return page("choice:folder_animation");
        if((action||navigate)&&has(s,"animation","opening effect")&&has(s,"menu","menus"))return page("choice:menu_animation");
        if((action||navigate||s.equals("transition animations"))&&has(s,"transition","screen effect")){
            int effect=-1,length=0;for(int i=0;i<PageTransitions.NAMES.length;i++){String name=PageTransitions.NAMES[i].toLowerCase(Locale.ROOT);if(name.length()>length&&s.matches(".*\\b"+Pattern.quote(name)+"\\b.*")){effect=i;length=name.length();}}if(effect>=0)return new Command("transition",effect,null);
            return page("choice:transition");
        }
        if((action||navigate)&&has(s,"shape","folder icon")&&!has(s,"size","radius","large","bigger","small","increase","decrease")){
            String id=folder?"folder_shape":"icon_shape";
            if(has(s,"rounded","round square"))return new Command(id,folder?1:0,null);
            if(has(s,"circle","circular","round icons"))return new Command(id,folder?0:1,null);
            if(s.contains("square"))return new Command(id,2,null);
            return page("choice:"+id);
        }
        if(navigate){
            String target=folder?"folders":has(s,"dock")?"dock":drawer?"drawer":has(s,"grid","home screen","desktop","icon size")?"home":has(s,"glass","transparent","opacity","appearance","look")?"look":has(s,"gesture")?"gestures":has(s,"feed","discover")?"feed":has(s,"backup","restore")?"backup":has(s,"card")?"cards":has(s,"scene","tilt","motion")?"motion":has(s,"search","nota")?"search":null;
            if(target!=null)return page(target);
        }
        String card=has(s,"media card","music card")?"card_media":has(s,"weather card")?"card_weather":has(s,"calendar card")?"card_calendar":has(s,"photo card","photos card")?"card_photos":has(s,"reading card")?"card_reading":has(s,"notes card")?"card_notes":null;
        String id=card!=null?card:folder&&has(s,"radius","corners")?"folder_radius":has(s,"row height","row spacing")?"grid_row_dp":has(s,"column","grid")?(folder?"folder_columns":drawer?"drawer_columns":"grid_columns"):has(s,"icon size","icons larger","icons bigger","icons smaller","icon larger","icon bigger","icon smaller")?(folder?"folder_icon_size":drawer?"drawer_icon_size":"icon_size"):has(s,"transparency","transparent")?"transparency":has(s,"feather","fade width")?"glass_feather":has(s,"opacity strength","opacity")?"glass_strength":has(s,"adaptive glass","fading menus","adaptive menus")?"adaptive_glass":has(s,"reduced motion","reduce motion")?"reduced_motion":has(s,"tilt","parallax")?"tilt":has(s,"labels","label")?(folder?"folder_labels":drawer?"drawer_labels":"desktop_labels"):has(s,"dock")?"show_dock":null;
        if(id==null||!action)return null;
        if(id.startsWith("card_")||has(id,"tilt","labels","show_dock","reduced_motion","adaptive_glass")){
            boolean off=s.matches(".*\\b(off|disable|hide|without)\\b.*"),on=s.matches(".*\\b(on|enable|show)\\b.*")||s.contains("reduce motion");
            if(!off&&!on)return null;return new Command(id,off?0:1,null);
        }
        Matcher number=Pattern.compile("(?<![\\d.])-?\\d+(?![\\d.])").matcher(s);if(number.find()){try{return new Command(id,Integer.parseInt(number.group()),null);}catch(NumberFormatException invalid){return null;}}
        if(id.endsWith("icon_size")&&has(s,"larger","bigger","increase","large"))return new Command(id,Integer.MIN_VALUE+1,null);
        if(id.endsWith("icon_size")&&has(s,"smaller","decrease","small"))return new Command(id,Integer.MIN_VALUE+2,null);
        // A setting without a value opens its controls; no guessed mutation.
        return page(folder?"folders":drawer?"drawer":id.equals("icon_size")?"home":id.startsWith("glass")||id.equals("transparency")?"look":"motion");
    }
}
