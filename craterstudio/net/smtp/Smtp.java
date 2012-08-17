/*
 * Created on 7-jun-2005
 */
package craterstudio.net.smtp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import craterstudio.io.Streams;
import craterstudio.net.DNS;
import craterstudio.text.Text;
import craterstudio.text.TextDateTime;
import craterstudio.text.TextHttpDate;

public class Smtp {
	public static final boolean VERBOSE_DEFAULT = true;
	public static String HELO_HOSTNAME = DNS.queryHostname();
	public static String DEFAULT_HOST = "localhost";
	public static int DEFAULT_PORT = 25;

	public static String OVERRIDE_MX_RECORDS_WITH_HOST = null; // "mail.kpnplanet.nl";
	public static String OVERRIDE_DATE = null;

	static {
		new Thread(new Runnable() {
			@Override
			public void run() {
				// HELO_HOSTNAME = DNS.queryHostname();
			}
		}).start();
	}

	//

	private final boolean verbose;
	private BufferedReader reader;
	private Writer writer;

	public Smtp() {
		this(VERBOSE_DEFAULT);
	}

	public Smtp(boolean verbose) {
		this.verbose = verbose;
	}

	public final void open() throws IOException {
		Socket socket;

		try {
			socket = new Socket(DEFAULT_HOST, DEFAULT_PORT);
		} catch (IOException exc) {
			throw new IOException("failed to connect to: " + DEFAULT_HOST + " @ " + DEFAULT_PORT, exc);
		}

		this.open(socket);
	}

	public final void open(Socket socket) throws IOException {
		if (HELO_HOSTNAME == null)
			throw new IllegalStateException("Specify " + Smtp.class.getName() + ".HELO_HOSTNAME first.");

		System.out.println("SMTP <--> connected: " + socket);

		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		writer = new OutputStreamWriter(socket.getOutputStream());

		String s;

		do {
			s = this.readln();
			if (s.startsWith("220-"))
				continue;
			if (s.startsWith("220 "))
				break;
			throw new IllegalStateException("failed on first line: '" + s + "''");
		} while (true);

		this.writeln("HELO " + HELO_HOSTNAME);
		if (!this.readln().startsWith("250"))
			throw new IllegalStateException("failed on 'HELO' command");
	}

	public final void send(SmtpMail mail, String rcpt) throws IOException {
		mail.verify();

		this.writeln("MAIL FROM: <" + mail.getFrom().address + ">");
		if (!this.readln().startsWith("250 "))
			throw new IllegalStateException("failed on 'MAIL FROM' command");

		this.writeln("RCPT TO: <" + rcpt + ">");
		if (!this.readln().startsWith("250 "))
			throw new IllegalStateException("failed on 'RCTP TO' command");

		this.writeln("DATA");
		if (!this.readln().startsWith("354 "))
			throw new IllegalStateException("failed on 'DATA' command");

		String from = mail.getFrom().toSmtpString();
		String to = SmtpContact.toSmtpString(mail.getToList());

		this.writeln("Date: " + (Smtp.OVERRIDE_DATE == null ? TextHttpDate.now() : Smtp.OVERRIDE_DATE));
		this.writeln("From: " + from);
		this.writeln("To: " + to);

		String cc = SmtpContact.toSmtpString(mail.getCcList());
		if (cc != null)
			this.writeln("Cc: " + cc);

		// String bcc = MailContact.asSMTPString(mail.getBccList());
		// if (bcc != null)
		// this.writeln("Bcc: " + bcc);

		this.writeln("Subject: " + mail.getSubject());

		String body = mail.getBody();

		if (body.startsWith("MIME-Version: 1.0")) {
			this.writeln(body);
		} else if (mail.getContentType() != null) {
			this.writeln("MIME-Version: " + mail.getMimeVersion());
			this.writeln("Content-Type: " + mail.getContentType());
			this.writeln("Content-Transfer-Encoding: 7bit");
			this.writeln("");

			//

			body = transformBody(body);
			body = Text.convertWhiteSpaceTo(body, ' ');
			body = Text.removeDuplicates(body, ' ');

			for (String line : wordWrap(body, 80)) {
				this.writeln(line);
			}
		} else {
			this.writeln("");

			for (String line : wordWrap(transformBody(body), 80)) {
				this.writeln(line);
			}
		}

		this.writeln("");
		this.writeln("."); // END_OF_MESSAGE

		if (!this.readln().startsWith("250 "))
			throw new IllegalStateException("failed on '.' command");
	}

	public static final String transformBody(String body) {
		// get rid of special characters
		for (int i = 0; i < '\t'; i++)
			body = Text.remove(body, (char) i);

		body = Text.replace(body, "\r\n", "\n");// collapse "\r\n"
		body = Text.replace(body, '\r', '\n'); // get rid of "\r"
		body = Text.replace(body, "\n", "\r\n"); // force "\r\n" line breaks

		// prevent END_OF_MESSAGE
		body = Text.replace(body, "\r\n.\r\n", "\r\n..\r\n");

		return body;
	}

	public final void quit() {
		try {
			this.writeln("QUIT");

			this.readln();
		} catch (IOException exc) {
			// ignore
		}

		Streams.safeClose(reader);
		Streams.safeClose(writer);

		System.out.println("SMTP <--> disconnected");
	}

	private String readln() throws IOException {
		String line = reader.readLine();
		if (this.verbose) {
			String output = line;
			if (output.length() > 80)
				output = "[__TRIMMED__] " + output.substring(0, 80) + " [__TRIMMED__]";
			System.out.println("SMTP <<-- " + output);
		}
		return line;
	}

	private void writeln(String line) throws IOException {
		if (this.verbose) {
			String output = line;
			if (output.length() > 80)
				output = "[__TRIMMED__] " + output.substring(0, 80) + " [__TRIMMED__]";
			System.out.println("SMTP -->> " + output);
		}

		writer.write(line + "\r\n");
		writer.flush();
	}

	private static List<String> wordWrap(String body, int maxLineLength) {
		List<String> lines = new ArrayList<String>();

		for (String line : Text.split(body, "\r\n")) {
			if (line.length() <= maxLineLength) {
				lines.add(line);
				continue;
			}

			StringBuilder current = new StringBuilder();
			for (String word : Text.split(line, ' ')) {
				if (current.length() + 1 + word.length() > maxLineLength) {
					lines.add(current.toString());
					current.setLength(0);
				}
				current.append(word).append(' ');
			}
			if (current.length() > 0) {
				current.setLength(current.length() - 1);
				lines.add(current.toString());
			}
		}

		return lines;
	}
}