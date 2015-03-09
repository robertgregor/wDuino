/*
 * The MIT License
 *
 * Copyright 2014 Remote-Home s.r.o..
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.remoteHome;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Date;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import static javax.swing.JFrame.EXIT_ON_CLOSE;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;
import org.remoteHome.stk500_v1.Logger;
import org.remoteHome.stk500_v1.STK500v1;
import org.remoteHome.util.Hex;
import org.remoteHome.util.Logfile;

/**
 *
 * @author gregorro
 */
public class NetworkProgramming extends Thread {
    
    enum Logging {CONSOLE, GUI, Logfile};
    enum LoggingSeverity {DEBUG, INFO, WARNING, ERROR, FATAL};
    public Logging loggingType = Logging.CONSOLE;
    public Logfile l;
    public LoggingSeverity logSeverity = LoggingSeverity.INFO;
    public Gui g = null;
    int listenPort;
    String tempDirectory;
    boolean terminate = false;
    ServerSocket socket;
    NetworkProgramming.HexfileScanner scn;
    byte hexFile[];
    
    public NetworkProgramming(int port, String tempDirectory) throws IOException {
        this.tempDirectory = tempDirectory;
        listenPort = port;        
        socket = new ServerSocket(listenPort);
        scn = this.new HexfileScanner(this);
        scn.setPriority(Thread.MIN_PRIORITY);
        scn.start();
    } 
    public NetworkProgramming() throws IOException {
        this(8081, "");
    }     
    public void run() {
        while(!terminate) {
            try {
                log("Programming port is waiting on connection...", LoggingSeverity.INFO);
                Socket s = socket.accept();
                log("Connection to programing port established.", LoggingSeverity.INFO);
                ProgramViaNetwork pvn = new ProgramViaNetwork(s, hexFile, this);
                pvn.setPriority(Thread.MAX_PRIORITY);
                pvn.start();
            } catch (Exception e) {
                return;
            }
        }
    }
    public void closeSocket() {
        if(!socket.isClosed()) {
           try { 
               socket.close();
           } catch (Exception e) {}           
        }
    }
    public void log(String message, LoggingSeverity severity) {
        if (loggingType == Logging.CONSOLE) {
                if (logSeverity == LoggingSeverity.DEBUG) {
                    System.out.println(new Date().toString()+" "+severity+" "+message);
                } else if (logSeverity == LoggingSeverity.INFO) {
                    if ((severity == LoggingSeverity.INFO) || 
                            (severity == LoggingSeverity.WARNING) || 
                            (severity == LoggingSeverity.ERROR) || 
                            (severity == LoggingSeverity.FATAL)) System.out.println(new Date().toString()+" "+severity+" "+message);
                } else if (logSeverity == LoggingSeverity.WARNING) {
                    if ((severity == LoggingSeverity.WARNING) || 
                            (severity == LoggingSeverity.ERROR) || 
                            (severity == LoggingSeverity.FATAL)) System.out.println(new Date().toString()+" "+severity+" "+message);
                } else if (logSeverity == LoggingSeverity.ERROR) {
                    if ((severity == LoggingSeverity.ERROR) || 
                            (severity == LoggingSeverity.FATAL)) System.out.println(new Date().toString()+" "+severity+" "+message);
                } else if (logSeverity == LoggingSeverity.FATAL) {
                    if ((severity == LoggingSeverity.FATAL)) System.out.println(new Date().toString()+" "+severity+" "+message);
                }
        } else if (loggingType == Logging.Logfile) {
            if (severity == LoggingSeverity.DEBUG) l.debug(message);
            else if (severity == LoggingSeverity.INFO) l.info(message);
            else if (severity == LoggingSeverity.WARNING) l.warning(message);
            else if (severity == LoggingSeverity.ERROR) l.error(7896,message);
            else if (severity == LoggingSeverity.FATAL) l.fatal(7897,message);
        } else if (loggingType == Logging.GUI) {
            if (g!=null) {
                if (!message.endsWith("\n")) message += "\n";
                if (message.indexOf("%") > 0) g.programmingProgress.setText(message);
                if (logSeverity == LoggingSeverity.DEBUG) {
                    g.statusWindow.append(new Date().toString()+" "+severity+" "+message);
                } else if (logSeverity == LoggingSeverity.INFO) {
                    if ((severity == LoggingSeverity.INFO) || 
                            (severity == LoggingSeverity.WARNING) || 
                            (severity == LoggingSeverity.ERROR) || 
                            (severity == LoggingSeverity.FATAL)) g.statusWindow.append(new Date().toString()+" "+severity+" "+message);
                } else if (logSeverity == LoggingSeverity.WARNING) {
                    if ((severity == LoggingSeverity.WARNING) || 
                            (severity == LoggingSeverity.ERROR) || 
                            (severity == LoggingSeverity.FATAL)) g.statusWindow.append(new Date().toString()+" "+severity+" "+message);
                } else if (logSeverity == LoggingSeverity.ERROR) {
                    if ((severity == LoggingSeverity.ERROR) || 
                            (severity == LoggingSeverity.FATAL)) g.statusWindow.append(new Date().toString()+" "+severity+" "+message);
                } else if (logSeverity == LoggingSeverity.FATAL) {
                    if ((severity == LoggingSeverity.FATAL)) g.statusWindow.append(new Date().toString()+" "+severity+" "+message);
                }
            }
        }
    }
    class HttpDebugger extends Thread {
        ServerSocket socket;
        int port;
        public HttpDebugger(String port) throws IOException {
            this.port = Integer.parseInt(port);
            socket = new ServerSocket(this.port);
            this.setPriority(Thread.MIN_PRIORITY);
            this.start();
        }
        public void run() {            
            while(true) {
                try {
                    log("Http debug port is waiting on connection...", LoggingSeverity.INFO);
                    Socket s = socket.accept();
                    InputStream in = s.getInputStream();
                    OutputStream out = s.getOutputStream();
                    try { 
                        BufferedReader r = new BufferedReader(new InputStreamReader(in));
                        int cnt = 20;
                        while(cnt-- > 0) {
                            if (r.ready()) {
                                log("HTTP debug received:" + URLDecoder.decode(r.readLine()), LoggingSeverity.INFO);
                            } else {
                                Thread.sleep(100);
                            }
                        }
                        out.write("HTTP/1.1 200 OK\nConnection: Close\n\n".getBytes());
                        out.flush();
                    } catch (IOException e) {                        
                    } finally {
                        try { in.close(); } catch (Exception e) {}
                        try { out.close(); } catch (Exception e) {}
                        try { s.close(); } catch (Exception e) {}
                        log("Http debug port is closed.", LoggingSeverity.INFO);
                    }
                } catch (Exception e) {
                    return;
                }
            }
        }
    }
    class ProgramViaNetwork extends Thread {
        
        Socket clientSocket;
        byte[] sketchHexData;
        NetworkProgramming n;
        
        public ProgramViaNetwork(Socket clientSocket, byte[] sketchHexData, NetworkProgramming n) {
            this.n = n;
            this.clientSocket = clientSocket;
            this.sketchHexData = sketchHexData;
        }        
        public void run() {
            OutputStream out = null;
            InputStream in = null;
            try {
                out = clientSocket.getOutputStream();
                in = clientSocket.getInputStream();
                int counter = 20;
                STK500v1 stk = new STK500v1(out, in, new StkLoggerImpl(n), sketchHexData);
                while (counter-- != 0) {
                    Thread.sleep(50);
                    if (in.available() > 0) {
                        in.read();
                        out.write("a".getBytes());
                        out.flush();
                        log("Device synced. Going to reset and program.", LoggingSeverity.INFO);
                        Thread.sleep(10);
                        break;
                    }                    
                }
                if (counter == 0) {
                    log("Device was not synchronized, probably the network connection is not stable. Please repeat the download again.", LoggingSeverity.ERROR);
                    return;
                }
                if (stk.programUsingOptiboot(false, 128)) {
                    try { Thread.sleep(100); } catch (InterruptedException e) {}
                    log("Programming successfull.", LoggingSeverity.INFO);
                } else {
                    try { Thread.sleep(100); } catch (InterruptedException e) {}
                    log("Programming failed. Sending download command to the device to repeat the action...", LoggingSeverity.ERROR);
                    String ipAddress = clientSocket.getInetAddress().getHostAddress();                    
                    try { in.close(); } catch (Throwable e) {}
                    try { out.close(); } catch (Throwable e) {}
                    try { clientSocket.close(); } catch (Throwable e) {}
                    try { Thread.sleep(5000); } catch (InterruptedException e) {} // wait for the device to reconnect to WiFi
                    try {
                        log("Going to send: http://"+ipAddress+"/cb", LoggingSeverity.INFO);
                        HttpURLConnection con = (HttpURLConnection) (new URL("http://"+ipAddress+"/cb")).openConnection();
                        con.setRequestMethod("GET");
                        int responseCode = con.getResponseCode();
                        con.disconnect();
                        log("Triggered sketch upload. Response code from the device: "+responseCode, LoggingSeverity.INFO);
                    } catch (Exception exc) {
                        log("Cannot trigger programming of the device: "+exc.getClass().getName() + " - " + exc.getMessage(), LoggingSeverity.ERROR);
                    }                    
                }
            } catch (Exception e) {
                String trace = "";
                for (StackTraceElement ste : e.getStackTrace()) trace += ste + "\n";
                log(trace, LoggingSeverity.ERROR);
            } finally {
                try { in.close(); } catch (Throwable e) {}
                try { out.close(); } catch (Throwable e) {}
                try { clientSocket.close(); } catch (Throwable e) {}
            }
        }
    }
    class StkLoggerImpl implements Logger {
        NetworkProgramming n;
        
        public StkLoggerImpl(NetworkProgramming n) {
            this.n = n;
        }
        public void makeToast(String msg){
            n.log(msg, LoggingSeverity.INFO);
        }        
        public void printToConsole(String msg) {
            n.log(msg, LoggingSeverity.INFO);
        }
        public void logcat(String msg, String level) {
                    if (level.equals("i")) {
                        n.log(msg, LoggingSeverity.INFO);
                    } else if (level.equals("w")) {
                        n.log(msg, LoggingSeverity.WARNING);
                    } else if (level.equals("e")) {
                        n.log(msg, LoggingSeverity.ERROR);
                    } else {
                        n.log(msg, LoggingSeverity.DEBUG);
                    }
            //if (msg.indexOf("%") > 0) System.out.println(msg);
        }
    }
    class HexfileScanner extends Thread {
        NetworkProgramming p = null;
        public HexfileScanner(NetworkProgramming p) {
            this.p = p;
        }
        File getLastModifHexFile(File dir) {
            File findedHexFile = null;
            if (dir == null) return null;
            if (!dir.exists()) return null;
            if (!dir.isDirectory()) {
                if (dir.getName().endsWith(".hex") || dir.getName().endsWith(".HEX")) return dir; else return null;
            }
            if (dir.listFiles() == null) return null;
            for (File f : dir.listFiles()) {
                if (f == null) continue;
                if (f.isFile()) {
                    if (f.getName().endsWith(".hex")) {
                        if (findedHexFile == null) {
                            findedHexFile = f;
                        } else {
                            if (f.lastModified() > findedHexFile.lastModified()) findedHexFile = f;
                        }
                    }
                } else if (f.isDirectory()) {
                    File ff = getLastModifHexFile(f);
                    if (ff != null) {
                        if (findedHexFile == null) {
                            findedHexFile = ff;
                        } else {
                            if (ff.lastModified() > findedHexFile.lastModified()) findedHexFile = ff;
                        }
                    }
                }
            }
            return findedHexFile;
        }
        private byte[] getByteStreamFromHexStream(byte[] in) {
            String[] input = new String(in).split("\n");
            ByteArrayOutputStream out = new ByteArrayOutputStream();        
            for (String line : input) {
                line = line.trim();
                if (line.startsWith(":")) line = line.substring(1);
                try {
                    out.write(":".getBytes());
                    out.write(Hex.fromString(line));
                } catch (IOException e) {
                    break;
                }
            }
            return out.toByteArray();
        }
        public void run() {
            long lastModified = 0L;
            boolean noFileSelectedReported = false;
            while (true) {                
                    try {
                        Thread.sleep(1000);
                        if (p.tempDirectory != null) {
                            File f = getLastModifHexFile(new File(p.tempDirectory));
                            noFileSelectedReported = false;
                            if ((f != null) && ((lastModified == 0L) || 
                                    f.lastModified() > lastModified || 
                                    (new File(p.tempDirectory).getAbsolutePath().endsWith(".hex") && (new File(p.tempDirectory)).lastModified() != lastModified))) {
                                byte b[] = new byte[(int) f.length()];
                                DataInputStream dis = new DataInputStream(new FileInputStream(f));
                                dis.readFully(b);
                                dis.close();
                                p.hexFile = getByteStreamFromHexStream(b);
                                lastModified = f.lastModified();
                                p.log("The new hex file has been read. Bytes: "+p.hexFile.length, LoggingSeverity.INFO);
                            }
                        }
                    } catch (NullPointerException e) {
                            if (!noFileSelectedReported) {
                                p.log("No hex file has been read.Please select correct hexFile/directory.", LoggingSeverity.WARNING);                            
                                noFileSelectedReported = true;
                                p.hexFile = null;
                            }
                    } catch (Exception e) {
                        String trace = e.getClass().getName()+" "+e.getMessage()+":\n";
                        for (StackTraceElement ste : e.getStackTrace()) trace += "     "+ste + "\n";
                        p.log(trace, LoggingSeverity.ERROR);
                    }
            }
        }
    }
    
    class Gui extends JFrame implements ActionListener {
      public JTextField progListenPortTextField;
      public JTextField httpDebugListenPortTextField;
      public JTextField directoryField;
      public JTextField hexField;
      public JTextField ipaddressField;
      public JTextArea statusWindow;
      public JLabel programmingProgress;
      private NetworkProgramming prg;
      private JButton startbutton;
      private JButton cleanLogButton;
      private JButton loadSketchButton;
      public Gui(NetworkProgramming prg) {
        this.prg = prg;
        setTitle("wDuino sketch downloader 1.0.0");
        progListenPortTextField = new JTextField(5);
        progListenPortTextField.setText("8081");
        httpDebugListenPortTextField = new JTextField(5);
        httpDebugListenPortTextField.setText("80");
        hexField = new JTextField(20);
        directoryField = new JTextField(20);
        setSize(800, 200);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JTabbedPane jtab = new JTabbedPane();
        JPanel listenPort = new JPanel();
	listenPort.add(new JLabel("Programing listen port:"));
	listenPort.add(progListenPortTextField);
        listenPort.add(new JLabel("         Http debug listen port:"));
        listenPort.add(httpDebugListenPortTextField);
        jtab.addTab("Listen ports configuration",null,listenPort);
        JPanel sketchFile = new JPanel();
        sketchFile.add(new JLabel("Directory scanner:"));
        sketchFile.add(directoryField);
        JButton browseDir = new JButton("Browse");
	browseDir.addActionListener(this);
	browseDir.setActionCommand("browseDir");
        sketchFile.add(browseDir);
        sketchFile.add(new JLabel("Hex file:"));        
        sketchFile.add(hexField);
        JButton browseHex = new JButton("Browse");
	browseHex.addActionListener(this);
	browseHex.setActionCommand("findHex"); 
        sketchFile.add(browseHex);
        jtab.addTab("Sketch file configuration",null,sketchFile);
        JPanel actionPanel = new JPanel();
        actionPanel.add(new JLabel("IP: "));
        ipaddressField = new JTextField(15);
        actionPanel.add(ipaddressField);
        loadSketchButton = new JButton("Load sketch");
        loadSketchButton.addActionListener(this);
        loadSketchButton.setActionCommand("load");
        actionPanel.add(loadSketchButton);
        startbutton = new JButton("Start processing");        
	startbutton.addActionListener(this);
	startbutton.setActionCommand("start");
        actionPanel.add(startbutton);
        cleanLogButton = new JButton("Clean log area");
	cleanLogButton.addActionListener(this);
	cleanLogButton.setActionCommand("cleanLog");
        actionPanel.add(cleanLogButton);
        programmingProgress = new JLabel("                                                  ");
        actionPanel.add(programmingProgress);
        jtab.addTab("Actions",null,actionPanel);
        this.add(jtab, BorderLayout.NORTH);
        this.setResizable(false);
        JPanel statusPanel = new JPanel();
        statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        this.add(statusPanel, BorderLayout.SOUTH);
        statusPanel.setPreferredSize(new Dimension(this.getWidth(), 128));
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
        statusWindow = new JTextArea();
        statusWindow.setLineWrap(true);
        DefaultCaret caret = (DefaultCaret)statusWindow.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane jScrollPane=new JScrollPane();
        jScrollPane.add(statusWindow);
        jScrollPane.setViewportView(statusWindow); 
        statusPanel.add(jScrollPane);
        pack();
      }
      public void actionPerformed(ActionEvent e) {
          if (e.getActionCommand().equals("start")) {
              prg.listenPort = Integer.parseInt(progListenPortTextField.getText());
              prg.start();
              startbutton.setEnabled(false);
              try {
                HttpDebugger d = new HttpDebugger(httpDebugListenPortTextField.getText());
              } catch (Exception ex) {
                  prg.log("Cannot start HTTP debugging on port "+httpDebugListenPortTextField.getText()+": "+ex.getClass().getName() + " - " + ex.getMessage(), LoggingSeverity.ERROR);
              }
          } else if (e.getActionCommand().equals("browseDir")) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                int returnVal = chooser.showOpenDialog(this);
                if(returnVal == JFileChooser.APPROVE_OPTION) {                
                    directoryField.setText(chooser.getSelectedFile().getAbsolutePath());
                    hexField.setText("");
                    prg.tempDirectory = chooser.getSelectedFile().getAbsolutePath();
                }
          } else if (e.getActionCommand().equals("findHex")) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                FileNameExtensionFilter filter = new FileNameExtensionFilter("HEX files", "hex");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showOpenDialog(this);
                if(returnVal == JFileChooser.APPROVE_OPTION) {                
                    hexField.setText(chooser.getSelectedFile().getAbsolutePath());
                    directoryField.setText("");
                    prg.tempDirectory = chooser.getSelectedFile().getAbsolutePath();
                }              
          } else if (e.getActionCommand().equals("cleanLog")) {
              statusWindow.setText("");
          } else if (e.getActionCommand().equals("load")) {
              try {
                  loadSketchButton.setEnabled(false);
                  if (startbutton.isEnabled()) {
                      prg.log("Programming mode not enabled. Please press \"Start processing\" first.", LoggingSeverity.ERROR);
                      return;
                  }
                  HttpURLConnection con = (HttpURLConnection) (new URL("http://"+ipaddressField.getText()+"/cb")).openConnection();
                  con.setRequestMethod("GET");
                  int responseCode = con.getResponseCode();
                  con.disconnect();
                  prg.log("Triggered sketch upload. Response code from the device: "+responseCode, LoggingSeverity.INFO);
              } catch (Exception exc) {
                  prg.log("Cannot trigger programming of the device: "+exc.getClass().getName() + " - " + exc.getMessage(), LoggingSeverity.ERROR);
              } finally {
                  loadSketchButton.setEnabled(true);
              }
          }
      }
    }
    
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            if (!GraphicsEnvironment.isHeadless()) {
                try {
                    NetworkProgramming prog = null;
                    if (System.getProperty("os.name").startsWith("Windows")) {
                        prog = new NetworkProgramming(8081, "C:\\Users\\"+System.getenv("username")+"\\AppData\\Local\\Temp");
                    } else {
                        prog = new NetworkProgramming(8081, "/tmp");
                    }
                    NetworkProgramming.Gui g = prog.new Gui(prog);
                    prog.g = g;
                    prog.loggingType = Logging.GUI;
                    g.progListenPortTextField.setText(Integer.toString(prog.listenPort));
                    g.progListenPortTextField.enableInputMethods(false);
                    g.directoryField.setText(prog.tempDirectory);
                    g.hexField.enableInputMethods(false);
                    g.setVisible(true);
                    prog.log("No command line arguments, gui is ready with default values.", LoggingSeverity.INFO);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }                
            } else {
                System.out.println("Wrong arguments. Using the default values. Usage: java -jar wDuino.jar 8081 /temp");
                System.out.println("  Param 1: Programming listen port. The same value should be configured at wDuino.");
                System.out.println("  Param 2: Either the temp directory, where the Arduino GUI saving the hex files. In that case");
                System.out.println("           the last generated hex file is used. In windows, this temp dir is usually in");
                System.out.println("           C:\\Users\\<USERNAME>\\AppData\\Local\\Temp, on linux it is in /temp");
                System.out.println("           or path to the hex file to load to the wDuino, i.e. C:\\Users\\Work\\abcdefgh.hex");
                if (System.getProperty("os.name").startsWith("Windows")) {
                    NetworkProgramming prog = new NetworkProgramming(8081, "C:\\Users\\"+System.getenv("username")+"\\AppData\\Local\\Temp");
                    prog.start();
                } else {
                    NetworkProgramming prog = new NetworkProgramming(8081, "/tmp");
                    prog.start();                    
                }
            }
        } else {
            if (!GraphicsEnvironment.isHeadless()) {
                try {
                    NetworkProgramming prog = new NetworkProgramming(Integer.parseInt(args[0]), args[1]);
                    NetworkProgramming.Gui g = prog.new Gui(prog);
                    prog.g = g;
                    prog.loggingType = Logging.GUI;
                    g.progListenPortTextField.setText(args[0]);
                    g.progListenPortTextField.enableInputMethods(false);
                    if (args[1].endsWith(".hex") || args[1].endsWith(".HEX")) {
                        g.hexField.setText(args[1]);
                        g.directoryField.enableInputMethods(false);
                    } else {
                        g.directoryField.setText(args[1]);
                        g.hexField.enableInputMethods(false);
                    }
                    g.setVisible(true);
                    prog.log("Gui is ready with with command line values: "+args[0]+" "+args[1], LoggingSeverity.INFO);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            } else {
                NetworkProgramming prog = new NetworkProgramming(Integer.parseInt(args[0]), args[1]);
                prog.start();                
            }               
        }
    }
}
