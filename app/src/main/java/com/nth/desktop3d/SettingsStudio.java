package com.nth.desktop3d;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@android.annotation.SuppressLint("ViewConstructor")
final class SettingsStudio extends LinearLayout {
    interface Host {void home();void arrange();void recenter();void feed();void reconnectFeed();void feedDetails();String feedStatus();void googleApp();void cards();void widgets();void folder();void overview();void autoSort();void apps();void about();void backup(boolean restore);boolean openApp(String query);}
    private static final String[][] PAGES={{"home","Home screen","Grid, icon size, labels and placement"},{"dock","Dock","Visibility, spacing and material"},{"folders","Folders","Live folder layout and appearance"},{"drawer","App drawer","Icon layout and names"},{"search","Search & Nth Nota","Folder search, keyboard and cloud questions"},{"cards","Cards","Media, calendar, weather, photos and reading in crystal"},{"look","Look & feel","Fading glass, color and menu animation"},{"motion","Scenes & motion","3D depth, transitions, tilt and Living Pond"},{"gestures","Gestures","Actions on empty desktop space"},{"feed","Feed page","Built-in news, interests and edge swipe"},{"backup","Settings backup & restore","Export or restore settings without replacing desktop items"},{"nota","Nth Nota","Find, explain and change launcher settings"},{"about","Launcher","Default Home and app information"}};
    final SettingsModel model;private final Host host;private final SettingsPreview preview;private final EditText search;private final TextView title;
    private final Button back;private final LinearLayout body;private final ScrollView scroll;private String current="root",parent="root";private boolean advanced,searching;private final NotaSettingsClient cloud=new NotaSettingsClient();private TextView answer;private Button send,undo;private EditText question;
    SettingsStudio(Context c,SettingsModel model,AppCatalog catalog,Host host,String page){
        super(c);this.model=model;this.host=host;setOrientation(VERTICAL);setClipChildren(false);setClipToPadding(false);
        LinearLayout navigation=Ui.row(c);back=Ui.quiet(c,"‹ Settings",()->back());navigation.addView(back,new LayoutParams(Ui.dp(c,110),Ui.dp(c,44)));title=Ui.text(c,"Settings",17,Ui.TEXT);title.setTypeface(null,Typeface.BOLD);navigation.addView(title,new LayoutParams(0,-2,1));Button ask=Ui.quiet(c,"Ask Nota",()->showPage("nota"));navigation.addView(ask,new LayoutParams(Ui.dp(c,90),Ui.dp(c,44)));addView(navigation);
        search=new EditText(c);search.setSingleLine(true);search.setHint("Search settings");search.setContentDescription("Search settings");search.setTextColor(Ui.TEXT);search.setHintTextColor(Ui.MUTED);addView(search,new LayoutParams(-1,Ui.dp(c,48)));
        preview=new SettingsPreview(c,model,catalog);LayoutParams pp=new LayoutParams(-1,Ui.dp(c,c.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE?96:172));pp.setMargins(0,Ui.dp(c,10),0,Ui.dp(c,12));addView(preview,pp);
        scroll=new FadingSettingsScrollView(c);body=Ui.column(c);body.setClipChildren(true);scroll.addView(body);addView(scroll,new LayoutParams(-1,0,1));
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int before,int count){if(searching)return;renderSearch(s.toString());}});
        setFocusableInTouchMode(true);requestFocus();showPage(page==null?"root":page);
    }
    void refreshPreview(){preview.refresh();}
    String page(){return current;}
    void tilt(float x,float y){preview.tilt(x,y);}
    boolean back(){if(!search.getText().toString().isEmpty()){search.setText("");return true;}if(current.equals("root"))return false;showPage(current.startsWith("choice:")?parent:"root");return true;}
    void pause(){cloud.cancel();if(send!=null)send.setEnabled(true);if(answer!=null&&answer.getText().toString().equals("Connecting to Nth Nota…"))answer.setText("Reply stopped while leaving this screen. Local commands remain available.");}
    void dispose(){model.endGesture();cloud.close();}
    private String pageTitle(String id){for(String[] p:PAGES)if(p[0].equals(id))return p[1];return "Settings";}
    private void clear(){pause();body.removeAllViews();question=null;answer=null;send=null;undo=null;scroll.scrollTo(0,0);}
    void showPage(String id){
        if(id.startsWith("choice:"))id="root";current=id;advanced=false;searching=true;search.setText("");searching=false;clear();title.setText(pageTitle(id));back.setVisibility(id.equals("root")?INVISIBLE:VISIBLE);preview.page(id);
        if(id.equals("root")){for(String[] page:PAGES)row(page[1],page[2],()->showPage(page[0]));return;}
        if(id.equals("nota")){nota();return;}
        if(id.equals("backup")){paragraph("This backup contains launcher settings. Notes, desktop items, widget bindings, cloud permission and signing keys are not included or replaced.");row("Export settings","Save a .nth3dsettings file",()->host.backup(false));row("Restore settings","Choose a settings backup; Undo is available afterward",()->host.backup(true));if(model.canUndo())row("Undo last setting change",model.lastChange,()->{model.undo();showPage("backup");});return;}
        if(id.equals("about")){row("Set default Home","Choose Nth Desktop 3D as your launcher",host::home);row("About Nth Desktop 3D","Version and project information",host::about);return;}
        renderControls(id);
    }
    private void renderControls(String id){
        List<SettingsModel.Spec> settings=new ArrayList<>();for(SettingsModel.Spec s:model.specs.values())if(s.page.equals(id))settings.add(s);
        int visible=id.equals("home")?6:id.equals("motion")?9:id.equals("look")?7:settings.size();
        for(int i=0;i<Math.min(visible,settings.size());i++)control(settings.get(i));
        if(settings.size()>visible){LinearLayout extra=Ui.column(getContext());Button advancedButton=Ui.quiet(getContext(),"Advanced  ▾",()->{advanced=!advanced;extra.setVisibility(advanced?VISIBLE:GONE);});body.addView(advancedButton,new LayoutParams(-1,Ui.dp(getContext(),48)));body.addView(extra);extra.setVisibility(GONE);for(int i=visible;i<settings.size();i++)control(settings.get(i),extra);}
        if(id.equals("home")){row("Widgets","Browse previews and add a widget",host::widgets);row("Arrange desktop","Arrange shortcuts around existing widgets",host::arrange);row("New folder","Create a folder on this desktop",host::folder);row("All desktop shortcuts","Open or manage shortcuts on every desktop",host::overview);}
        if(id.equals("home")||id.equals("folders")||id.equals("drawer"))row("Auto pick and sort","Preview the Nova folder arrangement and add a sorted desktop",host::autoSort);
        if(id.equals("drawer"))row("Open app drawer","Try the current layout",host::apps);
        if(id.equals("search"))row("Open Nth Nota","Local setting commands and optional cloud explanations",()->showPage("nota"));
        if(id.equals("motion"))row("Replay desktop transition","Animate the selected effect in the preview",preview::playTransition);
        if(id.equals("motion"))row("Recenter tilt","Center perspective while holding the phone comfortably",host::recenter);
        if(id.equals("cards"))row("Open crystal cards","Try your enabled cards",host::cards);
        if(id.equals("feed")){paragraph(host.feedStatus());paragraph("Nth Feed includes native news, interests, saved headlines and a publisher reader. It works without a companion app. Google Discover is the default and requires the Google app.");row("Open Nth Feed","News, sources and saved stories",host::feed);row("Open Google Discover","Connect the overlay inside Nth Desktop",host::reconnectFeed);row("Discover connection details","Connection stage, build mode and a copyable report",host::feedDetails);row("Open Google app","Open Google separately",host::googleApp);row("Open crystal cards","Media, calendar, weather, photos and reading",host::cards);}
        if(id.equals("gestures"))paragraph("These gestures act on empty desktop space. Widget touches, scrolling and Android navigation keep their own gestures.");
    }
    private void renderSearch(String value){
        String q=value.trim().toLowerCase(Locale.ROOT);if(q.isEmpty()){showPage(current.startsWith("choice:")?parent:current);return;}
        clear();title.setText("Search results");back.setVisibility(VISIBLE);int matches=0;
        for(SettingsModel.Spec s:model.specs.values())if((s.title+" "+s.help+" "+pageTitle(s.page)).toLowerCase(Locale.ROOT).contains(q)){matches++;row(s.title,pageTitle(s.page)+" · "+s.value(),()->{showPage(s.page);View target=body.findViewWithTag("setting:"+s.id);if(target!=null)scroll.post(()->scroll.smoothScrollTo(0,target.getTop()));});}
        if(matches==0)paragraph("No matching settings. Try grid, folders, opacity, gestures or tilt.");
    }
    private void control(SettingsModel.Spec s){control(s,body);}
    private void control(SettingsModel.Spec s,LinearLayout parent){
        LinearLayout card=Ui.column(getContext());card.setTag("setting:"+s.id);card.setPadding(0,Ui.dp(getContext(),8),0,Ui.dp(getContext(),12));parent.addView(card,new LayoutParams(-1,-2));
        if(s.toggle){Switch toggle=new Switch(getContext());toggle.setText(s.title);toggle.setTextSize(15);toggle.setTextColor(Ui.TEXT);toggle.setMinHeight(Ui.dp(getContext(),48));toggle.setChecked(s.get.getAsInt()==1);toggle.setOnCheckedChangeListener((button,value)->{model.set(s.id,value?1:0);preview.refresh();if(s.id.equals("cloud_ai")&&!value)pause();});card.addView(toggle,new LayoutParams(-1,-2));}
        else if(s.options!=null){Button option=Ui.quiet(getContext(),s.title+"  ·  "+s.value()+"  ▾",()->choice(s));option.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);card.addView(option,new LayoutParams(-1,-2));}
        else{
            TextView label=Ui.text(getContext(),s.title+"  ·  "+s.value(),15,Ui.TEXT);label.setTypeface(null,Typeface.BOLD);card.addView(label);
            SeekBar slider=new SeekBar(getContext());slider.setContentDescription(s.title);slider.setMax((s.max-s.min)/s.step);slider.setProgress((s.get.getAsInt()-s.min)/s.step);card.addView(slider,new LayoutParams(-1,Ui.dp(getContext(),44)));
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar bar,int value,boolean user){if(user){model.set(s.id,s.min+value*s.step);label.setText(s.title+"  ·  "+s.value());preview.refresh();}}public void onStartTrackingTouch(SeekBar bar){model.beginGesture();}public void onStopTrackingTouch(SeekBar bar){model.endGesture();}});
        }
        TextView help=Ui.text(getContext(),s.help,12,Ui.MUTED);help.setPadding(0,Ui.dp(getContext(),3),0,0);card.addView(help,new LayoutParams(-1,-2));
    }
    private void choice(SettingsModel.Spec s){parent=s.page;current="choice:"+s.id;clear();title.setText(s.title);preview.page(s.page);back.setVisibility(VISIBLE);
        for(int i=0;i<s.options.length;i++){final int index=i;row((s.get.getAsInt()==i?"✓  ":"")+s.options[i],"Apply to the live preview",()->{model.set(s.id,index);preview.refresh();choice(s);if(s.id.equals("transition"))preview.playTransition();});}
        row("Done","Return to "+pageTitle(s.page),()->showPage(s.page));
    }
    private void row(String title,String detail,Runnable action){Button button=Ui.quiet(getContext(),title+"\n"+detail,action);button.setContentDescription(title);button.setTextSize(14);button.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);button.setMinHeight(Ui.dp(getContext(),64));button.setElevation(Ui.dp(getContext(),2));button.setCameraDistance(Ui.dp(getContext(),1800));button.setOnTouchListener((v,e)->{if(!model.store.reducedMotion){if(e.getActionMasked()==android.view.MotionEvent.ACTION_DOWN)v.animate().translationZ(Ui.dp(getContext(),8)).scaleX(1.015f).scaleY(1.015f).setDuration(100).start();else if(e.getActionMasked()==android.view.MotionEvent.ACTION_UP||e.getActionMasked()==android.view.MotionEvent.ACTION_CANCEL)v.animate().translationZ(0).scaleX(1).scaleY(1).setDuration(180).start();}return false;});LayoutParams lp=new LayoutParams(-1,-2);lp.bottomMargin=Ui.dp(getContext(),6);body.addView(button,lp);}
    private void paragraph(String text){TextView view=Ui.text(getContext(),text,14,Ui.MUTED);view.setPadding(0,Ui.dp(getContext(),8),0,Ui.dp(getContext(),14));body.addView(view,new LayoutParams(-1,-2));}
    private void nota(){
        paragraph("Ask about a setting or give a direct command. Try “turn off tilt”, “set grid to five columns”, “make icons larger”, “show folder settings”, or “undo”.");
        question=new EditText(getContext());question.setHint("Ask Nth Nota");question.setContentDescription("Ask Nth Nota");question.setTextColor(Ui.TEXT);question.setHintTextColor(Ui.MUTED);question.setMaxLines(3);question.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2000)});question.setText(model.store.preferences.getString("nota_draft",""));body.addView(question,new LayoutParams(-1,-2));
        question.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int before,int count){model.store.preferences.edit().putString("nota_draft",s.toString()).apply();}});
        answer=Ui.text(getContext(),model.lastChange.isEmpty()?"Local commands are ready. Cloud questions are optional.":model.lastChange,15,Ui.TEXT);answer.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);answer.setTextIsSelectable(true);
        send=Ui.primary(getContext(),"Send to Nth Nota",()->ask(question.getText().toString()));body.addView(send,new LayoutParams(-1,Ui.dp(getContext(),48)));body.addView(answer,new LayoutParams(-1,-2));
        undo=Ui.quiet(getContext(),"Undo last setting change",()->{answer.setText(model.undo());preview.refresh();undo.setEnabled(model.canUndo());});undo.setEnabled(model.canUndo());body.addView(undo,new LayoutParams(-1,Ui.dp(getContext(),48)));
        Button stop=Ui.quiet(getContext(),"Stop reply",this::pause);body.addView(stop,new LayoutParams(-1,Ui.dp(getContext(),48)));
        paragraph("Local commands stay on this phone. Cloud replies provide advice; they do not execute setting changes.");control(model.specs.get("cloud_ai"));
    }
    private void ask(String input){
        String text=input.trim();if(text.isEmpty())return;cloud.cancel();
        SettingsCommands.Command command=SettingsCommands.parse(text);
        if(command!=null){if(command.page!=null){showPage(command.page);return;}if(command.id.equals("undo"))answer.setText(model.undo());else{int value=command.value;if(value==Integer.MIN_VALUE+1)value=model.value(command.id)+10;if(value==Integer.MIN_VALUE+2)value=model.value(command.id)-10;answer.setText(model.set(command.id,value));}preview.refresh();undo.setEnabled(model.canUndo());return;}
        if(text.toLowerCase(Locale.ROOT).startsWith("open ")&&host.openApp(text.substring(5).trim()))return;
        if(model.value("cloud_ai")==0){for(SettingsModel.Spec s:model.specs.values())if(text.toLowerCase(Locale.ROOT).contains(s.title.toLowerCase(Locale.ROOT))){answer.setText(s.title+": "+s.value()+". "+s.help);return;}answer.setText("I can find or change launcher settings locally. Try a command above, or enable Cloud questions for a free-form explanation.");return;}
        answer.setText("Connecting to Nth Nota…");send.setEnabled(false);cloud.ask(text,model.context(),reply->{if(answer!=null){answer.setText(reply);send.setEnabled(true);}});
    }
    void restored(){showPage("backup");}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);int desired=Ui.dp(getContext(),h<Ui.dp(getContext(),380)?80:getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE?96:172);LayoutParams p=(LayoutParams)preview.getLayoutParams();if(p.height!=desired){p.height=desired;preview.setLayoutParams(p);}}
}
