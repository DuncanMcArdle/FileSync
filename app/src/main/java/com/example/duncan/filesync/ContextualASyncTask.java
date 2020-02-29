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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class ContextualASyncTask extends AsyncTask
{
	// Miscellaneous variables
	private Context contextRef;
	String globalSourceFolder;
	NtlmPasswordAuthentication globalSourceCredentials;
	String globalTargetFolder;
	NtlmPasswordAuthentication globalTargetCredentials;
	boolean performTransfer;
	int deletionPolicy;

	// Synchronisation tracking variables
	int totalFolders = 0;
	int totalFiles = 0;
	int filesProcessed = 0;
	int filesTransferred = 0;
	int totalBytes = 0;
	int bytesTransferred = 0;
	int bytesProcessed = 0;

	// Tracking variables
	int addedFileSize;
	ArrayList<AnalysedFile> addedFileList;
	int updatedFilesSize;
	ArrayList<AnalysedFile> updatedFilesList;
	int deletedFileSize;
	ArrayList<AnalysedFile> deletedFileList;

	// Deletion policies
	int DELETION_POLICY_PERFECT_COPY = 0;
	int DELETION_POLICY_DONT_DELETE = 0;

	public ContextualInterface callingActivityInterface;

	public ContextualASyncTask(Context callingContext, String sourceFolderInput, NtlmPasswordAuthentication sourceCredentialsInput, String targetFolderInput, NtlmPasswordAuthentication targetCredentialsInput, int deletionPolicyInput, boolean performTransferInput)
	{
		contextRef = callingContext;

		// Store the passed in data
		globalSourceFolder = sourceFolderInput;
		globalSourceCredentials = sourceCredentialsInput;
		globalTargetFolder = targetFolderInput;
		globalTargetCredentials = targetCredentialsInput;
		deletionPolicy = deletionPolicyInput;
		performTransfer = performTransferInput;
	}

	@Override
	protected Object doInBackground(Object[] objects)
	{
		// Test access to the synchronisation source
		if(!TestAccess(globalSourceCredentials, globalSourceFolder, false))
		{
			callingActivityInterface.OnSynchronisationFailed("SOURCE_ACCESS");
		}
		else if(!TestAccess(globalTargetCredentials, globalTargetFolder, true))
		{
			callingActivityInterface.OnSynchronisationFailed("TARGET_ACCESS");
		}
		else
		{
			try
			{

				addedFileList = new ArrayList<AnalysedFile>();
				updatedFilesList = new ArrayList<AnalysedFile>();
				deletedFileList = new ArrayList<AnalysedFile>();

				// Perform / estimate target deletion (if required)
				// Loop through all source files and folders
				// Compare files with target to estimate / perform transfer
				// (optional) Loop through all target files and folders
				// (optional) Delete files / folders only required in the destination

				Log.i("STORAGE", "Calling SynchroniseFolders with " + globalTargetFolder);
				SynchroniseFolders(globalSourceCredentials != null ? null : DocumentFile.fromTreeUri(contextRef, Uri.parse(globalSourceFolder)), globalSourceFolder, globalSourceCredentials, globalTargetCredentials != null ? null : DocumentFile.fromTreeUri(contextRef, Uri.parse(globalTargetFolder)), globalTargetFolder, globalTargetCredentials, performTransfer);

				/*// Check if the the "delete target contents" flag is set
				if (deleteTargetContents)
				{
					// Determine whether the target folder is an SMB share or not
					if (targetCredentials == null)
					{
						AnalyseLocalFolder(DocumentFile.fromTreeUri(contextRef, Uri.parse(targetFolder)), deletedFileSize, deletedFileList);
					}
					else
					{
						Log.i("STORAGE", "perofminr ganalysis");
						// Analyse SMB folder
						AnalyseSMBFolder("smb://" + targetFolder + "/", targetCredentials, deletedFileSize, deletedFileList);
					}
				}*/


			/*try
			{


				// Check if the the "delete target contents" flag is set
				if(deleteTargetContents)
				{
					// Check if the transfer is being performed
					if(!performTransfer)
					{
						// Estimate what will be deleted
						//targetCredentials == null ? EstimateLocalFolderContents(DocumentFile.fromTreeUri(contextRef, Uri.parse(targetFolder))) : EstimateSMBFolderContents("smb://"+targetFolder+"/");


						// List what will be deleted

						// Estimate what will be transferred
					}
					else
					{
						// Determine whether the target folder is an SMB share or not
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

						// Perform transfer
					}
				}
				else
				{
					// Check if the transfer is being performed
					if(!performTransfer)
					{
						// Estimate what will be changed

						// List what will be changed
					}
					else
					{
						// Perform transfer
					}
				}*/





				/*// Check if the the "delete target contents" flag is set
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
				}*/
			} catch (Exception exception)
			{
				// Check if the synchronisation was manually cancelled
				if(isCancelled())
				{
					callingActivityInterface.OnSynchronisationFailed("MANUALLY_CANCELLED");
				}
				else
				{
					// Log details of the exception and return its message
					Log.i("STORAGE", "Exxxxxceptional");
					exception.printStackTrace();
					callingActivityInterface.OnSynchronisationFailed(exception.getMessage());
				}
			}
		}
		return null;
	}

	@Override
	protected void onPostExecute(Object o)
	{
		super.onPostExecute(o);

		// Report back to the calling activity
		callingActivityInterface.NewOnSyncComplete(performTransfer, addedFileSize, addedFileList, updatedFilesSize, updatedFilesList, deletedFileSize, deletedFileList);

		// Report back to the calling activity
		//callingActivityInterface.OnSynchronisationComplete(totalFiles, filesTransferred, totalBytes, bytesTransferred);
	}

	// Function to test access to a target folder
	public boolean TestAccess(NtlmPasswordAuthentication targetCredentials, String targetFolderPath, boolean needsWriteAccess)
	{
		// Check if the target is an SMB share
		if(targetCredentials != null)
		{
			try
			{
				// Attempt to connect to the target SMB share
				SmbFile smbFile = new SmbFile("smb://"+targetFolderPath, targetCredentials);
				if (!smbFile.canRead())
				{
					return false;
				}
				else if(needsWriteAccess && !smbFile.canWrite())
				{
					return false;
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				return false;
			}
		}
		else
		{
			DocumentFile targetFolder = DocumentFile.fromTreeUri(contextRef, Uri.parse(targetFolderPath));
			// Attempt to connect to the target DocumentFile
			if(!targetFolder.canRead())
			{
				return false;
			}
			else if(needsWriteAccess && !targetFolder.canWrite())
			{
				return false;
			}
		}

		return true;
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

	// Trim trailing slash
	public String TrimTrailingSlash(String original)
	{
		return original.substring(0, original.length() - 1);
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
			Log.i("STORAGE", "During DoesSMBFileFolderExist");
			e.printStackTrace();
		}
		return false;
	}

	void CopyBufferedFile(BufferedInputStream bufferedInputStream, BufferedOutputStream bufferedOutputStream, String currentFileName) throws IOException
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
				callingActivityInterface.OnSynchronisationProgress(filesTransferred, bytesProcessed, currentFileName);
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

		SmbFile smbFile = new SmbFile(targetFolder, globalTargetCredentials);
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

	// Function to analyse the contents of a local folder
	public void AnalyseLocalFolder(DocumentFile sourceFolder, long targetFileSize, ArrayList<AnalysedFile> targetList)
	{
		// Loop through the files & folders in the source location
		for (DocumentFile file : sourceFolder.listFiles())
		{
			if(file.isDirectory())
			{
				AnalyseLocalFolder(file, targetFileSize, targetList);
			}
			else
			{
				AnalysedFile newFile = new AnalysedFile(file.getName(), file.getUri().toString(), file.length(), file.isDirectory());
				targetList.add(newFile);
				targetFileSize += file.length();
			}
		}
	}

	// Function to analyse the contents of an SMB folder
	public void AnalyseSMBFolder(String targetFolder, NtlmPasswordAuthentication targetCredentials, long targetFileSize, ArrayList<AnalysedFile> targetList) throws MalformedURLException, SmbException
	{
		// Connect to the source folder
		SmbFile smbFile = new SmbFile(targetFolder, targetCredentials);
		SmbFile[] fileList = smbFile.listFiles();

		// Loop through the files & folders in the source location
		for (SmbFile file : fileList)
		{
			if(file.isDirectory())
			{
				Log.i("STORAGE", "Found a folder");
				AnalyseSMBFolder(file.getPath(), targetCredentials, targetFileSize, targetList);
			}
			else
			{
				Log.i("STORAGE", "Found a file");
				AnalysedFile newFile = new AnalysedFile(file.getName(), file.getPath(), file.length(), file.isDirectory());
				targetList.add(newFile);
				targetFileSize += file.length();
			}
		}
	}

	// Function to synchronise two folders
	public void SynchroniseFolders(DocumentFile sourceFolder, String sourceFolderPath, NtlmPasswordAuthentication sourceFolderCredentials, DocumentFile targetFolder, String targetFolderPath, NtlmPasswordAuthentication targetFolderCredentials, boolean createFiles) throws IOException
	{
		Log.i("STORAGE", "SynchroniseFolders called on "+sourceFolderPath+" and "+targetFolderPath+" (targetFolder "+(targetFolder == null ? "is" : "is not")+" null");
		Log.i("STORAGE", targetFolder == null ? "targetFolder is null" : "targetFolder is not null");
		// If the source is local
		if(sourceFolderCredentials == null)
		{
			// Loop through the source files
			for (DocumentFile sourceFile : sourceFolder.listFiles())
			{
				Log.i("STORAGE", "checking local file " + sourceFile.getName());
				SynchroniseFiles(sourceFolderCredentials, sourceFile, null, sourceFolderPath, targetFolderCredentials, targetFolder, targetFolderPath, createFiles);
			}
		}
		// If the source is remote
		else
		{
			// Obtain the target SMB folder
			SmbFile targetSMBFolder = new SmbFile("smb://" + sourceFolderPath + "/", sourceFolderCredentials);

			// Loop through the source files
			for (SmbFile sourceFile : targetSMBFolder.listFiles())
			{
				Log.i("STORAGE", "checking SMB file " + sourceFile.getName());
				SynchroniseFiles(sourceFolderCredentials, null, sourceFile, sourceFolderPath, targetFolderCredentials, targetFolder, targetFolderPath, createFiles);
			}
		}

		// Check if a perfect copy has been requested
		if(deletionPolicy == DELETION_POLICY_PERFECT_COPY)
		{
			// Check for files in the target folder that need to be marked for deletion / deleted
			DeleteSurplusTargetFiles(sourceFolderCredentials, sourceFolder, sourceFolderPath, targetFolderCredentials, targetFolder, targetFolderPath, createFiles);
		}
	}

	public void SynchroniseFiles(NtlmPasswordAuthentication sourceFolderCredentials, DocumentFile sourceDocumentFile, SmbFile sourceSMBFile, String sourceFolderPath, NtlmPasswordAuthentication targetFolderCredentials, DocumentFile targetFolder, String targetFolderPath, boolean createFiles) throws IOException
	{
		Log.i("STORAGE", "SynchroniseFiles called on "+sourceFolderPath+" and "+targetFolderPath+" (targetFolder "+(targetFolder == null ? "is" : "is not")+" null");

		// Obtain generic data regarding the source file
		boolean sourceIsDirectory = sourceFolderCredentials == null ? sourceDocumentFile.isDirectory() : sourceSMBFile.isDirectory();
		String sourceFileName = sourceFolderCredentials == null ? sourceDocumentFile.getName() : (sourceIsDirectory ? TrimTrailingSlash(sourceSMBFile.getName()) : sourceSMBFile.getName());
		long sourceFileLength = sourceFolderCredentials == null ? sourceDocumentFile.length() : sourceSMBFile.length();

		Log.i("STORAGE", "checking " + sourceFileName);

		// Initialise and obtain the target file
		DocumentFile targetDestinationFile = ((targetFolderCredentials != null || targetFolder == null) ? null : caseInsensitiveFindFile(sourceFileName, targetFolder));
		SmbFile targetDestinationSMBFile = (targetFolderCredentials == null ? null : new SmbFile("smb://" + targetFolderPath + "/" + sourceFileName, targetFolderCredentials));

		// Check if the source file is a directory
		if (sourceIsDirectory)
		{
			// Check if the source folder exists in the destination
			if (targetFolderCredentials == null ? targetDestinationFile == null : !targetDestinationSMBFile.exists())
			{
				// Record the addition
				addedFileList.add(new AnalysedFile(sourceFileName, targetFolderPath, 0, sourceIsDirectory));

				// If the folder needs to be created
				if(createFiles)
				{
					// Create the folder
					if (targetFolderCredentials == null)
					{
						targetDestinationFile = targetFolder.createDirectory(sourceFileName);
						Log.i("STORAGE", "targetFolder (sort of) set to "+targetDestinationFile.getUri().toString());
					}
					else
					{
						targetDestinationSMBFile.mkdir();
					}
				}
			}

			// Re-run the function on the folder
			SynchroniseFolders(sourceDocumentFile, sourceFolderPath + "/" + sourceFileName, sourceFolderCredentials, targetDestinationFile, targetFolderPath + "/" + sourceFileName, targetFolderCredentials, createFiles);
		}
		else
		{
			// Check if the file exists and is identical in the destination folder
			if (targetFolderCredentials == null ? (targetDestinationFile != null && sourceFileLength == targetDestinationFile.length()) : (targetDestinationSMBFile.exists() && sourceFileLength == targetDestinationSMBFile.length()))
			{
				Log.i("STORAGE", "Files are the same, no need to replace (" + sourceFileName + " and " + (targetFolderCredentials == null ? targetDestinationFile.getName() : targetDestinationSMBFile.getName()) + ")");
			}
			else
			{
				// Check if a non-identical file was found
				if (targetFolderCredentials == null ? (targetDestinationFile != null) : (targetDestinationSMBFile.exists()))
				{
					Log.i("STORAGE", "Non-identical file found");

					// Record the update
					updatedFilesList.add(new AnalysedFile(sourceFileName, targetFolderPath, sourceFileLength, sourceIsDirectory));
					updatedFilesSize += sourceFileLength;

					// If the file needs to be deleted
					if (createFiles)
					{
						Log.i("STORAGE", "Deleting non-identical file");
						// Delete the existing file
						if (targetFolderCredentials == null)
						{
							targetDestinationFile.delete();
						}
						else
						{
							targetDestinationSMBFile.delete();
						}
					}
				}
				else
				{
					Log.i("STORAGE", "File not found");

					// Record the addition
					addedFileList.add(new AnalysedFile(sourceFileName, targetFolderPath, sourceFileLength, sourceIsDirectory));
					addedFileSize += sourceFileLength;
				}

				// If the file needs to be created
				if (createFiles)
				{
					Log.i("STORAGE", "Creating file");

					// Initialise the transfer streams
					BufferedInputStream inputStream = new BufferedInputStream(globalSourceCredentials == null ? contextRef.getContentResolver().openInputStream(sourceDocumentFile.getUri()) : new BufferedInputStream(sourceSMBFile.getInputStream()));
					BufferedOutputStream outputStream;

					// If the target is a local folder
					if (targetFolderCredentials == null)
					{
						// Get the source file's type
						String sourceFileType = sourceFolderCredentials == null ? MimeTypeMap.getSingleton().getExtensionFromMimeType(contextRef.getContentResolver().getType(sourceDocumentFile.getUri())) : GetMimeType(sourceFileName);

						// Create the new (empty) file
						DocumentFile newFile = targetFolder.createFile(sourceFileType, sourceFileName);

						// Set the output to the target folder
						outputStream = new BufferedOutputStream(contextRef.getContentResolver().openOutputStream(newFile.getUri()));
					}
					else
					{
						// Set the output to the target SMB folder
						outputStream = new BufferedOutputStream(targetDestinationSMBFile.getOutputStream());
					}

					// Copy the file
					CopyBufferedFile(inputStream, outputStream, sourceFileName);

					Log.i("STORAGE", "File created.");
				}
			}
		}
	}

	public void DeleteSurplusTargetFiles(NtlmPasswordAuthentication sourceFolderCredentials, DocumentFile sourceFolder, String sourceFolderPath, NtlmPasswordAuthentication destinationFolderCredentials, DocumentFile destinationFolder, String destinationFolderPath, boolean createFiles) throws IOException
	{
		Log.i("STORAGE", "DeleteSurplusTargetFiles called with "+sourceFolderPath+" and "+destinationFolderPath);

		// If the destination is local and exists (this function may have been called after pre-check of a folder synchronisation)
		if(destinationFolderCredentials == null && destinationFolder != null)
		{
			// Loop through the files in the destination folder
			for (DocumentFile destinationFile : destinationFolder.listFiles())
			{
				Log.i("STORAGE", "Checking if "+destinationFile.getName()+" needs deleting.");

				// Check if the file in question is not present in the source location
				if ((sourceFolderCredentials == null && caseInsensitiveFindFile(destinationFile.getName(), sourceFolder) == null) || (sourceFolderCredentials != null && !new SmbFile("smb://" + sourceFolderPath + "/"+destinationFile.getName(), sourceFolderCredentials).exists()))
				{
					Log.i("STORAGE", "Deleting " + destinationFile.getName());
					// Record the file to be deleted
					deletedFileList.add(new AnalysedFile(destinationFile.getName(), destinationFolderPath, destinationFile.length(), destinationFile.isDirectory()));
					deletedFileSize += destinationFile.length();

					// Check if the file is a directory
					if(destinationFile.isDirectory())
					{
						// If so, recursively search the directory for files to add to the deletions list
						RecursivelyListDocumentFiles(destinationFile, deletedFileList);
					}

					// If the transfer is being performed
					if (createFiles)
					{
						// Delete the file
						destinationFile.delete();
					}
				}
			}
		}
		// If the target is an SMB share
		else if(destinationFolderCredentials != null)
		{
			// Obtain the target SMB folder
			SmbFile targetSMBFolder = new SmbFile("smb://" + destinationFolderPath + "/", destinationFolderCredentials);

			// Check if the target SMB folder exists (this function may have been called after pre-check of a folder synchronisation)
			if(targetSMBFolder.exists())
			{
				// Loop through the files in the destination folder
				SmbFile[] fileList = targetSMBFolder.listFiles();
				for (SmbFile destinationFile : fileList)
				{
					// Trim the file name (but only if it's a directory
					String fileName = destinationFile.isDirectory() ? TrimTrailingSlash(destinationFile.getName()) : destinationFile.getName();

					Log.i("STORAGE", "Checking if " + fileName + " needs deleting.");

					// Check if the file in question is not present in the source location
					if ((sourceFolderCredentials == null && caseInsensitiveFindFile(fileName, sourceFolder) == null) || (sourceFolderCredentials != null && !new SmbFile("smb://" + sourceFolderPath + "/" + destinationFile.getName(), sourceFolderCredentials).exists()))
					{
						Log.i("STORAGE", "Deleting " + destinationFile.getName()+" ("+destinationFolderPath+")");
						// Record the file to be deleted
						deletedFileList.add(new AnalysedFile(fileName, "smb://"+destinationFolderPath, destinationFile.length(), destinationFile.isDirectory()));
						deletedFileSize += destinationFile.length();

						// Check if the file is a directory
						if (destinationFile.isDirectory())
						{
							// If so, recursively search the directory for files to add to the deletions list
							RecursivelyListSMBFiles(destinationFile, deletedFileList);
						}

						// If the transfer is being performed
						if (createFiles)
						{
							// Delete the file
							destinationFile.delete();
						}
					}
				}
			}
		}
	}


	// Function to recursively list the contents of a DocumentFile folder
	public void RecursivelyListDocumentFiles(DocumentFile sourceFolder, ArrayList<AnalysedFile> deletedFileList) throws IOException
	{
		// Loop through the folder's contents
		for (DocumentFile file : sourceFolder.listFiles())
		{
			// Record the file / folder's deletion
			AnalysedFile newFile = new AnalysedFile(file.getName(), sourceFolder.getUri().toString(), file.length(), file.isDirectory());
			deletedFileList.add(newFile);
			deletedFileSize += file.length();

			// Check if file is a directory
			if (file.isDirectory())
			{
				// Re-run the function
				RecursivelyListDocumentFiles(file, deletedFileList);
			}
		}
	}

	// Function to recursively list the contents of an SMB folder
	public void RecursivelyListSMBFiles(SmbFile sourceFolder, ArrayList<AnalysedFile> deletedFileList) throws IOException
	{
		// Loop through the folder's contents
		for (SmbFile file : sourceFolder.listFiles())
		{
			// Record the file / folder's deletion
			AnalysedFile newFile = new AnalysedFile((file.isDirectory() ? TrimTrailingSlash(file.getName()) : file.getName()), TrimTrailingSlash(sourceFolder.getPath()), file.length(), file.isDirectory());
			deletedFileList.add(newFile);
			deletedFileSize += file.length();

			// Check if file is a directory
			if (file.isDirectory())
			{
				// Re-run the function
				RecursivelyListSMBFiles(file, deletedFileList);
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
		SmbFile smbFile = new SmbFile(sourceFolder, globalSourceCredentials);
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
				DocumentFile wouldBeFolder = caseInsensitiveFindFile(file.getName(), targetLocation);
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
				DocumentFile wouldBeFile = caseInsensitiveFindFile(file.getName(), targetLocation);
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
					CopyBufferedFile(new BufferedInputStream(contextRef.getContentResolver().openInputStream(file.getUri())), new BufferedOutputStream(contextRef.getContentResolver().openOutputStream(newFile.getUri())), file.getName());
					bytesTransferred += file.length();
					Log.i("STORAGE", "File created.");
				}

				// Increment the number of files processed
				filesProcessed++;

				// Report back to the calling activity
				callingActivityInterface.OnSynchronisationProgress(filesProcessed, bytesProcessed, file.getName());
			}
		}
	}

	// Function to duplicate all files and folders from a local folder to an SMB one
	void DuplicateLocalFolderToSMBFolder(DocumentFile sourceFolder, String targetFolder) throws IOException
	{
		// Connect to the SMB folder
		SmbFile smbFile = new SmbFile(targetFolder, globalTargetCredentials);
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
					SmbFile newFolder = new SmbFile(targetFolder+file.getName()+"/", globalTargetCredentials);
					newFolder.mkdir();
				}

				// Then re-run the function on that directory
				DuplicateLocalFolderToSMBFolder(file, targetFolder+file.getName()+"/");
			}
			else
			{
				// Create the new SMB File
				SmbFile wouldBeFile = new SmbFile(targetFolder+file.getName(), globalTargetCredentials);

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
					CopyBufferedFile(new BufferedInputStream(contextRef.getContentResolver().openInputStream(file.getUri())), new BufferedOutputStream(wouldBeFile.getOutputStream()), file.getName());
					bytesTransferred += file.length();
					Log.i("STORAGE", "File created.");
				}

				// Increment the number of files processed
				filesProcessed++;

				// Report back to the calling activity
				callingActivityInterface.OnSynchronisationProgress(filesProcessed, bytesProcessed, file.getName());
			}
		}
	}

	// Function to duplicate all files and folders from an SMB folder to a local one
	void DuplicateSMBFolderToLocalFolder(String sourceFolder, DocumentFile targetFolder) throws IOException
	{
		// Loop through the files & folders in the source location
		SmbFile smbFile = new SmbFile(sourceFolder, globalSourceCredentials);
		SmbFile[] fileList = smbFile.listFiles();
		for (SmbFile file : fileList)
		{
			// Check if the file / folder is a folder
			if (file.isDirectory())
			{
				// Check if the source folder already exists in the target folder
				String folderName = file.getName().substring(0, (file.getName().length() -1));
				DocumentFile wouldBeFolder = caseInsensitiveFindFile(folderName, targetFolder);
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
				DocumentFile wouldBeFile = caseInsensitiveFindFile(file.getName(), targetFolder);
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
					CopyBufferedFile(new BufferedInputStream(file.getInputStream()), new BufferedOutputStream(contextRef.getContentResolver().openOutputStream(newFile.getUri())), file.getName());
					bytesTransferred += file.length();
					Log.i("STORAGE", "File '" + file.getName() + "'created.");
				}

				// Increment the number of files processed
				filesProcessed++;

				// Report back to the calling activity
				callingActivityInterface.OnSynchronisationProgress(filesProcessed, bytesProcessed, file.getName());
			}
		}
	}

	// Function to duplicate all files and folders from an SMB folder to another SMB one
	void DuplicateSMBFolderToSMBFolder(String sourceFolder, String targetFolder) throws IOException
	{
		// Connect to the sourceSMB folder
		SmbFile sourceSmbFile = new SmbFile(sourceFolder, globalSourceCredentials);
		SmbFile[] sourceFileList = sourceSmbFile.listFiles();

		// Connect to the target SMB folder
		SmbFile targetSmbFile = new SmbFile(targetFolder, globalTargetCredentials);
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
					SmbFile newFolder = new SmbFile(targetFolder+file.getName()+"/", globalTargetCredentials);
					newFolder.mkdir();
				}

				// Then re-run the function on that directory
				DuplicateSMBFolderToSMBFolder(file.getPath(), targetFolder+file.getName()+"/");
			}
			else
			{
				// Create the new SMB File
				SmbFile wouldBeFile = new SmbFile(targetFolder+file.getName(), globalTargetCredentials);

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
					CopyBufferedFile(new BufferedInputStream(file.getInputStream()), new BufferedOutputStream(wouldBeFile.getOutputStream()), file.getName());
					bytesTransferred += file.length();
					Log.i("STORAGE", "File created.");
				}

				// Increment the number of files processed
				filesProcessed++;

				// Report back to the calling activity
				callingActivityInterface.OnSynchronisationProgress(filesProcessed, bytesProcessed, file.getName());
			}
		}
	}

	public DocumentFile caseInsensitiveFindFile(String fileName, DocumentFile folder)
	{
		Log.i("STORAGE", "caseInsensitiveFindFile called with "+folder.getUri().toString()+" and "+fileName);
		for (DocumentFile document : folder.listFiles())
		{
			if (fileName.toUpperCase().equals(document.getName().toUpperCase()))
			{
				return document;
			}
		}
		Log.i("STORAGE", "After exception?");
		return null;
	}

	// MainActivity interface
	public interface ContextualInterface
	{
		public void OnSynchronisationProgress(int filesProcessed, int bytesProcessed, String currentFile);
		public void OnSynchronisationComplete(int totalFiles, int filesTransferred, int totalBytes, int bytesTransferred);
		public void OnSynchronisationFailed(String error);

		public void NewOnSyncComplete(boolean performTransfer, long addedFileSize, ArrayList<AnalysedFile> addedFileList, long updatedFileSize, ArrayList<AnalysedFile> updatedFileList, long deletedFileSize, ArrayList<AnalysedFile> deletedFileList);
	}
}
