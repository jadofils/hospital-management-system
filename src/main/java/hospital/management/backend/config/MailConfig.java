package hospital.management.backend.config;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import java.util.Properties;

/**
 * Provides a pre-configured Jakarta Mail Session for Gmail SMTP (STARTTLS, port 587).
 *
 * Required .env keys:
 *   GMAIL_HOST        — smtp.gmail.com
 *   GMAIL_PORT        — 587
 *   GMAIL_USERNAME    — sender Gmail address
 *   GMAIL_PASSWORD    — Gmail App Password (not your account password)
 *   GMAIL_FROM_NAME   — display name shown in the From header
 *
 * Gmail App Password setup:
 *   Google Account → Security → 2-Step Verification → App passwords
 *   Generate one for "Hospital Management System" and put it in GMAIL_PASSWORD.
 *
 * Usage:
 *   Session session = MailConfig.getSession();
 *   Message msg = new MimeMessage(session);
 *   msg.setFrom(new InternetAddress(MailConfig.getFromAddress(), MailConfig.getFromName()));
 *   msg.setRecipient(Message.RecipientType.TO, new InternetAddress("patient@email.com"));
 *   msg.setSubject("Appointment Confirmed");
 *   msg.setContent("<h1>Hello</h1>", "text/html; charset=utf-8");
 *   Transport.send(msg);
 */
public final class MailConfig {

    private static final AppLogger logger   = AppLogger.getLogger(MailConfig.class);
    private static final Session   SESSION;

    static {
        Properties props = new Properties();
        props.put("mail.smtp.host",            EnvConfig.getMailHost());
        props.put("mail.smtp.port",            String.valueOf(EnvConfig.getMailPort()));
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.protocols",   "TLSv1.2 TLSv1.3");

        SESSION = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                    EnvConfig.getMailUsername(),
                    EnvConfig.getMailPassword()
                );
            }
        });

        logger.info("Mail session configured — SMTP " +
                    EnvConfig.getMailHost() + ":" + EnvConfig.getMailPort());
    }

    private MailConfig() {}

    /** Returns the shared, authenticated Jakarta Mail Session. */
    public static Session getSession() { return SESSION; }

    /** The RFC-5321 envelope sender address (GMAIL_USERNAME). */
    public static String getFromAddress() { return EnvConfig.getMailUsername(); }

    /** The human-readable display name shown in the From header (GMAIL_FROM_NAME). */
    public static String getFromName() { return EnvConfig.getMailFromName(); }
}