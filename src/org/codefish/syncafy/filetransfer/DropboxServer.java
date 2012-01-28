/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.codefish.syncafy.filetransfer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.dropbox.client.DropboxAPI;
import com.dropbox.client.DropboxAPI.Config;
import java.io.File;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.codefish.syncafy.AppConstants;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

/**
 * Dropbox implementation using the dropbox api and android sdk
 * @author Matthew
 */
public class DropboxServer implements Server{
    private String name, baseDirectory, username, password;
    private boolean recursive;
    private DropboxAPI api = new DropboxAPI();
    private Config mConfig;
    private PBEStringEncryptor encryptor = new StandardPBEStringEncryptor();{
        encryptor.setPassword("sr0pb0xs3cretk3y");//set password on initialisation to keep password in memory for shortest time
    }


    private static final String KEY = "91v2l8lumd5u4ub";//dropbox api key
    private static final String SECRET_ENC = "I6gRDr0k0YQSg2m2iXgJ0b7zjjGQ8i7a";//encrypted secret

    public DropboxServer(String name, String baseDirectory, String username, String password, boolean recursive) {
        this.name = name;
        this.baseDirectory = baseDirectory;
        this.username = username;
        this.password = password;
        this.recursive = recursive;
    }



    public void putFiles(String directory) throws Exception {
        putFiles(directory, null);
    }

    public void putFiles(String directory, Handler handler) throws Exception {
        if(!api.isAuthenticated()){
            connect();
        }

        File baseFile = new File(FilenameUtils.getFullPath(baseDirectory));
        if (!baseFile.exists()) {
            throw new Exception("the chosen directory doesn't exist");
        }

        int total=0;

        if (baseFile.isDirectory()) {//it's a directory
            if(handler!=null){
                int numFiles = baseFile.listFiles().length;

                Message msg = handler.obtainMessage();
                Bundle b = new Bundle();
                b.putInt(AppConstants.MAX_FILES, numFiles);
                msg.setData(b);
                handler.sendMessage(msg);
            }
            //get a list of ALL the files in this directory
            Collection<File> fileList = FileUtils.listFiles(baseFile, null, recursive);
            for (File f : fileList) {
                //TODO:catch errors
                putFile(f,directory);
                if(handler!=null){
                    total++;
                    Message msg = handler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putInt(AppConstants.TOTAL_FILES, total);
                    msg.setData(b);
                    handler.sendMessage(msg);
                }
            }
        } else {//it's an ordinary file
            putFile(baseFile,directory);
            if(handler!=null){
                    total++;
                    Message msg = handler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putInt("total", total);
                    msg.setData(b);
                    handler.sendMessage(msg);
                }

        }
    }

    public boolean connect() throws Exception {
        if(api.isAuthenticated()){
            return true;
        }
        api.authenticate(getConfig(), username, password);
        //check the status of the authentication
        int success = mConfig.authStatus;
        return success == DropboxAPI.STATUS_SUCCESS;
    }

    private void putFile(File file, String directory) throws Exception{
        if(!api.isAuthenticated()){
            connect();
        }
        api.putFile("", directory, file);
    }

    private Config getConfig(){        
        mConfig = api.getConfig(null, false);
	mConfig.consumerKey=KEY;
        try {
            mConfig.consumerSecret = encryptor.decrypt(SECRET_ENC);
        } catch (Exception ex) {
            //set to blank string, guaranteed fail but the exception will be thrown at the api level
            mConfig.consumerSecret = "";
        }
        mConfig.server="api.dropbox.com";
	mConfig.contentServer="api-content.dropbox.com";
	mConfig.port=80;
        return mConfig;
    }

    public String getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }


}
