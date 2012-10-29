/* Copyright (c) 2009 Matthias Kaeppler
 * Copyright (c) 2012 Coding Fingers S. C. Marcin �picki i Daniel Dudek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codingfingers.fastimagelist;


import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import com.codingfingers.fastimagelist.cache.FastImageCache;
import com.google.common.cache.Cache;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
/**
 * Support image download, and view updates.
 * @author Daniel Dudek (daniel@codingfingers.com)
 *
 * @param <V> view class that will be updated
 */
public class FastImageDownloader<V extends View> {

	/**
	 * just after 'download' call
	 */
	public static final int IMAGE_STATUS_NEW_IN_QUEUE = 0;
	
	/**
	 * when async task is launched to download image
	 */
	public static final int IMAGE_STATUS_DOWNLOADING = 1;
	
	/**
	 * after image was downloaded and set. fired after view update
	 */
	public static final int IMAGE_STATUS_DOWNLOADED = 2;
	
	/**
	 * there is no connection and image will be not downloaded
	 */
	public static final int IMAGE_STATUS_NO_CONNECTION = 3;
	
	/**
	 * some other error occured and image will be not downloaded
	 */
	public static final int IMAGE_STATUS_ERROR = 4;
	
	
	private int viewTag = -1;
	
	
	/**
	 * cache to handle saving to disk, and in-memory cache
	 */
	private FastImageCache imageCache;
	
	/**
	 * Affects loading image from disk cache. If true, the loading will be done in async task
	 * just like downloading from url. If false, the 'download' call will load image from disk
	 * in main thread.
	 */
	private boolean runLoadingImageFromDiskCacheAsync = false;
	
	
	public void setRunLoadingImageFromDiskCacheAsync(boolean loadingImageFromDiskCacheAsync) {
		this.runLoadingImageFromDiskCacheAsync = loadingImageFromDiskCacheAsync;
	}

	public interface IStatusUpdateListener<V>
	{
		void processViewOnStatusUpdate(V view, String url, int position, int status);
	}

	public interface IImageAvailableListener<V>
	{
		void processViewOnImageAvailable(Bitmap bmp, V view);
	}
	
	public interface IDownloadImageListener<V>
	{
		byte[] processBytesAfterDownload(byte[] source);

		void processViewOnProgress(V imageView, Integer bytesRead,
				Integer bytesTotalOrMinusOne);
	}
	
	/**
	 * Constructor
	 * @param ctx
	 * @param viewTag application specific tag id
	 */
	public FastImageDownloader(Context ctx, int viewTag)
	{
		this.imageCache = FastImageCache.getInstance(ctx);
		this.setViewTag(viewTag);
		FastImageCache.setInMemoryCount(90);
		
	    connectivityService = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);		
	}

	private boolean isOnline() {
		NetworkInfo netInfo = connectivityService.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
	        return true;
	    }
	    return false;
	}

	/**
	 * Constructor
	 * @param imageCache
	 */
//	public FastImageDownloader(FastImageCache imageCache)
//	{
//		this.imageCache = imageCache;
//	}
	
	private IImageAvailableListener<V> imageAvailableListener = null;
	private IStatusUpdateListener<V> statusUpdateListener = null;
	private IDownloadImageListener<V> downloadListener = null;

	private ConnectivityManager connectivityService;
	
	
	
	public void setImageAvailableListener(
			IImageAvailableListener<V> imageAvailableListener) {
		this.imageAvailableListener = imageAvailableListener;
	}

	public void setStatusUpdateListener(
			IStatusUpdateListener<V> statusUpdateListener) {
		this.statusUpdateListener = statusUpdateListener;
	}

	public void setDownloadListener(IDownloadImageListener<V> downloadListener) {
		this.downloadListener = downloadListener;
	}
	


	public void processViewOnImageAvailable(Bitmap bmp, V view)
	{
		if(imageAvailableListener!=null)
		{
			imageAvailableListener.processViewOnImageAvailable(bmp, view);
			return;
		}
		
		ImageView v = (ImageView) view;
		
		if(v!=null)
			v.setImageBitmap(bmp);
	}
	

	public void processViewOnProgress(V imageView, Integer bytesRead,
			Integer bytesTotalOrMinusOne) {
		if(downloadListener!=null)
		{
			downloadListener.processViewOnProgress(imageView, bytesRead,
					bytesTotalOrMinusOne);
			return;
		}
		return;
	}
	
	protected byte[] processBytesAfterDownload(byte[] source)
	{
		if(downloadListener!=null)
		{
			return downloadListener.processBytesAfterDownload(source);
		}
		
		return source;
	}
	
	protected void processViewOnStatusUpdate(V view, String url, int position, int status)
	{
		if(statusUpdateListener!=null)
		{
			statusUpdateListener.processViewOnStatusUpdate(view, url, position, status);
			return;
		}
		
		ImageView v = (ImageView) view;

		if(v==null) return;
			
		switch(status)
		{
			case IMAGE_STATUS_NEW_IN_QUEUE:
				v.setImageBitmap(null);
			break;
			case IMAGE_STATUS_ERROR:
				v.setImageBitmap(null);
			break;
		}
	}
	
	
	@SuppressLint("NewApi")
	public boolean downloadImage(V view, String url, int position)
	{
		
		if(url == null || url.length() == 0)
		{
			// set error status
			processViewOnStatusUpdate(view, url, position, IMAGE_STATUS_ERROR);
			return false;
		}
		
		ViewHolder<V> vh = (ViewHolder<V>)view.getTag(getViewTag());
		
		
		if(vh!=null)
		{
			boolean urlEquals = vh.url.equals(url);
			if(!urlEquals && vh.status == IMAGE_STATUS_DOWNLOADING)
			{
				this.cancelDownload(vh);
			}
			else
			if(!urlEquals && vh.status == IMAGE_STATUS_NEW_IN_QUEUE)
			{
				this.cancelDownload(vh);
			}
			else
			if(urlEquals && vh.status == IMAGE_STATUS_DOWNLOADING || vh.status == IMAGE_STATUS_NEW_IN_QUEUE)
			{
				//do nothing
				return false;
			}
		}
		else
		{
			vh = new ViewHolder<V>();	
			
			view.setTag(getViewTag(), vh);
		}

		vh.position = position;
		vh.imageView = view;
		vh.url = url;
		vh.downloadTask = null;
		
		if(runLoadingImageFromDiskCacheAsync == false)
		{
			Bitmap cachedImage =  imageCache.getBitmap(url);

			if(cachedImage!=null) 
			{
				processViewOnImageAvailable(cachedImage, view);
				vh.status = IMAGE_STATUS_DOWNLOADED;
				processViewOnStatusUpdate(view, url, position, IMAGE_STATUS_DOWNLOADED);
				return true;
			}
		}
	
		try
		{
			CachedDownloadImageTask task = new CachedDownloadImageTask(vh, imageCache);	
			vh.downloadTask = task;
			vh.status = IMAGE_STATUS_NEW_IN_QUEUE;
			processViewOnStatusUpdate(view, url, position, IMAGE_STATUS_NEW_IN_QUEUE);
			
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
			} else {
				task.execute(url);
			}

		}
		catch (RejectedExecutionException e) {
			
		}
		
		return false;
	}
	
	private void cancelDownload(ViewHolder<V> vh) {
		
		if(vh.downloadTask!=null)
			vh.downloadTask.cancel(true);
	}
	
	public String getCacheFilePath(String imageUrl)
	{
		return imageCache.getDiskCacheDirectory() + "/" + imageCache.getFileNameForKey(imageUrl);		
	}	

	public int getViewTag() {
		return viewTag;
	}

	public void setViewTag(int viewTag) {
		this.viewTag = viewTag;
	}

	/**
	 * *****************************************************************************
	 * Class responsible for downloading and caching images
	 * @author Daniel Dudek (kontakt@ddudek.pl)
	 * 
	 * *****************************************************************************
	 */
	private class CachedDownloadImageTask extends AsyncTask<Object, Integer, Bitmap>{

		private final WeakReference<ViewHolder<V>> viewHolderReference;
		int originalPosition;
		String originalURL;
		private final int defaultBufferSize = 10 * 1024;

		public CachedDownloadImageTask(ViewHolder<V> viewHolder, FastImageCache imageCacheInstance) {
			originalPosition = viewHolder.position;
			originalURL = viewHolder.url;
			viewHolderReference = new WeakReference<ViewHolder<V>>(viewHolder);
		}
		
		@Override
		protected void onCancelled() {
			cleanup();
			imageCache.removeKey(originalURL);
			super.onCancelled();
		}
		
		protected void cleanup()
		{
			this.viewHolderReference.clear();
		}

		@Override
		protected void onPreExecute() {
			if(!isOnline())
			{
				this.cancel(false);
				safeUpdateStatus(IMAGE_STATUS_NO_CONNECTION);
			}
		}

		@Override
		protected Bitmap doInBackground(Object... params) {
			
			String url = (String) params[0];
			
			safeUpdateStatus(IMAGE_STATUS_DOWNLOADING);

			Bitmap resultImage;
			Bitmap cachedImage =  imageCache.getBitmap(url);
			if(cachedImage == null)
			{
				try{
					url = (String) params[0];
					
					byte [] source = loadImageData(url);
					if(source!=null && source.length != 0)
					{
						byte[] processed = FastImageDownloader.this.processBytesAfterDownload(source);

						if(processed == null || processed.length == 0)
							throw new IOException("After download returned no bytes, url: " + url);
							
						resultImage = imageCache.putWithSource(url, processed);
					}
					else return null;
					
				}
				catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
			else
			{
				resultImage = cachedImage;
			}
			
			return resultImage;
		}
		
		protected void safeUpdateStatus(int status)
		{
			ViewHolder<V> originalViewHolder = viewHolderReference.get();
			
			// if the original view holeder was not gc'ed
			if(originalViewHolder!=null) {
				// and if this view is still showing this item
				if(originalViewHolder.position == originalPosition)
				{
					originalViewHolder.status = status;
					FastImageDownloader.this.processViewOnStatusUpdate(originalViewHolder.imageView, originalURL, originalPosition, status);
				}
			}
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			
			ViewHolder<V> originalViewHolder = viewHolderReference.get();
			
			// if the original view holeder was not gc'ed
			if(originalViewHolder!=null) {
				// and if this view is still showing this item
				if(originalViewHolder.position == originalPosition)
				{
					if(result == null)
					{
						originalViewHolder.status = IMAGE_STATUS_ERROR;
						FastImageDownloader.this.processViewOnStatusUpdate(originalViewHolder.imageView, originalURL, originalPosition, IMAGE_STATUS_ERROR);
					}
					else
					{
						FastImageDownloader.this.processViewOnImageAvailable(result, originalViewHolder.imageView);
						originalViewHolder.status = IMAGE_STATUS_DOWNLOADED;
						FastImageDownloader.this.processViewOnStatusUpdate(originalViewHolder.imageView, originalURL, originalPosition, IMAGE_STATUS_DOWNLOADED);
					}
				}
				else
				{
					// view was inflated
				}
			}
		}
		
		protected void onProgressUpdate(Integer... progress) {
			ViewHolder<V> originalViewHolder = viewHolderReference.get();
			
			// if the original view holeder was not gc'ed
			if(originalViewHolder!=null) {
				// and if this view is still showing this item
				if(originalViewHolder.position == originalPosition)
				{
					FastImageDownloader.this.processViewOnProgress(originalViewHolder.imageView, progress[0], progress[1]);
				}
			}
	    }
		
		public byte[] loadImageData(String imageUrl) throws IOException {
	        URL url = new URL(imageUrl);

	        if(isCancelled()) return null;
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

	        if(isCancelled()) return null;
	        // determine the image size and allocate a buffer
	        int fileSize = connection.getContentLength();
	        Log.d("DownloadImageTask", "fetching image " + imageUrl + " (" + (fileSize <= 0 ? "size unknown" : Integer.toString(fileSize)) + ")");
	        
	        BufferedInputStream istream = new BufferedInputStream(connection.getInputStream(), fileSize<=0?defaultBufferSize:fileSize);

	        try {   
	            if (fileSize <= 0) {
	                Log.w(imageUrl,
	                        "Server did not set a Content-Length header, will default to buffer size of "
	                                + defaultBufferSize + " bytes");
	                ByteArrayOutputStream buf = new ByteArrayOutputStream(defaultBufferSize);
	                
	                byte[] buffer = new byte[defaultBufferSize];
	                int bytesRead = 0;
	                int bytesReadTotal = 0;
	                while (bytesRead != -1) {
	                    bytesRead = istream.read(buffer, 0, defaultBufferSize);
	                    if (bytesRead > 0)
	                        buf.write(buffer, 0, bytesRead);
	                    
	                    bytesReadTotal += bytesRead;
	                    
	                    publishProgress(bytesReadTotal, -1);
//	                    if(isCancelled()) return null;
	                }
	                return buf.toByteArray();
	            } else {
	                byte[] imageData = new byte[fileSize];
	        
	                int bytesRead = 0;
	                int offset = 0;
	                while (bytesRead != -1 && offset < fileSize) {
	                    bytesRead = istream.read(imageData, offset, fileSize - offset);
	                    offset += bytesRead;
//	                    if(isCancelled()) return null;
	                    
	                    publishProgress(offset, fileSize);
	                }
	                return imageData;
	            }
	        } catch (Exception e) {
				e.printStackTrace();
				imageCache.remove(originalURL);
				return null;
			} finally {
	            // clean up
	            try {
	                istream.close();
	            } catch (Exception ignore) { ignore.printStackTrace(); }
	            try {
	                connection.disconnect();
	            } catch (Exception ignore) { ignore.printStackTrace(); }
	        }
	    }
	}
	
	private class ViewHolder<V extends View>
	{
		int position;
		String url;
		int status;
		V imageView;
		
		CachedDownloadImageTask downloadTask;		
	}

	
}
