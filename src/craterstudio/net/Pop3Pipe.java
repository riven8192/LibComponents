/*
 * Created on 29 jun 2009
 */

package craterstudio.net;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import craterstudio.bytes.Hash;
import craterstudio.io.FileUtil;
import craterstudio.net.smtp.Smtp;
import craterstudio.net.smtp.SmtpContact;
import craterstudio.net.smtp.SmtpMail;
import craterstudio.text.Text;
import craterstudio.util.HighLevel;

public class Pop3Pipe {
	public static void main(String[] args) throws Exception {

		int pop3_timeout = Integer.parseInt(Text.mainArgsLookup(args, "pop3_timeout", "600"));
		int smtp_timeout = Integer.parseInt(Text.mainArgsLookup(args, "smtp_timeout", "600"));
		int loop_delay = Integer.parseInt(Text.mainArgsLookup(args, "loop_delay", "60"));

		String dir = Text.mainArgsLookup(args, "dir", "./");
		final File dumpDir = new File(dir, "dump");
		final File sentDir = new File(dir, "sent");
		dumpDir.mkdirs();
		sentDir.mkdirs();

		Smtp.DEFAULT_HOST = Text.mainArgsLookup(args, "through");
		Smtp.OVERRIDE_MX_RECORDS_WITH_HOST = Smtp.DEFAULT_HOST;
		final Smtp smtp = new Smtp();

		final SmtpMail mail = new SmtpMail();
		mail.addTo(new SmtpContact(Text.mainArgsLookup(args, "to")));

		final List<String> pop3servers = Text.mainArgsLookups(args, "pop3server");
		final List<String> pop3users = Text.mainArgsLookups(args, "pop3user");
		final List<String> pop3passs = Text.mainArgsLookups(args, "pop3pass");

		if (pop3servers.size() != pop3users.size()) {
			throw new IllegalStateException();
		}
		if (pop3servers.size() != pop3passs.size()) {
			throw new IllegalStateException();
		}

		while (true) {

			Thread popToDisk = new Thread(new Runnable() {
				@Override
				public void run() {
					for (int i = 0; i < pop3servers.size(); i++) {
						Pop3 pop = null;
						try {
							pop = new Pop3(pop3servers.get(i));
							pop.login(pop3users.get(i), pop3passs.get(i));

							mail.setFrom(new SmtpContact(pop.getUser()));

							for (Pop3.Pop3ListEntry entry : pop.list()) {
								String uidl = pop.uidl(entry.index);
								String hash = Hash.toHexString(Hash.sha256(uidl));

								File emailFile1 = new File(dumpDir, hash + ".eml");
								File emailFile2 = new File(sentDir, hash + ".eml");
								if (emailFile1.exists() || emailFile2.exists()) {
									continue;
								}

								try (OutputStream out = new FileOutputStream(emailFile1);) {
									pop.retr(entry.index, out);
								}
							}

							pop.quit();
						} catch (Throwable cause) {
							cause.printStackTrace();
						} finally {
							if (pop != null) {
								try {
									pop.disconnect();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
					}
				}
			});
			popToDisk.start();
			popToDisk.join(pop3_timeout * 1000);
			if (popToDisk.isAlive()) {
				System.out.println("Killed popToDisk thread!");
				popToDisk.stop();
				continue;
			}

			// --

			Thread diskToSmtp = new Thread(new Runnable() {
				@Override
				public void run() {
					for (File file : dumpDir.listFiles()) {
						try {
							smtp.open();
							for (SmtpContact to : mail.getToList()) {
								try (InputStream emailFile = new FileInputStream(file)) {
									smtp.sendRaw(mail, to.address, emailFile);
								}
							}
							smtp.quit();

							String hash = Text.before(file.getName(), '.');
							File emailFile1 = new File(dumpDir, hash + ".eml");
							File emailFile2 = new File(sentDir, hash + ".eml");
							FileUtil.moveFile(emailFile1, emailFile2);
						} catch (Throwable cause) {
							cause.printStackTrace();
						}
					}
				}
			});
			diskToSmtp.start();
			diskToSmtp.join(smtp_timeout * 1000);
			if (diskToSmtp.isAlive()) {
				System.out.println("Killed diskToSmtp thread!");
				diskToSmtp.stop();
				continue;
			}

			HighLevel.sleep(loop_delay * 1000);
		}
	}
}
