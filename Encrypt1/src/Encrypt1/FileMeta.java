/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Encrypt1;

/**
 *
 * @author user0
 */
public class FileMeta {
    
    public FileMeta(String pathname, long timestamp, long filesize) {
        m_pathname = pathname;
        m_timestamp = timestamp;
        m_filesize = filesize;
    }
    
    public String GetPathName() {
        return m_pathname;
    }
    
    public long GetTimeStamp() {
        return m_timestamp;
    }
    
    public long GetFileSize() {
        return m_filesize;
    }
    
    private String m_pathname;
    private long m_timestamp;
    private long m_filesize;
}
