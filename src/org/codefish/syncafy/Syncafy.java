/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.codefish.syncafy;

import java.net.URISyntaxException;
import org.codefish.syncafy.filetransfer.Server;
import org.codefish.syncafy.filetransfer.SFtpServer;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.github.ysamlan.horizontalpager.HorizontalPager;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.codefish.syncafy.filetransfer.DropboxServer;
import org.codefish.syncafy.filetransfer.FTPServer;
import org.codefish.syncafy.utils.AndroidUtils;
import org.openintents.intents.FileManagerIntents;

/**
 *
 * @author Matthew
 */
public class Syncafy extends Activity {

    protected static final int REQUEST_CODE_PICK_FILE_OR_DIRECTORY = 1;
    protected static final int REQUEST_CODE_SCHEDULE = 2;
    private static HorizontalPager file_view, server_view;
    private EditText selectedDirLabel = null;
    private static final int PROGRESS = 0;
    private ProgressDialog progressDialog = null;

    private View currentServer,currentDirectory;
    private List<View> serverViewList = new ArrayList<View>();
    private List<View> directoryViewList = new ArrayList<View>();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);

        //TODO: save the users servers and directories to file to be recalled here for future use

        /* Directory Picker */
        file_view = (HorizontalPager) findViewById(R.id.dirfile_view);
        currentDirectory = View.inflate(this, R.layout.file_form, null);
        directoryViewList.add(currentDirectory);
        file_view.addView(currentDirectory);
        View addDirectoryButton = View.inflate(this, R.layout.add_button, null);
        addDirectoryButton.findViewById(R.id.add_panel).setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                View v = View.inflate(getApplicationContext(), R.layout.file_form, null);
                file_view.addView(v,directoryViewList.size());
                directoryViewList.add(v);                
            }
        });
        file_view.addView(addDirectoryButton);
        file_view.setOnScreenSwitchListener(directorSwitchListener);

        /* Server Picker */
        server_view = (HorizontalPager) findViewById(R.id.server_view);
        currentServer = View.inflate(this, R.layout.server_form, null);
        serverViewList.add(currentServer);
        server_view.addView(currentServer);
        View addServerButton = View.inflate(this, R.layout.add_button, null);
        addServerButton.findViewById(R.id.add_panel).setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                View v = View.inflate(getApplicationContext(), R.layout.server_form, null);
                server_view.addView(v,serverViewList.size());
                serverViewList.add(v);                
            }
        });
        server_view.addView(addServerButton);
        file_view.setOnScreenSwitchListener(serverSwitchListener);

        //show the welcome message
        AndroidUtils.showWelcomeMessage(this);
    }
    // Define the Handler that receives messages from the thread and update the progress
    final Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            Bundle b = msg.getData();
            int total = b.getInt(AppConstants.TOTAL_FILES, -1);
            if (total != -1) {
                progressDialog.setProgress(total);
                if (total >= progressDialog.getMax()) {
                    progressDialog.dismiss();
                }
            } else {//we are setting the maximum
                int maxFiles = b.getInt(AppConstants.MAX_FILES, -1);
                progressDialog.setMax(maxFiles);
                if (maxFiles == -1) {//we are getting an message from the thread
                    String message = b.getString(AppConstants.MESSAGE);
                    if (message != null) {//show the message in a dialog
                        AlertDialog.Builder adb = new AlertDialog.Builder(Syncafy.this);
                        adb.setMessage(message);
                        adb.setPositiveButton("Ok", null);
                        adb.show();
                    } else {
                        String toast = b.getString(AppConstants.TOAST);
                        if (toast != null) {
                            Toast.makeText(Syncafy.this, toast, Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        }
    };

    /**
     * sync the contents of the folder to the ftp server
     * @param v
     */
    public void sync(View v) {
        if(!AndroidUtils.haveInternet(this)){//check we have an internet connection
            Toast.makeText(this, "No internet connectivity.", Toast.LENGTH_LONG).show();
            return;
        }

        if(currentServer==null || currentDirectory==null){
            Toast.makeText(this, "A fatal error has occured. Please send feedback.", Toast.LENGTH_LONG).show();
            return;
        }

        /*get the visible panels*/
        //client
        selectedDirLabel = (EditText) currentDirectory.findViewById(R.id.selectedDirectory);
        CheckBox isRecursive = (CheckBox) currentDirectory.findViewById(R.id.isRecursive);

        //server
        RadioButton isSftp = (RadioButton) currentServer.findViewById(R.id.radio_sftp);
        RadioButton isFtp = (RadioButton) currentServer.findViewById(R.id.radio_ftp);
        RadioButton isDropbox = (RadioButton) currentServer.findViewById(R.id.radio_dropbox);
        EditText serverAddress = (EditText) currentServer.findViewById(R.id.server_address);
        EditText serverPort = (EditText) currentServer.findViewById(R.id.server_port);
        EditText username = (EditText) currentServer.findViewById(R.id.ftp_user);
        EditText password = (EditText) currentServer.findViewById(R.id.ftp_password);
        final EditText serverDirectory = (EditText) currentServer.findViewById(R.id.server_directory);

        //check that we have all the info we need
        if (StringUtils.isNotBlank(serverAddress.getText().toString())
                && StringUtils.isNotBlank(serverPort.getText().toString())
                && StringUtils.isNotBlank(username.getText().toString())
                && StringUtils.isNotBlank(password.getText().toString())
                && StringUtils.isNotBlank(serverDirectory.getText().toString())) {

            final String baseFile = selectedDirLabel.getText().toString();

            Server tmpServer;

            if (StringUtils.isNotBlank(baseFile)) {//SFTP
                if (isSftp.isChecked()) {
                    try{//instantiate server to an sftp server
                        tmpServer = new SFtpServer(Integer.valueOf(serverPort.getText().toString()).intValue(),
                            new URI(serverAddress.getText().toString()), "tmpServer", baseFile,
                            username.getText().toString(), password.getText().toString(), isRecursive.isChecked());
                    }catch(Exception e){
                        Log.e("sftpinit", e.toString());
                         Toast.makeText(this, "Error starting sftp. Server address not valid.", Toast.LENGTH_LONG).show();
                        return;
                    }
                }else if(isFtp.isChecked()) {//FTP
                    try {
                        //FTP
                        tmpServer = new FTPServer(Integer.valueOf(serverPort.getText().toString()).intValue(), new URI(serverAddress.getText().toString()), "tmpServer", baseFile, username.getText().toString(), password.getText().toString(), isRecursive.isChecked());
                    } catch (URISyntaxException ex) {
                         Log.e("sftpinit", ex.toString());
                         Toast.makeText(this, "Error starting ftp. Server address not valid.", Toast.LENGTH_LONG).show();
                        return;
                    }
                }else if(isDropbox.isChecked()){
                    //dropbox
                    String email = username.getText().toString();
                    if (email.length() < 5 || email.indexOf("@") < 0 || email.indexOf(".") < 0) {
                        Toast.makeText(this, "Error, invalid e-mail", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String passwordString = password.getText().toString();
                    if (password.length() < 6) {
                        Toast.makeText(this, "Error, password too short", Toast.LENGTH_LONG).show();
                        return;
                    }
                    
                    tmpServer = new DropboxServer("tmpServer", baseFile, email, passwordString, isRecursive.isChecked());
                }else{
                    Toast.makeText(this, "Please select a server type.", Toast.LENGTH_LONG);
                    return;
                }

                final Server server = tmpServer;// make the server final for use in the thread

                try {//start the server thread
                    showDialog(PROGRESS);

                    //create a new thread to to the ftp processing
                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            try {
                                //connect to the ftp server
                                try {
                                    if(!server.connect()){
                                        sendMessage("Failed to connect", AppConstants.MESSAGE);
                                        progressDialog.dismiss();
                                        return;
                                    }
                                } catch (Exception e) {
                                    Log.e("stfpcon", e.toString());
                                    sendMessage("Failed to connect \n\r" + e.toString(), AppConstants.MESSAGE);
                                    progressDialog.dismiss();
                                }
                                //loop through the directory and put all the files
                                server.putFiles(serverDirectory.getText().toString(), handler);
                                progressDialog.dismiss();
                                sendMessage("Finished sending files", AppConstants.TOAST);

                            } catch (Exception e) {
                                Log.e("sftput", e.toString());
                                progressDialog.dismiss();
                                sendMessage("Failed to send files \n\r" + e.toString(), AppConstants.MESSAGE);
                            }
                        }

                        private void sendMessage(String message, String tag) {
                            Message msg = handler.obtainMessage();
                            Bundle b = new Bundle();
                            b.putString(tag, message);
                            msg.setData(b);
                            handler.sendMessage(msg);
                        }
                    };
                    t.setName("Dir: "+baseFile);
                    t.start();//start the thread
                } catch (Exception e) {
                    Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        }else{
            Toast.makeText(this, "Please fill in all the boxes.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * called when the schedule button is clicked
     * brings up the wheel view to schedule ftp syncing
     * @param v
     */
    public void schedule(View v) {
        //TODO: cancel all schedules for this combination, then open the wheel to start a new schedule
        startActivityForResult(new Intent(this, ScheduleWheelView.class),REQUEST_CODE_SCHEDULE);//start the wheel activity
    }

    /**
     * Opens the file manager to pick a directory.
     */
    public void pickDirectory(View v) {
        selectedDirLabel = (EditText) ((RelativeLayout) v.getParent()).findViewById(R.id.selectedDirectory);

        // Note the different intent: PICK_DIRECTORY
        Intent intent = new Intent(FileManagerIntents.ACTION_PICK_DIRECTORY);

        // Set fancy title and button (optional)
        intent.putExtra(FileManagerIntents.EXTRA_TITLE, "Select a directory");
        intent.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT, "Select");

        try {
            startActivityForResult(intent, REQUEST_CODE_PICK_FILE_OR_DIRECTORY);
        } catch (ActivityNotFoundException e) {
            // No compatible file manager was found.
            Toast.makeText(this, "Error starting file manager", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * This is called after the file manager finished.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_PICK_FILE_OR_DIRECTORY:
                if (resultCode == RESULT_OK && data != null) {
                    // obtain the filename
                    Uri fileUri = data.getData();
                    if (fileUri != null) {
                        String filePath = fileUri.getPath();
                        if (filePath != null) {
                            selectedDirLabel.setText(filePath+"/");
                        }
                    }
                }
                break;
            case REQUEST_CODE_SCHEDULE:
                //TODO: pick up the result and set the schedule
                break;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case PROGRESS: {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                progressDialog = new ProgressDialog(this);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setMessage("Sending files..");
                progressDialog.setCancelable(false);
                return progressDialog;
            }
        }
        return null;
    }

     /**
     * create the options menu
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

     /**
     * when an options button is pressed
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.feedbackButton:
                startActivity(new Intent(this, FeedbackView.class));//start the feeback view
                //create form
                break;
        }
        return true;
    }
    

     private final HorizontalPager.OnScreenSwitchListener serverSwitchListener =
            new HorizontalPager.OnScreenSwitchListener() {
                @Override
                public void onScreenSwitched(final int screen) {
                    try{
                        currentServer = serverViewList.get(screen);
                    }catch(IndexOutOfBoundsException e){
                        Log.w("server_pageswitch","error setting currect server view:"+e);
                    }
                    Log.d("HorizontalPager", "switched to screen: " + screen);
                }
            };

    private final HorizontalPager.OnScreenSwitchListener directorSwitchListener =
            new HorizontalPager.OnScreenSwitchListener() {
                @Override
                public void onScreenSwitched(final int screen) {
                    try{
                        currentDirectory = directoryViewList.get(screen);
                    }catch(IndexOutOfBoundsException e){
                        Log.w("directory_pageswitch","error setting currect directory view:"+e);
                    }
                    Log.d("HorizontalPager", "switched to screen: " + screen);
                }
            };
}
