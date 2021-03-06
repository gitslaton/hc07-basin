package edu.hanover.basin.Events.Activities;

import android.content.Intent;

import java.util.Calendar;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.Profile;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.hanover.basin.Map.Activities.MapsActivity;
import edu.hanover.basin.R;
import edu.hanover.basin.Request.Objects.basinURL;

/**
 * Activity for editing or creating a new activity.
 * If an event id is passed in, it will update that one. If not, it will create a new event.
 *
 * @author Slaton Blickman
 * @see AppCompatActivity
 */
@SuppressWarnings("ALL")
public class EventCreationActivity extends AppCompatActivity {

    /**
     * Field necessary for setting the defaults of the editable fields
     */
    public static final String EXTRA_ACTIVITY_STARTED = "ActivityStarted";

    /**
     * Field necessary for giving event location
     */
    public static final String EXTRA_EVENT_LAT = "EventLat";

    /**
     * Field necessary for giving event location
     */
    public static final String EXTRA_EVENT_LNG = "EventLng";

    /**
     * Field necessary to check if this is updating a previous event
     */
    public static final String EXTRA_UPDATING = "EventUpdate";

    /**
     * Field necessary when updating
     */
    public static final String EXTRA_EVENT_ID = "EventID";

    /**
     * Field necessary for filling default title in form
     */
    public static final String EXTRA_TITLE = "EventTitle";

    /**
     * Field necessary for filling default description in form
     */
    public static final String EXTRA_DESCRIPTION = "EventDesc";

    /**
     * Field necessary for filling default time in form
     */
    public static final String EXTRA_TIME = "EventTime";

    /**
     * Field necessary for filling default date in form
     */
    public static final String EXTRA_DATE = "EventDate";



    //variables for views and layouts
    private EditText title;
    private EditText description;
    private EditText time;
    private DatePicker date;

    //instance variables
    private boolean updating;
    private String location, eventID, facebook_id; //bad coding: inconsistent naming
    private double lat, lng;
    private int requestMethod;
    private basinURL url;


    /**
     * Sets private variables for views and layouts.
     * Gets values from intents passed in that indicate non-user specified information: location and whether this is updates an already created event.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_creation);

        facebook_id = Profile.getCurrentProfile().getId();
        title = (EditText)findViewById(R.id.title);
        description = (EditText)findViewById(R.id.description);
        time = (EditText)findViewById(R.id.time);
        date = (DatePicker)findViewById(R.id.datePicker);
        date.setMinDate(System.currentTimeMillis() - 10000);

        lat = (Double)getIntent().getExtras().get(EXTRA_EVENT_LAT);
        lng = (Double)getIntent().getExtras().get(EXTRA_EVENT_LNG);
        updating = (Boolean)getIntent().getExtras().get(EXTRA_UPDATING);
        location = String.valueOf(lat) + ", " + String.valueOf(lng);

        url = new basinURL();

    }

    /**
     * Prepoluates editable fields if this Activity is used to update an event
     */
    @Override
    protected  void onResume(){
        super.onResume();

        if(updating){
            String editTitle, editDesc, editTime, editDate;
            int year, month, day;

            //get the minimimum date throigh a calendar instance
            Calendar cal = Calendar.getInstance();
            cal.set(date.getYear(), date.getMonth() + 1, date.getDayOfMonth());
            cal.add(Calendar.DATE, 120);

            editDate =(String)getIntent().getExtras().get(EXTRA_DATE);
            editTitle = (String)getIntent().getExtras().get(EXTRA_TITLE);
            editDesc = (String)getIntent().getExtras().get(EXTRA_DESCRIPTION);
            editTime = (String)getIntent().getExtras().get(EXTRA_TIME);
            eventID = (String)getIntent().getExtras().get(EXTRA_EVENT_ID);

            //editDate should have format "11-11-2011"
            year = Integer.parseInt(editDate.substring(5, 8));
            month = Integer.parseInt(editDate.substring(3, 4));
            day = Integer.parseInt(editDate.substring(0,1));

            try{
                date.updateDate(year, month, day);
            }
            catch(Exception e){
                Log.e("Exception setting date", e.toString());
            }

            title.setText(editTitle);
            description.setText(editDesc);
            time.setText(editTime);

            //set variables to identify the later request to be a PUT for an event
            requestMethod = Request.Method.PUT;
            url.getEventURL(eventID);
        }
        else{
            //set variables to identify the later request to be a POST to create a new event
            requestMethod = Request.Method.POST;
            url.getEventURL("");
        }
    }

    /**
     * Inflates the menu layout to use menu_edit.xml
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit, menu);

        return true;
    }


    /**
     * Handles on click of menu items.
     * Does the following:
     * (save_icon) Calls private function to validate fields and send request to update/create event
     * (cancel_icon) Finish current activity
     * @param item MenuItem clicked
     * @return boolean for success
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.save_icon:
                createEvent();

                return true;
            case R.id.cancel_icon:
                finish();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    //use regex to validate the time format
    private boolean validTime(){
        //regex reference http://www.mkyong.com/regular-expressions/how-to-validate-time-in-24-hours-format-with-regular-expression/
        Pattern pattern;
        Matcher matcher;
        String TIME24HOURS_PATTERN = "([01]?[0-9]|2[0-3]):[0-5][0-9]";
        pattern = Pattern.compile(TIME24HOURS_PATTERN);
        matcher = pattern.matcher(time.getText());

        return matcher.matches();
    }

    private void createEvent(){
        try{
            JSONObject body = new JSONObject();

            body.put("facebook_created_by", facebook_id);
            body.put("title", title.getText());
            body.put("description", description.getText());
            body.put("lat_coord", lat);
            body.put("long_coord", lng);
            body.put("date", (date.getMonth() + 1) + "-" + date.getDayOfMonth() + "-" + date.getYear());

            if(validTime()) {
                body.put("time_start", time.getText());
                request(requestMethod, url.toString(), body);
            }
            else{
                Toast.makeText(this, "Time is invalid!", Toast.LENGTH_SHORT).show();
            }
        }
        catch(JSONException e){
            Log.e("JSON EXCEPTION", e.toString());
        }
    }

    private void request(int method, String url, JSONObject body){
        // Request a string response
        Log.i("Requesting: ", url);
        Log.i("Body:", body.toString());
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(method, url, body,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        //event = response;
                        try{
                            Intent thisIntent = getIntent();
                            String activity;
                            if(thisIntent != null){
                                //do different actions depending on previous activity
                                activity = thisIntent.getExtras().getString(EXTRA_ACTIVITY_STARTED);

                                if(activity.equals("EventDetails")){
                                    finish();
                                }
                                else{
                                    //go to the location on hte map of hte new activity

                                    Toast.makeText(getApplicationContext(), "Success!", Toast.LENGTH_LONG).show();
                                    Intent intent = new Intent(EventCreationActivity.this, MapsActivity.class);
                                    intent.putExtra(MapsActivity.EXTRA_EVENT_LNG, lng);
                                    intent.putExtra(MapsActivity.EXTRA_EVENT_LAT, lat);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                }
                            }
                       }

                        catch(Exception e){
                            Log.e("JSON EXCEPTION", e.toString());
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                // Error handling
                Log.e("Volley error", error.toString());
                error.printStackTrace();
            }
        });

        // Add the request to the queue
        Volley.newRequestQueue(this).add(jsonObjectRequest);

    }

}
