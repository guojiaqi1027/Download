package com.example.download;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.download.util.DownLoadTask;
import com.example.download.util.DownloadThread;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
/**
 * This Sample app is used as an assignment on Mobile App course in ECE department
 * 
 * @author GawainGuo RUID 154001652
 *
 */
public class MainActivity extends ListActivity {
	/**
	 * Define types of Handler message
	 */
	public static final int DOWNLOAD_COMPLETE=0;
	public static final int URL_EXCEPTION=1;
	public static final int IO_EXCEPTION=2;
	public static final int UI_UPDATE=3;
	public static final int SPEED_MEASURE=4;
	public static final int NETWORK_UPDATE=5;
	
	/**
	 * Define download Task and Threads
	 */
	public DownLoadTask currentTask=null;
	public Thread updateThread=null;
	public Thread speedMeasureThread=null;
	public Thread networkStatusThread=null;
	public boolean onThread=true;
	
	/**
	 * Define UI components
	 */
	public Activity currentActivity;
	public ListView fileView=null;
	public Button addButton=null;
	public Button pauseButton=null;
	public EditText urlText=null;
	public EditText fileNameText=null;
	public TextView notationText=null;
	public TextView downloadFile=null;
	public TextView downloadFileSize=null;
	public TextView downloadSpeed=null;
	public TextView downloadNetworkType=null;
	public TextView downloadPercent=null;
	public int networkType=0;
	public int networkSubType=0;
	public int speed=0;
	public long previousSize=0;
	
	/**
	 * Define Listview Adapter Components
	 */
	List<Map<String, Object>> list=null;
	public String[] tag={"fileName","size"};
	public int[] id={R.id.filename_text,R.id.size_text};
	public SimpleAdapter listAdapter;
	public HashMap<String, String> fileType=null;
	
	/**
	 * Define Handler
	 */
	public Handler downloadHandler=null;
	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		currentActivity=this;
		onThread=true;
		
		setContentView(R.layout.activity_main);
		addButton=(Button)findViewById(R.id.download_button);
		addButton.setOnClickListener(listener);
		pauseButton=(Button)findViewById(R.id.pause_button);
		pauseButton.setOnClickListener(listener);
		urlText=(EditText)findViewById(R.id.url_text);
		fileNameText=(EditText)findViewById(R.id.file_text);
		downloadFile=(TextView)findViewById(R.id.filename_downloadpanel);
		downloadFileSize=(TextView)findViewById(R.id.size_downloadpanel);
		downloadSpeed=(TextView)findViewById(R.id.speed_downloadpanel);
		downloadNetworkType=(TextView)findViewById(R.id.network_type);
		downloadPercent=(TextView)findViewById(R.id.percent_downloadpanel);

		list=new ArrayList<Map<String,Object>>();
		listAdapter=new SimpleAdapter(this, list, R.layout.list_layout, tag, id);
		setListAdapter(listAdapter);
		fileType=new HashMap<String,String>();
		initialFileType(fileType);
		
		downloadHandler=new Handler(){
			public void handleMessage(Message msg){
				switch(msg.what){
				case DOWNLOAD_COMPLETE:
					/*
					 * Download Complete 
					 * Update ListView
					 */
					Log.v("MAINACTIVITY_DOWNLOADHANDLER_COMPLETE", "DOWNLOAD_COMPLETE");
					Map<String, Object> map=new HashMap<String, Object>();
					DownLoadTask completeTask=(DownLoadTask) msg.obj;
					map.put("fileName", completeTask.getPath());
					map.put("size", String.valueOf(completeTask.getFileSize()/1024)+"kB");
					list.add(map);
					listAdapter.notifyDataSetChanged();
					currentTask=null;
					break;
				case URL_EXCEPTION:
					break;
				case IO_EXCEPTION:
					/*
					 * IO Exception
					 * Start a new download thread
					 */
					Thread download=new Thread(new DownloadThread(currentTask, downloadHandler, currentActivity));
					download.start();
					break;
				case UI_UPDATE:
					updateUI();
					//Log.v("MAINACTIVITY_DOWNLOADHANDLER_UI_UPDATE", "handle ui thread");
					break;
				case SPEED_MEASURE:
					updateSpeed();
					break;
				}
			}
		};
		/**
		 * Update UI Components each 100ms
		 */
		updateThread=new Thread(new Runnable(){

			@SuppressWarnings("static-access")
			@Override
			public void run() {
				while(onThread){
					try {
						Thread.currentThread().sleep(20);
						Message msg=new Message();
						msg.what=UI_UPDATE;
						downloadHandler.sendMessage(msg);
						
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}
			
		});
		new Thread(updateThread).start();
		/**
		 * Update speed each 500 ms
		 */
		speedMeasureThread=new Thread(new Runnable(){

			@SuppressWarnings("static-access")
			@Override
			public void run() {
				// TODO Auto-generated method stub
				while(onThread){
					try {
						Thread.currentThread().sleep(500);
						Message msg=new Message();
						msg.what=SPEED_MEASURE;
						downloadHandler.sendMessage(msg);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				
			}
			
		});
		new Thread(speedMeasureThread).start();
		/**
		 * Update the Network time each 500ms
		 */
		networkStatusThread=new Thread(new Runnable(){

			@SuppressWarnings("static-access")
			@Override
			public void run() {
				while(onThread){
					try {
						
						Thread.currentThread().sleep(500);
						ConnectivityManager mManager=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
						NetworkInfo mInfor=mManager.getActiveNetworkInfo();
						TelephonyManager tManager=(TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
						if(mInfor!=null&&mInfor.isConnected()){
							networkType=mInfor.getType();
						}
						else networkType=-1;
						networkSubType=tManager.getNetworkType();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				
			}
			
		});
		new Thread(networkStatusThread).start();
	}
	/**
	 * On Button Click Listener
	 * AddButton to Add a New Task
	 * Stop/Resume Button to pause or continue a task
	 */
	private OnClickListener listener=new OnClickListener(){

		@Override
		public void onClick(View view) {
			/**
			 * Add Button Click
			 */
			if(view.equals(addButton)){
				/*
				 * Current no download
				 */
				if(currentTask==null){
					{
						/*
						 * Url and Filename is not empty
						 */
						if(urlText.getText().toString()!=null&&fileNameText.getText().toString()!=null){
							currentTask=new DownLoadTask(fileNameText.getText().toString(),urlText.getText().toString());
							//currentTask=new DownLoadTask(fileNameText.getText().toString(),"http://www.winlab.rutgers.edu/~janne/mobisys14gesturesecurity.pdf");
							Thread downloadthread=new Thread(new DownloadThread(currentTask, downloadHandler,currentActivity));
							downloadthread.start();
							Log.v("MAIN_ACTIVITY_DOWNLOADSTART", currentTask.getUrl());
							
						}
						else{
							
						}
					}
					
				}
			}
			/**
			 * Pause Button Click
			 */
			else if(view.equals(pauseButton)){
				if(currentTask!=null&&currentTask.onDownload==true){
					currentTask.onDownload=false;
				}
				else if(currentTask!=null&&currentTask.onDownload==false){
					Log.v("MAIN_ACTIVITY_PAUSE","RESUME_DOWNLOAD");
					currentTask.onDownload=true;
					Thread downloadthread=new Thread(new DownloadThread(currentTask, downloadHandler, currentActivity));
					downloadthread.start();
				}
			}
			
		}
		
	};
	/**
	 * Update UI Components in each 100ms
	 * If current task is not null
	 */
	public void updateUI(){
		if(currentTask!=null){
			downloadFile.setText(currentTask.getPath());
			downloadFileSize.setText(String.valueOf(currentTask.getCompleteSize()/1024)+"kb/"+String.valueOf(currentTask.getFileSize()/1024)+"kb");
			if(currentTask.getFileSize()!=0){
				double percent_d=(double)currentTask.getCompleteSize()/1024/(currentTask.getFileSize()/1024)*100;
				DecimalFormat df=new DecimalFormat("#.00");
				String percent_result=df.format(percent_d);
				downloadPercent.setText(percent_result+"%");
			}
			downloadSpeed.setText(String.valueOf(speed)+"kb/s "+String.valueOf(currentTask.latency)+"ms");
			/**
			 * Update Network type
			 * Type include WIFI and Mobile
			 * In mobile also includes 2G, 3G, 4G, and LTE
			 */
			switch(networkType){
			case -1:
				downloadNetworkType.setText("DISCONNECTED");
				break;
			case ConnectivityManager.TYPE_WIFI:
				downloadNetworkType.setText("WIFI");
				break;
			case ConnectivityManager.TYPE_MOBILE:{
				switch(networkSubType){
				case TelephonyManager.NETWORK_TYPE_EDGE:
					downloadNetworkType.setText("2G");
					break;
				case TelephonyManager.NETWORK_TYPE_HSDPA:
					downloadNetworkType.setText("3G");
					break;
				case TelephonyManager.NETWORK_TYPE_HSPAP:
					downloadNetworkType.setText("4G");
					break;
				}
			}
			break;
			}
			
		}
		else{
			downloadFile.setText("File");
			downloadFileSize.setText("File Size");
			downloadSpeed.setText("0kB/S");
			downloadPercent.setText("0");
		}
	}
	/**
	 * Update download speed each 500ms
	 * Using (currentsize-previoussize)/time;
	 */
	public void updateSpeed(){
		if(currentTask!=null){
			speed=(int)(currentTask.getCompleteSize()-previousSize)/1024*2;
			previousSize=currentTask.getCompleteSize();
		}
	}
	/**
	 * get Filetype from fileName
	 * @param fileName filetype from download file
	 * @return String fileType ignore "."
	 */
	public String getFileType(String fileName){
		if(fileName.length()<=0){
			return null;
		}
		int i=fileName.length()-1;
		while(i>=0&&fileName.charAt(i)!='.'){
			i--;
		}
		if(i==0||i==fileName.length()-1){
			return null;
		}
		return fileName.substring(i+1,fileName.length());
	}
	/**
	 * Open the file when click on item
	 */
	@Override
	protected void onListItemClick(ListView l,View v,int position, long id){
		String fileName=list.get(position).get("fileName").toString();
		String type=getFileType(fileName);
		Intent intent=new Intent("android.intent.action.VIEW");
		intent.addCategory("android.intent.category.DEFAULT");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Uri uri=Uri.fromFile(new File(Environment.getExternalStorageDirectory()+"/"+fileName));
		intent.setDataAndType(uri,fileType.get(type));
		startActivity(intent);
		
		
	}
	/**
	 * Initial FileType map
	 * @param fileTypeMap HashMap to store all possible type
	 */
	public void initialFileType(HashMap<String,String> fileTypeMap){
		fileTypeMap.put("pdf", "application/pdf");
		fileTypeMap.put("jpg", "image/*");
		fileTypeMap.put("jpeg", "image/*");
		fileTypeMap.put("bmp", "image/*");
		fileTypeMap.put("png", "image/*");
		fileTypeMap.put("txt", "text/plain");
		
	}
	/**
	 * Stop threads onPause
	 */
	@Override
	protected void onPause(){
		super.onPause();
		onThread=false;
	}
	/**
	 * Restart threads onResume
	 */
	@Override
	protected void onResume(){
		super.onResume();
		if(onThread==false){
			onThread=true;
			new Thread(updateThread).start();
			new Thread(speedMeasureThread).start();
			new Thread(networkStatusThread).start();
		}
	}
}
