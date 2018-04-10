
package com.aware;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.aware.providers.ESM_Provider;
import com.aware.providers.ESM_Provider.ESM_Data;
import com.aware.ui.ESM_Queue;
import com.aware.utils.Aware_Sensor;
import edu.cmu.adroitness.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * AWARE ESM module
 * Allows a researcher to do ESM's on their studies
 * Listens to: 
 * - ACTION_AWARE_QUEUE_ESM
 * @author df
 *
 */
public class ESM extends Aware_Sensor {
    
    /**
     * Logging tag (default = "AWARE::ESM")
     */
    private static String TAG = "AWARE::ESM";
    
    /**
     * Received event: queue the specified ESM
     * Extras: (String) esm
     */
    public static final String ACTION_AWARE_QUEUE_ESM = "ACTION_AWARE_QUEUE_ESM";
    
    /**
     * Broadcasted event: the user has answered one answer from ESM queue
     */
    public static final String ACTION_AWARE_ESM_ANSWERED = "ACTION_AWARE_ESM_ANSWERED";
    
    /**
     * Broadcasted event: the user has dismissed one answer from ESM queue
     */
    public static final String ACTION_AWARE_ESM_DISMISSED = "ACTION_AWARE_ESM_DISMISSED";
    
    /**
     * Broadcasted event: the user did not answer the ESM on time from ESM queue
     */
    public static final String ACTION_AWARE_ESM_EXPIRED = "ACTION_AWARE_ESM_EXPIRED";
    
    /**
     * Broadcasted event: the user has finished answering the ESM queue
     */
    public static final String ACTION_AWARE_ESM_QUEUE_COMPLETE = "ACTION_AWARE_ESM_QUEUE_COMPLETE";
    
    /**
     * Broadcasted event: the user has started answering the ESM queue
     */
    public static final String ACTION_AWARE_ESM_QUEUE_STARTED = "ACTION_AWARE_ESM_QUEUE_STARTED";
    
    /**
     * ESM status: new on the queue, but not displayed yet
     */
    public static final int STATUS_NEW = 0;
    
    /**
     * ESM status: esm dismissed by the user, either by pressed back or home button
     */
    public static final int STATUS_DISMISSED = 1;
    
    /**
     * ESM status: esm answered by the user by pressing submit button
     */
    public static final int STATUS_ANSWERED = 2;
    
    /**
     * ESM status: esm not answered in time by the user
     */
    public static final int STATUS_EXPIRED = 3;
    
    /**
     * ESM status: esm is visible to the user
     */
    public static final int STATUS_VISIBLE = 4;
    
    /**
     * ESM Dialog with free text 
     * Example: [{'esm':{'esm_type':1,'esm_title':'ESM Freetext','esm_instructions':'The user can answer an open ended question.','esm_submit':'Next','esm_expiration_threshold':20,'esm_trigger':'esm trigger example'}}]
     */
    public static final int TYPE_ESM_TEXT = 1;
    
    /**
     * ESM Dialog with radio buttons 
     * Note: 'Other' will allow free text input from the user
     * Example: [{'esm':{'esm_type':2,'esm_title':'ESM Radio','esm_instructions':'The user can only choose one option','esm_radios':['Option one','Option two','Other'],'esm_submit':'Next','esm_expiration_threshold':30,'esm_trigger':'esm trigger example'}}]
     */
    public static final int TYPE_ESM_RADIO = 2;
    
    /**
     * ESM Dialog with checkboxes 
     * Note: 'Other' will allow free text input from the user
     * Example: [{'esm':{'esm_type':3,'esm_title':'ESM Checkbox','esm_instructions':'The user can choose multiple options','esm_checkboxes':['One','Two','Other'],'esm_submit':'Next','esm_expiration_threshold':40,'esm_trigger':'esm trigger example'}}]
     */
    public static final int TYPE_ESM_CHECKBOX = 3;
    
    /**
     * ESM Dialog with likert scale
     * Example: [{'esm':{'esm_type':4,'esm_title':'ESM Likert','esm_instructions':'User rating 1 to 5 or 7 at 1 step increments','esm_likert_max':5,'esm_likert_max_label':'Great','esm_likert_min_label':'Bad','esm_likert_step':1,'esm_submit':'OK','esm_expiration_threshold':50,'esm_trigger':'esm trigger example'}}]
     */
    public static final int TYPE_ESM_LIKERT = 4;
    
    /**
     * ESM Dialog with quick answers
     * Example: [{'esm':{'esm_type':5,'esm_title':'ESM Quick Answer','esm_instructions':'One touch answer','esm_quick_answers':['Yes','No'],'esm_expiration_threashold':60,'esm_trigger':'esm trigger example'}}]
     */
    public static final int TYPE_ESM_QUICK_ANSWERS = 5;

    /**
     * ESM Dialog with a discrete likert scale
     * Example: [{'esm':{'esm_type':6,'esm_title':'ESM Scale','esm_instructions':'User scaled value between minimum and maximum at X increments','esm_scale_min':0,'esm_scale_max':5,'esm_scale_start':3,'esm_scale_max_label':'5','esm_scale_min_label':'0','esm_scale_step':1,'esm_submit':'OK','esm_expiration_threshold':50,'esm_trigger':'esm trigger example'}}]
     */
    public static final int TYPE_ESM_SCALE = 6;
    
    /**
     * Required String extra for displaying an ESM. It should contain the JSON string that defines the ESM dialog.
     * Examples:<p>
     * Free text: [{'esm':{'esm_type':1,'esm_title':'ESM Freetext','esm_instructions':'The user can answer an open ended question.','esm_submit':'Next','esm_expiration_threshold':20,'esm_trigger':'esm trigger example'}}]
     * Radio: [{'esm':{'esm_type':2,'esm_title':'ESM Radio','esm_instructions':'The user can only choose one option','esm_radios':['Option one','Option two','Other'],'esm_submit':'Next','esm_expiration_threshold':30,'esm_trigger':'esm trigger example'}}]
     * Checkbox: [{'esm':{'esm_type':3,'esm_title':'ESM Checkbox','esm_instructions':'The user can choose multiple options','esm_checkboxes':['One','Two','Other'],'esm_submit':'Next','esm_expiration_threshold':40,'esm_trigger':'esm trigger example'}}]
     * Likert: [{'esm':{'esm_type':4,'esm_title':'ESM Likert','esm_instructions':'User rating 1 to 5 or 7 at 1 step increments','esm_likert_max':5,'esm_likert_max_label':'Great','esm_likert_min_label':'Bad','esm_likert_step':1,'esm_submit':'OK','esm_expiration_threshold':50,'esm_trigger':'esm trigger example'}}]
     * Quick answer: [{'esm':{'esm_type':5,'esm_title':'ESM Quick Answer','esm_instructions':'One touch answer','esm_quick_answers':['Yes','No'],'esm_expiration_threshold':60,'esm_trigger':'esm trigger example'}}]
     * Scale: [{'esm':{'esm_type':6,'esm_title':'ESM Scale','esm_instructions':'User scaled value between minimum and maximum at X increments','esm_scale_min':0,'esm_scale_max':5,'esm_scale_start':3,'esm_scale_max_label':'5','esm_scale_min_label':'0','esm_scale_step':1,'esm_submit':'OK','esm_expiration_threshold':50,'esm_trigger':'esm trigger example'}}]
     * </p>
     * Furthermore, you can chain several mixed ESM together as a JSON array: [{esm:{}},{esm:{}},...] 
     */
    public static final String EXTRA_ESM = "esm";
    
    public static final int ESM_NOTIFICATION_ID = 777;

    @Override
    public void onCreate() {
        super.onCreate();
        
        TAG = Aware.getSetting(getApplicationContext(),Aware_Preferences.DEBUG_TAG).length()>0?Aware.getSetting(getApplicationContext(),Aware_Preferences.DEBUG_TAG):TAG;
        
        DATABASE_TABLES = ESM_Provider.DATABASE_TABLES;
    	TABLES_FIELDS = ESM_Provider.TABLES_FIELDS;
    	CONTEXT_URIS = new Uri[]{ ESM_Data.CONTENT_URI };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(ESM.ACTION_AWARE_QUEUE_ESM);
        filter.addAction(ESM.ACTION_AWARE_ESM_ANSWERED);
        filter.addAction(ESM.ACTION_AWARE_ESM_DISMISSED);
        filter.addAction(ESM.ACTION_AWARE_ESM_EXPIRED);
        registerReceiver(esmMonitor, filter);

        if(Aware.DEBUG) Log.d(TAG,"ESM service created!");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        unregisterReceiver(esmMonitor);
        
        if(Aware.DEBUG) Log.d(TAG,"ESM service terminated...");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        
        TAG = Aware.getSetting(getApplicationContext(),Aware_Preferences.DEBUG_TAG).length()>0?Aware.getSetting(getApplicationContext(),Aware_Preferences.DEBUG_TAG):TAG;
        if(Aware.DEBUG) Log.d(TAG,"ESM service active... Queue = " + ESM_Queue.getQueueSize(getApplicationContext()));

        if( Aware.getSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM).equals("true") ) {
            if( isESMWaiting(getApplicationContext()) ) {
                notifyESM(getApplicationContext());
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Check if we have NEW ESMs that can be answered at any time
     * @param c
     * @return
     */
    public static boolean isESMWaiting( Context c ) {
        boolean is_waiting = false;
        Cursor esms_waiting = c.getContentResolver().query(ESM_Data.CONTENT_URI, null, ESM_Data.STATUS + " = " + ESM.STATUS_NEW + " AND " + ESM_Data.EXPIRATION_THRESHOLD + " = 0", null, ESM_Data.TIMESTAMP + " ASC LIMIT 1");
        if( esms_waiting != null && esms_waiting.moveToFirst() ) {
            is_waiting = (esms_waiting.getCount() > 0);
        }
        if( esms_waiting != null && ! esms_waiting.isClosed() ) esms_waiting.close();
        return is_waiting;
    }

    /**
     * Show notification with ESM waiting
     * @param c
     */
    private static void notifyESM(Context c) {
        NotificationManager mNotificationManager = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);

        int esm_count = 0;
        Cursor esm_waiting = c.getContentResolver().query(ESM_Data.CONTENT_URI, null, ESM_Data.STATUS + "=" + ESM.STATUS_NEW, null, null);
        if( esm_waiting != null && esm_waiting.moveToFirst() ) {
            esm_count = esm_waiting.getCount();
        }
        if( esm_waiting != null && ! esm_waiting.isClosed() ) esm_waiting.close();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(c);
        mBuilder.setSmallIcon(R.drawable.ic_stat_aware_esm);
        mBuilder.setContentTitle("AWARE");
        mBuilder.setContentText(c.getResources().getText(R.string.aware_esm_questions));
        mBuilder.setNumber( esm_count );
        mBuilder.setOnlyAlertOnce(true); //notify the user only once for the same notification ID
        mBuilder.setOngoing(true);
        mBuilder.setDefaults( NotificationCompat.DEFAULT_ALL );

        Intent intent_ESM = new Intent( c, ESM_Queue.class );
        intent_ESM.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pending_ESM = PendingIntent.getActivity( c, 0, intent_ESM, PendingIntent.FLAG_UPDATE_CURRENT );
        mBuilder.setContentIntent(pending_ESM);

        mNotificationManager.notify(ESM_NOTIFICATION_ID, mBuilder.build());
    }

    //Singleton instance of this service
    private static ESM esmSrv = ESM.getService();
    
    /**
     * Get singleton instance to service
     * @return ESM obj
     */
    public static ESM getService() {
        if( esmSrv == null ) esmSrv = new ESM();
        return esmSrv;
    }
    
    private final IBinder serviceBinder = new ServiceBinder();
    public class ServiceBinder extends Binder {
        ESM getService() {
            return ESM.getService();
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }
    
    /**
     * BroadcastReceiver for ESM module 
     * - ACTION_AWARE_QUEUE_ESM
     * - ACTION_AWARE_ESM_ANSWERED
     * - ACTION_AWARE_ESM_DISMISSED
     * - ACTION_AWARE_ESM_EXPIRED
     * @author df
     */
    public static class ESMMonitor extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ( Aware.getSetting(context, Aware_Preferences.STATUS_ESM).equals("false") ) return;

            if( intent.getAction().equals(ESM.ACTION_AWARE_QUEUE_ESM) ) {
            	Intent backgroundService = new Intent( context, BackgroundService.class );
                backgroundService.setAction(ESM.ACTION_AWARE_QUEUE_ESM);
                backgroundService.putExtra(EXTRA_ESM, intent.getStringExtra("esm"));
                context.startService( backgroundService );
            }

            if( intent.getAction().equals(ESM.ACTION_AWARE_ESM_ANSWERED) ) {
                if( ESM_Queue.getQueueSize(context) > 0 ) {
                    Intent intent_ESM = new Intent( context, ESM_Queue.class );
                    intent_ESM.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP );
                    context.startActivity( intent_ESM );
                } else {
                    if(Aware.DEBUG) Log.d(TAG,"ESM Queue is done!");
                    Intent esm_done = new Intent(ESM.ACTION_AWARE_ESM_QUEUE_COMPLETE);
                    context.sendBroadcast(esm_done);
                }
            }

            if ( intent.getAction().equals(ESM.ACTION_AWARE_ESM_DISMISSED) ) {
                if(Aware.DEBUG) Log.d(TAG,"Rest of ESM Queue is dismissed!");

                Cursor esm = context.getContentResolver().query(ESM_Data.CONTENT_URI, null, ESM_Data.STATUS + " IN (" + ESM.STATUS_NEW + "," + ESM.STATUS_VISIBLE + ")", null, null);
                if( esm != null && esm.moveToFirst() ) {
                    do {
                        ContentValues rowData = new ContentValues();
                        rowData.put(ESM_Data.ANSWER_TIMESTAMP, System.currentTimeMillis());
                        rowData.put(ESM_Data.STATUS, ESM.STATUS_DISMISSED);
                        context.getContentResolver().update(ESM_Data.CONTENT_URI, rowData, null, null);
                    } while(esm.moveToNext());
                }
                if( esm != null && ! esm.isClosed()) esm.close();

                Intent esm_done = new Intent(ESM.ACTION_AWARE_ESM_QUEUE_COMPLETE);
                context.sendBroadcast(esm_done);
            }

            if( intent.getAction().equals(ESM.ACTION_AWARE_ESM_EXPIRED ) ) {
                if(Aware.DEBUG) Log.d(TAG,"Rest of ESM Queue is expired!");

                Cursor esm = context.getContentResolver().query(ESM_Data.CONTENT_URI, null, ESM_Data.STATUS + " IN (" + ESM.STATUS_NEW + "," + ESM.STATUS_VISIBLE + ")", null, null);
                if( esm != null && esm.moveToFirst() ) {
                    do {
                        ContentValues rowData = new ContentValues();
                        rowData.put(ESM_Data.ANSWER_TIMESTAMP, System.currentTimeMillis());
                        rowData.put(ESM_Data.STATUS, ESM.STATUS_EXPIRED);
                        context.getContentResolver().update(ESM_Data.CONTENT_URI, rowData, null, null);
                    } while(esm.moveToNext());
                }
                if( esm != null && ! esm.isClosed()) esm.close();

                Intent esm_done = new Intent(ESM.ACTION_AWARE_ESM_QUEUE_COMPLETE);
                context.sendBroadcast(esm_done);
            }
        }
    }
    private static final ESMMonitor esmMonitor = new ESMMonitor();

    /**
     * ESM background service
     * - Queue ESM received to the local database
     * @author df
     *
     */
    public static class BackgroundService extends IntentService {
        public BackgroundService() {
            super(TAG+" background service");
        }
        
        @Override
        protected void onHandleIntent(Intent intent) {
            if( intent.getAction().equals(ESM.ACTION_AWARE_QUEUE_ESM) && intent.getStringExtra(EXTRA_ESM) != null && intent.getStringExtra(EXTRA_ESM).length() > 0 ) {
                try {
                    JSONArray esms = new JSONArray(intent.getStringExtra(EXTRA_ESM));
                    
                    long esm_timestamp = System.currentTimeMillis();
                    boolean is_persistent = false;
                    
                    for( int i = 0; i<esms.length(); i++ ) {
                        JSONObject esm = esms.getJSONObject(i).getJSONObject(EXTRA_ESM);
                        
                        ContentValues rowData = new ContentValues();
                        rowData.put(ESM_Data.TIMESTAMP, esm_timestamp);
                        rowData.put(ESM_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                        rowData.put(ESM_Data.TYPE, esm.optInt(ESM_Data.TYPE));
                        rowData.put(ESM_Data.TITLE, esm.optString(ESM_Data.TITLE));
                        rowData.put(ESM_Data.SUBMIT, esm.optString(ESM_Data.SUBMIT));
                        rowData.put(ESM_Data.INSTRUCTIONS, esm.optString(ESM_Data.INSTRUCTIONS));
                        rowData.put(ESM_Data.RADIOS, esm.optString(ESM_Data.RADIOS));
                        rowData.put(ESM_Data.CHECKBOXES, esm.optString(ESM_Data.CHECKBOXES));
                        rowData.put(ESM_Data.LIKERT_MAX, esm.optInt(ESM_Data.LIKERT_MAX));
                        rowData.put(ESM_Data.LIKERT_MAX_LABEL, esm.optString(ESM_Data.LIKERT_MAX_LABEL));
                        rowData.put(ESM_Data.LIKERT_MIN_LABEL, esm.optString(ESM_Data.LIKERT_MIN_LABEL));
                        rowData.put(ESM_Data.LIKERT_STEP, esm.optDouble(ESM_Data.LIKERT_STEP,0));
                        rowData.put(ESM_Data.QUICK_ANSWERS, esm.optString(ESM_Data.QUICK_ANSWERS));
                        rowData.put(ESM_Data.EXPIRATION_THRESHOLD, esm.optInt(ESM_Data.EXPIRATION_THRESHOLD));
                        rowData.put(ESM_Data.SCALE_MIN, esm.optInt(ESM_Data.SCALE_MIN));
                        rowData.put(ESM_Data.SCALE_MAX, esm.optInt(ESM_Data.SCALE_MAX));
                        rowData.put(ESM_Data.SCALE_START, esm.optInt(ESM_Data.SCALE_START));
                        rowData.put(ESM_Data.SCALE_MAX_LABEL, esm.optString(ESM_Data.SCALE_MAX_LABEL));
                        rowData.put(ESM_Data.SCALE_MIN_LABEL, esm.optString(ESM_Data.SCALE_MIN_LABEL));
                        rowData.put(ESM_Data.SCALE_STEP, esm.optInt(ESM_Data.SCALE_STEP));
                        rowData.put(ESM_Data.STATUS, ESM.STATUS_NEW);
                        rowData.put(ESM_Data.TRIGGER, esm.optString(ESM_Data.TRIGGER));
                        
                        if( rowData.getAsInteger(ESM_Data.EXPIRATION_THRESHOLD) == 0 ) {
                            is_persistent = true;
                        }
                        
                        try {
                            getContentResolver().insert(ESM_Data.CONTENT_URI, rowData);
                            if( Aware.DEBUG ) Log.d(TAG, "ESM: "+ rowData.toString() );
                        }catch( SQLiteException e ) {
                            if(Aware.DEBUG) Log.d(TAG,e.getMessage());
                        }
                    }
                    
                    if ( is_persistent ) {
                        notifyESM(getApplicationContext());
                    } else {
                        Intent intent_ESM = new Intent( getApplicationContext(), ESM_Queue.class );
                        intent_ESM.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent_ESM);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();                    
                }
            }
        }
    }
}