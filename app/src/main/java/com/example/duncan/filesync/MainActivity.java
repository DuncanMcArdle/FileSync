package com.example.duncan.filesync;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jcifs.smb.NtlmPasswordAuthentication;

public class MainActivity extends AppCompatActivity implements ContextualASyncTask.ContextualInterface
{

	Uri sourceFile;
	private static final byte[] buffer = new byte[60416];
	private SharedPreferences myPreferences;
	SharedPreferences.Editor myPrefsEdit;
	CustomAdapter synchronisationListAdapter;
	ContextualASyncTask synchronisationTask;

	// Request codes
	int REQUEST_CODE_ADD_SYNCHRONISATION = 1000;
	int REQUEST_CODE_EDIT_SYNCHRONISATION = 1001;

	// Array of synchronisation jobs
	JSONArray synchronisationArray = null;

	// Loader
	Loader syncLoader;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		android.support.v7.widget.Toolbar myToolbar = findViewById(R.id.my_toolbar);
		setSupportActionBar(myToolbar);

		// Initialise shared preferences
		myPreferences = getSharedPreferences("FileSync", 0);
		myPrefsEdit = myPreferences.edit();

		// Refresh the list of synchronisations
		LoadSynchronisations();

		// Populate the ListView with the retrieved synchronisations
		ListView listView = (ListView) findViewById(R.id.customListView);
		synchronisationListAdapter = new CustomAdapter();
		listView.setAdapter(synchronisationListAdapter);

		ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 255);
		ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 256);

		// When the user clicks the "ADD SYNCHRONISATION" button
		Button addSynchronisationButton = (Button) findViewById(R.id.addSynchronisation);
		addSynchronisationButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Load the "Add synchronisation page"
				Intent intent = new Intent(MainActivity.this, AddEditSynchronisation.class);
				startActivityForResult(intent, REQUEST_CODE_ADD_SYNCHRONISATION);
				overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		android.support.v7.widget.Toolbar toolbar = findViewById(R.id.my_toolbar);
		toolbar.inflateMenu(R.menu.main_activity_menu);
		toolbar.setOnMenuItemClickListener(new android.support.v7.widget.Toolbar.OnMenuItemClickListener()
		{
			@Override
			public boolean onMenuItemClick(MenuItem item)
			{
				switch (item.getItemId())
				{
					// When the user selects the "MANAGE SMB SHARES" option
					case R.id.main_activity_menu_manage_smb_shares:
					{
						// Create and start an intent to open the Manage SMB Credentials activity
						Intent manageSMBCredentialsIntent = new Intent(MainActivity.this, ManageSMBShares.class);
						startActivity(manageSMBCredentialsIntent);
						overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
						return true;
					}
				}

				Log.i("STORAGE", "Menu item clicked");
				return false;
			}
		});

		return super.onCreateOptionsMenu(menu);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent resultData)
	{
		if (resultCode == RESULT_OK)
		{
			// When the user returns from creating a new synchronisation job
			if (requestCode == REQUEST_CODE_ADD_SYNCHRONISATION)
			{
				Log.i("STORAGE", "Returned from getting new sync job");

				JSONObject newSynchronisationJob = new JSONObject();
				try
				{
					// Populate the JSON object with the passed in details
					newSynchronisationJob.put("Title", resultData.getStringExtra("Title"));
					newSynchronisationJob.put("SourceType", resultData.getStringExtra("SourceType"));
					newSynchronisationJob.put("SourceSMBShare", resultData.getStringExtra("SourceSMBShare"));
					newSynchronisationJob.put("SourceFolder", resultData.getStringExtra("SourceFolder"));
					newSynchronisationJob.put("TargetType", resultData.getStringExtra("TargetType"));
					newSynchronisationJob.put("TargetSMBShare", resultData.getStringExtra("TargetSMBShare"));
					newSynchronisationJob.put("TargetFolder", resultData.getStringExtra("TargetFolder"));
					newSynchronisationJob.put("DeleteTargetContents", resultData.getBooleanExtra("DeleteTargetContents", false));
					synchronisationArray.put(newSynchronisationJob);

					// Sort the synchronisation array
					synchronisationArray = SortJSONArray(synchronisationArray, "Title");

					// Update shared preferences
					myPrefsEdit.putString("Synchronisations", synchronisationArray.toString());
					myPrefsEdit.commit();
					Log.i("STORAGE", "Added synchronisation");

					// Update the ListView
					synchronisationListAdapter.notifyDataSetChanged();
					LoadSynchronisations();
				} catch (JSONException e)
				{
					e.printStackTrace();
				}
			}

			// When the user returns from editing an existing synchronisation job
			if (requestCode == REQUEST_CODE_EDIT_SYNCHRONISATION)
			{
				// Check if the user deleted the synchronisation
				if (resultData.hasExtra("Delete"))
				{
					// Delete the synchronisation
					synchronisationArray.remove(resultData.getIntExtra("ID", -1));

					// Update shared preferences
					myPrefsEdit.putString("Synchronisations", synchronisationArray.toString());
					myPrefsEdit.commit();
				}
				else
				{
					// Initialise a new JSON object
					JSONObject newSynchronisationJob = new JSONObject();

					try
					{
						// Populate the JSON object with the passed in details
						newSynchronisationJob.put("Title", resultData.getStringExtra("Title"));
						newSynchronisationJob.put("SourceType", resultData.getStringExtra("SourceType"));
						newSynchronisationJob.put("SourceSMBShare", resultData.getStringExtra("SourceSMBShare"));
						newSynchronisationJob.put("SourceFolder", resultData.getStringExtra("SourceFolder"));
						newSynchronisationJob.put("TargetType", resultData.getStringExtra("TargetType"));
						newSynchronisationJob.put("TargetSMBShare", resultData.getStringExtra("TargetSMBShare"));
						newSynchronisationJob.put("TargetFolder", resultData.getStringExtra("TargetFolder"));
						newSynchronisationJob.put("DeleteTargetContents", resultData.getBooleanExtra("DeleteTargetContents", false));

						Log.i("STORAGE", "JSON2: " + newSynchronisationJob.toString());

						// Remove the existing job
						synchronisationArray.remove(resultData.getIntExtra("ID", -1));

						// Add the new version of the job in
						synchronisationArray.put(newSynchronisationJob);

						// Sort the synchronisation array
						synchronisationArray = SortJSONArray(synchronisationArray, "Title");

						// Update shared preferences
						myPrefsEdit.putString("Synchronisations", synchronisationArray.toString());
						myPrefsEdit.commit();
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				}

				// Update the ListView
				synchronisationListAdapter.notifyDataSetChanged();
				LoadSynchronisations();
			}
		}
	}

	// Function to load and display synchronisations
	public void LoadSynchronisations()
	{
		// Obtain a reference to the ListView's loading text
		TextView listViewLoader = (TextView) findViewById(R.id.listViewLoader);

		try
		{
			// Load an array of existing synchronisations
			synchronisationArray = new JSONArray(myPreferences.getString("Synchronisations", "[]"));

			// Check if any synchronisations were retrieved
			if (synchronisationArray.length() <= 0)
			{
				// If not, append a notice to the list of synchronisations
				listViewLoader.setText("No synchronisations to show.");
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
			// Append a notice to the list of synchronisations
			listViewLoader.setText("An error occurred when loading your synchronisations.");
			e.printStackTrace();
		}
	}

	// Function called when a synchronisation completes
	@Override
	public void OnSynchronisationProgress(final int totalFiles, final int filesProcessed, final int totalBytes, final int bytesProcessed)
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				// Update the title
				syncLoader.UpdateTitle("Synchronising...");

				// Calculate the percentage complete
				int percentageComplete = (int) (((double) bytesProcessed / (double) totalBytes) * 100);

				// Update the loader
				syncLoader.ShowLoaderWithProgressBar(percentageComplete, filesProcessed, totalFiles);
				syncLoader.UpdateCloseButton(true, "Cancel", new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						// Stop the ongoing task
						synchronisationTask.cancel(true);
						Log.i("STORAGE", "User cancelled the synchronisation during synchronisation.");

						// Close the loader
						syncLoader.HideLoader();
					}
				});
			}
		});
	}

	// Function called when a synchronisation fails to coplete
	@Override
	public void OnSynchronisationFailed(final String error)
	{
		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				// Switch based on the cause of the exception
				switch(error)
				{
					case "write failed: ENOSPC (No space left on device)":
					{
						syncLoader.ShowLoaderWithIcon("Out of space", R.drawable.ic_highlight_off_black_24dp, "There was insufficient space remaining in the destination. Please free up some space and try again.");
						break;
					}
					default:
					{
						syncLoader.ShowLoaderWithIcon("Unknown error", R.drawable.ic_highlight_off_black_24dp, "An unknown error occurred during the synchronisation.");
					}
				}

				// Set the popup's close button
				syncLoader.UpdateCloseButton(true, "Close", null);
			}
		});

		// Cancel the task
		synchronisationTask.cancel(true);
	}

	// Function called when a synchronisation completes
	@Override
	public void OnSynchronisationComplete(int totalFiles, int filesTransferred, int totalBytes, int bytesTransferred)
	{
		Log.i("STORAGE", "Sync complete");

		// Update the loader
		syncLoader.UpdateTitle("Synchronised");

		// Show the summary
		syncLoader.ShowLoaderWithFileTransferSummary(totalFiles, (totalFiles - filesTransferred), bytesTransferred);

		// Update the loader's close button
		syncLoader.UpdateCloseButton(true, "Close", new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Close the loader
				syncLoader.HideLoader();
			}
		});
	}

	class CustomAdapter extends BaseAdapter
	{
		@Override
		public int getCount()
		{
			return synchronisationArray.length();
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
			convertView = getLayoutInflater().inflate(R.layout.synchronisation_listitem, null);

			TextView jobTitle = (TextView) convertView.findViewById(R.id.jobTitle);
			try
			{
				jobTitle.setText(synchronisationArray.getJSONObject(position).getString("Title").toString());
			} catch (Exception e)
			{
				e.printStackTrace();
			}

			// Assign an action to the "Run" button
			Button runButton = (Button) convertView.findViewById(R.id.runJobButton);
			runButton.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					// Obtain the source and target URIs
					try
					{
						// Show a loader
						syncLoader = new Loader(MainActivity.this, getLayoutInflater());
						syncLoader.UpdateTitle("Initialising...");
						syncLoader.ShowLoaderWithSpinner();
						syncLoader.UpdateCloseButton(true, "Cancel", new View.OnClickListener()
						{
							@Override
							public void onClick(View v)
							{
								// Stop the ongoing task
								synchronisationTask.cancel(true);
								Log.i("STORAGE", "User cancelled the synchronisation during synchronisation.");

								// Close the loader
								syncLoader.HideLoader();
							}
						});

						// Fetch the selected synchronisation details
						JSONObject targetSynchronisation = synchronisationArray.getJSONObject(position);

						Log.i("STORAGE", "JSON contents: '" + targetSynchronisation + "'.");

						NtlmPasswordAuthentication sourceAuthentication = (targetSynchronisation.getString("SourceType").equals("LOCAL") ? null : GetSMBCredentials(targetSynchronisation.getString("SourceSMBShare")));
						NtlmPasswordAuthentication targetAuthentication = (targetSynchronisation.getString("TargetType").equals("LOCAL") ? null : GetSMBCredentials(targetSynchronisation.getString("TargetSMBShare")));

						String sourceFolder = (targetSynchronisation.getString("SourceType").equals("LOCAL") ? targetSynchronisation.getString("SourceFolder") : GetSMBAddress(targetSynchronisation.getString("SourceSMBShare")) + targetSynchronisation.getString("SourceFolder"));
						String targetFolder = (targetSynchronisation.getString("TargetType").equals("LOCAL") ? targetSynchronisation.getString("TargetFolder") : GetSMBAddress(targetSynchronisation.getString("TargetSMBShare")) + targetSynchronisation.getString("TargetFolder"));

						synchronisationTask = new ContextualASyncTask(MainActivity.this, sourceFolder, sourceAuthentication, targetFolder, targetAuthentication, targetSynchronisation.getBoolean("DeleteTargetContents"));
						synchronisationTask.callingActivityInterface = MainActivity.this;
						synchronisationTask.execute();

					} catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			});

			// Assign an action to the "Edit" button
			Button editButton = (Button) convertView.findViewById(R.id.editJobButton);
			editButton.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					try
					{
						// Obtain the details of the selected synchronisation
						JSONObject synchronisationObject = synchronisationArray.getJSONObject(position);

						// Create an intent to open the Add / Edit synchronisation activity
						Intent editSynchronisationIntent = new Intent(MainActivity.this, AddEditSynchronisation.class);
						editSynchronisationIntent.putExtra("ID", position);
						editSynchronisationIntent.putExtra("Title", synchronisationObject.getString("Title"));
						editSynchronisationIntent.putExtra("SourceType", synchronisationObject.getString("SourceType"));
						editSynchronisationIntent.putExtra("SourceSMBShare", synchronisationObject.getString("SourceSMBShare"));
						editSynchronisationIntent.putExtra("SourceFolder", synchronisationObject.getString("SourceFolder"));
						editSynchronisationIntent.putExtra("TargetType", synchronisationObject.getString("TargetType"));
						editSynchronisationIntent.putExtra("TargetSMBShare", synchronisationObject.getString("TargetSMBShare"));
						editSynchronisationIntent.putExtra("TargetFolder", synchronisationObject.getString("TargetFolder"));
						editSynchronisationIntent.putExtra("DeleteTargetContents", synchronisationObject.getBoolean("DeleteTargetContents"));

						// Start the Add / Edit synchronisation activity
						startActivityForResult(editSynchronisationIntent, REQUEST_CODE_EDIT_SYNCHRONISATION);
						overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
					} catch (JSONException e)
					{
						e.printStackTrace();
					}
				}
			});

			return convertView;
		}
	}

	// Function to return the SMB credentials for an SMB share based on its title
	NtlmPasswordAuthentication GetSMBCredentials(String SMBShareTitle)
	{
		try
		{
			// Obtain the available SMB credentials
			JSONArray SMBShares = new JSONArray(myPreferences.getString("SMBShares", "[]"));

			for (int i = 0; i < SMBShares.length(); i++)
			{
				JSONObject SMBShare = SMBShares.getJSONObject(i);

				Log.i("STORAGE", "Comparing '" + SMBShare.getString("Title") + "' and '" + SMBShareTitle + "'.");

				if (SMBShare.getString("Title").equals(SMBShareTitle))
				{
					return new NtlmPasswordAuthentication(SMBShare.getString("Domain"), SMBShare.getString("Username"), SMBShare.getString("Password"));
				}
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	// Function to return the SMB address for an SMB share based on its title
	String GetSMBAddress(String SMBShareTitle)
	{
		try
		{
			// Obtain the available SMB credentials
			JSONArray SMBShares = new JSONArray(myPreferences.getString("SMBShares", "[]"));

			for (int i = 0; i < SMBShares.length(); i++)
			{
				if (SMBShares.getJSONObject(i).getString("Title").equals(SMBShareTitle))
				{
					return SMBShares.getJSONObject(i).getString("Address");
				}
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
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