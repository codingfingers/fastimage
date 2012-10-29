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

package com.codingfingers.fastimagelist.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.codingfingers.fastimagelist.bitmap.BitmapHolder;
import com.google.common.collect.MapMaker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * 
 * @author daniel
 */
public class FastImageCache extends AbstractCache<String, BitmapHolder> {

		private static final int INITIAL_IN_MEMORY_COUNT = 20;

		private static FastImageCache _instance;
		
		private static BitmapHolder lastHits[] = new BitmapHolder[INITIAL_IN_MEMORY_COUNT];
		private static int lastHitCounter = 0;
		private static int mInMemoryCount = INITIAL_IN_MEMORY_COUNT;
		
	    private FastImageCache() {
	        super("FastImageCache", 25, 60*24*160, 20);
	    }
	    
	    public static FastImageCache getInstance(Context ctx)
	    {
	    	if(_instance == null)
	    	{
	    		_instance = new FastImageCache();
	    		_instance.setDiskCacheEnabled("imagecache");
	    		_instance.enableDiskCache(ctx, AbstractCache.DISK_CACHE_SDCARD);
	    	}
	    	
	    	return _instance;
	    }

	    public synchronized void removeAllWithPrefix(String urlPrefix) {
	        CacheHelper.removeAllWithStringPrefix(this, urlPrefix);
	    }

	    @Override
	    public String getFileNameForKey(String imageUrl) {
	        return CacheHelper.getFileNameFromUrl(imageUrl);
	    }
	    
	    @Override
	    protected MapMaker createMapMaker(int initialCapacity, long expirationInMinutes,
	            int maxConcurrentThreads)
	    {
			MapMaker createMapMaker = super.createMapMaker(initialCapacity, expirationInMinutes, maxConcurrentThreads);
			createMapMaker.weakValues();
			return createMapMaker;
	    }
	    
	    @Override
	    protected BitmapHolder readValueFromDisk(File file) throws IOException {
	        BufferedInputStream istream = new BufferedInputStream(new FileInputStream(file));
	        long fileSize = file.length();
	        if (fileSize > Integer.MAX_VALUE) {
	            throw new IOException("Cannot read files larger than " + Integer.MAX_VALUE + " bytes");
	        }

	        int imageDataLength = (int) fileSize;

	        BitmapHolder holder = new BitmapHolder();
	        
	        byte[] imageData = new byte[imageDataLength];
	        istream.read(imageData, 0, imageDataLength);
	        istream.close();

	        holder.source = imageData;
	        holder.bitmap = createBitmap(imageData);			
			
	        return holder;
	    }
	    
	    protected Bitmap createBitmap(byte[] imageData)
	    {
	    	return BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
	    }

	    public synchronized Bitmap getBitmap(Object elementKey) {
	        BitmapHolder holder = super.get(elementKey);
	        
	        if (holder == null) {
	            return null;
	        }
	        
	        if(lastHits[lastHitCounter]!=holder)
	        {
		        lastHitCounter ++;
		        if(lastHitCounter>mInMemoryCount-1)
		        	lastHitCounter=0;
		        lastHits[lastHitCounter] = holder;
	        }
	        
	        holder.source = null;
	        return holder.bitmap;
	    }
	    
	    @Override
		public synchronized BitmapHolder put(String key, BitmapHolder value) {
			if(value.bitmap == null && value.source != null)
			{
				value.bitmap = BitmapFactory.decodeByteArray(value.source, 0, value.source.length);
			}
	    	return super.put(key, value);
		}

		public synchronized Bitmap putWithSource(String key, byte[] value) {
			BitmapHolder bh = new BitmapHolder();
			
			bh.source = value;
			bh.bitmap = createBitmap(value);
			
			this.put(key, bh);
			
	    	return bh.bitmap;
		}

		@Override
	    protected void writeValueToDisk(File file, BitmapHolder holder) throws IOException {
	        BufferedOutputStream ostream = new BufferedOutputStream(new FileOutputStream(file));

	        ostream.write(holder.source);

	        ostream.close();
	    }

		public static void setInMemoryCount(int inMemoryCount) {
			if(FastImageCache.mInMemoryCount != inMemoryCount)
			{
				FastImageCache.mInMemoryCount = inMemoryCount;
				lastHits = new BitmapHolder[mInMemoryCount];
				lastHitCounter = 0;
			}
		}
	}