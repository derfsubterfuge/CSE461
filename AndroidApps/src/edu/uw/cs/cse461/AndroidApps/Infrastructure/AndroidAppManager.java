package edu.uw.cs.cse461.AndroidApps.Infrastructure;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import edu.uw.cs.cse461.AndroidApps.R;
import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadableAndroidApp;
import edu.uw.cs.cse461.Net.RPC.RPCService;
import edu.uw.cs.cse461.util.IPFinder;

public class AndroidAppManager extends NetLoadableAndroidApp {
	
	public AndroidAppManager() {
		super("AndroidAppManager", true);
	}
	
	class AppLaunchButton extends Button {
		private String mClassname;
		AppLaunchButton(Context ctx, String appclassname) {
			super(ctx);
			mClassname = appclassname;
			setTextSize(16.0F);
            setText(appclassname);
            setOnClickListener(new View.OnClickListener() {
            	public void onClick(View v) {
            		try {
            			NetBase.theNetBase().startApp(mClassname);
            		} catch (Exception e) {
            			Toast.makeText(getContext(), "Start of " + mClassname + " failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            		}
            	}
            });
		}
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_android_app_manager);
        
        try {
        	String msg = ((RPCService)NetBase.theNetBase().getService("rpc")).localIP() + ":" + Integer.toString(((RPCService)NetBase.theNetBase().getService("rpc")).localPort());
        	TextView myIpText = (TextView)findViewById(R.id.appmanager_myiptext);
        	if ( myIpText != null ) myIpText.setText(msg);
        } catch (Exception e) {
        	
        }
        
//        LinearLayout layout = (LinearLayout)this.findViewById(R.id.appmanagerlayout);
//        //LinearLayout layout = (LinearLayout)this.findViewById(R.id.AndroidAppManagerLayout);
//        for (String appclassname : NetBase.theNetBase().loadedAppNames()) {
//        	Button btn = new AppLaunchButton(this, appclassname); 
//        	layout.addView(btn); 
//        }

//        ScrollView scrollView = (ScrollView)this.findViewById(R.id.appmanager_scrollview);
//        LinearLayout scrollLayout = new LinearLayout(getApplicationContext());
//        scrollView.addView(scrollLayout);
        LinearLayout scrollLayout = (LinearLayout)this.findViewById(R.id.appmanager_scrollviewlayout);
        //LinearLayout layout = (LinearLayout)this.findViewById(R.id.AndroidAppManagerLayout);
        for (String appclassname : NetBase.theNetBase().loadedAppNames()) {
            Button btn = new AppLaunchButton(this, appclassname); 
            scrollLayout.addView(btn); 
        }
}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_android_app_manager, menu);
        return true;
    }

}
