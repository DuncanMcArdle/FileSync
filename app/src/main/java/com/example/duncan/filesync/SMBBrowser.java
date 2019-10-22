package com.example.duncan.filesync;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

public class SMBBrowser extends AppCompatActivity
{

	CustomAdapter fileFolderListAdapter;
	List<List<String>> fileFolderList = new ArrayList<List<String>>();
	String SMBURL;
	String currentPath = "";
	String currentFolder;
	NtlmPasswordAuthentication SMBShareAuthentication;
	Button upButton;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_smbbrowser);

		// Initialise the toolbar
		Toolbar activityToolbar = findViewById(R.id.SMBBrowserToolbar);
		setSupportActionBar(activityToolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// Populate the ListView with the retrieved synchronisations
		ListView listView = (ListView) findViewById(R.id.SMBBrowserListView);
		fileFolderListAdapter = new CustomAdapter();
		listView.setAdapter(fileFolderListAdapter);

		Button useSMBFolder = findViewById(R.id.selectSMBFolder);
		useSMBFolder.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Log.i("STORAGE", "User selected '"+currentPath+"'");

				// Prepare the new job's details to be returned
				Intent activityResult = new Intent();
				activityResult.putExtra("PATH", currentPath);
				setResult(RESULT_OK, activityResult);

				// Finish the activity, returning to the calling one
				finish();
				overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
			}
		});

		upButton = findViewById(R.id.SMBBrowserUpFolderButton);
		upButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Navigate one folder up
				DisplayFolder(SMBShareAuthentication, currentPath.substring(0, (currentPath.lastIndexOf("/"))));
			}
		});

		try
		{
			// Obtain the SMB share and path details
			Intent SMBBrowserIntent = getIntent();
			JSONObject SMBJSONObject = new JSONObject(SMBBrowserIntent.getStringExtra("SMB_JSON"));
			currentPath = SMBBrowserIntent.getStringExtra("PATH");
			SMBShareAuthentication = new NtlmPasswordAuthentication(SMBJSONObject.getString("Domain"), SMBJSONObject.getString("Username"), SMBJSONObject.getString("Password"));
			SMBURL = "smb://"+SMBJSONObject.getString("Address");
			Log.i("STORAGE", "Set SMB URL to "+SMBURL);

			Log.i("STORAGE", "Current path set to '"+currentPath+"'");

			// Display the folder
			DisplayFolder(SMBShareAuthentication, currentPath);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			finish();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			// Back button
			case android.R.id.home:
			{
				finish();
				break;
			}
		}
		return true;
	}

	public void onBackPressed()
	{
		// Check if the user is currently in the root folder
		if(currentPath.length() <= 0)
		{
			// If so, allow the back button to propagate (as they want to leave the picking activity)
			super.onBackPressed();
		}
		else
		{
			// Navigate one folder up
			DisplayFolder(SMBShareAuthentication, currentPath.substring(0, (currentPath.lastIndexOf("/"))));
		}
	}

	private void DisplayFolder(NtlmPasswordAuthentication authentication, String targetPath)
	{
		Log.i("STORAGE", "Loading "+SMBURL+targetPath);

		try
		{
			new FetchSMBFolder(targetPath).execute().get();
			fileFolderListAdapter.notifyDataSetChanged();

			// Update the toolbar
			Toolbar pageToolbar = findViewById(R.id.SMBBrowserToolbar);
			pageToolbar.setTitle(currentFolder);

			// Update the "Current path"
			TextView currentPathTextView = (TextView) findViewById(R.id.SMBBrowserCurrentPath);
			currentPathTextView.setText(SMBURL+currentPath);

			// Check if the user is at the top path
			if(currentPath.length() <= 0)
			{
				// If so, disable the up button
				upButton.setAlpha((float) 0.25);
				upButton.setClickable(false);
			}
			else
			{
				// Otherwise, re-enable the up button
				upButton.setAlpha((float) 1);
				upButton.setClickable(true);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	class CustomAdapter extends BaseAdapter
	{
		@Override
		public int getCount()
		{
			return fileFolderList.size();
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
			convertView = getLayoutInflater().inflate(R.layout.smb_folder_listitem, null);

			TextView jobTitle = (TextView) convertView.findViewById(R.id.smbFileFolderTitle);
			ImageView jobIcon = (ImageView) convertView.findViewById(R.id.smbFileFolderIcon);
			try
			{
				// Set the file / folder's title
				jobTitle.setText(fileFolderList.get(position).get(1));

				// Check if the item is a folder
				if(fileFolderList.get(position).get(0).equals("folder"))
				{
					jobIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_folder_black_24dp));

					convertView.setOnClickListener(new View.OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							Log.i("STORAGE", "Selected folder: "+ fileFolderList.get(position).get(1));
							DisplayFolder(SMBShareAuthentication, currentPath+"/"+ fileFolderList.get(position).get(1));
						}
					});
				}
				else
				{
					jobIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_insert_drive_file_black_24dp));
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			return convertView;
		}
	}

	private class FetchSMBFolder extends AsyncTask
	{
		String targetPath;

		FetchSMBFolder(String passedTargetPath)
		{
			targetPath = passedTargetPath;
		}

		@Override
		protected List doInBackground(Object[] objects)
		{
			try
			{
				SmbFile testFile = new SmbFile(SMBURL+targetPath+"/", SMBShareAuthentication);
				testFile.setConnectTimeout(5000);
				testFile.connect();
				Log.i("STORAGE", "Connected successfully.");

				// Initialise lists for the files and folders
				List<List<String>> newFileFolderList = new ArrayList<List<String>>();

				SmbFile[] smbFile = testFile.listFiles();
				for(int i = 0; i < smbFile.length; i++)
				{
					// Initialise a list entry
					ArrayList<String> row = new ArrayList<>();

					// Check if the item is a folder
					if(smbFile[i].isDirectory())
					{
						// If so, add it as such
						row.add("folder");
						row.add(smbFile[i].getName().substring(0, smbFile[i].getName().length() - 1));
					}
					else
					{
						// Otherwise, add it as a file
						row.add("file");
						row.add(smbFile[i].getName());
					}

					newFileFolderList.add(row);
				}

				Collections.sort(newFileFolderList, new FileFolderComparator());

				fileFolderList = newFileFolderList;
				currentPath = targetPath;


				currentFolder = testFile.getName().substring(0, (testFile.getName().length() - 1));
				Log.i("STORAGE", "Folder name: "+currentFolder);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			return null;
		}
	}

	public class FileFolderComparator implements Comparator<List<String>>
	{
		public int compare(List<String> left, List<String> right)
		{
			if(left.get(0) != right.get(0))
			{
				return right.get(0).compareTo(left.get(0));
			}
			return left.get(1).compareTo(right.get(1));
		}
	}
}