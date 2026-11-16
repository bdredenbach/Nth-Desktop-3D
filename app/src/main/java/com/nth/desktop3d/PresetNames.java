package com.nth.desktop3d;

/** Names are labels; only generated identifiers ever become paths. */
final class PresetNames {
    static String checked(String value){
        String name=value==null?"":java.text.Normalizer.normalize(value.trim(),java.text.Normalizer.Form.NFC);if(name.isEmpty()||name.codePointCount(0,name.length())>40)throw new IllegalArgumentException("Use a name with 1–40 characters.");
        for(int i=0;i<name.length();i++)if(Character.isISOControl(name.charAt(i))||Character.getType(name.charAt(i))==Character.LINE_SEPARATOR||Character.getType(name.charAt(i))==Character.PARAGRAPH_SEPARATOR)throw new IllegalArgumentException("Use a name on one line.");return name;
    }
    static boolean same(String a,String b){return a.equalsIgnoreCase(b);}
    static boolean identifier(String value){return value!=null&&value.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");}
    private PresetNames(){}
}
