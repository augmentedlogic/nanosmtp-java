/**
 * (c) 2020 Wolfgang Hauptfleisch <dev@augmentedlogic.com>
 * This file is part of nanosmtp
 * Licence: Apache v2
 **/
package com.augmentedlogic.nanosmtp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import javax.net.ssl.*;
import java.security.SecureRandom;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;


public class SmtpClient
{

    private int port = 465;
    private String host = null;
    private String auth_plain = null;
    private String tls_version = "TLSv1.2";
    private Boolean starttls = false;

    private String username = null;
    private String password = null;


    private String recipient = null;
    private String from = null;
    private ArrayList<String> comms = new ArrayList<String>();

    private int auth_type = 1;

    protected String send(ArrayList<String> data_a, Boolean debug) throws Exception
    {
        String line = null;

        try
        {
            SSLSocket sslsocket = null;
            PrintStream sout = null;
            BufferedReader sin = null;

            if(this.starttls) {

                Socket socket = new Socket(this.host, this.port);
                PrintStream out = new PrintStream( socket.getOutputStream() );
                BufferedReader in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );

                line = in.readLine();
                comms.add("S: " + line);
                if(debug) {System.out.println("S: " + line);}
                String cmd = "EHLO "+ this.host;
                out.println(cmd +"\r");
                comms.add("C: " + cmd);

                while(true) {
                    line = in.readLine();
                    if(debug) {System.out.println("S: " + line);}
                    if(line.contains("STARTTLS")) {
                        cmd = "STARTTLS";
                        out.println(cmd +"\r");
                        comms.add("C: " + cmd);
                        break;
                    } else {
                        comms.add("S: " + line);
                    }
                }
                while(true) {
                    line = in.readLine();
                    if(debug) {System.out.println("S: " + line);}
                    if(line.contains("Go ahead") || line.contains("Ready to start TLS") || line.contains("SMTP server ready")) {
                        if(debug) {System.out.println("Switching to TLS.");}
                        break;
                    } else {
                        comms.add("S: " + line);
                    }
                }
                // negotiation ends

                // upgrade socket to TLS
                SSLContext sslContext = SSLContext.getInstance(this.tls_version);
                sslContext.init(null, null, new SecureRandom());
                SSLSocketFactory socketFactory = sslContext.getSocketFactory();
                sslsocket = (SSLSocket) socketFactory.createSocket(socket, this.host, this.port, true);
                sout = new PrintStream( sslsocket.getOutputStream() );
                sin = new BufferedReader( new InputStreamReader( sslsocket.getInputStream() ) );

            } else {

                // we go directly for TLS connection
                SSLContext sslContext = SSLContext.getInstance(this.tls_version);
                sslContext.init(null, null, new SecureRandom());
                SSLSocketFactory socketFactory = sslContext.getSocketFactory();
                sslsocket = (SSLSocket) socketFactory.createSocket(this.host, this.port);
                sout = new PrintStream( sslsocket.getOutputStream() );
                sin = new BufferedReader( new InputStreamReader( sslsocket.getInputStream() ) );

                line = sin.readLine();
                comms.add("S: " + line);
                if(debug) {System.out.println(line);}

            }


            String cmd = "EHLO "+ this.host;
            sout.println(cmd +"\r");
            comms.add("C: " + cmd);

            line = sin.readLine();
            comms.add("S: " + line);
            if(debug) {System.out.println(line);}

            while(true) {
                line = sin.readLine();
                if(line.startsWith("250-AUTH")) {
                   // Using AUTH PLAIN
                   if(this.auth_type == 1) {
                    if(debug) { System.out.println("Sending AUTH PLAIN to server.");}
                    sout.println("AUTH PLAIN "+ this.auth_plain +"\r");
                    comms.add("C: AUTH PLAIN "+ this.auth_plain);
                    break;
                   }

                   if(this.auth_type == 2) {
                    // Using AUTH LOGIN
                    if(debug) { System.out.println("Requesting AUTH LOGIN.");}
                     sout.println("AUTH LOGIN\r");
                     // get username line
                     while(true) {
                     line = sin.readLine();
                        comms.add("S: " + line);
                        if(debug) {System.out.println(line);}
                        if(line.contains("VXNlcm5hbWU6")) {
                           break;
                        }
                     }

                     // send username
                     String username_encoded = Base64.getEncoder().encodeToString(this.username.getBytes());
                     sout.println(username_encoded + "\r");
                     if(debug) {System.out.println("username sent.");}
                     // get password prompt
                     while(true) {
                     line = sin.readLine();
                        comms.add("S: " + line);
                        if(debug) {System.out.println(line);}
                        if(line.contains("UGFzc3dvcmQ6")) {
                           break;
                        }
                     }
                     String password_encoded = Base64.getEncoder().encodeToString(this.password.getBytes());
                     // send password
                     sout.println(password_encoded + "\r");
                     if(debug) {System.out.println("password sent.");}
                     break;
                   }



                } else {
                    comms.add("S: " + line);
                    if(debug) {System.out.println(line);}
                }
            }


            while(true) {
                line = sin.readLine();
                if(line.contains("successful") || line.contains("Accepted")) {
                    if(debug) {System.out.println("we passed auth.");}
                    break;
                } else if (line.contains("authentication failed") || line.contains("Authentication unsuccessful")) {
                    // Close our streams
                    sin.close();
                    sout.close();
                    sslsocket.close();
                    return line;
                } else {
                    comms.add("S: " + line);
                    if(debug) {System.out.println(line);}
                }
            }

            cmd = "MAIL FROM:<" + this.from+">";
            comms.add("C: " + cmd);
            sout.println(cmd + "\r");

            line = sin.readLine();
            comms.add("S: " + line);
            if(debug) {System.out.println(line);}

            cmd = "RCPT TO:<" + this.recipient+">";
            comms.add("C: " + cmd);
            sout.println(cmd + "\r");

            line = sin.readLine();
            comms.add("S: " + line);
            if(debug) { System.out.println(line);}

            //
            // we send the DATA
            //
            sout.println("DATA\r");
            // should contain "End data with"
            line = sin.readLine();
            comms.add("S: " + line);
            if(debug) { System.out.println(line);}

            for(String data_l : data_a) {
                sout.println(data_l + "\r");
            }
            sout.println(".\r");

            line = sin.readLine();
            comms.add("S: " + line);

            // Close our streams
            sin.close();
            sout.close();
            sslsocket.close();

        } catch( Exception e ) {
            throw e;
        }

        return line;
    }

    protected void setRecipient(String recipient)
    {
        this.recipient = recipient;
    }

    protected void useStarttls()
    {
        this.starttls = true;
    }

    protected void setFrom(String from)
    {
        this.from = from;
    }

    protected void setUsername(String username)
    {
        this.username = username;
    }

    protected void setPassword(String password)
    {
        this.password = password;
    }


    protected void setAuthPlain(String auth_plain)
    {
        this.auth_plain = auth_plain;
    }

    protected void setHost(String host, int port)
    {
        this.host = host;
        this.port = port;
    }

    protected void setAuthType(int auth_type)
    {
        this.auth_type = auth_type;
    }


    protected ArrayList<String> getComms()
    {
        return this.comms;
    }

}
