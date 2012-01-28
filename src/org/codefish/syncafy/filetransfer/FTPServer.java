/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.codefish.syncafy.filetransfer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.codefish.syncafy.AppConstants;

/**
 *
 * @author Matthew
 */
public class FTPServer implements Server{
    private int port;
    private URI address;
    private String name, baseDirectory, username, password;
    private boolean recursive;
    private FTPClient client = new FTPClient();

    public FTPServer(int port, URI address, String name, String baseDirectory, String username, String password, boolean recursive) {
         this.port = port;
        this.address = address;
        this.name = name;
        this.baseDirectory = baseDirectory;
        this.recursive = recursive;
        this.username = username;
        this.password = password;
    }

    public void putFiles(String directory) throws Exception {
        putFiles(directory, null);
    }

    public void putFiles(String directory, Handler handler) throws Exception {
        if(!client.isConnected()){
            connect();
        }

        client.cwd(FilenameUtils.getFullPath(directory));

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
                putFile(f);
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
            putFile(baseFile);
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
        if(!client.isConnected()){
            client.connect(getAddress().toString());
            return client.login(getUsername(), getPassword());
        }
        return true;
    }

    private void putFile(File file) throws Exception{
        client.storeFile(file.getName(), new FileInputStream(file));
    }

    public URI getAddress() {
        return address;
    }

    public void setAddress(URI address) {
        this.address = address;
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
