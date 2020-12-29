# nanosmtp-java

A Small SMTP Client Library for Java

## Usage 

```java
import com.augmentedlogic.nanosmtp.*;


       // simple usage (smtp via SSL/TLS)

       MailHost mailhost = new MailHost();
                mailhost.setHost("smtp.example.com", 465);
                mailhost.setAuth("me@example.com", "supersecretpassword");

       Email email = new Email();
             email.setFrom("me@example.com");
             email.setRecipient("someone@example.com");
             email.setSubject("Hello I am using a new java library");
             email.setBody("Hi. Just wanted to say I am testing SMTP!");

       try {
             EmailClient emailclient = new EmailClient();
             String response = emailclient.send(mailhost, email);
             System.out.println("RESPONSE: " + response);
       } catch( Exception e ) {
            e.printStackTrace();
       }



       // advanced Options

               // use STARTTLS instead of TLS directly

               mailhost.useStarttls(); 

               // use "AUTH LOGIN", default is "AUTH PLAIN" 

               mailhost.setAuthType(MailHost.AUTH_LOGIN); 

               // If you specify an HTML body in addition to setBody(), 
               // the email will be send as multipart/alternative

               email.setHtmlBody("Hi. Just wanted to say I am <b>testing</b> SMTP!");


       // debugging Options

               // print all messaging to stdout, default: false

               emailclient.setDebug(false);

               // do not actually send the email, instead just print the formatted 
               // email and headers to stdout.

               emailclient.setEnableSend(false);
       
```      
