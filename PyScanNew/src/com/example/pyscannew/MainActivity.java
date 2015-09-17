package com.example.pyscannew;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.example.pyscannew.db.DBHelper;
import com.example.pyscannew.model.Data;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSV;
import au.com.bytecode.opencsv.CSVWriteProc;
import au.com.bytecode.opencsv.CSVWriter;

public class MainActivity extends AppCompatActivity {
	private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;
    private DBHelper db;
    private static final String TAG = "MainActivity";

    TextView scanText;
    Button scanButton;

    ImageScanner scanner;
    
    SpannableString buttonText;
    
    FrameLayout preview;
    
    File csvDirectory;
	String csvFilename = "";
	File csvPathFile;
	String csvPath = "";
    
	CSV csv;
	FileWriter fw;
	CSVWriter cw;
	
	String[] data_array;
	String[] data_array_prev = {"",""};
	
    int flag = 0;
    private boolean barcodeScanned = true;
    private boolean previewing = true;

    static {
        System.loadLibrary("iconv");
    } 
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		SpannableString s = new SpannableString("PyScan");
        s.setSpan(new TypefaceSpan(this, "bebas_neue.otf"), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new RelativeSizeSpan(1.4f), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);  
        
        ActionBar bar = getSupportActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#00A78E")));
        bar.setTitle(s);
        
        
     	     			
        //autoFocusHandler = new Handler();
        //mCamera = getCameraInstance();

        /* Instance barcode scanner */
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        //mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
        //preview = (FrameLayout)findViewById(R.id.cameraPreview);
        //preview.addView(mPreview);

        scanText = (TextView)findViewById(R.id.scanText);

        scanButton = (Button)findViewById(R.id.ScanButton);
        
        buttonText = new SpannableString("START SCAN");
//        buttonText.setSpan(new TypefaceSpan(this, "icomoon.ttf"), 0, buttonText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        scanButton.setText(buttonText);
        
        buttonText = new SpannableString("CLICK THE BUTTON BELOW TO START SCANNING");
//        buttonText.setSpan(new TypefaceSpan(this, "bebas_neue.otf"), 0, buttonText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        scanText.setText(buttonText);
        
        //scanText.setVisibility(View.INVISIBLE);
        
        scanButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                	if (flag == 0){
                		autoFocusHandler = new Handler();
                		mCamera = getCameraInstance();
        	        	mPreview = new CameraPreview(MainActivity.this, mCamera, previewCb, autoFocusCB);
        	            preview = (FrameLayout)findViewById(R.id.cameraPreview);
                		preview.addView(mPreview);
                		flag = 1;
                	}
                	
                    if (barcodeScanned) {
                        barcodeScanned = false;
                        scanText.setVisibility(View.INVISIBLE);
                        buttonText = new SpannableString("SCANNING...");
//                        buttonText.setSpan(new TypefaceSpan(MainActivity.this, "bebas_neue.otf"), 0, buttonText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        scanButton.setText(buttonText);
                        mCamera.setPreviewCallback(previewCb);
                        mCamera.startPreview();
                        previewing = true;
                        //mCamera.autoFocus(autoFocusCB);
                    }
                }
            });
        
        db = new DBHelper(MainActivity.this);
	}
	
	@Override
	 public void onPause() {
	        super.onPause();
	        releaseCamera();
	    }
	
	@Override
	 public void onResume() {
		super.onResume();
	        if (flag == 1){
	        	autoFocusHandler = new Handler();
	        	mCamera = getCameraInstance();
	        	preview.removeAllViews();
	        	mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
	            preview = (FrameLayout)findViewById(R.id.cameraPreview);
	        	preview.addView(mPreview);
	        	mCamera.setPreviewCallback(previewCb);
                mCamera.startPreview();
                previewing = true;
                //mCamera.autoFocus(autoFocusCB);
                scanText.setVisibility(View.INVISIBLE);
	        }
	        
	    }

	    /** A safe way to get an instance of the Camera object. */
	    public static Camera getCameraInstance(){
	        Camera c = null;
	        try {
	            c = Camera.open();
	        } catch (Exception e){
	        }
	        return c;
	    }

	    private void releaseCamera() {
	        if (mCamera != null) {
	            previewing = false;
	            mCamera.setPreviewCallback(null);
	            mCamera.release();
	            mCamera = null;
	        }
	    }

	    public Runnable doAutoFocus = new Runnable() {
	            public void run() {
	                if (previewing){
	                    mCamera.autoFocus(autoFocusCB);
	                }
	                
	            }
	        };

	    PreviewCallback previewCb = new PreviewCallback() {
	            public void onPreviewFrame(byte[] data, Camera camera) {
	                Camera.Parameters parameters = camera.getParameters();
	                Size size = parameters.getPreviewSize();

	                Image barcode = new Image(size.width, size.height, "Y800");
	                barcode.setData(data);

	                int result = scanner.scanImage(barcode);
	                
	                if (result != 0) {
	                    previewing = false;
	                    mCamera.setPreviewCallback(null);
	                    mCamera.stopPreview();
	                    String qr_result = "";
	                    SymbolSet syms = scanner.getResults();
	                    for (Symbol sym : syms) {
	                    	qr_result = sym.getData();
	                    	buttonText = new SpannableString("QR DATA: " + qr_result);
//	                        buttonText.setSpan(new TypefaceSpan(MainActivity.this, "bebas_neue.otf"), 0, buttonText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	                    	scanText.setText(buttonText);
	                    	scanText.setVisibility(View.VISIBLE);
	                    	buttonText = new SpannableString("START SCAN");
//	                        buttonText.setSpan(new TypefaceSpan(MainActivity.this, "icomoon.ttf"), 0, buttonText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	                        scanButton.setText(buttonText);
	                        barcodeScanned = true;
	                       
	                    }
    					
    					Data newdata = new Data(qr_result);
    					boolean success = db.insertData(newdata);
    					String message = "Failed to Save!!";
    					if(success) {
    						message = "Saved Successfully!!";
    					}
    					Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
						
    					
	                }
	            }
	        };

	    // Mimic continuous auto-focusing
	    Camera.AutoFocusCallback autoFocusCB = new Camera.AutoFocusCallback() {
	            public void onAutoFocus(boolean success, Camera camera) {
	                autoFocusHandler.postDelayed(doAutoFocus, 1000);
	               
	            }
	        };

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_about) {
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
 			builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
 			           public void onClick(DialogInterface dialog, int id) {
 			               dialog.dismiss();
 			           }
 			       });
 			builder.setTitle("About PyScan");
 			SpannableString s = new SpannableString("PyScan is the official QR Code scanner app for PyCon India 2015.");
 	        s.setSpan(new TypefaceSpan(MainActivity.this, "Roboto-Light.ttf"), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
 			TextView msg = new TextView(MainActivity.this);
 			msg.setTextSize(18);
 			msg.setPadding(30, 10, 30, 10);
 			msg.setText(s);
 			msg.setMovementMethod(LinkMovementMethod.getInstance());
 	        builder.setView(msg);
 	        //builder.setIcon(R.drawable.ic_action_about_1);
 			AlertDialog dialogHelp = builder.create();
 			dialogHelp.setCancelable(false);
 			
 			dialogHelp.show();
			return true;
		} else if(id == R.id.export_as_csv) {
			try {
				csv = CSV
		                .separator(',')
		                .noQuote()
		                .skipLines(1)
		                .charset("UTF-8")
		                .create();
		         
		        csvDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		        csvFilename = "pyscan_data.csv";
		     	csvPathFile = new File(csvDirectory, csvFilename);
		     	csvPath = csvPathFile.getAbsolutePath();
		     	
		     	File file = new File(csvPath);
		     	boolean s = file.delete();
		     	if(!s) {
		     		Toast.makeText(MainActivity.this, "Aww Crap! Delete pyscan_data.csv or bad things will happen!!!", Toast.LENGTH_LONG).show();
		     	}
	     		csv.write(csvPath, new CSVWriteProc() {
	     	        public void process(CSVWriter out) {
	     	            out.writeNext("ticketNo");
	     	        }
	     	    });
				fw = new FileWriter(csvPath, true);
				cw = new CSVWriter(fw);
				final ArrayList<Data> dataList = db.getData(); 
				csv.write(fw, new CSVWriteProc() {
	    			public void process(CSVWriter out) {
	    				for(Data data : dataList) {
	    					out.writeNext(data.getValue());
	    				}
	    		    }
	    		});
				
				cw.close();
				fw.close();
				Toast.makeText(MainActivity.this, "Exported!!", Toast.LENGTH_SHORT).show();
			} catch(Exception e) {
				Log.e(TAG, e.getMessage());
			}
		}
		return super.onOptionsItemSelected(item);
	}
}
