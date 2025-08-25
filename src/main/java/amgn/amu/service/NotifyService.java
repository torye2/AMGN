package amgn.amu.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotifyService {
    private static final Logger log = LoggerFactory.getLogger(NotifyService.class);
    public void sendSms(String to, String text) {
        log.info("Send SMS to {}: {}", to, text);
    }
    public void sendEmail(String to, String subject, String body) {
        log.info("Send Email to {}: {} / {}", to, subject, body);
    }
}
