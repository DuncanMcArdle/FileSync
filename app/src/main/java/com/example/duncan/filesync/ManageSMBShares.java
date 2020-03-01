package com.example.duncan.filesync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ManageSMBShares extends AppCompatActivity
{
    private SharedPreferences myPreferences;
    SharedPreferences.Editor myPrefsEdit;
    CustomAdapter smbShareListAdapter;

    // Request codes
    int REQUEST_CODE_ADD_SMBSHARE = 1000;
    int REQUEST_CODE_EDIT_SMBSHARE = 1001;

    // Array of SMB shares
    JSONArray smbShareArray = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_smbshares);

        // Initialise the toolbar
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialise shared preferences
        myPreferences = getSharedPreferences("FileSync", 0);
        myPrefsEdit = myPreferences.edit();

        // Refresh the list of SMB shares
        LoadSMBShares();

        // Populate the ListView with the retrieved SMB shares
        ListView listView = (ListView) findViewById(R.id.customListView);
        smbShareListAdapter = new CustomAdapter();
        listView.setAdapter(smbShareListAdapter);

        // When the user clicks the "ADD NEW SMB SHARE" button
        Button addSMBShareButton = (Button) findViewById(R.id.addSMBShare);
        addSMBShareButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Load the "Add SMB share page"
                Intent intent = new Intent(ManageSMBShares.this, AddEditSMBShare.class);
                startActivityForResult(intent, REQUEST_CODE_ADD_SMBSHARE);
                overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent resultData)
    {
        if (resultCode == RESULT_OK)
        {
            // When the user returns from creating a new SMB share
            if (requestCode == REQUEST_CODE_ADD_SMBSHARE)
            {
                Log.i("STORAGE", "Returned from getting new SMB share");

                JSONObject newSMBShare = new JSONObject();
                try {
                    // Populate the JSON object with the passed in details
                    newSMBShare.put("Title", resultData.getStringExtra("Title"));
                    newSMBShare.put("Domain", resultData.getStringExtra("Domain"));
                    newSMBShare.put("Username", resultData.getStringExtra("Username"));
					newSMBShare.put("Password", resultData.getStringExtra("Password"));
					newSMBShare.put("Address", resultData.getStringExtra("Address"));
                    smbShareArray.put(newSMBShare);
                    Log.i("STORAGE", "Adding share: "+smbShareArray.toString());

                    // Sort the SMB array
                    smbShareArray = SortJSONArray(smbShareArray, "Title");

                    // Update shared preferences
                    myPrefsEdit.putString("SMBShares", smbShareArray.toString());
                    myPrefsEdit.commit();
                    Log.i("STORAGE", "Added SMB share");

                    // Update the ListView
                    smbShareListAdapter.notifyDataSetChanged();
                    LoadSMBShares();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // When the user returns from editing an existing SMB share
            if (requestCode == REQUEST_CODE_EDIT_SMBSHARE)
            {
                // Check if the user deleted the SMB share
                if (resultData.hasExtra("Delete"))
                {
                    // Delete the SMB share
                    smbShareArray.remove(resultData.getIntExtra("ID", -1));

                    // Update shared preferences
                    myPrefsEdit.putString("SMBShares", smbShareArray.toString());
                    myPrefsEdit.commit();
                }
                else
                    {
                    // Initialise a new JSON object
                    JSONObject newSMBShare = new JSONObject();

                    try {
                        // Populate the JSON object with the passed in details
                        newSMBShare.put("Title", resultData.getStringExtra("Title"));
                        newSMBShare.put("Domain", resultData.getStringExtra("Domain"));
                        newSMBShare.put("Username", resultData.getStringExtra("Username"));
						newSMBShare.put("Password", resultData.getStringExtra("Password"));
						newSMBShare.put("Address", resultData.getStringExtra("Address"));

                        Log.i("STORAGE", "JSON: " + newSMBShare.toString());

                        // Remove the existing SMB share
                        smbShareArray.remove(resultData.getIntExtra("ID", -1));

                        // Add the new version of the SMB share in
                        smbShareArray.put(newSMBShare);

                        // Sort the SMB array
                        smbShareArray = SortJSONArray(smbShareArray, "Title");

                        // Update shared preferences
                        myPrefsEdit.putString("SMBShares", smbShareArray.toString());
                        myPrefsEdit.commit();

                        // Get the share's original title
                        String originalTitle = resultData.getStringExtra("OriginalTitle");
                        String newTitle = resultData.getStringExtra("Title");

                        // Loop through an array of existing synchronisations
                        JSONArray synchronisationArray = new JSONArray(myPreferences.getString("Synchronisations", "[]"));
                        for (int i = 0; i < synchronisationArray.length(); i++)
                        {
                            // Check if the synchronisation references the SMB share being changed as its source
                            if(synchronisationArray.getJSONObject(i).getString("SourceSMBShare").equals(originalTitle))
                            {
                                // If so, update the title
                                synchronisationArray.getJSONObject(i).put("SourceSMBShare", newTitle);
                                Log.i("STORAGE","Updated SourceSMBShare for Synchronisation"+i+" from '"+originalTitle+"' to '"+newTitle+"'");
                            }

                            // Check if the synchronisation references the SMB share being changed as its source
                            if(synchronisationArray.getJSONObject(i).getString("TargetSMBShare").equals(originalTitle))
                            {
                                // If so, update the title
                                synchronisationArray.getJSONObject(i).put("TargetSMBShare", newTitle);
                                Log.i("STORAGE","Updated TargetSMBShare for Synchronisation"+i+" from '"+originalTitle+"' to '"+newTitle+"'");
                            }

                            // Update shared preferences
                            myPrefsEdit.putString("Synchronisations", synchronisationArray.toString());
                            myPrefsEdit.commit();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // Update the ListView
                smbShareListAdapter.notifyDataSetChanged();
                LoadSMBShares();
            }
        }
    }

    // Function to load and display the SMB shares
    public void LoadSMBShares()
    {
        // Obtain a reference to the ListView's loading text
        TextView listViewLoader = (TextView) findViewById(R.id.listViewLoader);

        try
        {
            // Load an array of existing SMB shares
            smbShareArray = new JSONArray(myPreferences.getString("SMBShares", "[]"));

            Log.i("STORAGE", smbShareArray.toString()+" "+ smbShareArray.length());

            // Check if any SMB shares were retrieved
            if(smbShareArray.length() <= 0)
            {
                // Append a notice to the list of SMB shares
                listViewLoader.setText("No SMB shares to show.");
                listViewLoader.setVisibility(View.VISIBLE);
            }
            else
            {
                // Hide the loader
                listViewLoader.setVisibility(View.GONE);
            }
        }
        catch (Exception e)
        {
            // Append a notice to the list of SMB shares
            listViewLoader.setText("An error occurred when loading your SMB shares.");
            e.printStackTrace();
        }

        // Update the ListView
        //smbShareListAdapter.notifyDataSetChanged();
    }

    class CustomAdapter extends BaseAdapter
    {
        @Override
        public int getCount()
        {
            return smbShareArray.length();
        }

        @Override
        public Object getItem(int position)
        {
            return null;
        }

        @Override
        public long getItemId(int position)
        {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent)
        {
            convertView = View.inflate(getApplicationContext(), R.layout.smbshare_listitem, null);

            TextView smbShareTitle = (TextView) convertView.findViewById(R.id.smbFileFolderTitle);
            try
            {
                smbShareTitle.setText(smbShareArray.getJSONObject(position).getString("Title").toString());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            // Assign an action to the "Edit" button
            Button editButton = (Button) convertView.findViewById(R.id.editJobButton);
            editButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    try
                    {
                        // Obtain the details of the selected SMB share
                        JSONObject smbShareObject = smbShareArray.getJSONObject(position);
                        Log.i("STORAGE", smbShareObject.toString());

                        // Create an intent to open the Add / Edit SMB share activity
                        Intent editSMBShareIntent = new Intent(ManageSMBShares.this, AddEditSMBShare.class);
                        editSMBShareIntent.putExtra("ID", position);
                        editSMBShareIntent.putExtra("Title", smbShareObject.getString("Title"));
                        editSMBShareIntent.putExtra("Domain", smbShareObject.getString("Domain"));
                        editSMBShareIntent.putExtra("Username", smbShareObject.getString("Username"));
						editSMBShareIntent.putExtra("Password", smbShareObject.getString("Password"));
						editSMBShareIntent.putExtra("Address", smbShareObject.getString("Address"));

                        // Start the Add / Edit SMB share activity
                        startActivityForResult(editSMBShareIntent, REQUEST_CODE_EDIT_SMBSHARE);
                        overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
                    }
                    catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                }
            });

            return convertView;
        }
    }

    // Function to sort an array of JSON objects
    public JSONArray SortJSONArray(JSONArray arrayToSort, final String attributeToSort) throws JSONException
    {
        // Convert the JSON Array to a list of JSON objects (so it can be sorted)
        List<JSONObject> jsonList = new ArrayList<JSONObject>();
        for (int i = 0; i < arrayToSort.length(); i++)
        {
            jsonList.add(arrayToSort.getJSONObject(i));
        }

        // Sort the list of JSON objects
        Collections.sort(jsonList, new Comparator<JSONObject>()
        {
            @Override
            public int compare(JSONObject lhs, JSONObject rhs)
            {
                String leftHandSide = "";
                String rightHandSide = "";
                try
                {
                    leftHandSide = lhs.getString(attributeToSort);
                    rightHandSide = rhs.getString(attributeToSort);

                } catch (JSONException e)
                {
                    e.printStackTrace();
                }

                // Here you could parse string id to integer and then compare.
                return leftHandSide.toUpperCase().compareTo(rightHandSide.toUpperCase());
            }
        });

        // Return the sorted list of JSON objects, converted back into a JSON Array
        return new JSONArray(jsonList);
    }
}
