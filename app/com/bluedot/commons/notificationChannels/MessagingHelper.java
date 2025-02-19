package com.bluedot.commons.notificationChannels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.utils.SendMail;
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.MessageFactory;
import com.twilio.sdk.resource.instance.Message;

import play.Play;

public class MessagingHelper {
	final static Logger logger = LoggerFactory.getLogger(MessagingHelper.class);

	private String from;
	private String host;
	private int port;
	private String username;
	private String password;

	private String signature;
	private Map<String, byte[]> attachments;

	public MessagingHelper() {

	}

	public MessagingHelper withPlayConfig() {
		from = Play.application().configuration().getString("mail.from", "");
		host = Play.application().configuration().getString("mail.host", "");
		port = Play.application().configuration().getInt("mail.port", 25);
		username = Play.application().configuration().getString("mail.user", "");
		password = Play.application().configuration().getString("mail.password", "");
		return this;
	}

	public MessagingHelper withCustomConfig(String from, String host, int port, String username, String password) {
		this.from = from;
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		return this;
	}

	public MessagingHelper withAttachment(Map<String, byte[]> attachments) {
		this.attachments = attachments;
		return this;
	}

	public MessagingHelper withSignature(String signature) {
		this.signature = signature;
		return this;
	}

	public String getValidationHost(String host) {
		return Play.application().configuration().getString("app.validation.host", "https://" + host);
	}

	public boolean sendEmail(String to, String textBody, String htmlBody, String subject, boolean html) {

		if (signature == null || "".equals(signature))
			signature = Play.application().configuration().getString("mail.signature", "");

		boolean logInstedOfSend = Play.application().configuration().getBoolean("mail.log", false);

		try {
			SendMail.sendMail(username, password, host, port, from, to, subject, textBody, null, attachments,
					logInstedOfSend);
		} catch (MessagingException e) {
			logger.error("Exception:", e);
			return false;
		}

		return true;
	}

	public boolean sendSMS(String to, String body, String subject, String signature) {
		return sendSMS(to, body, subject, signature, null);
	}

	public boolean sendSMS(String to, String body, String subject, String signature, String attachment) {
		String from = Play.application().configuration().getString("sms.from", "");
		String sid = Play.application().configuration().getString("sms.app.sid", "");
		String token = Play.application().configuration().getString("sms.app.token", "");

		boolean logInstedOfSend = Play.application().configuration().getBoolean("sms.log", false);

		TwilioRestClient client = new TwilioRestClient(sid, token);

		// Build the parameters
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("To", to));
		params.add(new BasicNameValuePair("From", from));

		if (signature != null && !"".equals(signature))
			body = "[" + signature + "]: " + body;

		params.add(new BasicNameValuePair("Body", body));

		if (attachment != null && attachment.endsWith("jpg"))
			params.add(new BasicNameValuePair("MediaUrl", attachment));

		MessageFactory messageFactory = client.getAccount().getMessageFactory();
		Message message;
		try {
			if (logInstedOfSend) {
				logger.info(">>>> SMS DUMP:");
				logger.info("FROM: " + from);
				logger.info("SUBJECT: " + subject);
				logger.info("TO: " + to);
				logger.info("BODY: " + body);
				logger.info("ATTACHMENT: " + attachment);
			} else {
				message = messageFactory.create(params);
				logger.info("SMS sent correctly to: " + to + ", sid: " + message.getSid());
			}
		} catch (TwilioRestException e) {
			logger.error("Exception:", e);
			return false;
		}
		return true;
	}
}
