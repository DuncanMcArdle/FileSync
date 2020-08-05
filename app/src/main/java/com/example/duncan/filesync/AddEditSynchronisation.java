package com.example.duncan.filesync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class AddEditSynchronisation extends AppCompatActivity
{
	// Request codes
	private final int REQUEST_CODE_SELECT_SOURCE_FOLDER_LOCAL = 100;
	private final int REQUEST_CODE_SELECT_TARGET_FOLDER_LOCAL = 101;
	private final int REQUEST_CODE_SELECT_SOURCE_FOLDER_SMB = 110;
	private final int REQUEST_CODE_SELECT_TARGET_FOLDER_SMB = 111;

	// Miscellaneous variables
	private Uri sourceFolder = null;
	private Uri targetFolder;
	private int modifyingSynchronisation = -1;
	private JSONArray SMBShareArray;
	private List<String> SMBShareList = new ArrayList<>();
	private SharedPreferences myPreferences;

	// Form inputs
	private Button deleteSynchronisationButton;
	private EditText jobTitleEditText;
	private TextInputLayout jobTitleContainer;
	private RadioGroup sourceTypeRadioGroup;
	private LinearLayout sourceTypeRadioGroupErrorContainer;
	private Spinner sourceSMBSpinner;
	private LinearLayout sourceSMBErrorContainer;
	private EditText sourceFolderEditText;
	private TextInputLayout sourceFolderEditTextContainer;
	private Button sourceBrowseButton;
	private RadioGroup targetTypeRadioGroup;
	private Spinner targetSMBSpinner;
	private LinearLayout targetSMBErrorContainer;
	private LinearLayout targetTypeRadioGroupErrorContainer;
	private EditText targetFolderEditText;
	private TextInputLayout targetFolderEditTextContainer;
	private Button targetBrowseButton;
	private Spinner deletionPolicySpinner;
	private Button addSynchronisationButton;

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == android.R.id.home)
		{
			// Finish the activity, returning to the calling one
			finish();
			overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
		}
		return true;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_addedit_synchronisation);

		// Initialise shared preferences
		myPreferences = getSharedPreferences("FileSync", 0);

		// Form inputs
		deleteSynchronisationButton = findViewById(R.id.deleteSynchronisation);
		jobTitleEditText = findViewById(R.id.jobTitleEditText);
		jobTitleContainer = findViewById(R.id.jobTitleEditTextLayout);

		// Source inputs
		sourceTypeRadioGroup = findViewById(R.id.sourceTypeRadioGroup);
		sourceTypeRadioGroupErrorContainer = findViewById(R.id.sourceTypeRadioGroupErrorContainer);
		sourceSMBSpinner = findViewById(R.id.sourceSMBSpinner);
		sourceSMBErrorContainer = findViewById(R.id.sourceSMBErrorContainer);
		sourceFolderEditText = findViewById(R.id.sourceEditText);
		sourceFolderEditTextContainer = findViewById(R.id.sourceEditTextLayout);
		sourceBrowseButton = findViewById(R.id.sourceBrowse);

		// Target inputs
		targetTypeRadioGroup = findViewById(R.id.targetTypeRadioGroup);
		targetTypeRadioGroupErrorContainer = findViewById(R.id.targetTypeRadioGroupErrorContainer);
		targetSMBSpinner = findViewById(R.id.targetSMBSpinner);
		targetSMBErrorContainer = findViewById(R.id.targetSMBErrorContainer);
		targetFolderEditText = findViewById(R.id.targetEditText);
		targetFolderEditTextContainer = findViewById(R.id.targetEditTextLayout);
		targetBrowseButton = findViewById(R.id.targetBrowse);

		// Additional options
		deletionPolicySpinner = findViewById(R.id.additionalOptionDeletionPolicy);

		// Submit button
		addSynchronisationButton = findViewById(R.id.addSynchronisationSubmit);

		// Add validation to the job title
		jobTitleEditText.addTextChangedListener(new TextValidator(jobTitleEditText)
		{
			@Override
			public void Validate(TextView textView, String text)
			{
				ValidateTitle();
			}
		});

		// Populate the SMB list
		SMBShareList.add("Please select...");
		try
		{
			// Obtain the existing SMB share list
			SMBShareArray = new JSONArray(myPreferences.getString("SMBShares", "[]"));

			// Loop through existing SMB shares
			for(int i = 0; i < SMBShareArray.length(); i++)
			{
				SMBShareList.add(SMBShareArray.getJSONObject(i).getString("Title"));
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		// Update the SMB drop-downs
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, SMBShareList);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sourceSMBSpinner.setAdapter(adapter);
		targetSMBSpinner.setAdapter(adapter);
		sourceSMBSpinner.setSelection(0,false);
		targetSMBSpinner.setSelection(0,false);

		// Initialise the toolbar
		Toolbar myToolbar = findViewById(R.id.my_toolbar);
		setSupportActionBar(myToolbar);
		if(getSupportActionBar() != null)
		{
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		// When the source type is changed
		sourceTypeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId)
			{
				// Reset the "browse" area
				sourceFolderEditText.setText("");
				sourceFolderEditText.setHint("Please browse to a source.");
				sourceFolderEditTextContainer.setError("");
				sourceFolderEditTextContainer.setErrorEnabled(false);

				// If the source type is local
				if(checkedId == R.id.sourceTypeLocal)
				{
					// Hide the SMB selection area
					LinearLayout sourceSMBShareSection = findViewById(R.id.sourceSMBSection);
					sourceSMBShareSection.setVisibility(View.GONE);

					// Enable the browse button
					sourceBrowseButton.setEnabled(true);
				}
				// If the source type is an SMB share
				else if(checkedId == R.id.sourceTypeNetwork)
				{
					// Show the SMB selection area
					LinearLayout sourceSMBShareSection = findViewById(R.id.sourceSMBSection);
					sourceSMBSpinner.setSelection(0);
					sourceSMBShareSection.setVisibility(View.VISIBLE);

					// Disable the browse button (SMB must be selected first)
					sourceBrowseButton.setEnabled(false);
				}

				// Show the source folder selection area
				LinearLayout sourceFolderSection = findViewById(R.id.sourceFolderSection);
				sourceFolderSection.setVisibility(View.VISIBLE);

				// Remove any displayed errors
				sourceTypeRadioGroupErrorContainer.setVisibility(View.GONE);
			}
		});

		// When the target type is changed
		targetTypeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId)
			{
				// If the target type is local
				if(checkedId == R.id.targetTypeLocal)
				{
					// Reset the "browse" area
					EditText targetEditText = findViewById(R.id.targetEditText);
					targetEditText.setText("");
					targetEditText.setHint("Please browse to a source.");
					findViewById(R.id.targetBrowse).setEnabled(true);

					// Hide the SMB selection area
					LinearLayout targetSMBShareSection = findViewById(R.id.targetSMBSection);
					targetSMBShareSection.setVisibility(View.GONE);
				}
				// If the source type is an SMB share
				else if(checkedId == R.id.targetTypeNetwork)
				{
					// Show the SMB selection area
					LinearLayout targetSMBShareSection = findViewById(R.id.targetSMBSection);
					targetSMBShareSection.setVisibility(View.VISIBLE);
				}

				// Show the target folder selection area
				LinearLayout targetFolderSection = findViewById(R.id.targetFolderSection);
				targetFolderSection.setVisibility(View.VISIBLE);

				// Remove any displayed errors
				targetTypeRadioGroupErrorContainer.setVisibility(View.GONE);
			}
		});

		// Check if a synchronisation is being edited
		Intent addSynchronisationIntent = getIntent();
		if(addSynchronisationIntent.hasExtra("ID"))
		{
			// Set the source type radio button (needs to be done first)
			if(addSynchronisationIntent.getStringExtra("SourceType").equals("LOCAL"))
			{
				sourceTypeRadioGroup.check(R.id.sourceTypeLocal);
			}
			else
			{
				sourceTypeRadioGroup.check(R.id.sourceTypeNetwork);
			}

			// Set the target type radio button (needs to be done first)
			if(addSynchronisationIntent.getStringExtra("TargetType").equals("LOCAL"))
			{
				targetTypeRadioGroup.check(R.id.targetTypeLocal);
			}
			else
			{
				targetTypeRadioGroup.check(R.id.targetTypeNetwork);
			}

			// Check if a local source is being loaded
			if(sourceTypeRadioGroup.getCheckedRadioButtonId() == R.id.sourceTypeLocal)
			{
				// Local
				sourceFolder = Uri.parse(addSynchronisationIntent.getStringExtra("SourceFolder"));
				sourceFolderEditText.setText(Uri.parse(addSynchronisationIntent.getStringExtra("SourceFolder")).getLastPathSegment());
			}
			else
			{
				// SMB
				sourceFolderEditText.setText(addSynchronisationIntent.getStringExtra("SourceFolder"));
			}

			// Check if a local target is being loaded
			if(targetTypeRadioGroup.getCheckedRadioButtonId() == R.id.targetTypeLocal)
			{
				// Local
				targetFolder = Uri.parse(addSynchronisationIntent.getStringExtra("TargetFolder"));
				targetFolderEditText.setText(Uri.parse(addSynchronisationIntent.getStringExtra("TargetFolder")).getLastPathSegment());
			}
			else
			{
				// SMB
				targetFolderEditText.setText(addSynchronisationIntent.getStringExtra("TargetFolder"));
			}

			// Populate the additional options
			deletionPolicySpinner.setSelection(addSynchronisationIntent.getIntExtra("DeletionPolicy", 0));

			// Populate the required variables
			modifyingSynchronisation = addSynchronisationIntent.getIntExtra("ID", -1);

			// Update the form
			this.setTitle(R.string.add_edit_synchronisation_title_edit);
			addSynchronisationButton.setText(R.string.add_edit_synchronisation_submit_button_edit);

			// Enable the delete button
			deleteSynchronisationButton.setVisibility(View.VISIBLE);

			// Set the text inputs
			jobTitleEditText.setText(addSynchronisationIntent.getStringExtra("Title"));

			// Check if the source is SMB based
			if(sourceTypeRadioGroup.getCheckedRadioButtonId() == R.id.sourceTypeNetwork)
			{
				Log.i("STORAGE", "Source SMB Share: '"+addSynchronisationIntent.getStringExtra("SourceSMBShare")+"'");
				// If so, select the relevant SMB share
				SelectSMBShare(sourceSMBSpinner, addSynchronisationIntent.getStringExtra("SourceSMBShare"));
			}
			else
			{
				Log.i("STORAGE", "Network option is not selected");
			}

			// Check if the target is SMB based
			if(targetTypeRadioGroup.getCheckedRadioButtonId() == R.id.targetTypeNetwork)
			{
				// If so, select the relevant SMB share
				SelectSMBShare(targetSMBSpinner, addSynchronisationIntent.getStringExtra("TargetSMBShare"));
			}
		}

		// When the user clicks the "DELETE" button
		deleteSynchronisationButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Prepare the new job's details to be returned
				Intent activityResult = new Intent();
				activityResult.putExtra("ID", modifyingSynchronisation);
				activityResult.putExtra("Delete", true);
				setResult(RESULT_OK, activityResult);

				// Notify the user
				final Loader loader = new Loader(AddEditSynchronisation.this, getLayoutInflater());
				loader.ShowLoaderWithIcon("Deleted successfully.", R.drawable.ic_done_black_24dp, null);
				loader.UpdateButtons(false, null, null, false, null, null);

				// Execute with a 1 second delay
				final Handler handler = new Handler();
				handler.postDelayed(new Runnable()
				{
					@Override
					public void run()
					{
						// Finish the activity, returning to the "Manage SMB Shares" page
						loader.HideLoader();
						finish();
						overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
					}
				}, 1000);
			}
		});

		// When the source SMB share drop-down is changed
		sourceSMBSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				ValidateSMBSelection(sourceSMBSpinner, sourceSMBErrorContainer);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent)
			{

			}
		});

		// When the target SMB share drop-down is changed
		targetSMBSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
			{
				ValidateSMBSelection(targetSMBSpinner, targetSMBErrorContainer);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent)
			{

			}
		});

		// When the "Browse" button on the source input is clicked
		sourceBrowseButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				Log.i("STORAGE", "Source type local: "+findViewById(R.id.sourceTypeLocal).isSelected());
				Log.i("STORAGE", "Source type SMB: "+findViewById(R.id.sourceTypeNetwork).isSelected());

				// Check if it's a "local" browse
				if(sourceTypeRadioGroup.getCheckedRadioButtonId() == R.id.sourceTypeLocal)
				{
					// If so, open a local browser
					Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
					intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
					intent.putExtra("android.content.extra.SHOW_ADVANCED",true);
					startActivityForResult(intent, REQUEST_CODE_SELECT_SOURCE_FOLDER_LOCAL);
				}
				else
				{
					try
					{
						Log.i("STORAGE", "Sending '"+sourceFolderEditText.getText()+"'");

						// Open an SMB browser
						Intent intent = new Intent(AddEditSynchronisation.this, SMBBrowser.class);
						intent.putExtra("SMB_JSON", SMBShareArray.getJSONObject(sourceSMBSpinner.getSelectedItemPosition() - 1).toString());
						intent.putExtra("PATH", sourceFolderEditText.getText().toString());
						startActivityForResult(intent, REQUEST_CODE_SELECT_SOURCE_FOLDER_SMB);
						overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		});

		// When the "Browse" button on the target input is clicked
		targetBrowseButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Check if it's a "local" browse
				if(targetTypeRadioGroup.getCheckedRadioButtonId() == R.id.targetTypeLocal)
				{
					// If so, open a local browser
					Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
					intent.putExtra("android.content.extra.SHOW_ADVANCED",true);
					startActivityForResult(intent, REQUEST_CODE_SELECT_TARGET_FOLDER_LOCAL);
				}
				else
				{
					try
					{
						// Open an SMB browser
						Intent intent = new Intent(AddEditSynchronisation.this, SMBBrowser.class);
						intent.putExtra("SMB_JSON", SMBShareArray.getJSONObject(targetSMBSpinner.getSelectedItemPosition() - 1).toString());
						intent.putExtra("PATH", targetFolderEditText.getText().toString());
						startActivityForResult(intent, REQUEST_CODE_SELECT_TARGET_FOLDER_SMB);
						overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		});

		// When the "Add synchronisation" button is clicked
		addSynchronisationButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Validate the job title
				ValidateTitle();

				// Validate the source type
				ValidateRadioGroup(sourceTypeRadioGroup, sourceTypeRadioGroupErrorContainer);

				// Validate the source SMB share
				ValidateSMBSelection(sourceSMBSpinner, sourceSMBErrorContainer);

				// Check if a source folder was selected
				if(sourceTypeRadioGroup.getCheckedRadioButtonId() == R.id.sourceTypeLocal && sourceFolder == null)
				{
					// If not, show an error
					sourceFolderEditTextContainer.setError("Please select a source folder.");
				}

				// Validate the target type
				ValidateRadioGroup(targetTypeRadioGroup, targetTypeRadioGroupErrorContainer);

				// Validate the target SMB share
				ValidateSMBSelection(targetSMBSpinner, targetSMBErrorContainer);

				// Check if a target folder was selected
				if(targetTypeRadioGroup.getCheckedRadioButtonId() == R.id.targetTypeLocal && targetFolder == null)
				{
					// If not, show an error
					targetFolderEditTextContainer.setError("Please select a target folder.");
				}

				// Check if any errors are on display
				if(		jobTitleContainer.isErrorEnabled() ||
						sourceTypeRadioGroupErrorContainer.getVisibility() == View.VISIBLE ||
						sourceSMBErrorContainer.getVisibility() == View.VISIBLE ||
						sourceFolderEditTextContainer.isErrorEnabled() ||
						targetTypeRadioGroupErrorContainer.getVisibility() == View.VISIBLE ||
						targetSMBErrorContainer.getVisibility() == View.VISIBLE ||
						targetFolderEditTextContainer.isErrorEnabled())
				{
					// If so, notify the user
					Log.i("STORAGE", "ERROR ON DISPLAY");
				}
				else
				{
					// Prepare the new job's details to be returned
					Intent activityResult = new Intent();
					activityResult.putExtra("ID", modifyingSynchronisation);
					activityResult.putExtra("Title", jobTitleEditText.getText().toString());
					activityResult.putExtra("SourceType", (sourceTypeRadioGroup.getCheckedRadioButtonId() == R.id.sourceTypeLocal ? "LOCAL" : "SMB"));
					activityResult.putExtra("SourceSMBShare", sourceSMBSpinner.getSelectedItem().toString());
					activityResult.putExtra("SourceFolder", (sourceTypeRadioGroup.getCheckedRadioButtonId() != R.id.sourceTypeNetwork ? sourceFolder.toString() : sourceFolderEditText.getText().toString()));
					activityResult.putExtra("TargetType", (targetTypeRadioGroup.getCheckedRadioButtonId() == R.id.targetTypeLocal ? "LOCAL" : "SMB"));
					activityResult.putExtra("TargetSMBShare", targetSMBSpinner.getSelectedItem().toString());
					activityResult.putExtra("TargetFolder", (targetTypeRadioGroup.getCheckedRadioButtonId() != R.id.targetTypeNetwork ? targetFolder.toString() : targetFolderEditText.getText().toString()));
					activityResult.putExtra("DeletionPolicy", deletionPolicySpinner.getSelectedItemPosition());
					setResult(RESULT_OK, activityResult);

					// Notify the user
					final Loader loader = new Loader(AddEditSynchronisation.this, getLayoutInflater());
					loader.ShowLoaderWithIcon(modifyingSynchronisation == -1 ? "Added successfully." : "Updated successfully.", R.drawable.ic_done_black_24dp, null);
					loader.UpdateButtons(false, null, null, false, null, null);

					// Execute with a 1 second delay
					final Handler handler = new Handler();
					handler.postDelayed(new Runnable()
					{
						@Override
						public void run()
						{
							// Finish the activity, returning to the "Manage SMB Shares" page
							loader.HideLoader();
							finish();
							overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
						}
					}, 1000);
				}
			}
		});

		// When the "info" button on the deletion policy is clicked
		final TextView deletionPolicyInfoButton = findViewById(R.id.additionalOptionDeletionPolicyInfoButton);
		deletionPolicyInfoButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				// Show a modal
				Loader deletionPolicyInfoLoader = new Loader(AddEditSynchronisation.this, getLayoutInflater());
				deletionPolicyInfoLoader.UpdateTitle("Deletion policy");
				deletionPolicyInfoLoader.ShowLoaderWithHTMLSummary(Html.fromHtml("<p><b>Don't delete anything (add / update only)</b><br />Files and folders will be synchronised to the target folder. No other files or folders in the target folder will be deleted.</p><br /><p><b>Perfect copy (delete extra files & folders)</b><br />After synchronising all files and folders from the source to the destination, any other files and folders found only in the destination folder will be removed.</p>"));
				deletionPolicyInfoLoader.UpdateButtons(false, null, null,true, "Close", null);
			}
		});
	}

	// Function to set the SMB share drop-downs value(s)
	private void SelectSMBShare(Spinner targetSpinner, String shareTitle)
	{
		for(int i = 0; i < SMBShareList.size(); i++)
		{
			Log.i("STORAGE", "Comparing '"+shareTitle+"' with '"+SMBShareList.get(i)+"'");
			if(shareTitle.equals(SMBShareList.get(i)))
			{
				Log.i("STORAGE", "Found, setting '"+targetSpinner.toString()+"' to '"+i+"'");
				targetSpinner.setSelection(i);
				break;
			}
		}
	}

	// When a user returns from an activity opened by this page
	public void onActivityResult(int requestCode, int resultCode, Intent resultData)
	{
		if (resultCode == RESULT_OK)
		{
			// When the user picks a source folder for the synchronisation
			if (requestCode == REQUEST_CODE_SELECT_SOURCE_FOLDER_LOCAL)
			{
				// Store the selected folder
				sourceFolder = resultData.getData();

				// Obtain persistent permissions on the selected URI
				final int takeFlags = resultData.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
				getContentResolver().takePersistableUriPermission(sourceFolder, takeFlags);

				// Update the relevant EditText
				sourceFolderEditText.setText(sourceFolder.getLastPathSegment());

				// Remove any displayed errors
				sourceFolderEditTextContainer.setError("");
				sourceFolderEditTextContainer.setErrorEnabled(false);
			}

			// When the user picks a target folder for the synchronisation
			if (requestCode == REQUEST_CODE_SELECT_TARGET_FOLDER_LOCAL)
			{
				// Store the selected folder
				targetFolder = resultData.getData();

				// Obtain persistent permissions on the selected URI
				final int takeFlags = resultData.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
				getContentResolver().takePersistableUriPermission(targetFolder, takeFlags);

				// Update the relevant EditText
				targetFolderEditText.setText(targetFolder.getLastPathSegment());

				// Remove any displayed errors
				targetFolderEditTextContainer.setError("");
				targetFolderEditTextContainer.setErrorEnabled(false);
			}

			// When the user returns from selecting a source folder in an SMB share
			if (requestCode == REQUEST_CODE_SELECT_SOURCE_FOLDER_SMB)
			{
				// Populate the source SMB share input
				sourceFolderEditText.setText(resultData.getStringExtra("PATH"));
			}

			// When the user returns from selecting a target folder in an SMB share
			if (requestCode == REQUEST_CODE_SELECT_TARGET_FOLDER_SMB)
			{
				// Populate the target SMB share input
				targetFolderEditText.setText(resultData.getStringExtra("PATH"));
			}
		}
	}

	private void ValidateTitle()
	{
		// Validate the input
		if(jobTitleEditText.getText().toString().length() <= 0 || jobTitleEditText.getText().toString().equals(""))
		{
			jobTitleContainer.setError("Please enter a title.");
		}
		else
		{
			boolean foundDuplicateTitle = false;

			try
			{
				// Obtain the existing synchronisation list
				JSONArray synchronisationArray = new JSONArray(myPreferences.getString("Synchronisations", "[]"));

				// Loop through existing synchronisations
				for(int i = 0; i < synchronisationArray.length(); i++)
				{
					// Check if another synchronisation has the same title
					if(modifyingSynchronisation != i && synchronisationArray.getJSONObject(i).getString("Title").equals(jobTitleEditText.getText().toString()))
					{
						jobTitleContainer.setError("Title already in use.");
						foundDuplicateTitle = true;
						break;
					}
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}

			if(!foundDuplicateTitle)
			{
				jobTitleContainer.setError("");
				jobTitleContainer.setErrorEnabled(false);
			}
		}
	}

	private void ValidateRadioGroup(RadioGroup radioGroup, LinearLayout radioGroupErrorContainer)
	{
		// Check if an option was selected
		if(radioGroup.getCheckedRadioButtonId() == -1)
		{
			// If not, show an error
			radioGroupErrorContainer.setVisibility(View.VISIBLE);
		}
		else
		{
			// Otherwise, hide any errors
			radioGroupErrorContainer.setVisibility(View.GONE);
		}
	}

	private void ValidateSMBSelection(Spinner SMBSpinner, LinearLayout SMBSpinnerErrorContainer)
	{
		// Check if the SMB share selection required validation
		if((SMBSpinner == sourceSMBSpinner && sourceTypeRadioGroup.getCheckedRadioButtonId() != R.id.sourceTypeNetwork) || (SMBSpinner == targetSMBSpinner && targetTypeRadioGroup.getCheckedRadioButtonId() != R.id.targetTypeNetwork))
		{
			// If not, hide any errors
			SMBSpinnerErrorContainer.setVisibility(View.GONE);
		}
		else
		{
			// Check if an option was selected
			if (SMBSpinner.getSelectedItemPosition() == AdapterView.INVALID_POSITION || SMBSpinner.getSelectedItem().equals("Please select..."))
			{
				// If not, show an error
				SMBSpinnerErrorContainer.setVisibility(View.VISIBLE);

				// Disable the browse button
				Button targetBrowseButton = (SMBSpinner == sourceSMBSpinner ? (Button) findViewById(R.id.sourceBrowse) : (Button) findViewById(R.id.targetBrowse));
				targetBrowseButton.setEnabled(false);
			}
			else
			{
				// Otherwise, hide any errors and enable the corresponding browse button
				SMBSpinnerErrorContainer.setVisibility(View.GONE);

				// Enable the browse button
				Button targetBrowseButton = (SMBSpinner == sourceSMBSpinner ? (Button) findViewById(R.id.sourceBrowse) : (Button) findViewById(R.id.targetBrowse));
				targetBrowseButton.setEnabled(true);
			}
		}
	}

}
