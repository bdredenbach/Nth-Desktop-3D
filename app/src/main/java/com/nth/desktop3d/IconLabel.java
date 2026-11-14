package com.nth.desktop3d;

/** Visual labels stay one line; identity, search and accessibility keep the full name. */
public final class IconLabel {
    private IconLabel() {}
    public static String menu(String name){return name==null?"":name.replaceAll("\\s+"," ").trim();}
    public static String display(String name) {
        if(name==null)return "";
        String text=name.replaceAll("\\s+"," ").trim();
        if(text.codePointCount(0,text.length())<=8)return text;
        return text.substring(0,text.offsetByCodePoints(0,8)).trim()+"...";
    }
}
