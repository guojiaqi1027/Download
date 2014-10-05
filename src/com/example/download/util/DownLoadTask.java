package com.example.download.util;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
/**
 * Download task class
 * 
 * @author GawainGuo
 *
 */
public class DownLoadTask {
	private String path;
	private String url;
	private long fileSize;
	private long completeSize;
	public int latency;
	public boolean onDownload=true;
	/**
	 * Constructor of task
	 * @param filePath Path to store the 
	 * @param fileUrl Download URL
	 */
	public DownLoadTask(String filePath,String fileUrl){
		path=filePath;
		url=fileUrl;
		latency=0;
	}
	/**
	 * Set the size of file
	 * @param size Total size(byte) of download file
	 */
	public String getUrl(){
		return url;
	}
	public String getPath(){
		return path;
	}
	public long getFileSize(){
		return fileSize;
	}
	public long getCompleteSize(){
		return completeSize;
	}
	public void setFileSize(long size){
		fileSize=size;
	}
	/**
	 * Set the size of complete
	 * @param size Completed size 
	 */
	public void setCompleteSize(long size){
		completeSize=size;
	}
	
}
