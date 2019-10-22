/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

package com.example.duncan.filesync;

import android.content.Context;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;
import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

public class MD5
{
    private static final String TAG = "MD5";

    public static String calculateSMBMD5(SmbFile file)
    {
        Log.i("STORAGE", "Calculating MD5 for SMB file");

        InputStream is = null;
        try
        {
            is = new SmbFileInputStream(file);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // Calculate the MD5
        String md5 = calculateMD5(is);

        Log.i("STORAGE", "Returning MD5 '"+md5+"'");
        return md5;
    }

    public static String calculateFileMD5(Uri targetURI, Context context)
    {
        Log.i("STORAGE", "Calculating MD5 for local file");

        InputStream is = null;
        try
        {
            is = new BufferedInputStream(context.getContentResolver().openInputStream(targetURI));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // Calculate the MD5
        String md5 = calculateMD5(is);

        Log.i("STORAGE", "Returning MD5 '"+md5+"'");
        return md5;
    }

    public static String calculateMD5(InputStream is)
    {
        Log.i("STORAGE", "InputStream passed to MD5 calculator");

        try
        {
            //String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(is);
            String md5 = new String(Hex.encodeHex(DigestUtils.md5(is)));
            Log.i("STORAGE", "New MD5: "+md5);
            return md5;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;

        /*MessageDigest digest;
        try
        {
            digest = MessageDigest.getInstance("MD5");
        } catch (Exception e)
        {
            Log.e(TAG, "Exception while getting digest", e);
            e.printStackTrace();
            return null;
        }

        Log.i("STORAGE", "About read in file (MD5)");

        byte[] buffer = new byte[8192];
        int read;
        try
        {
            while ((read = is.read(buffer)) > 0)
            {
                digest.update(buffer, 0, read);
                Log.i("STORAGE", "updating dsigest");
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);

            // Fill to 32 chars
            output = String.format("%32s", output).replace(' ', '0');
            return output;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException("Unable to process file for MD5", e);
        }
        finally
        {
            try
            {
                is.close();
            }
            catch (IOException e)
            {
                Log.e(TAG, "Exception on closing MD5 input stream", e);
            }
        }*/
    }
}