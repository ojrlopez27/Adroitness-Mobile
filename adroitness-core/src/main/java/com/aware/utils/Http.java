
package com.aware.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.aware.Aware;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.GZIPInputStream;

/**
 * HTML POST/GET client wrapper
 * @author denzil
 */
public class Http {
	
	/**
	 * Logging tag (default = "AWARE")
	 */
	private static String TAG = "AWARE::HTML";

	private static Context sContext;

	public Http(Context c) {
		sContext = c;
        if( c.getPackageName().equalsIgnoreCase("com.aware") ) {
            //ojrlopez
//            Intent wearClient = new Intent(sContext, WearClient.class);
//            sContext.startService(wearClient);
        }
	}

    /**
     * Request a GET from an URL.
     * @param url
     * @return String with the content of the reply
     */
    public synchronized String dataGET(String url, boolean is_gzipped) {
        if( url.length() == 0 ) return null;

        if( Aware.is_watch(sContext) ) {

            if( Aware.DEBUG ) Log.d(TAG, "Waiting for phone's HTTP GET request...\n" + "URL:" + url );

            //ojrlopez
//            Intent phoneRequest = new Intent(WearClient.ACTION_AWARE_ANDROID_WEAR_HTTP_GET);
//            phoneRequest.putExtra(WearClient.EXTRA_URL, url);
//            phoneRequest.putExtra(WearClient.EXTRA_GZIP, is_gzipped);
//            sContext.sendBroadcast(phoneRequest);
//
//            long time = System.currentTimeMillis();
//            while( WearClient.wearResponse == null ){
//                if( WearClient.wearResponse != null || (System.currentTimeMillis()-time) > 60000 ) {
//                    if( System.currentTimeMillis() - time > 60000 ) Log.w(TAG,"HTTP request timeout...");
//                    break;
//                }
//            }
//
//            if( Aware.DEBUG ) {
//                Log.d(TAG, "AndroidWear GET benchmark: " + (System.currentTimeMillis() - time)/1000 + " seconds");
//            }
//
//            String response = WearClient.wearResponse;
//            WearClient.wearResponse = null;
//
//            return response;
        }
        try {

            URL path = new URL(url);
            HttpURLConnection path_connection = (HttpURLConnection) path.openConnection();
            path_connection.setReadTimeout(10000);
            path_connection.setConnectTimeout(10000);
            path_connection.setRequestMethod("GET");
            if( is_gzipped ) path_connection.setRequestProperty("accept-encoding","gzip");

            path_connection.connect();

            if( path_connection.getResponseCode() != HttpURLConnection.HTTP_OK ) {
                if (Aware.DEBUG) {
                    Log.d(TAG,"Request: GET, URL: " + url);
                    Log.d(TAG, "Status: " + path_connection.getResponseCode() );
                    Log.e(TAG, path_connection.getResponseMessage() );
                }
                return null;
            }

            InputStream stream = path_connection.getInputStream();
            if("gzip".equals(path_connection.getContentEncoding())) {
                stream = new GZIPInputStream(stream);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(stream));

            String page_content = "";
            String line;
            while( (line = br.readLine()) != null ) {
                page_content+=line;
            }

            if (Aware.DEBUG) {
                Log.d(TAG,"Request: GET, URL: " + url);
                Log.d(TAG,"Answer:" + page_content );
            }

            return page_content;

        } catch (IOException e) {
            if(Aware.DEBUG) Log.e(TAG,e.getMessage());
            return null;
        }
    }

	/**
	 * Make a POST to the URL, with the Hashtable<String, String> data, using gzip compression
	 * @param url
	 * @param data
     * @param is_gzipped
	 * @return String with server response. If GZipped, use Http.undoGZIP to recover data
	 */
	public synchronized String dataPOST(String url, Hashtable<String, String> data, boolean is_gzipped) {
        if( url.length() == 0 ) return null;

        if( Aware.is_watch(sContext) ) {
			JSONObject data_json = new JSONObject();
            Enumeration e = data.keys();

            while(e.hasMoreElements()) {
                String key = (String) e.nextElement();
                try {
                    data_json.put(key, data.get(key));
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
            }

			if( Aware.DEBUG ) Log.d(TAG, "Waiting for phone's HTTP POST request...\n" + "URL:" + url + "\nData:" + data_json.toString());

            //ojrlopez
//            Intent phoneRequest = new Intent(WearClient.ACTION_AWARE_ANDROID_WEAR_HTTP_POST);
//            phoneRequest.putExtra(WearClient.EXTRA_URL, url);
//            phoneRequest.putExtra(WearClient.EXTRA_DATA, data_json.toString());
//            phoneRequest.putExtra(WearClient.EXTRA_GZIP, is_gzipped);
//            sContext.sendBroadcast(phoneRequest);
//
//            long time = System.currentTimeMillis();
//            while( WearClient.wearResponse == null ){
//                if( WearClient.wearResponse != null || (System.currentTimeMillis()-time) > 60000 ) {
//                    if( System.currentTimeMillis() - time > 60000 ) Log.w(TAG,"HTTP request timeout...");
//                    break;
//                }
//            }
//
//            if( Aware.DEBUG ) {
//                Log.d(TAG, "AndroidWear POST benchmark: " + (System.currentTimeMillis() - time)/1000 + " seconds");
//            }
//
//            String response = WearClient.wearResponse;
//            WearClient.wearResponse = null;
//
//            return response;
		}

		try{

            URL path = new URL(url);
            HttpURLConnection path_connection = (HttpURLConnection) path.openConnection();
            path_connection.setReadTimeout(10000);
            path_connection.setConnectTimeout(10000);
            path_connection.setRequestMethod("POST");
            if( is_gzipped ) path_connection.setRequestProperty("accept-encoding","gzip");

            Uri.Builder builder = new Uri.Builder();
            Enumeration e = data.keys();
            while(e.hasMoreElements()) {
                String key = (String) e.nextElement();
                builder.appendQueryParameter(key, data.get(key));
            }

            OutputStream os = path_connection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(builder.build().getEncodedQuery());
            writer.flush();
            writer.close();
            os.close();

            path_connection.connect();

            if( path_connection.getResponseCode() != HttpURLConnection.HTTP_OK ) {
                if (Aware.DEBUG) {
                    Log.d(TAG,"Request: POST, URL: " + url + "\nData:" + builder.build().getEncodedQuery());
                    Log.d(TAG, "Status: " + path_connection.getResponseCode() );
                    Log.e(TAG, path_connection.getResponseMessage() );
                }
                return null;
            }

            InputStream stream = path_connection.getInputStream();
            if("gzip".equals(path_connection.getContentEncoding())) {
                stream = new GZIPInputStream(stream);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(stream));

            String page_content = "";
            String line;
            while( (line = br.readLine()) != null ) {
                page_content+=line;
            }

            if (Aware.DEBUG) {
                Log.d(TAG, "Request: POST, URL: " + url + "\nData:" + builder.build().getEncodedQuery());
                Log.d(TAG,"Answer:" + page_content );
            }

            return page_content;
		}catch (UnsupportedEncodingException e) {
			Log.e(TAG,e.getMessage());
			return null;
		} catch (IOException e) {
			Log.e(TAG,e.getMessage());
			return null;
		} catch (IllegalStateException e ) {
			Log.e(TAG,e.getMessage());
			return null;
		}
	}
}
