package com.example.duncan.filesync;

class AnalysedFile
{
	String fileName;
	String filePath;
	long fileSize;
	boolean isAFolder;

	public AnalysedFile(String fileNameInput, String filePathInput, long fileSizeInput, boolean isAFolderInput)
	{
		fileName = fileNameInput;
		filePath = filePathInput;
		fileSize = fileSizeInput;
		isAFolder = isAFolderInput;
	}
}
