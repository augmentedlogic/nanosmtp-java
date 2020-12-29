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

public class EmailClient
{

    private Boolean debug = false;
    private Boolean send_enabled = true;

    private ArrayList<String> comms = new ArrayList<String>();

    private String getBoundary()
    {
        int leftLimit = 48;
        int rightLimit = 122;
        int length = 42;
        Random random = new Random();

        String boundary = random.ints(leftLimit, rightLimit + 1)
            .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97)).limit(length)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();

        return boundary;
    }

    private String getIsoDate()
    {
        Date date = new Date();
        DateFormat df = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z");
        return df.format(date);
    }

    public String send(MailHost mailhost, Email emailc) throws Exception
    {

        // perform some checks

        String boundary = null;
        ArrayList<String> data_a = new ArrayList<String>();
        data_a.add("From: " + emailc.getFrom());
        data_a.add("To: " + emailc.getRecipient());
        data_a.add("Date: " + getIsoDate());

        // todo X-Mailer, Sender

        if(!emailc.isHtmlMultipart()) {
            data_a.add("Content-Type: text/plain; charset=utf-8");
        }
        if(emailc.isHtmlMultipart()) {
            // else add with boundary
            boundary = getBoundary();
            data_a.add("MIME-Version: 1.0");
            data_a.add("Content-Type: multipart/alternative; boundary=\""+ boundary+ "\"");
        }

        data_a.add("Subject: " + emailc.getSubject());

        if(emailc.isHtmlMultipart()) {
            data_a.add("");
            data_a.add("--" + boundary);
            data_a.add("Content-Type: text/plain; charset=UTF-8");
            data_a.add("Content-Transfer-Encoding: quoted-printable");
            data_a.add("");
            data_a.add(emailc.getBody());
        } else {
            data_a.add("");
            data_a.add(emailc.getBody());
        }

        if(emailc.isHtmlMultipart()) {
            data_a.add("--" + boundary);
            data_a.add("Content-Type: text/html; charset=UTF-8");
            data_a.add("Content-Transfer-Encoding: quoted-printable");
            data_a.add("");
            data_a.add(emailc.getHtmlBody());
            data_a.add("--" + boundary + "--");
        }

        String response = null;

        try {

            SmtpClient  smtp = new SmtpClient();
            smtp.setHost(mailhost.getHost(), mailhost.getPort());
            smtp.setAuthPlain(mailhost.getAuth());
            smtp.setUsername(mailhost.getUsername());
            smtp.setPassword(mailhost.getPassword());
            if(mailhost.getStarttls()) {
                smtp.useStarttls();
            }
            smtp.setFrom(emailc.getFrom());
            smtp.setRecipient(emailc.getRecipient());
            smtp.setAuthType(mailhost.getAuthType());

            if(this.debug) {
                for(String data_l : data_a) {
                    System.out.println(data_l);
                }
            }
            if(this.send_enabled) {
                response =  smtp.send(data_a, this.debug);
                this.comms = smtp.getComms();
            }

        } catch(Exception e) {
            throw e;
        }

        return response;
    }

    public void setDebug(Boolean debug)
    {
        this.debug = debug;
    }

    public void setSendingEnabled(Boolean sending)
    {
        this.send_enabled = sending;
    }

    public ArrayList<String> getComms()
    {
        return this.comms;
    }


}
