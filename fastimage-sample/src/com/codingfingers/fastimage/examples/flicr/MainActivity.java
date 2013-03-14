package com.codingfingers.fastimage.examples.flicr;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;


import com.codingfingers.fastimage.examples.flicr.R;
import com.codingfingers.fastimage.examples.flicr.remote.EntryListReader;
import com.codingfingers.fastimage.examples.flicr.remote.EntryVO;
import com.codingfingers.fastimage.examples.flicr.utils.IOUtils;
import com.codingfingers.fastimagelist.cache.FastImageCache;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	public static final String TAG = "FastImageSample" + MainActivity.class;
	
	// widgets
	private GridView mGVGridView;
	private View mPBProgress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // you can find most of thumnails handling code in PhotosAdapter class
        
        // widgets
        mGVGridView = (GridView)findViewById(R.id.activity_main_grid_view);
        mPBProgress = (View)findViewById(R.id.activity_main_progress);

        // download feed list
        GetLatestListTask asyncTask = new GetLatestListTask();
        asyncTask.execute();
    }
    
    
    private class GetLatestListTask extends AsyncTask<Void, String, ArrayList<EntryVO>>
    {
		@Override
		protected void onPreExecute() {
			mPBProgress.setVisibility(View.VISIBLE);
			super.onPreExecute();
		}

		// in background
		@Override
		protected ArrayList<EntryVO> doInBackground(Void... params) {
			
			try {
				// create url
				URI targetUrl = new URI("http://api.flickr.com/services/feeds/photos_public.gne?format=rss2");
				
				// GET request
				HttpClient client = new DefaultHttpClient();
				HttpGet get = new HttpGet(targetUrl);

				// send request				
				HttpResponse response = client.execute(get);                     
			    HttpEntity entity = response.getEntity();
			    int statusCode = response.getStatusLine().getStatusCode();

			    if(entity != null&&(statusCode==201||statusCode==200))
			    {
			    	String xmlString = IOUtils.convertStreamToString(entity.getContent());
			    	
			    	Log.d("Server", "response: " + xmlString);
			    	
			        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			        factory.setNamespaceAware(false);
			        XmlPullParser xpp = factory.newPullParser();

			        xpp.setInput( new StringReader (xmlString));
			    	
			    	EntryListReader reader = new EntryListReader();
			    	ArrayList<EntryVO> entries =  reader.readContent(xpp);
			    	
			    	return entries;
			    }
			    else
			    {			        
					Log.e(TAG, "Server returned errorCode: " + statusCode);
					return new ArrayList<EntryVO>();
			    }
			}
			catch (IOException e) {
				e.printStackTrace();
				return new ArrayList<EntryVO>();
			}
			catch (Exception e) {
				e.printStackTrace();
				return new ArrayList<EntryVO>();
			}
		}

		@Override
		protected void onCancelled() {
			mPBProgress.setVisibility(View.INVISIBLE);
			super.onCancelled();
		}

		@Override
		protected void onPostExecute(ArrayList<EntryVO> result) {
			mPBProgress.setVisibility(View.INVISIBLE);
			
			// probably cancelled
			if(result == null) return;
			
			// success
			if(result.size() > 0)
			{
				
				PhotosAdapter photosAdapter = new PhotosAdapter(MainActivity.this, result);
				mGVGridView.setAdapter(photosAdapter);
			}
			
			// failure
			else
			{
				Toast.makeText(MainActivity.this, "Unable to load photos ", Toast.LENGTH_LONG).show();
			}
			
			super.onPostExecute(result);
		}
    }   
    
    
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
	    return true;
	}
	
	// menu item selected (logout or clear cache)
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.menu_clear_cache:
		        FastImageCache.getInstance(this).clear(true);
	            return true;
	    }
	    return false;
	}
	

}