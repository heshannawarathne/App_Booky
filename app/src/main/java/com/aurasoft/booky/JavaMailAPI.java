package com.aurasoft.booky;

import android.os.AsyncTask;
import android.util.Log;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

    public class JavaMailAPI extends AsyncTask<Void, Void, Void> {

        private String email;
        private String subject;
        private String message;

        private final String MY_EMAIL = "pramodnawarathna0@gmail.com";
        private final String MY_PASSWORD = "asla ahlw zphy dosy";

        public JavaMailAPI(String email, String subject, String message) {
            this.email = email;
            this.subject = subject;
            this.message = message;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d("MAIL_API", "Starting Email Sending Process...");

            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

            Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(MY_EMAIL, MY_PASSWORD);
                }
            });

            session.setDebug(true);

            try {
                MimeMessage mm = new MimeMessage(session);
                mm.setFrom(new InternetAddress(MY_EMAIL));
                mm.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
                mm.setSubject(subject);
                mm.setContent(message, "text/html; charset=utf-8");

                Transport.send(mm);
                Log.d("MAIL_API", "SUCCESS: Email Sent! 🎉");

            } catch (Exception e) {
                Log.e("MAIL_API", "FATAL ERROR: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }
    }

