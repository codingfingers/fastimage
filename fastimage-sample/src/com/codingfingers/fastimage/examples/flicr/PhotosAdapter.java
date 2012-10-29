package com.codingfingers.fastimage.examples.flicr;

import java.util.List;

import com.codingfingers.fastimage.examples.flicr.R;
import com.codingfingers.fastimage.examples.flicr.remote.EntryVO;
import com.codingfingers.fastimage.examples.flicr.utils.BitmapUtils;
import com.codingfingers.fastimagelist.FastImageDownloader;
import com.codingfingers.fastimagelist.FastImageDownloader.IDownloadImageListener;
import com.codingfingers.fastimagelist.FastImageDownloader.IImageAvailableListener;
import com.codingfingers.fastimagelist.FastImageDownloader.IStatusUpdateListener;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
public class PhotosAdapter extends ArrayAdapter<EntryVO> {

	
	// feed with thumbnails from flicr
	private List<EntryVO> mPhotos;
	
	// our image downloader instance, to be used with ImageView
	private FastImageDownloader<ImageView> mImageDownloader;
	
	// we resize thumbnails to this size, to get best performance
	private float mThumbnailWidthInPx;
	
	
	public PhotosAdapter(Context context, List<EntryVO> photos ) {
		super(context, R.layout.photo_item);
		
		// feed list
		this.mPhotos = photos;
		
		// size of thumbnails
		Resources r = this.getContext().getResources();
		mThumbnailWidthInPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 99, r.getDisplayMetrics());
		
		// downloader instance
		mImageDownloader = new FastImageDownloader<ImageView>(this.getContext(), R.id.view_tag_id);
		// if image is available in disk cache, try to load it immediately, instead of running new thread
		mImageDownloader.setRunLoadingImageFromDiskCacheAsync(false);
		
		// image available handler, here you should apply your image to the target. 
		mImageDownloader.setImageAvailableListener(new IImageAvailableListener<ImageView>() {

			@Override
			public void processViewOnImageAvailable(Bitmap bmp, ImageView view) {
				TextView text = (TextView) view.getTag();

				view.setImageBitmap(bmp);
				view.setVisibility(View.VISIBLE);
				text.setVisibility(View.VISIBLE);
				
				Animation fadeIn = (Animation)view.getTag(R.id.animation_tag_id);
				if(fadeIn != null)
				{
					view.startAnimation(fadeIn);
					view.setTag(R.id.animation_tag_id, null);
				}
			}
		});
		
		
		// to handle the download process
		mImageDownloader.setDownloadListener(new IDownloadImageListener<ImageView>() {
			
			// after download complete and just before the image is stored in disk cache
			// you can process bytes, resize for example. This is done in async thread
			@Override
			public byte[] processBytesAfterDownload(byte[] source) {
				return BitmapUtils.resizeImageAfterDownload(source, mThumbnailWidthInPx);
			}

			// update progress information
			@Override
			public void processViewOnProgress(ImageView view,
					Integer bytesRead, Integer bytesTotal) {

				float percent = 0.0f;
				
				if(bytesTotal>0)
				{
					percent = (bytesRead * 100.0f) / (bytesTotal + 100) ;
				}
				else
				{
					percent = (bytesRead/(20.0f * 1024.0f)) * 80.0f;
					
					if(percent > 80.0f)
					{
						percent = 80.0f + ((bytesRead - (20.0f * 1024.0f)) / (280.0f * 1024.0f) ) * 20.0f;
					}
					
					if(percent > 99.0f) percent = 100.0f;
				}
				
				Object o = view.getTag();
				TextView text = (TextView)o;
				if(text!=null)
					text.setText(String.format("%0$.0f", percent) + "%");
			}
		});

		// handle changes of download status
		mImageDownloader.setStatusUpdateListener(new IStatusUpdateListener<ImageView>() {
			
			@Override
			public void processViewOnStatusUpdate(ImageView view, String url,
					int position, int status) {
				
				TextView progressText = (TextView)view.getTag();
				
				switch(status)
				{
				// fired when download thread is started
				case FastImageDownloader.IMAGE_STATUS_NEW_IN_QUEUE:
					progressText.setVisibility(View.VISIBLE);
					break;

				// fired when something goes wrong
				case FastImageDownloader.IMAGE_STATUS_ERROR:
					progressText.setVisibility(View.GONE);
					break;

				// fired when there is no connection available
				case FastImageDownloader.IMAGE_STATUS_NO_CONNECTION:
					progressText.setVisibility(View.GONE);
					break;

				// fired when loading just finished, just before processViewOnImageAvailable.
				case FastImageDownloader.IMAGE_STATUS_DOWNLOADED:
//					progressText.setVisibility(View.GONE);
					break;
				}
			}
		});
	}
	
	@Override
	public int getCount() {
		return mPhotos.size();
	}
	@Override
	public int getPosition(EntryVO item) {
		return mPhotos.indexOf(item);
	}
	@Override
	public EntryVO getItem(int position) {
		return mPhotos.get(position);
	}
	@Override
	public void clear() {
		mPhotos.clear();
	}
	
	
	@Override
	public View getView(int position, View row, ViewGroup parent) {		
		ViewHolder holder = null;
		
		final EntryVO photo = mPhotos.get(position);
		
		// get view widgets and store them in view holder for better performance
		if (row == null) {
			LayoutInflater inflater = (LayoutInflater) this.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			row = inflater.inflate(R.layout.photo_item, parent, false);
			
			holder = new ViewHolder();
			
			ImageView thumbnail1 = (ImageView) row.findViewById(R.id.list_photos_thumbnail1);
			TextView textInfo1 = (TextView) row.findViewById(R.id.list_photos_text_info);

			holder.thumbnail1 = thumbnail1;
			holder.textInfo1 = textInfo1;
			row.setTag(holder);
			
			holder.thumbnail1.setTag(textInfo1);
		}
		else
		{
			holder = (ViewHolder) row.getTag();
		}

		holder.thumbnail1.setVisibility(View.GONE);
		holder.thumbnail1.setImageBitmap(null);
		holder.textInfo1.setText("");
		
		boolean wasInCache = mImageDownloader.downloadImage(holder.thumbnail1, photo.getThumb(), position);
		
		// fade in support, you can omit this
		Animation fadeIn = (Animation)holder.thumbnail1.getTag(R.id.animation_tag_id);
		if(fadeIn != null) { 
			fadeIn.reset();
		}
		
		if(!wasInCache) {
			Animation fadeInAnimation = AnimationUtils.loadAnimation(PhotosAdapter.this.getContext(), R.anim.animation_fade_in);
			holder.thumbnail1.setTag(R.id.animation_tag_id, fadeInAnimation);
		}
		else {
			holder.thumbnail1.setTag(R.id.animation_tag_id, null);
			holder.thumbnail1.setAlpha(255);
		}

		return row;
	}
	
	private static final class ViewHolder {
		ImageView thumbnail1;
		TextView textInfo1;
	}	
}
