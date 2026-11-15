package com.nth.desktop3d;

import android.content.Context;
import android.view.*;
import android.widget.*;
import java.util.function.IntConsumer;

/** A saved draft is separate from the applied wallpaper. Applying is always explicit. */
@android.annotation.SuppressLint("ViewConstructor")
final class SceneComposer extends LinearLayout {
    interface Host {void pick(boolean object);void saveDraft(LayeredScene.Document d);void apply(LayeredScene.Document d);void discard();}
    final LayeredScene.Document document;final LayerSceneView preview;
    private final Host host;private final LinearLayout controls,navigation;private final TextView heading;
    private int step;private String selected;private boolean applying,disposed;
    private final Runnable persist;
    SceneComposer(Context c,LayeredScene.Document d,Host host){
        super(c);setOrientation(VERTICAL);this.document=d;this.host=host;persist=()->{if(!applying&&!disposed)this.host.saveDraft(document);};
        heading=Ui.text(c,"",16,Ui.ACCENT);heading.setPadding(0,0,0,Ui.dp(c,8));addView(heading);
        ScrollView scroll=new ScrollView(c);scroll.setFillViewport(false);addView(scroll,new LayoutParams(-1,0,1));LinearLayout body=Ui.column(c);scroll.addView(body);
        preview=new LayerSceneView(c);preview.document(d);preview.editing(true,this::changed,id->{selected=id;renderControls();});LayoutParams pp=new LayoutParams(Ui.dp(c,150),Ui.dp(c,290));pp.gravity=Gravity.CENTER_HORIZONTAL;pp.bottomMargin=Ui.dp(c,12);body.addView(preview,pp);
        controls=Ui.column(c);body.addView(controls);navigation=Ui.row(c);addView(navigation,new LayoutParams(-1,Ui.dp(c,54)));if(!d.objects.isEmpty())selected=d.objects.get(d.objects.size()-1).id;render();
    }
    int step(){return step;}
    void step(int value){step=Math.max(0,Math.min(2,value));render();}
    void tilt(float x,float y){if(step==2)preview.tilt(x,y);}
    void reduced(boolean reduced){preview.moving(!reduced);}
    private void changed(){preview.invalidate();removeCallbacks(persist);postDelayed(persist,400);}
    void persistNow(){removeCallbacks(persist);if(!applying&&!disposed)host.saveDraft(document);}
    void applied(){applying=true;removeCallbacks(persist);}
    void failed(){applying=false;render();}
    void image(byte[] bytes,boolean object){if(disposed)return;if(object){if(document.objects.size()>=LayeredScene.MAX_OBJECTS)return;LayeredScene.ObjectLayer o=new LayeredScene.ObjectLayer();o.kind="image";o.name="Image "+(document.objects.size()+1);o.image=bytes;document.objects.add(o);selected=o.id;step=1;}else{document.background=bytes;step=0;}preview.refreshAssets();changed();render();}
    void add(String kind){if(document.objects.size()>=LayeredScene.MAX_OBJECTS)return;LayeredScene.ObjectLayer o=new LayeredScene.ObjectLayer();o.kind=kind;o.name=kind.equals("orb")?"Glass orb":kind.equals("star")?"Prism star":"Crystal";o.x=.35f+document.objects.size()%3*.15f;o.y=.38f+document.objects.size()%3*.1f;document.objects.add(o);selected=o.id;preview.selected(selected);changed();render();}
    private void render(){heading.setText(new String[]{"1 · Background","2 · Objects","3 · Depth & preview"}[step]);preview.editing(step==1,this::changed,id->{selected=id;renderControls();});if(step!=2)preview.tilt(0,0);preview.selected(step==1?selected:null);renderControls();navigation.removeAllViews();Button back=Ui.quiet(getContext(),step==0?"Discard draft":"Back",()->{if(step==0)new android.app.AlertDialog.Builder(getContext()).setTitle("Discard this scene draft?").setMessage("Your applied wallpaper will stay as it is.").setNegativeButton("Keep editing",null).setPositiveButton("Discard",(dialog,which)->{applying=true;removeCallbacks(persist);host.discard();}).show();else step(step-1);});navigation.addView(back,new LayoutParams(0,-1,1));Button next=Ui.primary(getContext(),step==2?"Apply scene":"Next",()->{if(step<2)step(step+1);else{applied();host.apply(document);}});next.setEnabled(!applying);navigation.addView(next,new LayoutParams(0,-1,1));}
    private void paragraph(String value){TextView t=Ui.text(getContext(),value,13,Ui.MUTED);t.setPadding(0,Ui.dp(getContext(),5),0,Ui.dp(getContext(),9));controls.addView(t);}
    private void action(String label,Runnable run){Button b=Ui.button(getContext(),label,run);controls.addView(b,new LayoutParams(-1,Ui.dp(getContext(),48)));}
    private LayeredScene.ObjectLayer selected(){for(LayeredScene.ObjectLayer o:document.objects)if(o.id.equals(selected))return o;return null;}
    private void slider(String label,int min,int max,int value,IntConsumer set){TextView title=Ui.text(getContext(),label+" · "+value,13,Ui.TEXT);controls.addView(title);SeekBar bar=new SeekBar(getContext());bar.setContentDescription(label);bar.setMax(max-min);bar.setProgress(value-min);controls.addView(bar,new LayoutParams(-1,Ui.dp(getContext(),44)));bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){persistNow();}public void onProgressChanged(SeekBar b,int n,boolean user){if(user){int v=n+min;title.setText(label+" · "+v);set.accept(v);changed();}}});}
    private void renderControls(){
        controls.removeAllViews();if(step==0){action(document.background==null?"Choose background from gallery":"Replace background",()->host.pick(false));paragraph("Use a photo, or keep the built-in gradient. A photo can be your whole wallpaper or the background behind separate objects.");action(document.fit==0?"Photo fit: Fill screen":"Photo fit: Fit image",()->{document.fit=1-document.fit;changed();renderControls();});return;}
        if(step==1){paragraph("Add optional objects, then drag them in the preview. Transparent PNG or WebP cutouts let a subject move separately from the background.");if(document.objects.size()<LayeredScene.MAX_OBJECTS){LinearLayout choices=Ui.row(getContext());for(String kind:new String[]{"crystal","orb","star"})choices.addView(Ui.button(getContext(),kind.equals("crystal")?"Crystal":kind.equals("orb")?"Orb":"Star",()->add(kind)),new LayoutParams(0,Ui.dp(getContext(),48),1));controls.addView(choices);action("Add image layer",()->host.pick(true));}else paragraph("Six objects added. Remove one to add another.");
            for(LayeredScene.ObjectLayer o:document.objects)action((o.id.equals(selected)?"✓  ":"")+o.name,()->{selected=o.id;preview.selected(selected);renderControls();});
            LayeredScene.ObjectLayer o=selected();if(o!=null){slider("Object size",8,85,Math.round(o.size*100),v->o.size=v/100f);slider("Object rotation",-180,180,Math.round(o.rotation),v->o.rotation=v);action("Move to front",()->{document.objects.remove(o);document.objects.add(o);changed();renderControls();});action("Remove object",()->{document.objects.remove(o);selected=document.objects.isEmpty()?null:document.objects.get(document.objects.size()-1).id;preview.selected(selected);preview.refreshAssets();changed();renderControls();});}return;}
        paragraph("Tilt your phone to preview depth. Nearer objects travel farther. Reduced motion and battery saving keep the scene still.");Switch motion=new Switch(getContext());motion.setText("Scene parallax");motion.setTextColor(Ui.TEXT);motion.setChecked(document.motion);controls.addView(motion,new LayoutParams(-1,Ui.dp(getContext(),48)));motion.setOnCheckedChangeListener((v,checked)->{document.motion=checked;changed();});slider("Motion strength",0,100,Math.round(document.strength*100),v->document.strength=v/100f);slider("Background depth",0,100,Math.round(document.backgroundDepth*100),v->document.backgroundDepth=v/100f);
        for(LayeredScene.ObjectLayer o:document.objects)slider(o.name+" depth",0,100,Math.round(o.depth*100),v->o.depth=v/100f);
        paragraph("Apply saves this scene inside Nth Desktop. Desktop backups include its image layers and depth settings.");
    }
    void dispose(){removeCallbacks(persist);disposed=true;preview.dispose();}
}
