package com.example.download.util;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
/**
 * Download Thread
 * Download file from HTTP url and store in path
 * @author GawainGuo
 *
 */
public class DownloadThread implements Runnable {
	DownLoadTask task;
	Handler mHandler;
	Activity mActivity;
	/**
	 * Constructor create a thread with a task
	 * @param download
	 */
	public DownloadThread(DownLoadTask download, Handler handler, Activity m){
		task=download;
		mHandler=handler;
		mActivity=m;
	}
	/**
	 * 
	 */
	public void run(){
		try {
			int count=0;
			/*
			 * Connect URL connection and open file stream
			 * Set total size from URL
			 */
			boolean latency=true;
			Stopwatch timer=new Stopwatch();
			URL url=new URL(task.getUrl());
			HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
			urlConnection.setRequestMethod("GET");
			if(task.getFileSize()!=0){
				urlConnection.setRequestProperty("Range", "Bytes="+String.valueOf(task.getCompleteSize()+"-"+String.valueOf(task.getFileSize())));
			}
			urlConnection.connect();
			if(task.getFileSize()==0){
				long totalsize=urlConnection.getContentLength();
				task.setFileSize(totalsize);
				System.out.println(totalsize);
			}
			Log.v("DOWNLOAD_THREAD_CONNECT", "Connect!");
			BufferedInputStream in=new BufferedInputStream(urlConnection.getInputStream());
			/*
			 * Get SD Card path
			 */
			RandomAccessFile fout=new RandomAccessFile(new File(Environment.getExternalStorageDirectory()+"/"+task.getPath()),"rw");
			/*
			 * Find the previous position if the task exist
			 */
			fout.seek(task.getCompleteSize());
			/*
			 * Set buffer size to 1024
			 * Each time add 1kb to the file
			 */
			byte[] buffer=new byte[1024];
			/*
			 * Continue download until thread interrupt or download finish 
			 * When user interrupt download set onDownload to false
			 */
			while((count=in.read(buffer))!=-1&&task.onDownload){
				if(latency){
					latency=false;
					task.latency=(int)(timer.elapsedTime());
				}
				fout.write(buffer, 0, count);
				task.setCompleteSize(task.getCompleteSize()+count);
			}
			fout.close();
			/*
			 * Download Complete
			 * Send 0 to UI thread
			 */
			if(task.onDownload){
				Log.v("DOWNLOAD_THREAD_COMPLETE",this.task.getUrl());
				Message msg=new Message();
				msg.what=0;
				msg.obj=task;
				mHandler.sendMessage(msg);
			}
		} catch (MalformedURLException e) {
			/*
			 * Invalid URL Exception
			 */
			mHandler.obtainMessage(1);
		} catch (IOException e) {
			/*
			 * IO Exception
			 * Send 2 to UI thread
			 */
			Log.v("DOWNLOAD_THREAD", "IO_EXCEPTION");
			Message msg=new Message();
			msg.what=2;
			mHandler.sendMessage(msg);
		}
	}
}
