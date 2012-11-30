package edu.uw.cs.cse461.AndroidApps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import edu.uw.cs.cse461.DB461.DB461.DB461Exception;
import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadableAndroidApp;
import edu.uw.cs.cse461.Net.DDNS.DDNSFullName;
import edu.uw.cs.cse461.SNet.SNetController;
import edu.uw.cs.cse461.SNet.SNetDB461;
import edu.uw.cs.cse461.util.ConfigManager;
import edu.uw.cs.cse461.util.ContextManager;
import edu.uw.cs.cse461.util.IPFinder;

import edu.uw.cs.cse461.SNet.SNetDB461.CommunityRecord;
import edu.uw.cs.cse461.SNet.SNetDB461.Photo;
import edu.uw.cs.cse461.SNet.SNetDB461.PhotoRecord;
import edu.uw.cs.cse461.DB461.DB461.DB461Exception;
import edu.uw.cs.cse461.DB461.DB461.RecordSet;

public class SNetActivity extends NetLoadableAndroidApp {
	private static final String TAG="SNetActivity";
    public static final String PREFS_NAME = "CSE461";
    
    private static final int CHOOSE_PICTURE_ACTIVITY_REQUEST_CODE = 200;
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final String SD_CARD_PATH = Environment.getExternalStorageDirectory().getPath();
	
	private SNetController controller;
	private String myName;
	private File galleryDir;
	private String dbPath;

	public SNetActivity() {
		super("SNet", true);
        ConfigManager config = NetBase.theNetBase().config();
        myName = config.getProperty("net.hostname");
		galleryDir = new File(SD_CARD_PATH + "/snetpictures");
		dbPath = SD_CARD_PATH;
		controller = new SNetController(dbPath);

	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        
        // display main screen
        goToMain();
        
        // save my context so that this app can retrieve it later (?)
        ContextManager.setActivityContext(this);
    }
    
    /**
     * Called after we've been unloaded from memory and are restarting.  E.g.,
     * 1st launch after power-up; relaunch after going Home.
     */
    @Override
    protected void onStart() {
    	super.onStart();
        Log.d(TAG, "--------onStart-------");

    }

    /**
     * Called, for example, if the user hits the Home button.
     */
    @Override
    protected void onStop() {
    	super.onStop();
    	Log.d(TAG, "onStop");
    }
    
    /**
     * System is shutting down...
     */
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	Log.d(TAG,"onDestroy");
    }

 
    ///////////////////////////////
    // MAIN VIEW BUTTON HANDLERS //
    ///////////////////////////////
  
    // called when user clicks the TakePhoto button from the main screen
    public void onTakePhoto(View b) {
    	Log.d(TAG, "clicked Take Photo");
    	Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    	startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
    }
    
    // called when user clicks the Choose Picture button from the main screen
    public void onChoosePicture(View b) {
    	Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    	intent.setType("image/*");
    	
    	Log.d(TAG, "clicked Choose Picture");

    	startActivityForResult(intent, CHOOSE_PICTURE_ACTIVITY_REQUEST_CODE);
    }
    
    // called when the user clicks the Update Pictures button from the main screen
    public void onUpdatePictures(View b) {
    	Log.d(TAG, "clicked Update Pictures");
    	
    	// this method should bring us to the new layout
    	goToContact();
    }
    
    
    
    //////////////////////////////////
    // CONTACT VIEW BUTTON HANDLERS //
    //////////////////////////////////
    
    // TODO
    // click Befriend
    public void onFriend(View b) {
    	Log.d(TAG, "clicked Befriend");
    	
    	// get current name displayed in drop-down menu
    	Spinner spinner = (Spinner) findViewById(R.id.memberspinner);
    	String name = spinner.getSelectedItem().toString();
    	Log.d(TAG, "friending member: " + name);
    	
    	// call set friend for that name with isFriend = true;
    	try {
			controller.setFriend(new DDNSFullName(name), true);
		} catch (DB461Exception e) {
			Log.e(TAG, e.getMessage());
		}
    }
    
    // TODO
    // click Unfriend
    public void onUnfriend(View b) {
    	Log.d(TAG, "clicked UnFriend");
    	
    	// get current name displayed in drop-down menu
    	Spinner spinner = (Spinner) findViewById(R.id.memberspinner);
    	String name = spinner.getSelectedItem().toString();
    	Log.d(TAG, "unfriending member: " + name);
    	
    	// call set friend for that name with isFriend = false;
    	try {
			controller.setFriend(new DDNSFullName(name), false);
		} catch (DB461Exception e) {
			Log.e(TAG, e.getMessage());
		}
    }
    
    // TODO
    // click Contact
    public void onContact(View b) {
    	Log.d(TAG, "clicked Contact");
    	
    	// fetchUpdatesCaller(name, galleryDir)
    }
    
    // click Fix DB
    public void onFixDB(View b) {
    	Log.d(TAG, "clicked Fix DB");
    	
    	try {
			controller.fixDB(galleryDir);
		} catch (DB461Exception e) {
			Log.e(TAG, e.getMessage());
		}
    }
    
    // called when the user clicks the back button from the contact screen
    public void onBack(View b) {
    	Log.d(TAG, "clicked Back");
    	// go back to main screen
    	goToMain();
    }
    
    
    ////////////////////
    // RESULT HANDLER //
    ////////////////////
    
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.d(TAG, "got a result!");
    	switch (requestCode) {
	    	case CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE:
	    		// TODO : save photo to gallery
	    		// controller.newMyPhoto(new DDNSFullName(myName), filename, galleryDir)
	    		
	    		// Try to update gallery viewer
	    	    sendBroadcast(new Intent (Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
	    	    
	    	    // get the picture I just took:
	    	    Bitmap photoBmp = (Bitmap)data.getExtras().get("data");
    		    ImageView myImage = (ImageView) findViewById(R.id.mypicture);
    		    myImage.setImageBitmap(photoBmp);
	    	    
	    		break;
	    	case CHOOSE_PICTURE_ACTIVITY_REQUEST_CODE:
	    		// retrieve the picture selected: 
	    		Uri selectedImage = data.getData();
	    		String[] filePathColumn = {MediaStore.Images.Media.DATA};

	    		Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
	    		cursor.moveToFirst();

	    		int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
	    		String filePath = cursor.getString(columnIndex);	
	    		cursor.close();
	    		
	    		// filePath = filepath to the img selected, do something with it
	    		Log.d(TAG, "File path of chosen picture: " + filePath);
	    		
	    		File imgFile = new  File(filePath);
	    		if(imgFile.exists()){
	    		    Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

	    		    ImageView chosenImage = (ImageView) findViewById(R.id.chosenpicture);
	    		    chosenImage.setImageBitmap(myBitmap);
	    		    
	    		    // save to DB
	    		    try {
						controller.setChosenPhoto(new DDNSFullName(myName), filePath, galleryDir);
					} catch (Exception e) {
						Log.e(TAG, e.getMessage());
					}
	    		}
	    		break;
	    	default:
	    		break;
    	}
    }
    
    
    // handles redirecting to the home screen of the app as well as 
    // getting the myphoto and chosen photo from the db and displaying them
    public void goToMain() {
        setContentView(R.layout.snet_main);
        SNetDB461 db = null;
        try {
           db = new SNetDB461(controller.DBName());
           controller.registerBaseUsers(new DDNSFullName(myName));	// no-op if db already exists, ok to leave in for all cases
    
           SNetDB461.CommunityTable community = db.COMMUNITYTABLE;
    	   SNetDB461.PhotoTable photos = db.PHOTOTABLE;
           CommunityRecord myrecord = community.readOne(myName);
           // fetch myphoto: 
           int myPhotoHash = myrecord.myPhotoHash;
           if (myPhotoHash != 0) {	// display my photo
        	   // look up file path in photo table
        	   PhotoRecord myPhotoRecord = photos.readOne(myPhotoHash);
        	   File imgFile = myPhotoRecord.file;
        	   if(imgFile.exists()){
        		   Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
				
        		   ImageView chosenImage = (ImageView) findViewById(R.id.mypicture);
				   chosenImage.setImageBitmap(myBitmap);
        	   }
        	   
           }
           
           // fetch chosenphoto:
           int chosenPhotoHash = myrecord.chosenPhotoHash;
           if (chosenPhotoHash != 0) { // display chosen photo
        	   PhotoRecord myPhotoRecord = photos.readOne(chosenPhotoHash);
        	   File imgFile = myPhotoRecord.file;
        	   if(imgFile.exists()){
        		   Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
				
        		   ImageView chosenImage = (ImageView) findViewById(R.id.chosenpicture);
				   chosenImage.setImageBitmap(myBitmap);
        	   }
           }
           
        } catch (DB461Exception e) {
        	Log.e(TAG, e.getMessage());
		} finally {
           if ( db != null ) db.discard();
        }
    	
    }
    
    
    // handles redirecting to the contact/friends page as well as 
    // getting all names from the database and populating the drop-down list
    public void goToContact() {
        setContentView(R.layout.snet_contact);
        
        // find spinner to add to, create list to put names in
        Spinner spinner = (Spinner) findViewById(R.id.memberspinner);
        List<String> members = new ArrayList<String>();
        
        SNetDB461 db = null;
        try {
           db = new SNetDB461(controller.DBName());
           
           // look up all names in the community table and add to members list
           RecordSet<CommunityRecord> allMemberInfo = db.COMMUNITYTABLE.readAll();
           for(CommunityRecord member : allMemberInfo) {
        	   members.add(member.name.toString());
           }
        } catch (DB461Exception e) {
        	Log.e(TAG, e.getMessage());
		} finally {
           if ( db != null ) db.discard();
        }

        // attach list to spinner
        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, members);
        spinner.setAdapter(adapter);
    }
    
    public void updateFriend(boolean setFriend) {
    	
    }
    
   
    
}
