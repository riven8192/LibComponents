/*
 * Created on 7-jun-2005
 */
package craterstudio.net;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Writer;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;

import craterstudio.streams.BinaryLineReader;
import craterstudio.text.Text;
import craterstudio.text.TextValues;

public class Pop3 {
	private final InputStream in;
	// private final BufferedReader reader;
	private final Writer writer;

	public Pop3(String host) throws IOException {
		this(host, 110);
	}

	public Pop3(String host, int port) throws IOException {
		this(new Socket(host, port));
	}

	public Pop3(Socket socket) throws IOException {
		// reader = new BufferedReader(new
		// InputStreamReader(socket.getInputStream()));
		in = new BufferedInputStream(socket.getInputStream());
		writer = new OutputStreamWriter(socket.getOutputStream());

		this.readLine();
	}

	private void writeln(String line) throws IOException {
		System.out.println("POP3 -->> " + line);
		writer.write(line + "\r\n");
		writer.flush();
	}

	private String readLine() {
		String line = BinaryLineReader.readLineAsString(this.in);
		System.out.println("POP3 <<-- " + line);
		return line;
	}

	private String user, pass;

	public String getUser() {
		return this.user;
	}

	public String getPass() {
		return this.pass;
	}

	public final void login(String user, String pass) throws IOException {
		this.user = null;
		this.pass = null;
		
		this.writeln("USER " + user);
		String line1 = this.readLine();
		if (!line1.startsWith("+OK"))
			throw new IllegalStateException("error response: " + line1);
		this.user = user;

		this.writeln("PASS " + pass);
		String line2 = this.readLine();
		if (!line2.startsWith("+OK"))
			throw new IllegalStateException("error response: " + line2);
		this.pass = pass;
	}

	public final Pop3Stat stat() throws IOException {
		this.writeln("STAT");
		String response = this.readLine();
		if (!response.startsWith("+OK"))
			throw new IllegalStateException("error response: " + response);

		String text = Text.splitPair(response, ' ')[1];

		int[] values = TextValues.parseInts(Text.splitPair(text, ' '));
		return new Pop3Stat(values[0], values[1]);
	}

	public final Pop3ListEntry list(int msg) throws IOException {
		this.writeln("LIST " + msg);
		String response = this.readLine();
		if (!response.startsWith("+OK"))
			throw new IllegalStateException("error response: " + response);

		String line = response.substring(4);
		int[] values = TextValues.parseInts(Text.splitPair(line, ' '));
		return new Pop3ListEntry(values[0], values[1]);
	}

	public final List<Pop3ListEntry> list() throws IOException {
		this.writeln("LIST");
		String response = this.readLine();
		if (!response.startsWith("+OK")) {
			throw new IllegalStateException("error response: " + response);
		}

		List<Pop3ListEntry> entries = new ArrayList<Pop3ListEntry>();

		while (true) {
			String line = this.readLine(); // n len

			if (line.equals("."))
				break;

			int[] values = TextValues.parseInts(Text.splitPair(line, ' '));
			entries.add(new Pop3ListEntry(values[0], values[1]));
		}

		return entries;
	}

	public final String uidl(int msg) throws IOException {
		this.writeln("UIDL " + msg);
		String response = this.readLine();
		if (!response.startsWith("+OK"))
			throw new IllegalStateException("error response: " + response);
		return Text.split(response, ' ')[2];
	}

	public final byte[] retr(int msg) throws IOException {
		this.writeln("RETR " + msg);
		String response = this.readLine();
		if (!response.startsWith("+OK"))
			throw new IllegalStateException("error response: " + response);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while (true) {
			byte[] line = BinaryLineReader.readLineInc(this.in, 64 * 1024);
			if (line.length == ".\r\n".length() && Text.ascii(line).equals(".\r\n")) {
				break;
			}
			baos.write(line);
		}

		return baos.toByteArray();
	}

	/**
	 * DELE
	 */

	public final boolean dele(int msg) throws IOException {
		this.writeln("DELE " + msg);
		String response = this.readLine();
		return response.startsWith("+OK");
	}

	/**
	 * NOOP
	 */

	public final boolean noop() throws IOException {
		this.writeln("NOOP");
		String response = this.readLine();
		return response.startsWith("+OK");
	}

	/**
	 * QUIT
	 */

	public final boolean quit() throws IOException {
		this.writeln("QUIT");
		String response = this.readLine();
		return response.startsWith("+OK");
	}

	/**
    * 
    */

	public final void disconnect() throws IOException {
		in.close();
		writer.close();
	}

	public class Pop3ListEntry {
		public Pop3ListEntry(int index, int size) {
			this.index = index;
			this.size = size;
		}

		public final int index;
		public final int size;

		public String toString() {
			return "Entry[index=" + index + ", size=" + size + "]";
		}
	}

	public class Pop3Stat {
		public Pop3Stat(int count, int size) {
			this.count = count;
			this.size = size;
		}

		public final int count;
		public final int size;

		public String toString() {
			return "Stat[count=" + count + ", size=" + size + "]";
		}
	}
}
