package edu.uw.cs.cse461.AndroidApps.Infrastructure;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import edu.uw.cs.cse461.AndroidApps.R;
import edu.uw.cs.cse461.AndroidApps.R.id;
import edu.uw.cs.cse461.AndroidApps.R.layout;
import edu.uw.cs.cse461.AndroidApps.R.menu;
import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetBaseAndroid;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.ContextManager;

public class AndroidStart extends Activity {
	private final String TAG="AndroidStart";
	
	private String mConfigFilename;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextManager.setActivityContext(this);
        setContentView(R.layout.activity_android_start);
        setupConfigChooserPanel();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_android_start, menu);
        return true;
    }
    
    private void setupConfigChooserPanel() {
        AssetManager mgr = getAssets();
        String[] assetFiles = null;
	    try {
	        assetFiles = mgr.list("");
	    } catch (IOException e) {
	        Log.e("List error:", "can't list assets");
	    }
        
	    Spinner configSpinner = (Spinner)findViewById(R.id.configspinner);
		ArrayAdapter<CharSequence> configList = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item);

		for ( String cf : assetFiles ) {
			if ( !cf.endsWith("ini.png") ) continue;
			configList.add(cf);
		}
		
		if ( configList.isEmpty() ) {
			// we require a config file name
			AlertDialog.Builder dBuilder = new AlertDialog.Builder(this);
			dBuilder.setMessage("CSE461 apps require a config file.  Get one, give it a name like xxx.config.ini.png, and put it in the project's assets folder.");
			dBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                AndroidStart.this.finish();
		           }
		       });

			AlertDialog dialog = dBuilder.create();
			dialog.show();
			return;
		}

		configList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    configSpinner.setAdapter(configList);
		
		configSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				mConfigFilename = parent.getItemAtPosition(pos).toString();
			}
			public void onNothingSelected(AdapterView parent) {
				
			}
		});
    }
    
	private String readSpinner(int id) {
		Spinner memberSpinner = (Spinner)findViewById(id);
		Object selection = memberSpinner.getSelectedItem();
		if ( selection == null ) return null;
		String m = selection.toString();
		if ( m.equals("root")) m = "";
		return m;
	}
	
	public void onConfigOk(View b) {
		mConfigFilename = readSpinner(R.id.configspinner);
		
		try {
			NetBaseAndroid theNetBase = (NetBaseAndroid)NetBase.theNetBase();
			if ( theNetBase != null ) {
				// if theNetBase isn't null, something is wrong -- this is a repeated click on the OK button, but the earlier one(s) has(ve) failed
			} else {
				// try to read configuration file
				ConfigManager configMgr = new ConfigManager(getAssets().openFd(mConfigFilename).createInputStream());
				theNetBase = new NetBaseAndroid();
				theNetBase.init(configMgr);
				theNetBase.loadApps();  // load all Android applications
			}

			//  Start the initial application, if one has been specified
			String initialAppName = NetBase.theNetBase().config().getProperty("android.initialapp");
			if (initialAppName != null) {
				try {
					theNetBase.startApp(initialAppName);
				} catch (Exception e) {
					Log.e(TAG,	initialAppName + " threw exception: " + e.getMessage());
				}
				//NetBase.theNetBase().shutdown(); // we're done when the inital app terminates
			}
			else {
				Log.w(TAG, "Possible config file bug -- No android.initialapp entry.");
			}
		} catch (Exception e) {
			Log.e(TAG, "onConfigOK caught exception: " + e.getMessage());
		}

		
	}
}
