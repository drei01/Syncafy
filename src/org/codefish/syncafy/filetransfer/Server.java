/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.codefish.syncafy.filetransfer;

import android.os.Handler;

/**
 * interface to map all server objects (ftp,sftp etc)
 * @author Matthew
 */
public interface Server {
    public void putFiles(String directory)throws Exception;

    public void putFiles(String directory, Handler handler)throws Exception;

    public boolean connect()throws Exception;
}
