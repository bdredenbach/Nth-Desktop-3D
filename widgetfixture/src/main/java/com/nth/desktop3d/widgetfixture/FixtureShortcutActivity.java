package com.nth.desktop3d.widgetfixture;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Separate-UID browser-style pin request and a distinct, verifiable shortcut destination. */
public final class FixtureShortcutActivity extends Activity {
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(24,72,24,24);
        TextView title=new TextView(this);body.addView(title);
        if(Intent.ACTION_VIEW.equals(getIntent().getAction()))title.setText("Opened recipe: "+getIntent().getDataString());
        else{
            title.setText("Browser shortcut fixture");
            Button pin=new Button(this);pin.setText("Pin recipe shortcut");body.addView(pin);
            pin.setOnClickListener(v->{
                String id=getIntent().getStringExtra("shortcutId");if(id==null)id="pot-roast";
                ShortcutInfo shortcut=new ShortcutInfo.Builder(this,id).setShortLabel("Pot Roast").setLongLabel("Instant Pot Roast Recipe")
                    .setIcon(Icon.createWithResource(this,android.R.drawable.ic_menu_compass))
                    .setIntent(new Intent(this,FixtureShortcutActivity.class).setAction(Intent.ACTION_VIEW).setData(android.net.Uri.parse("https://example.test/recipe/pot-roast"))).build();
                boolean supported=getSystemService(ShortcutManager.class).requestPinShortcut(shortcut,null);
                if(!supported)title.setText("Pin request is not supported by the default launcher");
            });
        }
        setContentView(body);
    }
}
