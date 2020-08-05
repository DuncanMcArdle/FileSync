package com.example.duncan.filesync;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Locale;

public class Loader
{
	private AlertDialog.Builder alertDialogBuilder;
	private Context context;
	private AlertDialog alertDialog;

	// Titles
	private TextView addedFilesTitle;
	private TextView updatedFilesTitle;
	private TextView deletedFilesTitle;

	// List views
	private ListView addedFilesListViewShow;
	private ListView updatedFilesListViewShow;
	private ListView deletedFilesListViewShow;

	public Loader(Context passedInContext, LayoutInflater passedInLayoutInflater)
	{
		context = passedInContext;
		alertDialogBuilder = new AlertDialog.Builder(context);

		// Setup the loader
		alertDialogBuilder.setView(View.inflate(context, R.layout.loader, null));
		alertDialogBuilder.setCancelable(false);
		alertDialogBuilder.setNegativeButton("Cancel", null);
		alertDialog = alertDialogBuilder.create();

		// Show the dialog
		alertDialog.show();

		// Obtain references to the ListViews and their titles
		addedFilesTitle = alertDialog.findViewById(R.id.loaderSummaryFilesAddedShow);
		addedFilesListViewShow = alertDialog.findViewById(R.id.loaderSummaryFilesAddedListView);
		updatedFilesTitle = alertDialog.findViewById(R.id.loaderSummaryFilesUpdatedShow);
		updatedFilesListViewShow = alertDialog.findViewById(R.id.loaderSummaryFilesUpdatedListView);
		deletedFilesTitle = alertDialog.findViewById(R.id.loaderSummaryFilesDeletedShow);
		deletedFilesListViewShow = alertDialog.findViewById(R.id.loaderSummaryFilesDeletedListView);

		// Add listeners for the expansion buttons on transfer summaries
		View.OnClickListener showHideFilesListener = new View.OnClickListener()
		{
			@Override
			public void onClick(View clickedView)
			{
				// Targets
				TextView targetTextView = (TextView) clickedView;
				ListView targetListView = null;

				switch(clickedView.getId())
				{
					case R.id.loaderSummaryFilesAddedShow:
					{
						targetListView = addedFilesListViewShow;
						break;
					}
					case R.id.loaderSummaryFilesUpdatedShow:
					{
						targetListView = updatedFilesListViewShow;
						break;
					}
					case R.id.loaderSummaryFilesDeletedShow:
					{
						targetListView = deletedFilesListViewShow;
						break;
					}
				}

				// Show / Hide the target ListView (and update the associated button)
				targetTextView.setText(targetListView.getVisibility() == View.VISIBLE ? "(show)" : "(hide)");
				targetListView.setVisibility(targetListView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
			}
		};
		addedFilesTitle.setOnClickListener(showHideFilesListener);
		updatedFilesTitle.setOnClickListener(showHideFilesListener);
		deletedFilesTitle.setOnClickListener(showHideFilesListener);
	}

	// Update the loader's title
	public void UpdateTitle(String loaderTitle)
	{
		// Set the loader's contents
		TextView progressBarTitle = alertDialog.findViewById(R.id.loaderTitle);
		progressBarTitle.setText(loaderTitle);
	}

	// Show the spinner section
	public void ShowLoaderWithSpinner()
	{
		// Hide the other sections
		alertDialog.findViewById(R.id.loaderProgressBarArea).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderSummaryArea).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderIcon).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderHTMLSummaryText).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderDescriptionText).setVisibility(View.GONE);

		// Show the spinner section
		alertDialog.findViewById(R.id.loaderSpinner).setVisibility(View.VISIBLE);
	}

	// Update the loader's progress bar
	public void ShowLoaderWithProgressBar(int progress, int filesProcessed, int totalFiles, String currentFileName)
	{
		// Hide the other sections
		alertDialog.findViewById(R.id.loaderSpinner).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderSummaryArea).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderIcon).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderHTMLSummaryText).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderDescriptionText).setVisibility(View.GONE);

		// Obtain a reference to the progress bar elements
		ProgressBar progressBar = alertDialog.findViewById(R.id.loaderProgressBar);
		TextView progressBarPercentage = alertDialog.findViewById(R.id.loaderProgressBarPercentage);
		TextView progressBarFileNumber = alertDialog.findViewById(R.id.loaderProgressBarFileNumber);
		TextView progressBarFileName = alertDialog.findViewById(R.id.loaderProgressBarFileName);

		// Update the progress bar elements
		progressBar.setProgress(progress);
		progressBarPercentage.setText(String.format(Locale.UK, "%d%%", progress));
		progressBarFileNumber.setText(String.format(Locale.UK, "%d of %d", filesProcessed + 1, totalFiles));
		progressBarFileName.setText(currentFileName == null ? "" : currentFileName);
		progressBar.setVisibility(View.VISIBLE);
		alertDialog.findViewById(R.id.loaderProgressBarArea).setVisibility(View.VISIBLE);
	}

	// Show the summary section
	public void ShowLoaderWithFileTransferSummary(boolean performTransfer, int timeTaken, int filesTransferred, long dataTransferred, ArrayList<AnalysedFile> addedFileList, ArrayList<AnalysedFile> updatedFileList, ArrayList<AnalysedFile> deletedFileList)
	{
		// Hide the other sections
		alertDialog.findViewById(R.id.loaderSpinner).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderProgressBarArea).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderIcon).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderHTMLSummaryText).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderDescriptionText).setVisibility(View.GONE);

		// Show / Hide the "Time taken" section
		LinearLayout timeTakenSection = alertDialog.findViewById(R.id.loaderSummaryTimeTakenSection);
		timeTakenSection.setVisibility(performTransfer ? View.VISIBLE : View.GONE);

		// Obtain a reference to the various labels
		TextView filesTransferredLabel = alertDialog.findViewById(R.id.loaderSummaryFilesTransferredLabel);
		TextView dataTransferredLabel = alertDialog.findViewById(R.id.loaderSummaryDataTransferredLabel);
		TextView filesAddedLabel = alertDialog.findViewById(R.id.loaderSummaryFilesAddedLabel);
		TextView filesUpdatedLabel = alertDialog.findViewById(R.id.loaderSummaryFilesUpdatedLabel);
		TextView filesDeletedLabel = alertDialog.findViewById(R.id.loaderSummaryFilesDeletedLabel);

		// Update the labels
		filesTransferredLabel.setText(performTransfer ? "Files transferred:" : "Files to transfer:");
		dataTransferredLabel.setText(performTransfer ? "Data transferred:" : "Data to transfer:");
		filesAddedLabel.setText(performTransfer ? "Files added:" : "Files to add:");
		filesUpdatedLabel.setText(performTransfer ? "Files updated:" : "Files to update:");
		filesDeletedLabel.setText(performTransfer ? "Files deleted" : "Files to delete:");

		// Populate the results
		TextView timeTakenTextView = alertDialog.findViewById(R.id.loaderSummaryTimeTaken);
		timeTakenTextView.setText(MakeSecondsReadable(timeTaken));
		TextView filesTransferredTextView = alertDialog.findViewById(R.id.loaderSummaryFilesTransferred);
		filesTransferredTextView.setText(String.format(Locale.UK, "%,d", filesTransferred));
		TextView dataTransferredTextView = alertDialog.findViewById(R.id.loaderSummaryDataTransferred);
		dataTransferredTextView.setText(HumanReadableByteCount(dataTransferred, true));
		TextView filesAddedTextView = alertDialog.findViewById(R.id.loaderSummaryFilesAddedNumber);
		filesAddedTextView.setText(String.format(Locale.UK,"%,d", addedFileList.size()));
		TextView filesUpdatedTextView = alertDialog.findViewById(R.id.loaderSummaryFilesUpdatedNumber);
		filesUpdatedTextView.setText(String.format(Locale.UK,"%,d", updatedFileList.size()));
		TextView filesDeletedTextView = alertDialog.findViewById(R.id.loaderSummaryFilesDeletedNumber);
		filesDeletedTextView.setText(String.format(Locale.UK,"%,d", deletedFileList.size()));

		// Minimise the file lists
		addedFilesListViewShow.setVisibility(View.GONE);
		updatedFilesListViewShow.setVisibility(View.GONE);
		deletedFilesListViewShow.setVisibility(View.GONE);
		addedFilesTitle.setText(R.string.loader_show);
		updatedFilesTitle.setText(R.string.loader_show);
		deletedFilesTitle.setText(R.string.loader_show);

		// Check if any files were added
		if(addedFileList.size() > 0)
		{
			// Populate the added files list
			FileListAdapter fileListAdapter = new FileListAdapter(alertDialog.getContext(), addedFileList);
			addedFilesListViewShow.setAdapter(fileListAdapter);
		}
		else
		{
			// Hide the "(show)" / "(hide)" button
			TextView showFilesAddedButton = alertDialog.findViewById(R.id.loaderSummaryFilesAddedShow);
			showFilesAddedButton.setVisibility(View.GONE);
		}

		// Check if any files were updated
		if(updatedFileList.size() > 0)
		{
			// Populate the added files list
			FileListAdapter fileListAdapter = new FileListAdapter(alertDialog.getContext(), updatedFileList);
			updatedFilesListViewShow.setAdapter(fileListAdapter);
		}
		else
		{
			// Hide the "(show)" / "(hide)" button
			TextView showFilesUpdatedButton = alertDialog.findViewById(R.id.loaderSummaryFilesUpdatedShow);
			showFilesUpdatedButton.setVisibility(View.GONE);
		}

		// Check if any files were deleted
		if(deletedFileList.size() > 0)
		{
			// Populate the added files list
			FileListAdapter fileListAdapter = new FileListAdapter(alertDialog.getContext(), deletedFileList);
			deletedFilesListViewShow.setAdapter(fileListAdapter);
		}
		else
		{
			// Hide the "(show)" / "(hide)" button
			TextView showFilesDeletedButton = alertDialog.findViewById(R.id.loaderSummaryFilesDeletedShow);
			showFilesDeletedButton.setVisibility(View.GONE);
		}

		// Show the summary section
		alertDialog.findViewById(R.id.loaderSummaryArea).setVisibility(View.VISIBLE);
	}

	// Show the summary section
	public void ShowLoaderWithHTMLSummary(Spanned spannedText)
	{
		// Hide the other sections
		alertDialog.findViewById(R.id.loaderSpinner).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderProgressBarArea).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderIcon).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderSummaryArea).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderDescriptionText).setVisibility(View.GONE);

		// Show the summary section
		TextView loaderHTMLSummaryText = alertDialog.findViewById(R.id.loaderHTMLSummaryText);
		loaderHTMLSummaryText.setText(spannedText);
		alertDialog.findViewById(R.id.loaderHTMLSummaryText).setVisibility(View.VISIBLE);
	}

	// Show the loader with an icon
	public void ShowLoaderWithIcon(String titleText, int iconResourceID, String descriptionText)
	{
		// Update the title
		UpdateTitle(titleText);

		// Hide the other sections
		alertDialog.findViewById(R.id.loaderSpinner).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderProgressBarArea).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderSummaryArea).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderHTMLSummaryText).setVisibility(View.GONE);

		// Set and show the icon
		ImageView progressBarIcon = alertDialog.findViewById(R.id.loaderIcon);
		progressBarIcon.setImageResource(iconResourceID);
		alertDialog.findViewById(R.id.loaderIcon).setVisibility(View.VISIBLE);

		// Set and show / Hide the description
		TextView descriptionTextView = alertDialog.findViewById(R.id.loaderDescriptionText);
		if(descriptionText != null)
		{
			descriptionTextView.setText(descriptionText);
			descriptionTextView.setVisibility(View.VISIBLE);
		}
		else
		{
			descriptionTextView.setVisibility(View.GONE);
		}
	}

	// Update the loader's close button
	public void UpdateButtons(boolean showPrimaryButton, String primaryButtonText, View.OnClickListener primaryButtonListener, boolean showSecondaryButton, String secondaryButtonText, View.OnClickListener secondaryButtonListener)
	{
		// Check if the primary button should be shown
		if(showPrimaryButton)
		{
			// Update the primary button's text
			alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setText(primaryButtonText);

			// Set the primary button's listener
			alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(primaryButtonListener);
		}

		// Check if the secondary button should be shown
		if(showSecondaryButton)
		{
			// Update the secondary button's text (default to "close")
			alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setText(secondaryButtonText == null ? "Close" : secondaryButtonText);

			// Set the secondary button's listener (default to just hide the loader)
			alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(secondaryButtonListener == null ? new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					HideLoader();
				}
			} : secondaryButtonListener);
		}

		// Show / Hide both buttons
		alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(showPrimaryButton? View.VISIBLE : View.GONE);
		alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(showSecondaryButton ? View.VISIBLE : View.GONE);
	}

	// Function that converts bytes into a human readable format
	private static String HumanReadableByteCount(long bytes, boolean si)
	{
		int unit = si ? 1000 : 1024;
		if (bytes < unit) return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
		return String.format(Locale.UK,"%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	// Function that converts sections into a human readable string
	private static String MakeSecondsReadable(int seconds)
	{
		int sec = seconds % 60;
		int min = (seconds / 60)%60;
		int hours = (seconds/60)/60;

		if(hours > 0)
		{
			return hours+" hours"+(min > 0 ? " and "+min+" minutes" : "");
		}
		else if(min > 0)
		{
			return min+" minutes"+(sec > 0 ? " and "+sec+" seconds" : "");
		}
		else
		{
			return seconds+" seconds";
		}
	}

	public void HideLoader()
	{
		alertDialog.dismiss();
	}

	class FileListAdapter extends BaseAdapter
	{
		private Context context; //context
		private ArrayList<AnalysedFile> files; //data source of the list adapter

		// Constructor
		FileListAdapter(Context context, ArrayList<AnalysedFile> files)
		{
			this.context = context;
			this.files = files;
		}

		@Override
		public int getCount()
		{
			return this.files.size();
		}

		@Override
		public Object getItem(int position)
		{
			return files.get(position);
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent)
		{
			// Inflate the layout for each list item
			if (convertView == null)
			{
					convertView = LayoutInflater.from(context).
					inflate(R.layout.file_list_item, parent, false);
			}

			AnalysedFile currentFile = (AnalysedFile) getItem(position);
			TextView fileNumber = convertView.findViewById(R.id.fileNumber);
			TextView fileName = convertView.findViewById(R.id.fileName);

			// URL decode the file's path
			String decodedPath = null;
			try
			{
				decodedPath = URLDecoder.decode(currentFile.filePath, "UTF-8");
			} catch (UnsupportedEncodingException e)
			{
				e.printStackTrace();
			}

			// Obtain the final segment of the URI
			String finalPathSegment = decodedPath.substring(decodedPath.lastIndexOf(":") + 1);

			// Set the file's remaining details
			fileNumber.setText(String.format(Locale.UK, "%d. ", position + 1));
			fileName.setText(String.format(Locale.UK, "%s/%s (%s)", finalPathSegment, currentFile.fileName, currentFile.isAFolder ? "folder" : HumanReadableByteCount(currentFile.fileSize, true)));

			return convertView;
		}
	}
}

