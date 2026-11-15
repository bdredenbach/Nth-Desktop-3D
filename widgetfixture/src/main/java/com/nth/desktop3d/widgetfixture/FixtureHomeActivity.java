package com.nth.desktop3d.widgetfixture;

/** A second eligible Home app makes the real Android role chooser testable. */
public final class FixtureHomeActivity extends android.app.Activity {
    @Override public void onCreate(android.os.Bundle state){super.onCreate(state);android.widget.TextView label=new android.widget.TextView(this);label.setText("Setup test Home");setContentView(label);}
}
