package org.remoteHome.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


/** A Logfile provides logging facility. The logging helps to debug and maintain
 *  an application. The human readable messages (log lines) with fixed structure
 *  are written into a log file. The log file might be archived periodically.
 *  The default template is:
 *  <pre> {0, date,dd-MM-yy kk:mm:ss} : {1} : {6} : {2} : {3} : {4} : {5} </pre>
 *  ,which produces, for instance, following logline:
 *  <pre>Mon August 05 07:53:06 : main : INFO      : Main.main : Server started</pre>
 *  <p>
 *
 *  <pre>
 *  Logfile log = new Logfile("my_server.log", "{0} - {5}",".");
 *  log.schedule("09:00",1);
 *  log.write(Logfile.DEBUG, "LogTest.main", "Test");
 *  </pre>
 */

public class Logfile {
    
    protected Writer out;
    private String hostname;
    private List <Logfile.LogLine>logCache = null;
    private long sequence = 1;
    // Default loglevel
    private int logLevel = 0;
    private int cacheSize = 1000;
    
    // Loglevels
    public static final int FATAL = 20;
    public static final int ERROR = 15;
    public static final int WARNING = 10;
    public static final int INFO = 5;
    public static final int DEBUG = 0;
    
    // Output stream property
    private static int NONE = 0;
    private static int LASTLINES = 50;
    
    // Severity labels which appears in a log file
    private static final String[] labels = {
        "DEBUG    ", "INFO     ", "WARNING  ", "ERROR    ", "FATAL    "};
    
    private static final String LOG_LINE =
            "{0, date,dd-MM-yy kk:mm:ss} : {1} : {2} : {3} : {4} : {5} {6}";
    private  MessageFormat logFormater;

    public Logfile() throws LogException {
        logFormater = new MessageFormat(LOG_LINE);
        // Find out own hostname
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown";
        }
        logCache = Collections.synchronizedList(new <Logfile.LogLine>ArrayList());
    }

    /**
     * Writes a message into  a logfile. The format of the message is
     * given by template supplied in the {@link Logfile(String,String) constructor}
     * or by the default template.
     *
     * @param gravity gravity of message.
     * @param method name of method which is the log spited out from.
     * @param message content of message..
     */
    public synchronized void write(int gravity, int code, String method, String message) throws LogException {
        if (logLevel <= gravity) {
            String errCode;
            if ( code == NONE ) errCode = "";
            else errCode = "[ERR-"+code+"]";

            //store the logline to logCache
            LogLine logLine = new LogLine();
            logLine.sequence = sequence++;
            logLine.severity = getLabel(gravity);
            logLine.methodName = method;
            logLine.date = getDateTime();
            logLine.threadName = Thread.currentThread().getName();
            logLine.message = message;
            logLine.hostname = hostname;
            logLine.errorCode = errCode;
            logLine.gravity = gravity;
            logLine.code = code;
            cacheAdd(logLine);
            if (out != null) {
                Object[] args = { getDateTime(), hostname, Thread.currentThread().getName(), getLabel(gravity), method, errCode, message };
                try {
                    out.write(logFormater.format(args) + System.getProperty("line.separator"));
                    out.flush();
                } catch (IOException e) {
                    throw new LogException("Cannot write into log file.", e);
                }
            }
        }
    }

    private void cacheAdd(LogLine logLine) {
        logCache.add(0, logLine);
        if (logCache.size() > cacheSize) logCache.remove(cacheSize-1);
    }
    
    public List <Logfile.LogLine>getCache() {
        return logCache;
    }
    public List <Logfile.LogLine>cleanCache() {
        logCache.clear();
        return logCache;
    }
    
    public List <Logfile.LogLine>getCacheByThreadName(String name) {
        List <Logfile.LogLine>retVec = new <Logfile.LogLine>ArrayList();
        for (int i=0; i<logCache.size(); i++) {
            LogLine log = logCache.get(i);
            if (log.threadName.equals(name)) retVec.add(0, log);
        }
        return changeOrder(retVec);
    }
    
    public List <Logfile.LogLine>getCacheByMethod(String name) {
        List <Logfile.LogLine>retVec = new <Logfile.LogLine>ArrayList();
        for (int i=0; i<logCache.size(); i++) {
            LogLine log = (LogLine)logCache.get(i);
            if (log.methodName.equals(name)) retVec.add(0, log);
        }
        return changeOrder(retVec);
    }
    
    public List <Logfile.LogLine>getCacheBySeverity(String name) {
        int severity = getGravity(name);
        List <Logfile.LogLine>retVec = new <Logfile.LogLine>ArrayList();
        for (int i=0; i<logCache.size(); i++) {
            LogLine log = (LogLine)logCache.get(i);
            if (log.gravity >= severity) retVec.add(0, log);
        }
        return changeOrder(retVec);
    }
    
    private List <Logfile.LogLine>changeOrder(List <Logfile.LogLine>vec) {
        List <Logfile.LogLine>retVec = new <Logfile.LogLine>ArrayList();
        if (vec.size() == 0) return retVec;        
        for (int i=vec.size()-1; i>=0; i--) {
            retVec.add(vec.get(i));
        }
        return retVec;
    }
    
    public void startLoggingToFile(String fileName) {
        try {
            if (out != null) out.close();
            File f = new File(fileName);
            out = new FileWriter(new File(fileName), true);
        } catch (Exception e) {
            if (out != null) try { out.close(); } catch (Exception ee) {}
            out = null;
            error(1, e);
        }
    }
    
    public void stopLoggingToFile() {
        try {
            if (out != null) out.close();
            out = null;
        } catch (Exception e) {
            error(2, e);
        }
    }
    
    public void setLogLevel(String label) {
        logLevel = getGravity(label);
    }
    /* ----------- C O N V E N I E N C E   M E T H O D ---------- */

    public void debug( String message ){
        // Resolve caller class and method
        Exception e = new Exception();
        StackTraceElement[] st = e.getStackTrace();
        StackTraceElement caller = st[1];
        write( DEBUG, NONE, caller.getClassName()+"."+caller.getMethodName(), message );
    }        
    public void info( String message ){
        // Resolve caller class and method
        Exception e = new Exception();
        StackTraceElement[] st = e.getStackTrace();
        StackTraceElement caller = st[1];
        write( INFO, NONE, caller.getClassName()+"."+caller.getMethodName(), message );
    }    
    public void warning(String message) {
        // Resolve caller class and method
        Exception e = new Exception();
        StackTraceElement[] st = e.getStackTrace();
        StackTraceElement caller = st[1];
        write( WARNING, NONE, caller.getClassName()+"."+caller.getMethodName(), message );
    }
    public void error(int code, String message){
        // Resolve caller class and method
        writeLastLinesFromCache(LASTLINES);
        Exception e = new Exception();
        StackTraceElement[] st = e.getStackTrace();
        StackTraceElement caller = st[1];
        write( ERROR,code, caller.getClassName()+"."+caller.getMethodName(), message );
    }
    
    public void error(int code, Throwable exception){
        writeLastLinesFromCache(LASTLINES);
        StackTraceElement[] stackTrace = exception.getStackTrace();
        StackTraceElement caller = stackTrace[0];
        
        write( ERROR,code, caller.getClassName()+"."+caller.getMethodName(), exception.toString() );
        for (int i=0; i< stackTrace.length; i++){
            write( ERROR,code, caller.getClassName()+"."+caller.getMethodName(), "******* StackTrace ******* "+stackTrace[i].toString() );
        }
    }
    
    public void fatal( int code, String method, String message ){
        //writeLastLinesFromCache(LASTLINES);
        //write(FATAL, code, method, message);
        fatal(code, message);
    }
    
    public void fatal( int code, String message ) {
        writeLastLinesFromCache(LASTLINES);
        // Resolve caller class and method
        Exception e = new Exception();
        StackTraceElement[] st = e.getStackTrace();
        StackTraceElement caller = st[1];
        write( FATAL, code, caller.getClassName()+"."+caller.getMethodName(), message );
    }
    
    public void fatal( int code, Throwable exception ){
        writeLastLinesFromCache(LASTLINES);
        StackTraceElement[] stackTrace = exception.getStackTrace();
        StackTraceElement caller = stackTrace[0];
        
        write( FATAL,code, caller.getClassName()+"."+caller.getMethodName(), exception.toString() );
        for (int i=0; i< stackTrace.length; i++){
            write( FATAL,code, caller.getClassName()+"."+caller.getMethodName(), "******* StackTrace ******* "+stackTrace[i].toString() );
        }
    }

    
    /* --------------- PRIVATE METHODS ----------------- */
    
    // Find suitable label for a loglevel
    // 0..4   DEBUG
    // 5..9   INFO
    // 10..14 WARNING
    // 15..19 ERROR
    // 20..24   FATAL
    
    private static String getLabel(int gravity) {
        int x = Math.abs(gravity) / 5;
        if (x == 3) return labels[x];
        else return x < 6 ? labels[x] : labels[4];
    }
    
    private void writeLastLinesFromCache(int lines) {
        try {
            if (logLevel <= DEBUG) return;
            int maxCount = 0;
            if (logCache.size() > lines) maxCount = lines; else maxCount = logCache.size()-1;
            for (int i=maxCount; i==0; i--) {
                LogLine logLine = (LogLine)logCache.get(i);
                write(logLine.gravity, logLine.code, logLine.methodName, "******* CACHE ******* "+logLine.message);
            }
        } catch (Throwable e) {}
    }
    
    private static int getGravity(String label) {
        if ((label.trim()).equals(labels[0].trim())) return DEBUG;
        else if ((label.trim()).equals(labels[1].trim())) return INFO;
        else if ((label.trim()).equals(labels[2].trim())) return WARNING;
        else if ((label.trim()).equals(labels[3].trim())) return ERROR;
        else return FATAL;
    }
    
    private Date getDateTime() {
        Date dateTime = new Date();
        dateTime.setTime(System.currentTimeMillis());
        return dateTime;
    }
    
    public class LogLine {
        public long sequence;
        public String severity;
        public String threadName;
        public String methodName;
        public Date date;
        public String message;
        public String hostname;
        public String errorCode;
        protected int gravity;
        protected int code;
    }
    
    /* ------------------ E X C E P T I O N ------------------ */
    public class LogException extends RuntimeException {
        
        private Exception cause;
        
        public LogException() {
            super();
        }
        
        public LogException(String msg) {
            super(msg);
        }
        
        public LogException(String msg, Exception cause) {
            super(msg);
            this.cause = cause;
        }
        
    }
}

