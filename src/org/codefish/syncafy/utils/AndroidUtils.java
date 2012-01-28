/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.codefish.syncafy.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import org.codefish.syncafy.AppConstants;
import org.codefish.syncafy.R;

/**
 * utilities for android
 * @author Matthew
 */
public class AndroidUtils {
    /**
     * Checks if we have a valid Internet Connection on the device.
     * @param ctx
     * @return True if device has internet
     *
     * Code from: http://www.androidsnippets.org/snippets/131/
     */
    public static boolean haveInternet(Context ctx) {

        NetworkInfo info = (NetworkInfo) ((ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        if (info == null || !info.isConnected()) {
            return false;
        }
        if (info.isRoaming()) {
            // here is the roaming option you can change it if you want to
            // disable internet while roaming, just return false
            return true;
        }
        return true;
    }

    /**
     * show the welcome message if it has not already been shown
     */
    public static void showWelcomeMessage(Context ctx) {
        String pkg = ctx.getPackageName();
        //get the version number
        String versionNumber;
        try {
            versionNumber = ctx.getPackageManager().getPackageInfo(pkg, 0).versionName;
        } catch (NameNotFoundException ex) {
            Log.w("WELCOMEMSG", "Error getting version number"+ex.getMessage());
            versionNumber="?";
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        //if we have already shown the mesage then return (using version number for uniqueness from each version)
        if(sharedPreferences.getBoolean(AppConstants.SHOWN_WELCOME_PREF+versionNumber, false)){
            return;
        }

        AlertDialog.Builder adb = new AlertDialog.Builder(ctx);
        adb.setTitle(ctx.getString(R.string.app_name)+" v"+versionNumber);
        adb.setMessage(ctx.getResources().getString(R.string.welcome_message));
        adb.setPositiveButton("Ok", null);
        adb.show();

        //save in shared prefs that we have shown the mesage
        sharedPreferences.edit().putBoolean(AppConstants.SHOWN_WELCOME_PREF+versionNumber, true).commit();
    }

    public static void shareContent(Context ctx, String shareMessage){
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        shareIntent.setType("text/plain");//text/plain to show all options to share this
        try{
            ctx.startActivity(shareIntent);
        }catch(Exception e){
            Log.e("SHARE", e.getMessage());
        }
    }

    /**
     * Create an email intent with content
     * @param ctx
     * @param to
     * @param cc
     * @param bcc
     * @param subject
     * @param message
     */
    public static void createEmail(Context ctx, String[] to, String[] cc, String[] bcc, String subject, String message){
        //Create an email to send
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

        emailIntent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);//important to create a new email task

        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, to);
        emailIntent.putExtra(android.content.Intent.EXTRA_CC, cc);
        emailIntent.putExtra(android.content.Intent.EXTRA_BCC, bcc);

        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);

        emailIntent.setType("plain/text");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);

        try{
            ctx.startActivity(emailIntent);
        }catch(Exception e){
            Log.e("SNDEMAIL", e.getMessage());
        }
    }
}
