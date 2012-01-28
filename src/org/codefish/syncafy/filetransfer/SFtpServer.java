/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.codefish.syncafy.filetransfer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Properties;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.codefish.syncafy.AppConstants;

/**
 * Object to represent an SFTP server
 * @author Matthew
 */
public class SFtpServer implements Server {

    private int port;
    private URI address;
    private String name, baseDirectory, username, password;
    private boolean recursive;
    private static final JSch jsch = new JSch();
    private Session session;
    private ChannelSftp channel;

    public SFtpServer(int port, URI address, String name, String baseDirectory, String username, String password, boolean recursive) {
        this.port = port;
        this.address = address;
        this.name = name;
        this.baseDirectory = baseDirectory;
        this.recursive = recursive;
        this.username = username;
        this.password = password;
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
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * connect to this sftp server
     * @return
     */
    public boolean connect() throws Exception {
        ChannelSftp thisChannel = getChannel();
        if (!thisChannel.isConnected()) {
            thisChannel.connect();
        }
        return true;
    }

    /**
     * put files from a local directory but calling back to a handler each file
     * @param directory
     * @param handler
     */
    public void putFiles(String remoteDirectory, Handler handler) throws Exception{
        if (!this.channel.isConnected()) {//check if we're already connected
            connect();
        }
        //make sure we are at the base directory
        getChannel().cd(FilenameUtils.getFullPath(remoteDirectory));

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

    /**
     * put files from a directory
     * @param directory
     */
    public void putFiles(String directory) throws Exception {
        putFiles(directory, null);
    }

    /**
     * upload a file to the sftp server
     * @param file
     */
    private void putFile(File file) throws Exception {
        try{//try and put the file
            getChannel().put(new FileInputStream(file),file.getName());
        }catch(Exception e){
            Log.e("SFTPput", e.toString());
        }
    }

    /**
     * get the sftp session
     * @return
     * @throws JSchException
     * @throws Exception
     */
    private Session getSession() throws JSchException, Exception {
        if (this.session == null) {
            if (StringUtils.isBlank(this.username) || StringUtils.isBlank(this.username.toString()) || this.port <= 0) {
                throw new Exception("not enough info to connect");
            }
            this.session = jsch.getSession(this.username, this.address.toString(), this.port);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("compression.s2c", "zlib,none");//compression
            config.put("compression.c2s", "zlib,none");//compression
            config.put("PreferredAuthentications", "password,gssapi-with-mic,publickey,keyboard-interactive");
            this.session.setConfig(config);
            this.session.setTimeout(15000);//15 sec timeout
            this.session.setPassword(this.password);
        }
        return session;
    }

    /**
     * gets the sftp channel
     * @return
     * @throws JSchException
     * @throws Exception
     */
    private ChannelSftp getChannel() throws JSchException, Exception {
        if (this.session == null) {
            this.session = getSession();
        }
        if (!this.session.isConnected()) {
            this.session.connect();
        }
        if (channel == null) {
            this.channel = (ChannelSftp) session.openChannel("sftp");//open a new sftp session
        }
        return channel;
    }

    public static class JSCHLogger implements com.jcraft.jsch.Logger {

        static java.util.Hashtable<Integer, String> name = new java.util.Hashtable<Integer, String>();

        static {

            name.put(new Integer(DEBUG),
                    "DEBUG:");
            name.put(new Integer(INFO), "INFO:");
            name.put(new Integer(WARN), "WARN:");
            name.put(new Integer(ERROR), "ERROR:");
            name.put(new Integer(FATAL), "FATAL:");
        }

        public boolean isEnabled(int level) {
            return true;


        }

        public void log(int level, String message) {
            if (level == DEBUG) {
                Log.d("DEBUG", message);


            } else if (level == INFO) {
                Log.i("INFO", message);


            } else if (level == WARN) {
                Log.w("WARN", message);


            } else if (level == ERROR) {
                Log.e("ERROR", message);


            } else if (level == FATAL) {
                Log.e("ERROR", message);

            }
        }
    }
}
