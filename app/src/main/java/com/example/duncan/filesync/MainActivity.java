package com.example.duncan.filesync;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

import jcifs.smb.NtlmPasswordAuthentication;

public class MainActivity extends AppCompatActivity implements SynchronisationASyncTask.ContextualInterface
{
	private SharedPreferences myPreferences;
	private SharedPreferences.Editor myPrefsEdit;
	private CustomAdapter synchronisationListAdapter;
	private SynchronisationASyncTask synchronisationTask;
	private long synchronisationStart;
	private int lastFilesProcessed;
	private int lastPercentCompleted;

	// Variables for storing the current synchronisation's details
	private NtlmPasswordAuthentication sourceAuthentication;
	private NtlmPasswordAuthentication targetAuthentication;
	private String sourceFolder;
	private String targetFolder;
	private int deletionPolicy;
	private int filesToSynchronise;
	private long dataToSynchronise;

	// Request codes
	private final int REQUEST_CODE_ADD_SYNCHRONISATION = 1000;
	private final int REQUEST_CODE_EDIT_SYNCHRONISATION = 1001;

	// Array of synchronisation jobs
	private JSONArray synchronisationArray = null;

	// Loader
	private Loader syncLoader;

	@SuppressLint("CommitPrefEdits")
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
		ListView listView = findViewById(R.id.customListView);
		synchronisationListAdapter = new CustomAdapter();
		listView.setAdapter(synchronisationListAdapter);

		// Request the required permissions
		ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 255);
		ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 256);

		// When the user clicks the "ADD SYNCHRONISATION" button
		Button addSynchronisationButton = findViewById(R.id.addSynchronisation);
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
				// When the user selects the "MANAGE SMB SHARES" option
				if (item.getItemId() == R.id.main_activity_menu_manage_smb_shares)
				{
					// Create and start an intent to open the Manage SMB Credentials activity
					Intent manageSMBCredentialsIntent = new Intent(MainActivity.this, ManageSMBShares.class);
					startActivity(manageSMBCredentialsIntent);
					overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
					return true;
				}
				return false;
			}
		});
		return super.onCreateOptionsMenu(menu);
	}

	// When a user returns from an activity opened by this page
	public void onActivityResult(int requestCode, int resultCode, Intent resultData)
	{
		if (resultCode == RESULT_OK)
		{
			// When the user returns from creating a new synchronisation job
			if (requestCode == REQUEST_CODE_ADD_SYNCHRONISATION)
			{
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
					newSynchronisationJob.put("DeletionPolicy", resultData.getIntExtra("DeletionPolicy",0));
					synchronisationArray.put(newSynchronisationJob);

					// Sort the synchronisation array
					synchronisationArray = SortJSONArray(synchronisationArray, "Title");

					// Update shared preferences
					myPrefsEdit.putString("Synchronisations", synchronisationArray.toString());
					myPrefsEdit.commit();

					// Update the ListView
					synchronisationListAdapter.notifyDataSetChanged();
					LoadSynchronisations();
				}
				catch (JSONException e)
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
						newSynchronisationJob.put("DeletionPolicy", resultData.getIntExtra("DeletionPolicy", 0));

						// Remove the existing job
						synchronisationArray.remove(resultData.getIntExtra("ID", -1));

						// Add the new version of the job in
						synchronisationArray.put(newSynchronisationJob);

						// Sort the synchronisation array
						synchronisationArray = SortJSONArray(synchronisationArray, "Title");

						// Update shared preferences
						myPrefsEdit.putString("Synchronisations", synchronisationArray.toString());
						myPrefsEdit.commit();
					}
					catch (Exception e)
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
	private void LoadSynchronisations()
	{
		// Obtain a reference to the ListView's loading text
		TextView listViewLoader = findViewById(R.id.listViewLoader);

		try
		{
			// Load an array of existing synchronisations
			synchronisationArray = new JSONArray(myPreferences.getString("Synchronisations", "[]"));

			// Check if any synchronisations were retrieved
			if (synchronisationArray.length() <= 0)
			{
				// If not, append a notice to the list of synchronisations
				listViewLoader.setText(R.string.main_list_no_syncs);
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
			listViewLoader.setText(R.string.main_list_error);
			e.printStackTrace();
		}
	}

	// Function called when a synchronisation completes
	@Override
	public void OnSynchronisationProgress(final int filesProcessed, final int bytesProcessed, final String currentFileName)
	{
		// Calculate the percentage complete
		final int percentageComplete = (int) (((double) bytesProcessed / (double) dataToSynchronise) * 100);

		// Check if any visual changes have occurred
		if(filesProcessed > lastFilesProcessed || percentageComplete > lastPercentCompleted)
		{
			runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					// Update the loader
					syncLoader.ShowLoaderWithProgressBar(percentageComplete, filesProcessed, filesToSynchronise, currentFileName);
				}
			});
		}

		// Store the latest progress variables
		lastFilesProcessed = filesProcessed;
		lastPercentCompleted = percentageComplete;
	}

	// Function called when a synchronisation fails to complete
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
					case "SOURCE_ACCESS":
					{
						syncLoader.ShowLoaderWithIcon("Source folder access", R.drawable.ic_highlight_off_black_24dp, "Could not access the source folder, try editing the synchronisation and re-browsing to the source folder.");
						break;
					}
					case "TARGET_ACCESS":
					{
						syncLoader.ShowLoaderWithIcon("Target folder write access", R.drawable.ic_highlight_off_black_24dp, "Could not write to the target folder, try editing the synchronisation and re-browsing to the target folder. In addition, ensure you specifically have write access to the folder.");
						break;
					}
					case "MANUALLY_CANCELLED":
					{
						syncLoader.ShowLoaderWithIcon("Cancelled synchronisation", R.drawable.ic_highlight_off_black_24dp, "The synchronisation was manually cancelled. Files may have been left in a part-synchronised state.");
						break;
					}
					case "Access is denied.":
					{
						syncLoader.ShowLoaderWithIcon("Permission denied", R.drawable.ic_highlight_off_black_24dp, "One of the source or target folder's sub-folders had a different security policy to its parent, which prevented the synchronisation from completing.");
						break;
					}
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
				syncLoader.UpdateButtons(false, null, null, true, "Close", null);
			}
		});

		// Cancel the task
		synchronisationTask.cancel(true);
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
			convertView = View.inflate(getApplicationContext(), R.layout.synchronisation_listitem, null);

			TextView jobTitle = convertView.findViewById(R.id.jobTitle);
			try
			{
				jobTitle.setText(synchronisationArray.getJSONObject(position).getString("Title"));
			} catch (Exception e)
			{
				e.printStackTrace();
			}

			// Assign an action to the "Run" button
			Button runButton = convertView.findViewById(R.id.runJobButton);
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
						syncLoader.UpdateTitle("Estimating changes...");
						syncLoader.ShowLoaderWithSpinner();
						syncLoader.UpdateButtons(false, null, null, true, "Cancel", new View.OnClickListener()
						{
							@Override
							public void onClick(View v)
							{
								// Stop the ongoing task
								synchronisationTask.cancel(true);

								// Close the loader
								//syncLoader.HideLoader();
							}
						});

						// Fetch the selected synchronisation details
						JSONObject targetSynchronisation = synchronisationArray.getJSONObject(position);

						// Authentication
						sourceAuthentication = (targetSynchronisation.getString("SourceType").equals("LOCAL") ? null : GetSMBCredentials(targetSynchronisation.getString("SourceSMBShare")));
						targetAuthentication = (targetSynchronisation.getString("TargetType").equals("LOCAL") ? null : GetSMBCredentials(targetSynchronisation.getString("TargetSMBShare")));

						// Folders
						sourceFolder = (targetSynchronisation.getString("SourceType").equals("LOCAL") ? targetSynchronisation.getString("SourceFolder") : GetSMBAddress(targetSynchronisation.getString("SourceSMBShare")) + targetSynchronisation.getString("SourceFolder"));
						targetFolder = (targetSynchronisation.getString("TargetType").equals("LOCAL") ? targetSynchronisation.getString("TargetFolder") : GetSMBAddress(targetSynchronisation.getString("TargetSMBShare")) + targetSynchronisation.getString("TargetFolder"));

						// Miscellaneous
						deletionPolicy = targetSynchronisation.getInt("DeletionPolicy");

						// Run a task to perform the synchronisation
						synchronisationTask = new SynchronisationASyncTask(MainActivity.this, sourceFolder, sourceAuthentication, targetFolder, targetAuthentication, deletionPolicy, false);
						synchronisationTask.callingActivityInterface = MainActivity.this;
						synchronisationTask.execute();
						synchronisationStart = System.currentTimeMillis() / 1000L;

					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			});

			// Assign an action to the "Edit" button
			Button editButton = convertView.findViewById(R.id.editJobButton);
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
						editSynchronisationIntent.putExtra("DeletionPolicy", synchronisationObject.getInt("DeletionPolicy"));

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
	private NtlmPasswordAuthentication GetSMBCredentials(String SMBShareTitle)
	{
		try
		{
			// Obtain the available SMB credentials
			JSONArray SMBShares = new JSONArray(myPreferences.getString("SMBShares", "[]"));

			// Loop through all SMB shares
			for (int i = 0; i < SMBShares.length(); i++)
			{
				// Check if the current share is the one being looked for
				JSONObject SMBShare = SMBShares.getJSONObject(i);
				if (SMBShare.getString("Title").equals(SMBShareTitle))
				{
					// Return the share's credentials
					return new NtlmPasswordAuthentication(SMBShare.getString("Domain"), SMBShare.getString("Username"), SMBShare.getString("Password"));
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	// Function to return the SMB address for an SMB share based on its title
	private String GetSMBAddress(String SMBShareTitle)
	{
		try
		{
			// Obtain the available SMB credentials
			JSONArray SMBShares = new JSONArray(myPreferences.getString("SMBShares", "[]"));

			// Loop through all SMB shares
			for (int i = 0; i < SMBShares.length(); i++)
			{
				// Check if the current share is the one being looked for
				if (SMBShares.getJSONObject(i).getString("Title").equals(SMBShareTitle))
				{
					// Return the share's address
					return SMBShares.getJSONObject(i).getString("Address");
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	// Function to sort an array of JSON objects
	private JSONArray SortJSONArray(JSONArray arrayToSort, final String attributeToSort) throws JSONException
	{
		// Convert the JSON Array to a list of JSON objects (so it can be sorted)
		List<JSONObject> jsonList = new ArrayList<>();
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

				}
				catch (JSONException e)
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

	// Function called when a synchronisation completes
	public void OnSynchronisationComplete(boolean performTransfer, long addedFileSize, ArrayList<AnalysedFile> addedFileList, long updatedFileSize, ArrayList<AnalysedFile> updatedFileList, long deletedFileSize, ArrayList<AnalysedFile> deletedFileList)
	{
		// Check if a transfer was being performed
		if(!performTransfer)
		{
			// Store the total number and size of files to be synchronised
			filesToSynchronise = (addedFileList.size() + updatedFileList.size());
			dataToSynchronise = (addedFileSize + updatedFileSize);

			// Update the title
			syncLoader.UpdateTitle("Estimated changes");

			// Create a listener for when the user clicks "Perform transfer"
			View.OnClickListener performTransferListener = new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					// Show a loader
					syncLoader.UpdateTitle("Synchronising...");
					syncLoader.ShowLoaderWithSpinner();
					syncLoader.UpdateButtons(false, null, null, true, "Cancel", new View.OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							// Stop the ongoing task
							synchronisationTask.cancel(true);

							// Close the loader
							//syncLoader.HideLoader();
						}
					});

					// Start a synchronisation task
					synchronisationTask = new SynchronisationASyncTask(MainActivity.this, sourceFolder, sourceAuthentication, targetFolder, targetAuthentication, deletionPolicy, true);
					synchronisationTask.callingActivityInterface = MainActivity.this;
					synchronisationTask.execute();
					synchronisationStart = System.currentTimeMillis() / 1000L;
				}
			};

			// Update the buttons
			syncLoader.UpdateButtons(true, "Perform transfer", performTransferListener, true, "Cancel", null);
		}
		else
		{
			// Update the title
			syncLoader.UpdateTitle("Synchronisation complete");

			// Update the buttons
			syncLoader.UpdateButtons(false, null, null, true, "Close", null);
		}

		// Show a summary
		syncLoader.ShowLoaderWithFileTransferSummary(performTransfer, (int) ((System.currentTimeMillis() / 1000L) - synchronisationStart), (addedFileList.size() + updatedFileList.size()), (addedFileSize + updatedFileSize), addedFileList, updatedFileList, deletedFileList);
	}
}