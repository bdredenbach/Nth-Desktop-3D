package com.nth.desktop3d;

import java.util.Locale;

/** Local hints, not Play Store tags: declared Android category plus app name and package. */
public final class FolderRules {
    public static final String[] NAMES={"Stores","Recipes","Browser","Weather","AI","Projects","Readers","Shopping","Emulation","Work","Google","Social","Fitness","Games","DnD","Camera","Phone","Finances","Stream","Utilities"};
    public static final int[] X={2,1,3,0,2,4,1,3,0,2,4,1,3,0,2,4,0,1,3,4};
    public static final int[] Y={2,3,3,4,4,4,5,5,6,6,6,7,7,8,8,8,9,9,9,9};
    private static boolean has(String value,String... words){for(String w:words)if(value.contains(w))return true;return false;}
    public static int classify(String label,String packageName,int category){
        String s=(" "+label+" "+packageName).toLowerCase(Locale.ROOT);
        if(has(s,"companion","adventure","dnd","d&d","dungeons","dice","roll20","beyond","tabletop"))return 14;
        if(has(s,"emulat","retroarch","aethersx","duckstation","epsxe","ps3","dolphin","ppsspp","citra","yuzu","snes","nes.emu","melon","gba"))return 8;
        if(has(s,"chatgpt","openai","claude","gemini","perplexity","copilot","nth nota","anim ochi","animochi","character.ai","grok","poe "))return 4;
        if(has(s,"weather","forecast","accuweather","weatherbug"))return 3;
        if(has(s,"camera","photo","gallery","snapseed","lightroom","lens","adobe.scan"))return 15;
        if(has(s,"recipe","cooking","cookpad","allrecipes","kitchen","tasty","mealime"))return 1;
        if(has(s,"browser","chrome","firefox","brave","vivaldi","opera","edge","duckduckgo","internet explorer"))return 2;
        if(has(s,"aurora","play store","vending","fdroid","f-droid","obtainium","app store","galaxy.store"))return 0;
        if(has(s,"bank","finance","credit","capital one","cash app","cashapp","paypal","venmo","allstate","ally","current","earnin","chime","robinhood","wallet","dave","fidelity"))return 17;
        if(has(s,"fitness","fitbit","health","strava","workout","exercise","samsung.health","google.android.apps.fitness","geocach","c:geo"," cgeo"))return 12;
        if(has(s,"kindle","reader","comics","books","audible","kobo","moon+","readera","librera","shelf"))return 6;
        if(has(s,"netflix","disney","stream","youtube","spotify","music","audify","audify","plex","hulu","primevideo","dropout","tubi","soundcloud","soundcore","vlc","media player"))return 18;
        if(has(s,"facebook","instagram","discord","reddit","tiktok","twitter","mastodon","bluesky","snapchat","threads","telegram"))return 11;
        if(has(s,"phone","dialer","contacts","messages","messaging","whatsapp","signal","voicemail"))return 16;
        if(has(s,"shopping","amazon","ebay","walmart","target","chewy","dollar general","dollargeneral","temu","etsy","burger","denny","applebee","restaurant","mcdonald","starbucks","doordash","ubereats","amc","cineplus"))return 7;
        if(has(s,"project","github","gitlab","code","studio","termux","tasker","automation","nth desktop"))return 5;
        if(has(s,"office","outlook","teams","slack","zoom","calendar","docs","sheets","slides","acrobat","adobe","print","epson","brother","notion","drive","dropbox","onenote","nth notes"))return 9;
        if(packageName.startsWith("com.google.")||has(s,"google"))return 10;
        // Android's public category values are stable since API 26.
        switch(category){case 0:return 13;case 1:case 2:return 18;case 3:return 15;case 4:return 11;case 5:return 6;case 7:return 9;default:return 19;}
    }
}
