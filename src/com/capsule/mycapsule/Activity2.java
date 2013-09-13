package com.capsule.mycapsule;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.os.Bundle;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import ar.com.daidalos.afiledialog.FileChooserActivity;
import ar.com.daidalos.afiledialog.FileChooserDialog;


public class Activity2 extends Activity {
	int serverResponseCode = 0;
	ProgressDialog dialog = null;      
	/**********  File Path *************/
	String uploadFilePath = null;
	String uploadFileName = null;
	/************* Php script path ****************/
	String upLoadServerUri = "http://ec2-54-218-168-182.us-west-2.compute.amazonaws.com/mysql2/UploadToServer.php";
	TextView messageText;
	Button btn;
	Button uploadButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_activity2);
		btn = (Button) this.findViewById(R.id.activity2_button1);
		uploadButton = (Button)findViewById(R.id.activity2_button2);
		messageText  = (TextView)findViewById(R.id.activity2_textView1);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Create the dialog.
				FileChooserDialog dialog = new FileChooserDialog(Activity2.this);

				// Assign listener for the select event.
				dialog.addListener(Activity2.this.onFileSelectedListener);

				// Activate the button for create files.
				dialog.setCanCreateFiles(true);

				// Activate the confirmation dialogs.
				dialog.setShowConfirmation(true, true);

				// Show the dialog.
				dialog.show();
			}
		});

		uploadButton.setOnClickListener(new OnClickListener() {            
			@Override
			public void onClick(View v) {

				dialog = ProgressDialog.show(Activity2.this, "", "Uploading file...", true);

				new Thread(new Runnable() {
					public void run() {
						runOnUiThread(new Runnable() {
							public void run() {
								messageText.setText("uploading started.....");
							}
						});                      

						uploadFile(uploadFilePath);

					}
				}).start();        
			}
		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity2, menu);
		return true;
	}

	private FileChooserDialog.OnFileSelectedListener onFileSelectedListener = new FileChooserDialog.OnFileSelectedListener() {
		public void onFileSelected(Dialog source, File file) {
			source.hide();
			Toast toast = Toast.makeText(Activity2.this, "File selected: " + file.getName(), Toast.LENGTH_LONG);
			toast.show();
			//preparing upload
			uploadFilePath=file.getAbsolutePath();
			uploadFileName=file.getName();
			messageText.setText("Uploading file path :"+uploadFilePath);
		}
		public void onFileSelected(Dialog source, File folder, String name) {
			source.hide();
			Toast toast = Toast.makeText(Activity2.this, "File created: " + folder.getName() + "/" + name, Toast.LENGTH_LONG);
			toast.show();
		}
	};

	public int uploadFile(String sourceFileUri) {


		String fileName = sourceFileUri;

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

			dialog.dismiss(); 

			Log.e("uploadFile", "Source File not exist :"
					+uploadFilePath);

			runOnUiThread(new Runnable() {
				public void run() {
					messageText.setText("Source File not exist :"
							+uploadFilePath);
				}
			}); 

			return 0;

		}
		else
		{
			try { 

				// open a URL connection to the Servlet
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
				conn.setRequestProperty("uploaded_file", fileName); 

				dos = new DataOutputStream(conn.getOutputStream());

				dos.writeBytes(twoHyphens + boundary + lineEnd); 
				dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + fileName + "\"" + lineEnd);

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

							String msg = "File Upload Completed.";

							messageText.setText(msg);
							Toast.makeText(Activity2.this, "File Upload Complete.", 
									Toast.LENGTH_SHORT).show();
						}
					});                
				}    

				//close the streams //
				fileInputStream.close();
				dos.flush();
				dos.close();

			} catch (MalformedURLException ex) {

				dialog.dismiss();  
				ex.printStackTrace();

				runOnUiThread(new Runnable() {
					public void run() {
						messageText.setText("MalformedURLException Exception : check script url.");
						Toast.makeText(Activity2.this, "MalformedURLException", 
								Toast.LENGTH_SHORT).show();
					}
				});

				Log.e("Upload file to server", "error: " + ex.getMessage(), ex);  
			} catch (Exception e) {

				dialog.dismiss();  
				e.printStackTrace();

				runOnUiThread(new Runnable() {
					public void run() {
						messageText.setText("Got Exception : see logcat ");
						Toast.makeText(Activity2.this, "Got Exception : see logcat ", 
								Toast.LENGTH_SHORT).show();
					}
				});
				Log.e("Upload file to server Exception", "Exception : "
						+ e.getMessage(), e);  
			}
			dialog.dismiss();       
			return serverResponseCode; 

		} // End else block 
	} 

}
