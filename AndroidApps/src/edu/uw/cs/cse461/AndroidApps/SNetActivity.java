package edu.uw.cs.cse461.AndroidApps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.ProgressBar;
import edu.uw.cs.cse461.DB461.DB461.DB461Exception;
import edu.uw.cs.cse461.Net.Base.NetBase;
import edu.uw.cs.cse461.Net.Base.NetLoadableAndroidApp;
import edu.uw.cs.cse461.Net.DDNS.DDNSFullName;
import edu.uw.cs.cse461.Net.RPC.RPCCallableMethod;
import edu.uw.cs.cse461.Net.RPC.RPCService;
import edu.uw.cs.cse461.SNet.SNetController;
import edu.uw.cs.cse461.SNet.SNetDB461;
import edu.uw.cs.cse461.SNet.SNetDB461.PhotoRecord;
import edu.uw.cs.cse461.util.BitmapLoader;
import edu.uw.cs.cse461.util.ContextManager;
import edu.uw.cs.cse461.SNet.SNetDB461.CommunityRecord;
import edu.uw.cs.cse461.DB461.DB461.RecordSet;

public class SNetActivity extends NetLoadableAndroidApp {
	private static final String TAG="SNetActivity";
    public static final String PREFS_NAME = "CSE461";
    
    private static final int CHOOSE_PICTURE_ACTIVITY_REQUEST_CODE = 200;
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final String SD_CARD_PATH = Environment.getExternalStorageDirectory().getPath();
	
    private RPCCallableMethod fetchupdates;
	private RPCCallableMethod fetchphoto;
	
	private SNetController controller;
	private DDNSFullName myName;
	private File galleryDir;
	private String dbPath;
	private Exception onCreateException = null;
	private File chosenPhoto = null;
	
	public SNetActivity() throws Exception {
		super("snet", true);
		Log.d(TAG, "constructor");
		if(onCreateException != null)
			throw onCreateException;	
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        
        myName = new DDNSFullName(NetBase.theNetBase().hostname());
		galleryDir = new File(SD_CARD_PATH + "/snetpictures");
		if (!galleryDir.exists()) {
			galleryDir.mkdir();
		}
		
		dbPath = SD_CARD_PATH;
		try {
			controller = new SNetController(dbPath);
			//Log.d(TAG, "Couldn't unlock db... deleting DB: " + (new File(controller.DBName()).delete()));
			// register rpc interface
			fetchupdates = new RPCCallableMethod(this, "_rpcFetchUpdates");
			fetchphoto = new RPCCallableMethod(this, "_rpcFetchPhoto");
			
			RPCService rpcService = (RPCService)NetBase.theNetBase().getService("rpc");
			if ( rpcService == null) throw new Exception("The SNet app requires that the RPC resolver service be loaded");
			rpcService.registerHandler(loadablename(), "fetchUpdates", fetchupdates );
			rpcService.registerHandler(loadablename(), "fetchPhoto", fetchphoto );
			
			// make sure user is in db
			myName = new DDNSFullName(NetBase.theNetBase().hostname());
			controller.registerBaseUsers(myName);
		} catch(Exception e) {
			Log.d(TAG, e.toString());
			onCreateException = e;
		}
      
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
    
    // click Befriend
    public void onFriend(View b) {
    	Log.d(TAG, "clicked Befriend");
    	updateFriend(true);
    }
    
    // click Unfriend
    public void onUnfriend(View b) {
    	Log.d(TAG, "clicked UnFriend");
    	updateFriend(false);
    }
    
    // TODO
    // click Contact
    public void onContact(View b) {
    	Log.d(TAG, "clicked Contact");
    	
    	// get current name displayed in drop-down menu
    	Spinner spinner = (Spinner) findViewById(R.id.memberspinner);
    	String name = spinner.getSelectedItem().toString();
    	
    	try {
			controller.fetchUpdatesCaller(name, galleryDir);
		} catch (DB461Exception e) {
			e.printStackTrace();
			Log.e(TAG, e.getMessage());
		}
    	
    	sendBroadcast(new Intent (Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
    	updateSpinner();
    	
    }
    
    // click Fix DB
    public void onFixDB(View b) {
    	Log.d(TAG, "clicked Fix DB");
    	
    	try {
			controller.fixDB(galleryDir);
	    	sendBroadcast(new Intent (Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));

			updateSpinner();
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
    	if(resultCode == Activity.RESULT_CANCELED || data == null)
    		return;
    	Log.d(TAG, "got a result!");
    	switch (requestCode) {
	    	case CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE:    		
	    	    // get the picture I just took:
	    	    Bitmap photoBmp = (Bitmap)data.getExtras().get("data");
    		    
    		    File myPhoto = null;
    		    FileOutputStream myPhotoStream = null;
				try {
					// create file for it

					myPhoto = File.createTempFile("myphoto", ".jpg", galleryDir);
					myPhotoStream = new FileOutputStream(myPhoto);
					
					// write the data to that file
					photoBmp.compress(Bitmap.CompressFormat.JPEG, 100, myPhotoStream);
					
					// update the db
					controller.newMyPhoto(myName, myPhoto.getAbsolutePath(), galleryDir);
					
					
				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG, e.getMessage());
				} catch (DB461Exception e) {
					Log.e(TAG, e.getMessage());
				} finally {
					if(myPhotoStream != null)
						try {
							myPhotoStream.close();
						} catch (IOException e) {
						}
					if(myPhoto != null)
						myPhoto.delete();
				}
       		    	    	    
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
	    		if(imgFile.isFile()){	    		
    
	    		    // save to DB
	    		    try {
						controller.setChosenPhoto(myName, filePath, galleryDir);
					} catch (Exception e) {
						Log.e(TAG, e.getMessage());
					}
	    		}
	    		break;
	    	default:
	    		break;
    	}
    	
    	sendBroadcast(new Intent (Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
    	goToMain();
    }
    
    
    /////////////
    // HELPERS //
    /////////////
    
    // handles redirecting to the home screen of the app as well as 
    // getting the myphoto and chosen photo from the db and displaying them
    public void goToMain() {
    	setContentView(R.layout.snet_main);
        SNetDB461 db = null;
        try {
           db = new SNetDB461(controller.DBName());
           controller.registerBaseUsers(myName);	// no-op if db already exists, ok to leave in for all cases
    
           SNetDB461.CommunityTable community = db.COMMUNITYTABLE;
    	   SNetDB461.PhotoTable photos = db.PHOTOTABLE;
           CommunityRecord myrecord = community.readOne(myName.toString());
           // fetch myphoto: 
          
           int myPhotoHash = myrecord.myPhotoHash;
           int chosenPhotoHash = myrecord.chosenPhotoHash;
           
           
           Log.d(TAG, myrecord.toString());
           if (myPhotoHash != 0) {	// display my photo
        	   // look up file path in photo table
        	   PhotoRecord myPhotoRecord = photos.readOne(myPhotoHash);
        	   Log.d(TAG, "MY PHOTO REC: " + myPhotoRecord.toString());
        	   if(myPhotoRecord != null) {
	        	   File imgFile = myPhotoRecord.file;
	        	   
	        	   if(imgFile != null && imgFile.isFile()){
	        		   Log.d(TAG, "photoname: " + myPhotoRecord.file.getAbsolutePath());
	        		   Bitmap myBitmap = null;
					try {
						myBitmap = BitmapLoader.loadBitmap(imgFile.getCanonicalPath(), 100, 100);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	        		   ImageView myImg = (ImageView) findViewById(R.id.mypicture);
	        		   myImg.setImageBitmap(myBitmap);
	        	   }
        	   }
           }
           
           // fetch chosenphoto:
           if (chosenPhotoHash != 0) { // display chosen photo
        	   PhotoRecord myPhotoRecord = photos.readOne(chosenPhotoHash);
        	   Log.d(TAG, "CHOSEN PHOTO REC: " + myPhotoRecord.toString());
        	   if(myPhotoRecord != null) {
        		   File imgFile = myPhotoRecord.file;
        		 
	        	   if(imgFile != null && imgFile.exists()){
	        		   Bitmap myBitmap = null;
					try {
							myBitmap = BitmapLoader.loadBitmap(imgFile.getCanonicalPath(), 100, 100);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
	        		   ImageView chosenImage = (ImageView) findViewById(R.id.chosenpicture);
					   chosenImage.setImageBitmap(myBitmap);
	        	   }
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
        updateSpinner();
    }
    
    public void updateSpinner() {
    	Spinner spinner = (Spinner) findViewById(R.id.memberspinner);
    	String curSelected = (spinner.getSelectedItem() == null) ? "" : spinner.getSelectedItem().toString();
    	int selectedPosition = 0;
    	List<String> members = null;
    	SNetDB461 db = null;
    	try {
    		db = new SNetDB461(controller.DBName());

    		// look up all names in the community table and add to members list
    		RecordSet<CommunityRecord> allMemberInfo = db.COMMUNITYTABLE.readAll();
    		members = new ArrayList<String>(allMemberInfo.size());
    		for(CommunityRecord member : allMemberInfo) {
    			members.add(member.name.toString());
      		}
    		Collections.sort(members);
    		selectedPosition = Collections.binarySearch(members, curSelected);
    		if(selectedPosition < 0)
    			selectedPosition = 0;
    	} catch (DB461Exception e) {
    		Log.e(TAG, e.getMessage());
    	} finally {
    		if ( db != null )
    			db.discard();
    		if(members == null)
    			members = new ArrayList<String>(1);
    	}

    	// attach list to spinner
    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, members);
    	spinner.setAdapter(adapter);
    	spinner.setSelection(selectedPosition);
    }
    
    public void updateFriend(boolean setFriend) {
    	// get current name displayed in drop-down menu
    	Spinner spinner = (Spinner) findViewById(R.id.memberspinner);
    	String name = spinner.getSelectedItem().toString();
    	
    	// call set friend for that name with isFriend = false;
    	try {
			controller.setFriend(new DDNSFullName(name), setFriend);
		} catch (DB461Exception e) {
			Log.e(TAG, e.getMessage());
		}
    }
    
   /**************************************
    * METHODS FOR RPC CALL
    *************************************/
    
    public JSONObject _rpcFetchUpdates(JSONObject args) throws Exception {
		Log.i(TAG, "started fetch updates");
		JSONObject resultObj = controller.fetchUpdatesCallee(args);
		Log.i(TAG, "finished fetch updates");
		return resultObj;
	}

	public JSONObject _rpcFetchPhoto(JSONObject args) throws Exception {
		Log.i(TAG, "started fetch photo");
		JSONObject resultObj = controller.fetchPhotoCallee(args);
		Log.i(TAG, "started fetch photo");
		return resultObj;
	}
    
}
