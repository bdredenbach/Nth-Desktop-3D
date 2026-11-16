package com.nth.desktop3d;

final class CardLayout {
    static final String[] TYPES={"media","ai","weather","calendar","photos","reading","notes","apps"};
    static String type(String key){if(key!=null&&key.startsWith("card:")){String type=key.substring(5);for(String valid:TYPES)if(type.equals(valid))return type;}return null;}
    static String key(String type){if(CardLayout.type("card:"+type)==null)throw new IllegalArgumentException("Unknown card");return "card:"+type;}
    static int height(String type){return type.equals("media")||type.equals("ai")||type.equals("notes")?2:3;}
    private CardLayout(){}
}
