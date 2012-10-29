package com.codingfingers.fastimage.examples.flicr.utils;

import java.io.ByteArrayOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;


public class BitmapUtils {
	
	public static Bitmap getBitmapScaled(Bitmap bitmap, int maxSize) {
		Bitmap output = null;
		
		int bmpWidth = maxSize;
		int bmpHeight= maxSize;
		
		if(bitmap!= null)
		{
			bmpWidth = bitmap.getWidth();
			bmpHeight = bitmap.getHeight();
		}
		
		float resizeFactor = (float)maxSize / (float)Math.max(bmpWidth, bmpHeight); 

		int dstWidth = (int)(bmpWidth * resizeFactor);
		int dstHeight= (int)(bmpHeight * resizeFactor);


	    output = Bitmap.createBitmap( dstWidth, dstHeight, Config.ARGB_8888);
			
	    Canvas canvas = new Canvas(output);

	    final int color = 0xffcccccc;
	    final Paint paint = new Paint();
	    final Rect rect = new Rect(0, 0, dstWidth, dstHeight);
	    final Rect rectSrc = new Rect(0, 0, bmpWidth, bmpHeight);
	    //final RectF rectF = new RectF(rect);
	    
	    paint.setAntiAlias(true);
	    paint.setColor(color);
	    
	    canvas.drawARGB(0, 0, 0, 0);
	    
	    if(bitmap != null)
	    {
	    	paint.setFilterBitmap(true);
		    canvas.drawBitmap(bitmap, rectSrc, rect, paint);
	    }

	    return output;
	  }
	
	public static byte[] resizeImageAfterDownload(byte[] source, float thumbnailWidthInPx)
	{
		if(source == null || source.length == 0)
		{
			return null;
		}
		
		Bitmap bitmap = BitmapFactory.decodeByteArray(source, 0, source.length);
		
		if(bitmap == null)
		{
			return null;
		}
		
		Bitmap roundedBitmap = BitmapUtils.getBitmapScaled(bitmap, Math.round(thumbnailWidthInPx));
		roundedBitmap.setDensity(Bitmap.DENSITY_NONE);
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream(source.length);
		roundedBitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
		byte[] byteArray = stream.toByteArray();
		
		return byteArray;
	}
	

}
