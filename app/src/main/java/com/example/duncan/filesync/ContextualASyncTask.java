package com.example.duncan.filesync;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class ContextualASyncTask extends AsyncTask
{
	// Miscellaneous variables
	private Context contextRef;
	String sourceFolder;
	NtlmPasswordAuthentication sourceCredentials;
	String targetFolder;
	NtlmPasswordAuthentication targetCredentials;
	boolean deleteTargetContents;

	// Synchronisation tracking variables
	int totalFolders = 0;
	int totalFiles = 0;
	int filesProcessed = 0;
	int filesTransferred = 0;
	int totalBytes = 0;
	int bytesTransferred = 0;
	int bytesProcessed = 0;

	public ContextualInterface callingActivityInterface;

	public ContextualASyncTask(Context context, String sourceFolderInput, NtlmPasswordAuthentication sourceCredentialsInput, String targetFolderInput, NtlmPasswordAuthentication targetCredentialsInput, boolean targetDeleteTargetContents)
	{
		contextRef = context;

		// Store the passed in data
		sourceFolder = sourceFolderInput;
		sourceCredentials = sourceCredentialsInput;
		targetFolder = targetFolderInput;
		targetCredentials = targetCredentialsInput;
		deleteTargetContents = targetDeleteTargetContents;
	}

	@Override
	protected Object doInBackground(Object[] objects)
	{
		try
		{
			// Check if the the "delete target contents" flag is set
			if(deleteTargetContents)
			{
				// If so, determine whether the target folder is an SMB share or not
				if(targetCredentials == null)
				{
					// Delete local folder's contents
					DeleteLocalFolderContents(DocumentFile.fromTreeUri(contextRef, Uri.parse(targetFolder)));
				}
				else
				{
					// Delete SMB folder's contents
					DeleteSMBFolderContents("smb://"+targetFolder+"/");
				}
			}

			// Check if the source is local
			if(sourceCredentials == null)
			{
				EstimateLocalFolderContents(DocumentFile.fromTreeUri(contextRef, Uri.parse(sourceFolder)));
				Log.i("STORAGE", "Folders: "+ totalFolders +", files: "+totalFiles+", data: "+ bytesTransferred);

				// Report back to the calling activity
				callingActivityInterface.OnSynchronisationProgress(totalFiles, filesProcessed, totalBytes, bytesProcessed);

				// Check if the target is local
				if(targetCredentials == null)
				{
					Log.i("STORAGE", "Performing local to local transfer...");
					DuplicateLocalFolderToLocalFolder(DocumentFile.fromTreeUri(contextRef, Uri.parse(sourceFolder)), DocumentFile.fromTreeUri(contextRef, Uri.parse(targetFolder)));
				}
				else
				{
					Log.i("STORAGE", "Performing local to SMB transfer...");
					DuplicateLocalFolderToSMBFolder(DocumentFile.fromTreeUri(contextRef, Uri.parse(sourceFolder)), "smb://"+targetFolder+"/");
				}
			}
			else
			{
				EstimateSMBFolderContents("smb://"+sourceFolder+"/");
				Log.i("STORAGE", "Folders: "+ totalFolders +", files: "+totalFiles+", data: "+ bytesTransferred);

				// Report back to the calling activity
				callingActivityInterface.OnSynchronisationProgress(totalFiles, filesProcessed, totalBytes, bytesProcessed);

				// Check if the target is local
				if(targetCredentials == null)
				{
					Log.i("STORAGE", "Performing SMB to local transfer...");
					DuplicateSMBFolderToLocalFolder("smb://"+sourceFolder+"/", DocumentFile.fromTreeUri(contextRef, Uri.parse(targetFolder)));
				}
				else
				{
					Log.i("STORAGE", "Performing SMB to SMB transfer...");
					DuplicateSMBFolderToSMBFolder("smb://"+sourceFolder+"/", "smb://"+targetFolder+"/");
				}
			}
		}
		catch (Exception exception)
		{
			if(exception.getMessage().equals("write failed: ENOSPC (No space left on device)"))
			{
				Log.i("STORAGE", "OUT OF SPAAAAACEE");
			}
			else
			{
				exception.printStackTrace();
			}
		}
		return null;
	}

	@Override
	protected void onPostExecute(Object o)
	{
		super.onPostExecute(o);

		// Report back to the calling activity
		callingActivityInterface.OnSynchronisationComplete(totalFiles, filesTransferred, totalBytes, bytesTransferred);
	}

	// Function to determine the MIME type of a file based on its extension
	public static String GetMimeType(String url)
	{
		String type = null;
		String extension = url.substring(url.lastIndexOf(".") + 1);
		if (extension != null)
		{
			type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
		}
		return type;
	}

	boolean DoesSMBFileFolderExist(SmbFile[] fileFolderList, String fileFolderName, boolean isFolder)
	{
		// Add a trailing slash, if a folder is being sought
		fileFolderName = (isFolder ? fileFolderName : fileFolderName);

		try
		{
			for (SmbFile fileFolder : fileFolderList)
			{
				Log.i("STORAGE", "Comparing "+fileFolder.getName()+" and "+fileFolderName);

				if (fileFolder.getName().equals(fileFolderName) && fileFolder.isDirectory() == isFolder)
				{
					return true;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}

	void CopyBufferedFile(BufferedInputStream bufferedInputStream, BufferedOutputStream bufferedOutputStream) throws IOException
	{
		try (BufferedInputStream in = bufferedInputStream; BufferedOutputStream out = bufferedOutputStream)
		{
			byte[] buf = new byte[1024];
			int nosRead;
			while ((nosRead = in.read(buf)) != -1)  // read this carefully ...
			{
				out.write(buf, 0, nosRead);

				// Increment the number of bytes processed
				bytesProcessed += nosRead;

				// Report back to the calling activity
				callingActivityInterface.OnSynchronisationProgress(totalFiles, filesProcessed, totalBytes, bytesProcessed);
			}
		}

		// Increment the number of files copied
		filesTransferred++;
		Log.i("STORAGE", "File transfer complete");
	}

	// Function to delete all files and folders in a DocumentFile folder
	public void DeleteLocalFolderContents(DocumentFile targetFolder)
	{
		Log.i("STORAGE", "Deleting contents of "+targetFolder.getName());

		// Loop through the files & folders in the target location
		for (DocumentFile file : targetFolder.listFiles())
		{
			// Check if the file / folder is a folder
			if (file.isDirectory())
			{
				DeleteLocalFolderContents(file);
				file.delete();
			}
			else
			{
				file.delete();
			}
		}
	}

	// Function to delete all files and folders in a SMB folder
	public void DeleteSMBFolderContents(String targetFolder) throws MalformedURLException, SmbException
	{
		Log.i("STORAGE", "Deleting contents of "+targetFolder);

		SmbFile smbFile = new SmbFile(targetFolder, targetCredentials);
		SmbFile[] fileList = smbFile.listFiles();

		// Loop through the files & folders in the target location
		for (SmbFile file : fileList)
		{
			// Check if the file / folder is a folder
			if (file.isDirectory())
			{
				DeleteSMBFolderContents(file.getPath());
				file.delete();
			}
			else
			{
				file.delete();
			}
		}
	}

	// Function to estimate the contents of a local source folder
	public void EstimateLocalFolderContents(DocumentFile sourceFolder)
	{
		// Loop through the files & folders in the source location
		for (DocumentFile file : sourceFolder.listFiles())
		{
			if(file.isDirectory())
			{
				totalFolders++;
				EstimateLocalFolderContents(file);
			}
			else
			{
				totalFiles++;
				totalBytes +=file.length();
			}
		}
	}

	// Function to estimate the contents of an SMB source folder
	public void EstimateSMBFolderContents(String sourceFolder) throws MalformedURLException, SmbException
	{
		// Connect to the source folder
		SmbFile smbFile = new SmbFile(sourceFolder, sourceCredentials);
		SmbFile[] fileList = smbFile.listFiles();

		// Loop through the files & folders in the source location
		for (SmbFile file : fileList)
		{
			if(file.isDirectory())
			{
				totalFolders++;
				EstimateSMBFolderContents(file.getPath());
			}
			else
			{
				totalFiles++;
				totalBytes +=file.length();
			}
		}
	}

	// Function to duplicate all files and folders from one DocumentFile to another
	public void DuplicateLocalFolderToLocalFolder(DocumentFile sourceLocation, DocumentFile targetLocation) throws IOException
	{
		// Loop through the files & folders in the source location
		for (DocumentFile file : sourceLocation.listFiles())
		{
			// Check if the file / folder is a folder
			if(file.isDirectory())
			{
				// Check if the current folder already exists in the destination folder
				DocumentFile wouldBeFolder = targetLocation.findFile(file.getName());
				if(wouldBeFolder != null)
				{
					Log.i("STORAGE", "Folder '" + file.getName() + "' already exists, skipping creation...");
				}
				else
				{
					// Create the folder
					Log.i("STORAGE", "Folder '"+file.getName()+"' does not yet exist, creating...");
					wouldBeFolder = targetLocation.createDirectory(file.getName());
				}

				// Then re-run the function on that directory
				DuplicateLocalFolderToLocalFolder(file, wouldBeFolder);
			}
			else
			{
				// Check if the current file already exists in the destination folder
				DocumentFile wouldBeFile = targetLocation.findFile(file.getName());
				if(wouldBeFile != null && file.length() == wouldBeFile.length())
				{
					Log.i("STORAGE", "File '" + file.getName() + "' already exists and is identical (checked by file size), skipping...");

					// Increment the number of bytes processed
					bytesProcessed += file.length();
				}
				else
				{
					// Check if a file exists with the same name
					if (wouldBeFile != null)
					{
						Log.i("STORAGE", "File '" + file.getName() + "' already exists but is NOT identical (checked by file size), deleting...");

						// Delete the file
						wouldBeFile.delete();
					}
					else
					{
						Log.i("STORAGE", "File '" + file.getName() + "' does not exist in '" + targetLocation.getUri() + "', creating...");
					}

					// Get the source file's type
					String sourceFileType = MimeTypeMap.getSingleton().getExtensionFromMimeType(contextRef.getContentResolver().getType(file.getUri()));

					// Create the new (empty) file
					DocumentFile newFile = targetLocation.createFile(sourceFileType, file.getName());

					// Copy the file
					CopyBufferedFile(new BufferedInputStream(contextRef.getContentResolver().openInputStream(file.getUri())), new BufferedOutputStream(contextRef.getContentResolver().openOutputStream(newFile.getUri())));
					bytesTransferred += file.length();
					Log.i("STORAGE", "File created.");
				}

				// Increment the number of files processed
				filesProcessed++;

				// Report back to the calling activity
				callingActivityInterface.OnSynchronisationProgress(totalFiles, filesProcessed, totalBytes, bytesProcessed);
			}
		}
	}

	// Function to duplicate all files and folders from a local folder to an SMB one
	void DuplicateLocalFolderToSMBFolder(DocumentFile sourceFolder, String targetFolder) throws IOException
	{
		// Connect to the SMB folder
		SmbFile smbFile = new SmbFile(targetFolder, targetCredentials);
		SmbFile[] files = smbFile.listFiles();

		// Loop through the files & folders in the source location
		for (DocumentFile file : sourceFolder.listFiles())
		{
			// Check if the file / folder is a folder
			if(file.isDirectory())
			{
				// Check if the current folder already exists in the destination folder
				if(DoesSMBFileFolderExist(files, file.getName()+"/", true))
				{
					Log.i("STORAGE", "Folder '" + file.getName() + "' already exists, skipping creation...");
				}
				else
				{
					// Create the folder
					Log.i("STORAGE", "Folder '"+file.getName()+"' does not yet exist, creating...");
					SmbFile newFolder = new SmbFile(targetFolder+file.getName()+"/", targetCredentials);
					newFolder.mkdir();
				}

				// Then re-run the function on that directory
				DuplicateLocalFolderToSMBFolder(file, targetFolder+file.getName()+"/");
			}
			else
			{
				// Create the new SMB File
				SmbFile wouldBeFile = new SmbFile(targetFolder+file.getName(), targetCredentials);

				// Check if the current file already exists in the destination folder
				if(wouldBeFile.exists() && file.length() == wouldBeFile.length())
				{
					Log.i("STORAGE", "File '" + file.getName() + "' already exists and is identical (checked by file size), skipping...");

					// Increment the number of bytes processed
					bytesProcessed += file.length();
				}
				else
				{
					// Check if target file exists
					if(!wouldBeFile.exists())
					{
						Log.i("STORAGE", "File '"+file.getName()+"' does not exist, creating...");
					}
					else
					{
						Log.i("STORAGE", "File '" + file.getName() + "' already exists but is NOT identical (checked by file size), deleting...");
						wouldBeFile.delete();
					}

					// Copy the file
					CopyBufferedFile(new BufferedInputStream(contextRef.getContentResolver().openInputStream(file.getUri())), new BufferedOutputStream(wouldBeFile.getOutputStream()));
					bytesTransferred += file.length();
					Log.i("STORAGE", "File created.");
				}

				// Increment the number of files processed
				filesProcessed++;

				// Report back to the calling activity
				callingActivityInterface.OnSynchronisationProgress(totalFiles, filesProcessed, totalBytes, bytesProcessed);
			}
		}
	}

	// Function to duplicate all files and folders from an SMB folder to a local one
	void DuplicateSMBFolderToLocalFolder(String sourceFolder, DocumentFile targetFolder) throws IOException
	{
		// Loop through the files & folders in the source location
		SmbFile smbFile = new SmbFile(sourceFolder, sourceCredentials);
		SmbFile[] fileList = smbFile.listFiles();
		for (SmbFile file : fileList)
		{
			// Check if the file / folder is a folder
			if (file.isDirectory())
			{
				// Check if the source folder already exists in the target folder
				String folderName = file.getName().substring(0, (file.getName().length() -1));
				DocumentFile wouldBeFolder = targetFolder.findFile(folderName);
				if(wouldBeFolder != null)
				{
					Log.i("STORAGE", "Folder '" + folderName + "' already exists, skipping creation...");
				}
				else
				{
					// Create the folder
					Log.i("STORAGE", "Folder '"+folderName+"' does not yet exist, creating...");
					wouldBeFolder = targetFolder.createDirectory(folderName);
				}

				// Then re-run the function on that directory
				DuplicateSMBFolderToLocalFolder(file.getPath(), wouldBeFolder);
			}
			else
			{
				// Check if the current file already exists in the destination folder
				DocumentFile wouldBeFile = targetFolder.findFile(file.getName());
				if(wouldBeFile != null && file.length() == wouldBeFile.length())
				{
					Log.i("STORAGE", "File '" + file.getName() + "' already exists and is identical (checked by file size), skipping...");

					// Increment the number of bytes processed
					bytesProcessed += file.length();
				}
				else
				{
					if (wouldBeFile != null)
					{
						Log.i("STORAGE", "File '" + file.getName() + "' already exists but is NOT identical (checked by file size), deleting...");
						wouldBeFile.delete();
					}
					else
					{
						Log.i("STORAGE", "File '" + file.getName() + "' does not exist in '" + targetFolder.getUri() + "', creating...");
					}

					// Create the new (empty) file
					DocumentFile newFile = targetFolder.createFile(GetMimeType(file.getName()), file.getName());

					// Copy the file
					CopyBufferedFile(new BufferedInputStream(file.getInputStream()), new BufferedOutputStream(contextRef.getContentResolver().openOutputStream(newFile.getUri())));
					bytesTransferred += file.length();
					Log.i("STORAGE", "File '" + file.getName() + "'created.");
				}

				// Increment the number of files processed
				filesProcessed++;

				// Report back to the calling activity
				callingActivityInterface.OnSynchronisationProgress(totalFiles, filesProcessed, totalBytes, bytesProcessed);
			}
		}
	}

	// Function to duplicate all files and folders from an SMB folder to another SMB one
	void DuplicateSMBFolderToSMBFolder(String sourceFolder, String targetFolder) throws IOException
	{
		// Connect to the sourceSMB folder
		SmbFile sourceSmbFile = new SmbFile(sourceFolder, sourceCredentials);
		SmbFile[] sourceFileList = sourceSmbFile.listFiles();

		// Connect to the target SMB folder
		SmbFile targetSmbFile = new SmbFile(targetFolder, targetCredentials);
		SmbFile[] targetFileList = targetSmbFile.listFiles();

		// Loop through the files & folders in the source location
		for (SmbFile file : sourceFileList)
		{
			// Check if the file / folder is a folder
			if (file.isDirectory())
			{
				// Check if the current folder already exists in the destination folder
				if(DoesSMBFileFolderExist(targetFileList, file.getName(), true))
				{
					Log.i("STORAGE", "Folder '" + file.getName() + "' already exists, skipping creation...");
				}
				else
				{
					// Create the folder
					Log.i("STORAGE", "Folder '"+file.getName()+"' does not yet exist, creating...");
					SmbFile newFolder = new SmbFile(targetFolder+file.getName()+"/", targetCredentials);
					newFolder.mkdir();
				}

				// Then re-run the function on that directory
				DuplicateSMBFolderToSMBFolder(file.getPath(), targetFolder+file.getName()+"/");
			}
			else
			{
				// Create the new SMB File
				SmbFile wouldBeFile = new SmbFile(targetFolder+file.getName(), targetCredentials);

				// Check if the current file already exists in the destination folder
				if(wouldBeFile.exists() && file.length() == wouldBeFile.length())
				{
					Log.i("STORAGE", "File '" + file.getName() + "' already exists and is identical (checked by file size), skipping...");

					// Increment the number of bytes processed
					bytesProcessed += file.length();
				}
				else
				{
					// Check if target file exists
					if(!wouldBeFile.exists())
					{
						Log.i("STORAGE", "File '"+file.getName()+"' does not exist, creating...");
					}
					else
					{
						Log.i("STORAGE", "File '" + file.getName() + "' already exists but is NOT identical (checked by file size), deleting...");
						wouldBeFile.delete();
					}

					// Copy the file
					CopyBufferedFile(new BufferedInputStream(file.getInputStream()), new BufferedOutputStream(wouldBeFile.getOutputStream()));
					bytesTransferred += file.length();
					Log.i("STORAGE", "File created.");
				}

				// Increment the number of files processed
				filesProcessed++;

				// Report back to the calling activity
				callingActivityInterface.OnSynchronisationProgress(totalFiles, filesProcessed, totalBytes, bytesProcessed);
			}
		}
	}

	// MainActivity interface
	public interface ContextualInterface
	{
		public void OnSynchronisationProgress(int totalFiles, int filesProcessed, int totalBytes, int bytesProcessed);
		public void OnSynchronisationComplete(int totalFiles, int filesTransferred, int totalBytes, int bytesTransferred);
	}
}
