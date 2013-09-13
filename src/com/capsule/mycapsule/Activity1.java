package com.capsule.mycapsule;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

import com.capsule.mycapsule.LocationUtils;
import com.capsule.mycapsule.GeofenceUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.MapFragment;

import android.location.Location;
import android.net.ParseException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.capsule.mycapsule.GeofenceUtils.REMOVE_TYPE;
import com.capsule.mycapsule.GeofenceUtils.REQUEST_TYPE;
import com.capsule.mycapsule.GeofenceRemover;
import com.capsule.mycapsule.GeofenceRequester;
import com.capsule.mycapsule.SimpleGeofenceStore;
import com.capsule.mycapsule.Activity1.GeofenceSampleReceiver;
import com.capsule.mycapsule.SimpleGeofence;
import com.google.android.gms.location.Geofence;

public class Activity1 extends Activity implements LocationListener,
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {

	private GoogleMap mMap;
	
	private double tempLat;
	private double tempLon;
	private double geofenceRadius=100;
	// A request to connect to Location Services
	private LocationRequest mLocationRequest;

	// Stores the current instantiation of the location client in this object
	private LocationClient mLocationClient;

	// sign to update the location
	boolean mUpdatesRequested = true;

	// Handle to SharedPreferences for this app
	SharedPreferences mPrefs;

	// Handle to a SharedPreferences editor
	SharedPreferences.Editor mEditor;

	//////////////////////////////////////////////////
	//for geofences
	//////////////////////////////////////////////////
	private static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;
    private static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS =
            GEOFENCE_EXPIRATION_IN_HOURS * DateUtils.HOUR_IN_MILLIS;
	
	
    // Store the current request
    private REQUEST_TYPE mRequestType;

    // Store the current type of removal
    private REMOVE_TYPE mRemoveType;
	
    // Persistent storage for geofences
    private SimpleGeofenceStore mGeoStore;

    // Store a list of geofences to add
    List<Geofence> mCurrentGeofences;
    
    // Add geofences handler
    private GeofenceRequester mGeofenceRequester;
    // Remove geofences handler
    private GeofenceRemover mGeofenceRemover;
    
 // decimal formats for latitude, longitude, and radius
    private DecimalFormat mLatLngFormat;
    private DecimalFormat mRadiusFormat;
    
    /*
     * An instance of an inner class that receives broadcasts from listeners and from the
     * IntentService that receives geofence transition events
     */
    private GeofenceSampleReceiver mBroadcastReceiver;

    // An intent filter for the broadcast receiver
    private IntentFilter mIntentFilter;

    // Store the list of geofences to remove
    private List<String> mGeofenceIdsToRemove;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// System.out.println("created!");
		setContentView(R.layout.activity1);
		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
				.getMap();
		setUpMapIfNeeded();

		// Open Shared Preferences
		mPrefs = getSharedPreferences(LocationUtils.SHARED_PREFERENCES,
				Context.MODE_PRIVATE);

		// Get an editor
		mEditor = mPrefs.edit();

		// Create a new global location parameters object
		mLocationRequest = LocationRequest.create();
		/*
		 * Set the update interval
		 */
		mLocationRequest
				.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);
		// Use high accuracy
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		// Set the interval ceiling to one minute
		mLocationRequest
				.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);

		/*
		 * Create a new location client, using the enclosing class to handle
		 * callbacks.
		 */
		mLocationClient = new LocationClient(this, this, this);

		final TextView detail = (TextView) findViewById(R.id.textView1poi);
		
		
		//////////////////////////////////////////////
		//geofence
		//////////////////////////////////////////////
		// Set the pattern for the latitude and longitude format
        String latLngPattern = getString(R.string.lat_lng_pattern);
        
        // Set the format for latitude and longitude
        mLatLngFormat = new DecimalFormat(latLngPattern);

        // Localize the format
        mLatLngFormat.applyLocalizedPattern(mLatLngFormat.toLocalizedPattern());

        // Set the pattern for the radius format
        String radiusPattern = getString(R.string.radius_pattern);
        
        // Set the format for the radius
        mRadiusFormat = new DecimalFormat(radiusPattern);

        // Localize the pattern
        mRadiusFormat.applyLocalizedPattern(mRadiusFormat.toLocalizedPattern());

        // Create a new broadcast receiver to receive updates from the listeners and service
        mBroadcastReceiver = new GeofenceSampleReceiver();

        // Create an intent filter for the broadcast receiver
        mIntentFilter = new IntentFilter();

        // Action for broadcast Intents that report successful addition of geofences
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_ADDED);

        // Action for broadcast Intents that report successful removal of geofences
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_REMOVED);
        

        // Action for broadcast Intents containing various types of geofencing errors
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_ERROR);

        // All Location Services sample apps use this category
        mIntentFilter.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);

        // Instantiate a new geofence storage area
        mGeoStore = new SimpleGeofenceStore(this);

        // Instantiate the current List of geofences
        mCurrentGeofences = new ArrayList<Geofence>();

        // Instantiate a Geofence requester
        mGeofenceRequester = new GeofenceRequester(this);

        // Instantiate a Geofence remover
        mGeofenceRemover = new GeofenceRemover(this);
        
        
	}

	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the
		// map.
		if (mMap == null) {
			mMap = ((MapFragment) getFragmentManager().findFragmentById(
					R.id.map)).getMap();
			// Check if we were successful in obtaining the map.
			if (mMap != null) {
				// The Map is verified. It is now safe to manipulate the map.

			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public boolean checkGooglePlayStatus() {
		// Getting status
		int status = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(getBaseContext());

		// Showing status
		if (status == ConnectionResult.SUCCESS)
			return true;
		else {

			int requestCode = 10;
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this,
					requestCode);
			dialog.show();
			return false;
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {

		/*
		 * Google Play services can resolve some errors it detects. If the error
		 * has a resolution, try sending an Intent to start a Google Play
		 * services activity that can resolve error.
		 */
		if (connectionResult.hasResolution()) {
			try {

				// Start an Activity that tries to resolve the error
				connectionResult.startResolutionForResult(this,
						LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

				/*
				 * Thrown if Google Play services canceled the original
				 * PendingIntent
				 */

			} catch (IntentSender.SendIntentException e) {

				// Log the error
				e.printStackTrace();
			}
		} else {

			// If no resolution is available, display a dialog to the user with
			// the error.
			showErrorDialog(connectionResult.getErrorCode());
		}
	}

	/*
	 * Called by Location Services when the request to connect the client
	 * finishes successfully. At this point, you can request the current
	 * location or start periodic updates
	 */
	@Override
	public void onConnected(Bundle bundle) {

		if (mUpdatesRequested) {
			addCapsules();
			startPeriodicUpdates();
		}
	}

	@Override
	public void onDisconnected() {
		// remove all geofences
		mRemoveType = GeofenceUtils.REMOVE_TYPE.INTENT;
		/*
         * Check for Google Play services. Do this after
         * setting the request type. If connecting to Google Play services
         * fails, onActivityResult is eventually called, and it needs to
         * know what type of request was in progress.
         */
        if (!checkGooglePlayStatus()) {

            return;
        }
     // Try to make a removal request
        try {
        /*
         * Remove the geofences represented by the currently-active PendingIntent. If the
         * PendingIntent was removed for some reason, re-create it; since it's always
         * created with FLAG_UPDATE_CURRENT, an identical PendingIntent is always created.
         */
        mGeofenceRemover.removeGeofencesByIntent(mGeofenceRequester.getRequestPendingIntent());

        } catch (UnsupportedOperationException e) {
            // Notify user that previous request hasn't finished.
            Toast.makeText(this, R.string.remove_geofences_already_requested_error,
                        Toast.LENGTH_LONG).show();
        }

	}

	public static class getPOI implements Callable {
		ArrayList<String> list_latlon = new ArrayList();
		List<NameValuePair> nameValuePairs;
		HttpClient httpclient;
		HttpPost httppost;
		HttpResponse response;
		String result;
		JSONArray jArray;
		JSONObject json_data;
		String my_lat;
		String my_lon;
		
		public getPOI(String lat, String lon) {
			my_lat=lat;
			my_lon=lon;
			
		}

		public ArrayList<String> call() {
			try {
				//System.out.println("hahahere");
				httpclient = new DefaultHttpClient();
				httppost = new HttpPost(
						"http://ec2-54-218-168-182.us-west-2.compute.amazonaws.com/mysql2/Query_fixed_loc_var.php");
				nameValuePairs = new ArrayList<NameValuePair>(3);
				nameValuePairs.add(new BasicNameValuePair("lat", my_lat));
				nameValuePairs.add(new BasicNameValuePair("lon", my_lon));
				nameValuePairs.add(new BasicNameValuePair("r", "2000"));
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				response = httpclient.execute(httppost);
				result = EntityUtils
						.toString(response.getEntity());
				
				try {
					jArray = new JSONArray(result);
					json_data = null;

					for (int i = 0; i < jArray.length(); i++) {
						json_data = jArray.getJSONObject(i);
						String point_string = json_data
								.getString("AsText(location)");
						list_latlon.add(point_string.substring(6,
								point_string.indexOf(" ")));
						list_latlon.add(point_string.substring(
								point_string.indexOf(" "),
								point_string.length() - 2));
						// System.out.println("haha"+list_lat.size());

						
						// System.out.println("haha"+point_string.substring(6,point_string.indexOf(" ")));
						// System.out.println("haha"+point_string.substring(point_string.indexOf(" "),point_string.length()-1));
					}
					//System.out.println("haha"+list_latlon.size());

				} catch (JSONException e1) {
					e1.printStackTrace();
				} catch (ParseException e1) {
					e1.printStackTrace();
				}

			} catch (Exception e) {
				System.out.println("error in connection");
				System.out.println(e.toString());
			}
			return list_latlon;
		}
	}
	
	
	public void addCapsules(){
		
		
        if (!checkGooglePlayStatus()) {
        	System.out.println("google play not available!");
            return;
        }
        Location location = mLocationClient.getLastLocation();
        //System.out.println(Double.toString(location.getLatitude()));
		/*
         * Record the request as an ADD. If a connection error occurs,
         * the app can automatically restart the add request if Google Play services
         * can fix the error
         */
		mRequestType = GeofenceUtils.REQUEST_TYPE.ADD;
		/*
         * Check for Google Play services. Do this after
         * setting the request type. If connecting to Google Play services
         * fails, onActivityResult is eventually called, and it needs to
         * know what type of request was in progress.
         */
        
        
		String lat = Double.toString(location.getLatitude());
		String lon = Double.toString(location.getLongitude());
		double lat_db = location.getLatitude();
		double lon_db = location.getLongitude();
		ExecutorService es = Executors.newSingleThreadExecutor();
		 Callable<ArrayList<String>> poiTask = new getPOI(lat,lon);
		 //System.out.println("hahathere");
		 Future<ArrayList<String>> future = es.submit(poiTask);
		 try {
			ArrayList<String> data = future.get();
			System.out.println("haha"+data.size());
			//System.out.println(lat);
			//System.out.println(lon);
			for (int i=0;i<data.size()/2;i++){
				//System.out.println(data.get(2*i+1));
				//System.out.println(data.get(2*i));
				tempLat=Double.parseDouble(data.get(2*i+1));
				tempLon=Double.parseDouble(data.get(2*i));
				mMap.addMarker(new MarkerOptions()
				.position(new LatLng(tempLat,tempLon)));
				
				//add capsules here
				/*
				SimpleGeofence mGeofence=new SimpleGeofence(Double.toString(i),lat_db,lon_db,
			            100,
			            GEOFENCE_EXPIRATION_IN_MILLISECONDS,
			            Geofence.GEOFENCE_TRANSITION_ENTER);
		        mGeoStore.setGeofence(Double.toString(i), mGeofence);
		        mCurrentGeofences.add(mGeofence.toGeofence());
		        */
			}
			/*
			try {
	            // Try to add geofences
	            mGeofenceRequester.addGeofences(mCurrentGeofences);
	        } catch (UnsupportedOperationException e) {
	            // Notify user that previous request hasn't finished.
	            Toast.makeText(this, R.string.add_geofences_already_requested_error,
	                        Toast.LENGTH_LONG).show();
	        }
			*/
		} catch (InterruptedException e2) {
			e2.printStackTrace();
		} catch (ExecutionException e2) {
			e2.printStackTrace();
		}
		
	}
	
	@Override
	public void onLocationChanged(Location location) {
		final String lat = Double.toString(location.getLatitude());
		final String lon = Double.toString(location.getLongitude());
		final double lat_db = location.getLatitude();
		final double lon_db = location.getLongitude();
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
				new LatLng(lat_db, lon_db), 14));
		 
		
		
		
		/*
		new Thread(new Runnable() {
			public void run() {
				// http post
				try {
					final ArrayList<String> list_lat = new ArrayList();
					final ArrayList<String> list_lon = new ArrayList();

					// System.out.println("getting here!");
					HttpClient httpclient = new DefaultHttpClient();
					HttpPost httppost = new HttpPost(
							"http://capsule.duniap.com/mysql2/Query_fixed_loc.php");
					nameValuePairs = new ArrayList<NameValuePair>(3);
					nameValuePairs.add(new BasicNameValuePair("lat", lat));
					nameValuePairs.add(new BasicNameValuePair("lon", lon));
					nameValuePairs.add(new BasicNameValuePair("r", "2000"));
					httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
					HttpResponse response = httpclient.execute(httppost);
					final String result = EntityUtils.toString(response
							.getEntity());
					// System.out.println("haha"+result);

					// paring data
					try {
						JSONArray jArray = new JSONArray(result);
						JSONObject json_data = null;

						for (int i = 0; i < jArray.length(); i++) {
							json_data = jArray.getJSONObject(i);
							String point_string = json_data
									.getString("AsText(location)");
							list_lon.add(point_string.substring(6,
									point_string.indexOf(" ")));
							list_lat.add(point_string.substring(
									point_string.indexOf(" "),
									point_string.length() - 2));
							// System.out.println("haha"+list_lat.size());

							// System.out.println("haha"+point_string.substring(6,point_string.indexOf(" ")));
							// System.out.println("haha"+point_string.substring(point_string.indexOf(" "),point_string.length()-1));
						}
						// System.out.println("haha"+list_lat.size());

						Handler handler = new Handler(Looper.getMainLooper());
						handler.post(new Runnable() {
							public void run() {
								System.out.println("haha" + list_lat.size());
								for (int i = 0; i < list_lat.size(); i++) {
									mMap.addMarker(new MarkerOptions()
											.position(new LatLng(Double
													.parseDouble(list_lat
															.get(i)), Double
													.parseDouble(list_lon
															.get(i)))));
								}
							}
						});

					} catch (JSONException e1) {
						Toast.makeText(getBaseContext(), "No POI found",
								Toast.LENGTH_LONG).show();
					} catch (ParseException e1) {
						e1.printStackTrace();
					}

				} catch (Exception e) {
					System.out.println("error in connection");
					System.out.println(e.toString());
				}
			}
		}).start();

		// System.out.println("haha"+list_lat.size());

		// System.out.println("haha"+Double.parseDouble(list_lat.get(i))+" "+Double.parseDouble(list_lon.get(i)));
		*/
	}

	private void showErrorDialog(int errorCode) {

		// Get the error dialog from Google Play services
		Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode,
				this, LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

		// If Google Play services can provide an error dialog
		if (errorDialog != null) {

			// Create a new DialogFragment in which to show the error dialog
			ErrorDialogFragment errorFragment = new ErrorDialogFragment();

			// Set the dialog in the DialogFragment
			errorFragment.setDialog(errorDialog);

			// Show the error dialog in the DialogFragment
			// errorFragment.show(getSupportFragmentManager(),
			// LocationUtils.APPTAG);
		}
	}

	public static class ErrorDialogFragment extends DialogFragment {

		// Global field to contain the error dialog
		private Dialog mDialog;

		/**
		 * Default constructor. Sets the dialog field to null
		 */
		public ErrorDialogFragment() {
			super();
			mDialog = null;
		}

		/**
		 * Set the dialog to display
		 * 
		 * @param dialog
		 *            An error dialog
		 */
		public void setDialog(Dialog dialog) {
			mDialog = dialog;
		}

		/*
		 * This method must return a Dialog to the DialogFragment.
		 */
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return mDialog;
		}
	}

	/**
	 * In response to a request to start updates, send a request to Location
	 * Services
	 */
	private void startPeriodicUpdates() {
		mLocationClient.requestLocationUpdates(mLocationRequest, this);
		// System.out.println("start update!");
	}

	private void stopPeriodicUpdates() {
		mLocationClient.removeLocationUpdates(this);
	}

	/*
	 * Called when the Activity is no longer visible at all. Stop updates and
	 * disconnect.
	 */

	@Override
	public void onStop() {

		// If the client is connected
		if (mLocationClient.isConnected()) {
			stopPeriodicUpdates();
		}

		// After disconnect() is called, the client is considered "dead".
		mLocationClient.disconnect();

		super.onStop();
	}

	/*
	 * Called when the Activity is going into the background. Parts of the UI
	 * may be visible, but the Activity is inactive.
	 */
	@Override
	public void onPause() {

		// Save the current setting for updates
		mEditor.putBoolean(LocationUtils.KEY_UPDATES_REQUESTED,
				mUpdatesRequested);
		mEditor.commit();

		super.onPause();
	}

	/*
	 * Called when the Activity is restarted, even before it becomes visible.
	 */
	@Override
	public void onStart() {

		super.onStart();

		/*
		 * Connect the client. Don't re-start any requests here; instead, wait
		 * for onResume()
		 */
		mLocationClient.connect();

	}

	/*
	 * Called when the system detects that this Activity is now visible.
	 */

	@Override
	public void onResume() {
		super.onResume();

		if (mPrefs.contains(LocationUtils.KEY_UPDATES_REQUESTED)) {
			// If the app already has a setting for getting location updates,
			// get it
			// mUpdatesRequested =
			// mPrefs.getBoolean(LocationUtils.KEY_UPDATES_REQUESTED, false);
			mUpdatesRequested = true;

		} else {
			// Otherwise, turn off location updates until requested
			// mEditor.putBoolean(LocationUtils.KEY_UPDATES_REQUESTED, false);
			mEditor.putBoolean(LocationUtils.KEY_UPDATES_REQUESTED, true);
			mEditor.commit();
		}

	}
/////////////////////////////////////////////////////////////////////////////////
//defined for geofence
/////////////////////////////////////////////////////////////////////////////////
    /**
     * Define a Broadcast receiver that receives updates from connection listeners and
     * the geofence transition service.
     */
    public class GeofenceSampleReceiver extends BroadcastReceiver {
        /*
         * Define the required method for broadcast receivers
         * This method is invoked when a broadcast Intent triggers the receiver
         */
        @Override
        public void onReceive(Context context, Intent intent) {

            // Check the action code and determine what to do
            String action = intent.getAction();

            // Intent contains information about errors in adding or removing geofences
            if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_ERROR)) {

                handleGeofenceError(context, intent);

            // Intent contains information about successful addition or removal of geofences
            } else if (
                    TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCES_ADDED)
                    ||
                    TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCES_REMOVED)) {

                handleGeofenceStatus(context, intent);

            // Intent contains information about a geofence transition
            } else if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_TRANSITION)) {

                handleGeofenceTransition(context, intent);

            // The Intent contained an invalid action
            } else {
                Log.e(GeofenceUtils.APPTAG, getString(R.string.invalid_action_detail, action));
                Toast.makeText(context, R.string.invalid_action, Toast.LENGTH_LONG).show();
            }
        }

        /**
         * If you want to display a UI message about adding or removing geofences, put it here.
         *
         * @param context A Context for this component
         * @param intent The received broadcast Intent
         */
        private void handleGeofenceStatus(Context context, Intent intent) {

        }

        /**
         * Report geofence transitions to the UI
         *
         * @param context A Context for this component
         * @param intent The Intent containing the transition
         */
        private void handleGeofenceTransition(Context context, Intent intent) {
            /*
             * If you want to change the UI when a transition occurs, put the code
             * here. The current design of the app uses a notification to inform the
             * user that a transition has occurred.
             */
        }

        /**
         * Report addition or removal errors to the UI, using a Toast
         *
         * @param intent A broadcast Intent sent by ReceiveTransitionsIntentService
         */
        private void handleGeofenceError(Context context, Intent intent) {
            String msg = intent.getStringExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS);
            Log.e(GeofenceUtils.APPTAG, msg);
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
    }
   
	
    /*
     * Handle results returned to this Activity by other Activities started with
     * startActivityForResult(). In particular, the method onConnectionFailed() in
     * GeofenceRemover and GeofenceRequester may call startResolutionForResult() to
     * start an Activity that handles Google Play services problems. The result of this
     * call returns here, to onActivityResult.
     * calls
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Choose what to do based on the request code
        switch (requestCode) {

            // If the request code matches the code sent in onConnectionFailed
            case GeofenceUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST :

                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:

                        // If the request was to add geofences
                        if (GeofenceUtils.REQUEST_TYPE.ADD == mRequestType) {

                            // Toggle the request flag and send a new request
                            mGeofenceRequester.setInProgressFlag(false);

                            // Restart the process of adding the current geofences
                            mGeofenceRequester.addGeofences(mCurrentGeofences);

                        // If the request was to remove geofences
                        } else if (GeofenceUtils.REQUEST_TYPE.REMOVE == mRequestType ){

                            // Toggle the removal flag and send a new removal request
                            mGeofenceRemover.setInProgressFlag(false);

                            // If the removal was by Intent
                            if (GeofenceUtils.REMOVE_TYPE.INTENT == mRemoveType) {

                                // Restart the removal of all geofences for the PendingIntent
                                mGeofenceRemover.removeGeofencesByIntent(
                                    mGeofenceRequester.getRequestPendingIntent());

                            // If the removal was by a List of geofence IDs
                            } else {

                                // Restart the removal of the geofence list
                                mGeofenceRemover.removeGeofencesById(mGeofenceIdsToRemove);
                            }
                        }
                    break;

                    // If any other result was returned by Google Play services
                    default:

                        // Report that Google Play services was unable to resolve the problem.
                        Log.d(GeofenceUtils.APPTAG, getString(R.string.no_resolution));
                }

            // If any other request code was received
            default:
               // Report that this Activity received an unknown requestCode
               Log.d(GeofenceUtils.APPTAG,
                       getString(R.string.unknown_activity_request_code, requestCode));

               break;
        }
    }
}
