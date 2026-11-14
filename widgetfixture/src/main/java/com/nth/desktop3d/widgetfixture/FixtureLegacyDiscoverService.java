package com.nth.desktop3d.widgetfixture;

/** Enforces the legacy API instead of accepting modern bundle/lifecycle calls. */
public final class FixtureLegacyDiscoverService extends FixtureDiscoverService {
    @Override protected int apiVersion(){return 1;}
}
