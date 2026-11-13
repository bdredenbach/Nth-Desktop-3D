package com.nth.desktop3d.widgetfixture;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class FixtureConfigurationActivity extends Activity {
    @Override public void onCreate(Bundle state){
        super.onCreate(state);setResult(RESULT_CANCELED);
        int id=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(24,72,24,24);
        TextView title=new TextView(this);title.setText("Fixture widget setup");body.addView(title);
        Button save=new Button(this);save.setText("Save widget");body.addView(save);
        save.setOnClickListener(v->{FixtureWidgetProvider.update(this,id,"Configured widget ready");setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id));finish();});
        Button cancel=new Button(this);cancel.setText("Cancel widget");body.addView(cancel);cancel.setOnClickListener(v->finish());setContentView(body);
    }
}
