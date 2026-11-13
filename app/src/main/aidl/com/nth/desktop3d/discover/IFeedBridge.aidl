package com.nth.desktop3d.discover;
import com.nth.desktop3d.discover.IFeedBridgeCallback;
interface IFeedBridge { oneway void connect(IFeedBridgeCallback callback); }
