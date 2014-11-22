package com.example.mytest;




import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;











import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


public class MainActivity extends Activity {

	// Activity request codes
	private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
	public static final int MEDIA_TYPE_IMAGE = 1;
	private String upLoadServerUri = null;
	// directory name to store captured images and videos
	private static final String IMAGE_DIRECTORY_NAME = "Hello Camera";
	private ProgressDialog pDialog;
	private Uri fileUri; // file url to store image/video
	private int serverResponseCode = 0;
	private String imagepath=null;
	private ImageView imgPreview;
	private Button btnCapturePicture,upload;
	private double latitude;
	private double longitude;
	HttpPost httppost;
	HttpResponse response;
	HttpClient httpclient;
	private String cityName=null;
	private String streetname=null;
	private String state=null;
	private String postalcode=null;
	JSONParser jsonParser = new JSONParser();
	private static String url_upload = "http://phpmadmin.mybluemix.net/address.php";
	private static final String TAG_SUCCESS = "success";
	private static final String TAG_MESG = "message";
	GPSTracker gps;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		gps = new GPSTracker(MainActivity.this);
        if(gps.canGetLocation()){
        	
        	 latitude = gps.getLatitude();
        	 longitude = gps.getLongitude();
        	 Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show(); 
        	 // \n is for new line
        	  } else{
        	// can't get location
        	// GPS or Network is not enabled
        	// Ask user to enable GPS/network in settings
        	gps.showSettingsAlert();
        }

		imgPreview = (ImageView) findViewById(R.id.imgPreview);
		btnCapturePicture = (Button) findViewById(R.id.btnCapturePicture);
		upload = (Button)findViewById(R.id.btnupload);
		upLoadServerUri = "http://phpmadmin.mybluemix.net/mail.php";
		

		/*
		 * Capture image button click event
		 */
		btnCapturePicture.setOnClickListener(new View.OnClickListener() {
  
			@Override
			public void onClick(View v) {
				// capture picture
				captureImage();
				
			}
		});

		/*
		 * upload image to server
		 */
		upload.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				new updatelocation().execute();
				
			}
		});

		// Checking camera availability
		if (!isDeviceSupportCamera()) {
			Toast.makeText(getApplicationContext(),
					"Sorry! Your device doesn't support camera",
					Toast.LENGTH_LONG).show();
			// will close the app if the device does't have camera
			finish();
		}
	}

	/**
	 * Checking device has camera hardware or not
	 * */
	private boolean isDeviceSupportCamera() {
		if (getApplicationContext().getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA)) {
			// this device has a camera
			return true;
		} else {
			// no camera on this device
			return false;
		}
	}

	/*
	 * Capturing Camera Image will lauch camera app requrest image capture
	 */
	private void captureImage() {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);

		intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

		// start the image capture Intent
		startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
	}

	/*
	 * Here we store the file url as it will be null after returning from camera
	 * app
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		// save file url in bundle as it will be null on scren orientation
		// changes
		outState.putParcelable("file_uri", fileUri);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		// get the file url
		fileUri = savedInstanceState.getParcelable("file_uri");
	}

	/*
	 * Recording video
	 */
	

	/**
	 * Receiving activity result method will be called after closing the camera
	 * */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// if the result is capturing Image
		if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				// successfully captured the image
				// display it in image view
				previewCapturedImage();
			} else if (resultCode == RESULT_CANCELED) {
				// user cancelled Image capture
				Toast.makeText(getApplicationContext(),
						"User cancelled image capture", Toast.LENGTH_SHORT)
						.show();
			} else {
				// failed to capture image
				Toast.makeText(getApplicationContext(),
						"Sorry! Failed to capture image", Toast.LENGTH_SHORT)
						.show();
			}
		} 
	}

	/*
	 * Display image from a path to ImageView
	 */
	private void previewCapturedImage() {
		try {
			
			imgPreview.setVisibility(View.VISIBLE);

			// bimatp factory
			BitmapFactory.Options options = new BitmapFactory.Options();

			// downsizing image as it throws OutOfMemory Exception for larger
			// images
			options.inSampleSize = 8;
        imagepath = fileUri.getPath();
        
       // String filename=imagepath.substring(imagepath.lastIndexOf("/")+1);
			final Bitmap bitmap = BitmapFactory.decodeFile(fileUri.getPath(),
					options);
		imgPreview.setImageBitmap(bitmap);
		
		
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	
	
	/**
	 * ------------ Helper Methods ---------------------- 
	 * */

	/*
	 * Creating file uri to store image/video
	 */
	public Uri getOutputMediaFileUri(int type) {
		return Uri.fromFile(getOutputMediaFile(type));
	}

	/*
	 * returning image / video
	 */
	private static File getOutputMediaFile(int type) {

		// External sdcard location
		File mediaStorageDir = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
				IMAGE_DIRECTORY_NAME);

		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				Log.d(IMAGE_DIRECTORY_NAME, "Oops! Failed create "
						+ IMAGE_DIRECTORY_NAME + " directory");
				return null;
			}
		}

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
				Locale.getDefault()).format(new Date());
		File mediaFile;
		if (type == MEDIA_TYPE_IMAGE) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator
					+ "IMG_" + timeStamp + ".jpg");
		} 
		 else {
			return null;
		}

		return mediaFile;
	}
	class updatelocation extends AsyncTask<String, String, String>  {
        
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(MainActivity.this);
			pDialog.setMessage("sending Mail please wait..");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(true);
			pDialog.show();
		}
		protected String doInBackground(String... args) {
			Geocoder gcd = new Geocoder(getBaseContext(),   
			         Locale.getDefault());               
			            List<Address>  addresses;    
			            try {    
			            addresses = gcd.getFromLocation(gps.getLatitude(), gps  
			         .getLongitude(), 1);    
			            if (addresses.size() > 0)    
			                  
			             streetname=addresses.get(0).getAddressLine(0) + addresses.get(0).getAddressLine(1); 
			               cityName=addresses.get(0).getLocality();
			               state=addresses.get(0).getAdminArea();
			               
			               //Toast.makeText(getApplicationContext(), "Your Location is - \nstreet: " + streetname + "\ncity: " + cityName + "\nstate: " +state + "\npincode: " +postalcode, Toast.LENGTH_LONG).show();	
			               
			              } catch (IOException e) {              
			              e.printStackTrace();    
			            }  
try
{
    String filename=imagepath.substring(imagepath.lastIndexOf("/")+1);
    String androidId = Settings.Secure.getString(getContentResolver(),Settings.Secure.ANDROID_ID);
	List<NameValuePair> params = new ArrayList<NameValuePair>();
	params.add(new BasicNameValuePair("streetname", streetname));
	params.add(new BasicNameValuePair("cityname", cityName));
	params.add(new BasicNameValuePair("state", state));
	params.add(new BasicNameValuePair("filename", filename));
	params.add(new BasicNameValuePair("deviceid", androidId));
	httpclient=new DefaultHttpClient();
	httppost= new HttpPost(url_upload); // make sure the url is correct.
	httppost.setEntity(new UrlEncodedFormEntity(params));
	//Execute HTTP Post Request
	response=httpclient.execute(httppost);
	ResponseHandler<String> responseHandler = new BasicResponseHandler();
	final String response = httpclient.execute(httppost, responseHandler);
	runOnUiThread(new Runnable() {
	    public void run() {
	    	//Toast.makeText(MainActivity.this,response, Toast.LENGTH_SHORT).show();
	    }
	});
}catch(Exception e){
	
	System.out.println("Exception : " + e.getMessage());
}
uploadFile(imagepath);
		return null;
       } 
		protected void onPostExecute(String file_url) {
			// dismiss the dialog once done
			pDialog.dismiss();
		}

	}
	 public int uploadFile(String sourceFileUri) {
		 String file = sourceFileUri;
		 
         HttpURLConnection conn = null;
         DataOutputStream dos = null;  
         String lineEnd = "\r\n";
         String twoHyphens = "--";
         String boundary = "*****";
         int bytesRead, bytesAvailable, bufferSize;
         byte[] buffer;
         int maxBufferSize = 1 * 1024 * 1024; 
         File sourceFile = new File(sourceFileUri); 
         
         if (!sourceFile.isFile()) {
       	  
	           //dialog.dismiss(); 
	           
	           Log.e("uploadFile", "Source File not exist :"+imagepath);
	           
	           
	            	   
	           
	           
	           return 0;
          
         }
         else
         {
	           try {   // open a URL connection to the Servlet
	                 FileInputStream fileInputStream = new FileInputStream(sourceFile);
	                 URL url = new URL(upLoadServerUri);
	                  
	                 // Open a HTTP  connection to  the URL
	                 conn = (HttpURLConnection) url.openConnection();
	                 conn.setDoInput(true); // Allow Inputs
	                 conn.setDoOutput(true); // Allow Outputs
	                 conn.setUseCaches(false); // Don't use a Cached Copy
	                 conn.setRequestMethod("POST");
	                 conn.setRequestProperty("Connection", "Keep-Alive");
	                 conn.setRequestProperty("ENCTYPE", "multipart/form-data");
	                 conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
	                 conn.setRequestProperty("uploaded_file", file);
	                  
	                 dos = new DataOutputStream(conn.getOutputStream());
	                 dos.writeBytes(twoHyphens + boundary + lineEnd);
	                 dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
		                     + file + "\"" + lineEnd);
	               
	               
	                   dos.writeBytes(lineEnd);
	        
	                 // create a buffer of  maximum size
	                 bytesAvailable = fileInputStream.available();
	        
	                 bufferSize = Math.min(bytesAvailable, maxBufferSize);
	                 buffer = new byte[bufferSize];
	        
	                 // read file and write it into form...
	                 bytesRead = fileInputStream.read(buffer, 0, bufferSize); 
	                    
	                 while (bytesRead > 0) {
	                      
	                   dos.write(buffer, 0, bufferSize);
	                   bytesAvailable = fileInputStream.available();
	                   bufferSize = Math.min(bytesAvailable, maxBufferSize);
	                   bytesRead = fileInputStream.read(buffer, 0, bufferSize);  
	                    
	                  }
	        
	                 // send multipart form data necesssary after file data...
	                 dos.writeBytes(streetname);
	                 dos.writeBytes(lineEnd);
	                 dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
	                 
	        
	                 // Responses from the server (code and message)
	                 serverResponseCode = conn.getResponseCode();
	                 String serverResponseMessage = conn.getResponseMessage();
	                   
	                 Log.i("uploadFile", "HTTP Response is : "
	                         + serverResponseMessage + ": " + serverResponseCode);
	                  
	                 if(serverResponseCode == 200){
	                      
	                     runOnUiThread(new Runnable() {
	                          public void run() {
	                               
	                             // String msg = "File Upload Completed.\n\n See uploaded file here : \n\n"
	                               //             +" http://www.androidexample.com/media/uploads/"
	                              //              +uploadFileName;
	                               
	                            //  messageText.setText(msg);
	                              Toast.makeText(MainActivity.this, "Mail Sent Successfully.Thanks for part of swachhbarah campaign",
	                                           Toast.LENGTH_SHORT).show();
	                          }
	                      });               
	                 }   
	                  
	                 //close the streams //
	                 fileInputStream.close();
	                 dos.flush();
	                 dos.close();
	                    }catch (MalformedURLException ex) {
				        	  
				            //  dialog.dismiss();  
				              ex.printStackTrace();
	                    }catch (Exception e) {
				        	  
				              //dialog.dismiss();  
				              e.printStackTrace();
				             
				             // messageText.setText("Got Exception : see logcat ");
		                     // Toast.makeText(NewtempleActivity.this, "Got Exception : see logcat ", Toast.LENGTH_SHORT).show();
				              Log.e("Upload file to server Exception", "Exception : "  + e.getMessage(), e);  
				          }
		return serverResponseCode;
	 }
	 }
}
