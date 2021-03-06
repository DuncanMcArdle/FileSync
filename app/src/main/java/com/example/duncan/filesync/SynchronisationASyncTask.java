package com.example.duncan.filesync;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

public class SynchronisationASyncTask extends AsyncTask
{
	// Miscellaneous variables
	private Context contextRef;
	private String globalSourceFolder;
	private NtlmPasswordAuthentication globalSourceCredentials;
	private String globalTargetFolder;
	private NtlmPasswordAuthentication globalTargetCredentials;
	private boolean performTransfer;
	public ContextualInterface callingActivityInterface;

	// Synchronisation tracking variables
	private int filesTransferred = 0;
	private int bytesProcessed = 0;

	// Tracking variables
	private int addedFileSize;
	private ArrayList<AnalysedFile> addedFileList;
	private int updatedFilesSize;
	private ArrayList<AnalysedFile> updatedFilesList;
	private int deletedFileSize;
	private ArrayList<AnalysedFile> deletedFileList;

	// Deletion policies
	private final int DELETION_POLICY_PERFECT_COPY = 0;
	private int deletionPolicy;

	public SynchronisationASyncTask(Context callingContext, String sourceFolderInput, NtlmPasswordAuthentication sourceCredentialsInput, String targetFolderInput, NtlmPasswordAuthentication targetCredentialsInput, int deletionPolicyInput, boolean performTransferInput)
	{
		// Store the passed in data
		contextRef = callingContext;
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
		// Test access to the synchronisation target
		else if(!TestAccess(globalTargetCredentials, globalTargetFolder, true))
		{
			callingActivityInterface.OnSynchronisationFailed("TARGET_ACCESS");
		}
		else
		{
			try
			{
				// Initialise file lists
				addedFileList = new ArrayList<>();
				updatedFilesList = new ArrayList<>();
				deletedFileList = new ArrayList<>();

				// Attempt to synchronise the folders
				SynchroniseFolders(globalSourceCredentials != null ? null : DocumentFile.fromTreeUri(contextRef, Uri.parse(globalSourceFolder)), globalSourceFolder, globalSourceCredentials, globalTargetCredentials != null ? null : DocumentFile.fromTreeUri(contextRef, Uri.parse(globalTargetFolder)), globalTargetFolder, globalTargetCredentials, performTransfer);
			}
			catch (Exception exception)
			{
				// Check if the synchronisation was manually cancelled
				if(isCancelled())
				{
					callingActivityInterface.OnSynchronisationFailed("MANUALLY_CANCELLED");
				}
				else
				{
					// Log details of the exception and return its message
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
		callingActivityInterface.OnSynchronisationComplete(performTransfer, addedFileSize, addedFileList, updatedFilesSize, updatedFilesList, deletedFileSize, deletedFileList);
	}

	// Function to test access to a target folder
	private boolean TestAccess(NtlmPasswordAuthentication targetCredentials, String targetFolderPath, boolean needsWriteAccess)
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
			// Create a target DocumentFile
			DocumentFile targetFolder = DocumentFile.fromTreeUri(contextRef, Uri.parse(targetFolderPath));

			// Check if the target file can be read
			if(!targetFolder.canRead())
			{
				return false;
			}
			// Check if the target file needs write access and if so, can be written to
			else if(needsWriteAccess && !targetFolder.canWrite())
			{
				return false;
			}
		}

		return true;
	}

	// Function to determine the MIME type of a file based on its extension
	private static String GetMimeType(String url)
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
	private String TrimTrailingSlash(String original)
	{
		return original.substring(0, original.length() - 1);
	}

	// Copy a file from one location to another
	private void CopyBufferedFile(BufferedInputStream bufferedInputStream, BufferedOutputStream bufferedOutputStream, String currentFileName) throws IOException
	{
		try (BufferedInputStream in = bufferedInputStream; BufferedOutputStream out = bufferedOutputStream)
		{
			byte[] buf = new byte[1024];
			int nosRead;
			while ((nosRead = in.read(buf)) != -1)
			{
				// Check if the synchronisation has been cancelled
				if(isCancelled())
				{
					callingActivityInterface.OnSynchronisationFailed("MANUALLY_CANCELLED");
					break;
				}

				out.write(buf, 0, nosRead);

				// Increment the number of bytes processed
				bytesProcessed += nosRead;

				// Report back to the calling activity
				callingActivityInterface.OnSynchronisationProgress(filesTransferred, bytesProcessed, currentFileName);
			}
		}

		// Increment the number of files copied
		filesTransferred++;
	}

	// Function to synchronise two folders
	private void SynchroniseFolders(DocumentFile sourceFolder, String sourceFolderPath, NtlmPasswordAuthentication sourceFolderCredentials, DocumentFile targetFolder, String targetFolderPath, NtlmPasswordAuthentication targetFolderCredentials, boolean createFiles) throws IOException
	{
		// If the source is local
		if(sourceFolderCredentials == null)
		{
			// Loop through the source files
			for (DocumentFile sourceFile : sourceFolder.listFiles())
			{
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

	// Synchronise the contents of a source destination to a target destination
	private void SynchroniseFiles(NtlmPasswordAuthentication sourceFolderCredentials, DocumentFile sourceDocumentFile, SmbFile sourceSMBFile, String sourceFolderPath, NtlmPasswordAuthentication targetFolderCredentials, DocumentFile targetFolder, String targetFolderPath, boolean createFiles) throws IOException
	{
		// Obtain generic data regarding the source file
		boolean sourceIsDirectory = sourceFolderCredentials == null ? sourceDocumentFile.isDirectory() : sourceSMBFile.isDirectory();
		String sourceFileName = sourceFolderCredentials == null ? sourceDocumentFile.getName() : (sourceIsDirectory ? TrimTrailingSlash(sourceSMBFile.getName()) : sourceSMBFile.getName());
		long sourceFileLength = sourceFolderCredentials == null ? sourceDocumentFile.length() : sourceSMBFile.length();

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
				// Files are the same, no need to replace
			}
			else
			{
				// Check if a non-identical file was found
				if (targetFolderCredentials == null ? (targetDestinationFile != null) : (targetDestinationSMBFile.exists()))
				{
					// Record the update
					updatedFilesList.add(new AnalysedFile(sourceFileName, targetFolderPath, sourceFileLength, sourceIsDirectory));
					updatedFilesSize += sourceFileLength;

					// If the file needs to be deleted
					if (createFiles)
					{
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
					// Record the addition
					addedFileList.add(new AnalysedFile(sourceFileName, targetFolderPath, sourceFileLength, sourceIsDirectory));
					addedFileSize += sourceFileLength;
				}

				// If the file needs to be created
				if (createFiles)
				{
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
				}
			}
		}
	}

	// Delete files from the target folder that are not found in the source folder
	private void DeleteSurplusTargetFiles(NtlmPasswordAuthentication sourceFolderCredentials, DocumentFile sourceFolder, String sourceFolderPath, NtlmPasswordAuthentication destinationFolderCredentials, DocumentFile destinationFolder, String destinationFolderPath, boolean createFiles) throws IOException
	{
		// If the destination is local and exists (this function may have been called after pre-check of a folder synchronisation)
		if(destinationFolderCredentials == null && destinationFolder != null)
		{
			// Loop through the files in the destination folder
			for (DocumentFile destinationFile : destinationFolder.listFiles())
			{
				// Check if the file in question is not present in the source location
				if ((sourceFolderCredentials == null && caseInsensitiveFindFile(destinationFile.getName(), sourceFolder) == null) || (sourceFolderCredentials != null && !new SmbFile("smb://" + sourceFolderPath + "/"+destinationFile.getName(), sourceFolderCredentials).exists()))
				{
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

					// Check if the file in question is not present in the source location
					if ((sourceFolderCredentials == null && caseInsensitiveFindFile(fileName, sourceFolder) == null) || (sourceFolderCredentials != null && !new SmbFile("smb://" + sourceFolderPath + "/" + destinationFile.getName(), sourceFolderCredentials).exists()))
					{
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
	private void RecursivelyListDocumentFiles(DocumentFile sourceFolder, ArrayList<AnalysedFile> deletedFileList)
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
	private void RecursivelyListSMBFiles(SmbFile sourceFolder, ArrayList<AnalysedFile> deletedFileList) throws IOException
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

	// Function to case-insensitively find a DocumentFile
	private DocumentFile caseInsensitiveFindFile(String fileName, DocumentFile folder)
	{
		// Loop through the folder's files
		for (DocumentFile document : folder.listFiles())
		{
			// Check if the file matches the one being looked for
			if (fileName.toUpperCase().equals(document.getName().toUpperCase()))
			{
				return document;
			}
		}
		return null;
	}

	// MainActivity interface
	public interface ContextualInterface
	{
		void OnSynchronisationProgress(int filesProcessed, int bytesProcessed, String currentFile);
		void OnSynchronisationFailed(String error);
		void OnSynchronisationComplete(boolean performTransfer, long addedFileSize, ArrayList<AnalysedFile> addedFileList, long updatedFileSize, ArrayList<AnalysedFile> updatedFileList, long deletedFileSize, ArrayList<AnalysedFile> deletedFileList);
	}
}
