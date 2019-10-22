package com.example.duncan.filesync;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class Loader
{
	AlertDialog.Builder alertDialogBuilder;
	LayoutInflater layoutInflater;
	Context context;
	AlertDialog alertDialog;

	public Loader(Context passedInContext, LayoutInflater passedInLayoutInflater)
	{
		context = passedInContext;
		layoutInflater = passedInLayoutInflater;
		alertDialogBuilder = new AlertDialog.Builder(context);

		// Setup the loader
		alertDialogBuilder.setView(layoutInflater.inflate(R.layout.loader, null));
		alertDialogBuilder.setCancelable(false);
		alertDialogBuilder.setNegativeButton("Cancel", null);
		alertDialog = alertDialogBuilder.create();

		// Show the dialog
		alertDialog.show();

		//alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "HUHWHAT", (DialogInterface.OnClickListener) null);
	}

	// Update the loader's title
	public void UpdateTitle(String loaderTitle)
	{
		// Set the loader's contents
		TextView progressBarTitle = (TextView) alertDialog.findViewById(R.id.loaderTitle);
		progressBarTitle.setText(loaderTitle);
	}

	// Show the spinner section
	public void ShowLoaderWithSpinner()
	{
		// Hide the other sections
		alertDialog.findViewById(R.id.loaderProgressBarArea).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderFileTransferSummaryArea).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderIcon).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderHTMLSummaryText).setVisibility(View.GONE);

		// Show the spinner section
		alertDialog.findViewById(R.id.loaderSpinner).setVisibility(View.VISIBLE);
	}

	// Update the loader's progress bar
	public void ShowLoaderWithProgressBar(int progress, int filesProcessed, int totalFiles)
	{
		// Hide the other sections
		alertDialog.findViewById(R.id.loaderSpinner).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderFileTransferSummaryArea).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderIcon).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderHTMLSummaryText).setVisibility(View.GONE);

		// Obtain a reference to the progress bar elements
		ProgressBar progressBar = alertDialog.findViewById(R.id.loaderProgressBar);
		TextView progressBarPercentage = alertDialog.findViewById(R.id.loaderProgressBarPercentage);
		TextView progressBarFileNumber = alertDialog.findViewById(R.id.loaderProgressBarFileNumber);

		// Update the progress bar elements
		progressBar.setProgress(progress);
		progressBarPercentage.setText(progress+"%");
		progressBarFileNumber.setText("File: "+(filesProcessed + 1)+" of "+totalFiles);
		progressBar.setVisibility(View.VISIBLE);
		alertDialog.findViewById(R.id.loaderProgressBarArea).setVisibility(View.VISIBLE);
	}

	// Show the summary section
	public void ShowLoaderWithFileTransferSummary(int totalFiles, int totalFilesSkipped, int totalDataTransferred)
	{
		// Hide the other sections
		alertDialog.findViewById(R.id.loaderSpinner).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderProgressBarArea).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderIcon).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderHTMLSummaryText).setVisibility(View.GONE);

		// Show the summary section
		TextView progressBarSummaryFilesProcessed = alertDialog.findViewById(R.id.loaderFileTransferSummaryFilesProcessed);
		progressBarSummaryFilesProcessed.setText(String.format("%,d", totalFiles)+ (totalFilesSkipped > 0 ? " ("+String.format("%,d", totalFilesSkipped)+" files skipped)" : ""));
		TextView progressBarSummaryDataTransferred = alertDialog.findViewById(R.id.loaderFileTransferSummaryDataTransferred);
		progressBarSummaryDataTransferred.setText(HumanReadableByteCount(totalDataTransferred, true));
		alertDialog.findViewById(R.id.loaderFileTransferSummaryArea).setVisibility(View.VISIBLE);
	}

	// Show the summary section
	public void ShowLoaderWithHTMLSummary(Spanned spannedText)
	{
		// Hide the other sections
		alertDialog.findViewById(R.id.loaderSpinner).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderProgressBarArea).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderIcon).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderFileTransferSummaryArea).setVisibility(View.GONE);

		// Show the summary section
		TextView loaderHTMLSummaryText = alertDialog.findViewById(R.id.loaderHTMLSummaryText);
		loaderHTMLSummaryText.setText(spannedText);
		alertDialog.findViewById(R.id.loaderHTMLSummaryText).setVisibility(View.VISIBLE);
	}

	// Show the loader with an icon
	public void ShowLoaderWithIcon(int iconResourceID)
	{
		// Hide the other sections
		alertDialog.findViewById(R.id.loaderSpinner).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderProgressBarArea).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderFileTransferSummaryArea).setVisibility(View.GONE);
		alertDialog.findViewById(R.id.loaderHTMLSummaryText).setVisibility(View.GONE);

		// Show the icon
		ImageView progressBarIcon = alertDialog.findViewById(R.id.loaderIcon);
		progressBarIcon.setImageResource(iconResourceID);
		alertDialog.findViewById(R.id.loaderIcon).setVisibility(View.VISIBLE);
	}

	// Update the loader's close button
	public void UpdateCloseButton(boolean showCloseButton, String newText, View.OnClickListener closeButtonListener)
	{
		// Show / hide the close button
		alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setVisibility(showCloseButton ? View.VISIBLE : View.GONE);

		// Update the close button
		alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setText(newText);
		alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(closeButtonListener);
	}

	// Function that converts bytes into a human readable format
	public static String HumanReadableByteCount(long bytes, boolean si)
	{
		int unit = si ? 1000 : 1024;
		if (bytes < unit) return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
		return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

	public void HideLoader()
	{
		alertDialog.hide();
	}
}

