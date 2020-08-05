package com.example.duncan.filesync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import jcifs.smb.NtlmPasswordAuthentication;

public class AddEditSMBShare extends AppCompatActivity implements TestSMBShare.TestResponse
{
    // Miscellaneous variables
    private int modifyingSMBShare = -1;
	private Loader testSMBShareLoader;
	private TestSMBShare testSMBShare;
	private String originalTitle = "";
	private SharedPreferences myPreferences;

    // Form inputs
	private Button deleteSMBShareButton;
	private EditText titleEditText;
	private TextInputLayout titleEditTextContainer;
	private EditText domainEditText;
	private TextInputLayout domainEditTextContainer;
	private EditText usernameEditText;
	private TextInputLayout usernameEditTextContainer;
	private EditText passwordEditText;
	private TextInputLayout passwordEditTextContainer;
	private EditText addressEditText;
	private TextInputLayout addressEditTextContainer;
	private Button addSMBShareButton;
	private TextView smbShareResult;

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Clicking the back button at the top-left
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
        setContentView(R.layout.activity_addedit_smbshare);

		// Initialise shared preferences
		myPreferences = getSharedPreferences("FileSync", 0);

        // Form inputs
        deleteSMBShareButton = findViewById(R.id.deleteSMBShare);
        titleEditText = findViewById(R.id.smbShareTitleEditText);
        titleEditTextContainer = findViewById(R.id.smbShareTitleEditTextLayout);
        domainEditText = findViewById(R.id.smbShareDomainEditText);
        domainEditTextContainer = findViewById(R.id.smbShareDomainEditTextLayout);
        usernameEditText = findViewById(R.id.smbShareUsernameEditText);
        usernameEditTextContainer = findViewById(R.id.smbShareUsernameEditTextLayout);
		passwordEditText = findViewById(R.id.smbSharePasswordEditText);
		passwordEditTextContainer = findViewById(R.id.smbSharePasswordEditTextLayout);
		addressEditText = findViewById(R.id.smbShareAddressEditText);
		addressEditTextContainer = findViewById(R.id.smbShareAddressEditTextLayout);
        addSMBShareButton = findViewById(R.id.addSMBShareSubmit);
		smbShareResult = findViewById(R.id.smbShareResult);

		// Add validation to the SMB share title
		titleEditText.addTextChangedListener(new TextValidator(titleEditText)
		{
			@Override
			public void Validate(TextView textView, String text)
			{
				ValidateTitle();
			}
		});

		// Add validation to the username
		usernameEditText.addTextChangedListener(new TextValidator(usernameEditText)
		{
			@Override
			public void Validate(TextView textView, String text)
			{
				ValidateUsername();
			}
		});

		// Add validation to the password
		passwordEditText.addTextChangedListener(new TextValidator(passwordEditText)
		{
			@Override
			public void Validate(TextView textView, String text)
			{
				ValidatePassword();
			}
		});

		// Add validation to the SMB address
		addressEditText.addTextChangedListener(new TextValidator(addressEditText)
		{
			@Override
			public void Validate(TextView textView, String text)
			{
				ValidateSMBAddress();
			}
		});

        // Initialise the toolbar
		Toolbar activityToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(activityToolbar);
        if(getSupportActionBar() != null)
		{
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

        // Check if an existing SMB share is being edited
        Intent addSMBShareIntent = getIntent();
        if(addSMBShareIntent.hasExtra("ID"))
        {
			// Populate the required variables
			modifyingSMBShare = addSMBShareIntent.getIntExtra("ID", -1);

            // Update the form
            this.setTitle(R.string.add_edit_share_title_edit);
            addSMBShareButton.setText(R.string.add_edit_share_submit_button_edit);

            // Enable the delete button
            deleteSMBShareButton.setVisibility(View.VISIBLE);

            // Store the original title
			originalTitle = addSMBShareIntent.getStringExtra("Title");

            // Set the text inputs
            titleEditText.setText(addSMBShareIntent.getStringExtra("Title"));
            domainEditText.setText(addSMBShareIntent.getStringExtra("Domain"));
            usernameEditText.setText(addSMBShareIntent.getStringExtra("Username"));
			passwordEditText.setText(addSMBShareIntent.getStringExtra("Password"));
			addressEditText.setText(addSMBShareIntent.getStringExtra("Address"));
        }

        // When the user clicks the "DELETE" button
        deleteSMBShareButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
				try
				{
					// Initialise a loader
					final Loader loader = new Loader(AddEditSMBShare.this, getLayoutInflater());

					// Loop through an array of existing synchronisations
					JSONArray synchronisationArray = new JSONArray(myPreferences.getString("Synchronisations", "[]"));
					for (int i = 0; i < synchronisationArray.length(); i++)
					{
						// Check if the synchronisation references the SMB share being changed as its source
						if (synchronisationArray.getJSONObject(i).getString("SourceSMBShare").equals(originalTitle))
						{
							// Show an unsuccessful loader
							loader.UpdateTitle("Delete failed.");
							loader.ShowLoaderWithHTMLSummary(Html.fromHtml("Cannot delete SMB share as it is in use as the source for the synchronisation '"+synchronisationArray.getJSONObject(i).getString("Title")+"'"));
							loader.UpdateButtons(false, null, null, true, "Close", new View.OnClickListener()
							{
								@Override
								public void onClick(View v)
								{
									// Close the loader
									loader.HideLoader();
								}
							});
							return;
						}

						// Check if the synchronisation references the SMB share being changed as its source
						if (synchronisationArray.getJSONObject(i).getString("TargetSMBShare").equals(originalTitle))
						{
							// Show an unsuccessful loader
							loader.UpdateTitle("Delete failed.");
							loader.ShowLoaderWithHTMLSummary(Html.fromHtml("Cannot delete SMB share as it is in use as the target for the synchronisation '"+synchronisationArray.getJSONObject(i).getString("Title")+"'"));
							loader.UpdateButtons(false, null, null, true, "Close", new View.OnClickListener()
							{
								@Override
								public void onClick(View v)
								{
									// Close the loader
									loader.HideLoader();
								}
							});
							return;
						}
					}

					// Show a successful loader
					loader.ShowLoaderWithIcon("Deleted successfully.", R.drawable.ic_done_black_24dp, null);
					loader.UpdateButtons(false, null, null, false, null, null);

					// Prepare the new job's details to be returned
					Intent activityResult = new Intent();
					activityResult.putExtra("ID", modifyingSMBShare);
					activityResult.putExtra("Delete", true);
					setResult(RESULT_OK, activityResult);

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
				catch(Exception e)
				{
					e.printStackTrace();
				}
            }
        });

        // When the "Add SMB share" button is clicked
        addSMBShareButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Validate the required inputs
                ValidateTitle();
                ValidateUsername();
                ValidatePassword();
                ValidateSMBAddress();

                // Ensure there are no errors on display
                if(		!titleEditTextContainer.isErrorEnabled() &&
						!domainEditTextContainer.isErrorEnabled() &&
						!usernameEditTextContainer.isErrorEnabled() &&
						!passwordEditTextContainer.isErrorEnabled() &&
						!addressEditTextContainer.isErrorEnabled()
				)
                {
					// Initialise & show a loader
					testSMBShareLoader = new Loader(AddEditSMBShare.this, getLayoutInflater());
					testSMBShareLoader.ShowLoaderWithSpinner();
					testSMBShareLoader.UpdateTitle("Testing SMB share...");
					testSMBShareLoader.UpdateButtons(false, null, null, false, null, null);

					testSMBShareLoader.UpdateButtons(false, null, null, true, "Cancel", new View.OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							// Stop the ongoing task
							testSMBShare.cancel(true);

							// Re-enable the form
							ToggleFormInputs(true);

							// Close the loader
							testSMBShareLoader.HideLoader();
						}
					});

                	// Disable the form's inputs and buttons
					ToggleFormInputs(false);

                	// Assemble the SMB credentials
					NtlmPasswordAuthentication SMBShareAuthentication = new NtlmPasswordAuthentication(domainEditText.getText().toString(), usernameEditText.getText().toString(), passwordEditText.getText().toString());

					// Test the SMB share
					testSMBShare = new TestSMBShare(AddEditSMBShare.this, addressEditText.getText().toString(), SMBShareAuthentication, AddEditSMBShare.this);
					testSMBShare.execute();
                }
            }
        });
    }

    // Validate the title of an SMB share
    private void ValidateTitle()
    {
        // Validate the input
        if(titleEditText.getText().toString().length() <= 0 || titleEditText.getText().toString().equals(""))
        {
            titleEditTextContainer.setError("Please enter a title.");
        }
        else
        {
			// Initialise shared preferences
			SharedPreferences myPreferences = getSharedPreferences("FileSync", 0);
			boolean foundDuplicateTitle = false;

			try
			{
        		// Obtain the existing SMB share list
				JSONArray smbShareArray = new JSONArray(myPreferences.getString("SMBShares", "[]"));

				// Loop through existing SMB shares
				for(int i = 0; i < smbShareArray.length(); i++)
				{
					// Check if another SMB share has the same title
					if(modifyingSMBShare != i && smbShareArray.getJSONObject(i).getString("Title").equals(titleEditText.getText().toString()))
					{
						titleEditTextContainer.setError("Title already in use.");
						foundDuplicateTitle = true;
						break;
					}
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}

			// If no duplicate SMB share titles were found
			if(!foundDuplicateTitle)
			{
				// Hide any errors (allows the form to be submitted)
				titleEditTextContainer.setError("");
				titleEditTextContainer.setErrorEnabled(false);
			}
        }
    }

	// Validate the username of an SMB share
    private void ValidateUsername()
    {
        // No value entered
        if(usernameEditText.getText().toString().length() <= 0 || usernameEditText.getText().toString().equals(""))
        {
            usernameEditTextContainer.setError("Please enter a username.");
        }
        else
        {
			// Hide any errors (allows the form to be submitted)
            usernameEditTextContainer.setError("");
            usernameEditTextContainer.setErrorEnabled(false);
        }
    }

	// Validate the password of an SMB share
	private void ValidatePassword()
	{
		// No value entered
		if(passwordEditText.getText().toString().length() <= 0 || passwordEditText.getText().toString().equals(""))
		{
			passwordEditTextContainer.setError("Please enter a password.");
		}
		else
		{
			// Hide any errors (allows the form to be submitted)
			passwordEditTextContainer.setError("");
			passwordEditTextContainer.setErrorEnabled(false);
		}
	}

	// Validate the address of an SMB share
	private void ValidateSMBAddress()
	{
		// No value entered
		if(addressEditText.getText().toString().length() <= 0 || addressEditText.getText().toString().equals(""))
		{
			addressEditTextContainer.setError("Please enter an SMB address.");
		}
		else
		{
			// Hide any errors (allows the form to be submitted)
			addressEditTextContainer.setError("");
			addressEditTextContainer.setErrorEnabled(false);
		}
	}

	// Function to enable or disable the form inputs
	private void ToggleFormInputs(boolean enabled)
	{
		deleteSMBShareButton.setClickable(enabled);
		deleteSMBShareButton.setAlpha(enabled ? 1 : 0.5f);
		titleEditText.setEnabled(enabled);
		domainEditText.setEnabled(enabled);
		usernameEditText.setEnabled(enabled);
		passwordEditText.setEnabled(enabled);
		addressEditText.setEnabled(enabled);
		addSMBShareButton.setAlpha(enabled ? 1 : 0.5f);
		addSMBShareButton.setClickable(enabled);
	}

	@Override
	// Function called when testing of an SMB share returns
	public void TestCompleted(String testResult)
	{
		// Hide the loader
		testSMBShareLoader.HideLoader();

		// Enable the form's inputs and buttons
		ToggleFormInputs(true);

		// Check if the test completed successfully
		switch(testResult)
		{
			case "SUCCESS":
			{
				// Notify the user
				final Loader loader = new Loader(this, getLayoutInflater());
				loader.ShowLoaderWithIcon(modifyingSMBShare == -1 ? "Added successfully." : "Updated successfully.", R.drawable.ic_done_black_24dp, null);
				loader.UpdateButtons(false, null, null, false, null, null);

				// Prepare the new share's details to be returned
				Intent activityResult = new Intent();
				activityResult.putExtra("ID", modifyingSMBShare);
				activityResult.putExtra("Title", titleEditText.getText().toString());
				activityResult.putExtra("Domain", domainEditText.getText().toString());
				activityResult.putExtra("Username", usernameEditText.getText().toString());
				activityResult.putExtra("Password", passwordEditText.getText().toString());
				activityResult.putExtra("Address", addressEditText.getText().toString());
				activityResult.putExtra("OriginalTitle", originalTitle);
				setResult(RESULT_OK, activityResult);

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
				break;
			}
			case "CANCELLED":
			{
				break;
			}
			case "AUTHENTICATION":
			{
				smbShareResult.setText(R.string.add_edit_smb_share_error_smb_credentials);
				smbShareResult.setVisibility(View.VISIBLE);
				break;
			}
			case "SMB_EXCEPTION":
			{
				smbShareResult.setText(R.string.add_edit_smb_share_error_smb_exception);
				smbShareResult.setVisibility(View.VISIBLE);
				break;
			}
			case "MALFORMED_URL_EXCEPTION":
			{
				smbShareResult.setText(R.string.add_edit_smb_share_error_malformed_url);
				smbShareResult.setVisibility(View.VISIBLE);
				break;
			}
			case "UNKNOWN_HOST_EXCEPTION":
			{
				smbShareResult.setText(R.string.add_edit_smb_share_error_unknown_host);
				smbShareResult.setVisibility(View.VISIBLE);
				break;
			}
			case "UNKNOWN_EXCEPTION":
			{
				smbShareResult.setText(R.string.add_edit_smb_share_error_unknown);
				smbShareResult.setVisibility(View.VISIBLE);
				break;
			}
			default:
			{
				smbShareResult.setText(R.string.add_edit_smb_share_error_default);
				smbShareResult.setVisibility(View.VISIBLE);
				break;
			}
		}
	}
}
