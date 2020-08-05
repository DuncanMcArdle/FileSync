package com.example.duncan.filesync;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.net.MalformedURLException;
import java.net.UnknownHostException;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class TestSMBShare extends AsyncTask
{
	private String SMBShareURL;
	private NtlmPasswordAuthentication SMBShareAuthentication;
	private String testResult;

    public interface TestResponse
	{
		void TestCompleted(String result);
	}

	private TestResponse delegate;

    // Initialisation
    public TestSMBShare(Context inputContext, String inputSMBShareURL, NtlmPasswordAuthentication inputSMBShareAuthentication, TestResponse delegate)
    {
		// Store the passed in details
		SMBShareURL = "smb://"+inputSMBShareURL;
		SMBShareAuthentication = inputSMBShareAuthentication;
		this.delegate = delegate;
	}

    @Override
    protected Object doInBackground(Object[] objects)
    {
		try
        {
			// Artificially delay (to allow the loader to show)
			Thread.sleep(500);

        	SmbFile testFile = new SmbFile(SMBShareURL, SMBShareAuthentication);
        	testFile.connect();
			testResult = "SUCCESS";
			Log.i("STORAGE", "Connected successfully.");
		}
        catch(SmbAuthException e)
		{
			testResult = "AUTHENTICATION";
			e.printStackTrace();
		}
		catch(SmbException e)
		{
			testResult = "SMB_EXCEPTION";
			e.printStackTrace();
		}
		catch(MalformedURLException e)
		{
			testResult = "MALFORMED_URL_EXCEPTION";
			e.printStackTrace();
		}
		catch(UnknownHostException e)
		{
			testResult = "UNKNOWN_HOST_EXCEPTION";
			e.printStackTrace();
		}
        catch (Exception e)
        {
			testResult = "UNKNOWN_EXCEPTION";
            e.printStackTrace();
        }
        return null;
    }

	@Override
	protected void onPostExecute(Object o)
	{
		delegate.TestCompleted(testResult);
	}
}
