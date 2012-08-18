package craterstudio.net;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.List;

import craterstudio.io.Streams;
import craterstudio.io.TransferListener;
import craterstudio.text.Text;
import craterstudio.text.TextValues;

public class FtpConnection
{
   static int counter = 10000;

   public FtpConnection(Socket socket) throws IOException
   {
      this(null, socket, false);
   }

   public FtpConnection(String name, Socket socket) throws IOException
   {
      this(name, socket, false);
   }

   public FtpConnection(String name, Socket socket, boolean verbose) throws IOException
   {
      this(name, socket, verbose, true);
   }

   private FtpConnection(String name, Socket socket, boolean verbose, boolean consumeWelcome) throws IOException
   {
      this.verbose = verbose;

      if (name == null)
         name = String.valueOf(++counter);

      this.name = name;
      this.socket = socket;
      reader = new BufferedReader(new InputStreamReader(input = socket.getInputStream()));
      writer = new OutputStreamWriter(output = socket.getOutputStream());

      while (consumeWelcome)
      {
         String line = this.readLine();
         if (!line.startsWith("220-"))
            break;
      }
   }

   private final boolean verbose;

   String                name;
   Socket                socket;

   InputStream           input;
   OutputStream          output;

   BufferedReader        reader;
   Writer                writer;

   public final void login(String user, String pass) throws IOException
   {
      this.socket.setSoTimeout(10 * 1000);

      this.sendLine("USER " + user);
      this.readLine();
      this.sendLine("PASS " + pass);

      String line = this.readLine();

      this.socket.setSoTimeout(10 * 60 * 1000);

      if (this.isResponse(line, 230))
         return;
      if (this.isResponse(line, 530))
         throw new IllegalStateException("invalid credentials");

      throw new IllegalStateException(line);
   }

   public final void binaryType() throws IOException
   {
      this.sendLine("TYPE I");
      this.readLine();
   }

   public final void asciiType() throws IOException
   {
      this.sendLine("TYPE A");
      this.readLine();
   }

   public final boolean stor(String filename, byte[] data) throws IOException
   {
      return this.stor(filename, new ByteArrayInputStream(data));
   }

   public final boolean stor(String filename, File localFile) throws IOException
   {
      if (!localFile.exists())
         return false;

      FileInputStream fis = new FileInputStream(localFile);
      boolean success = this.stor(filename, fis);
      Streams.safeClose(fis);
      return success;
   }

   public final boolean stor(String filename, InputStream in) throws IOException
   {
      return this.stor(filename, in, null);
   }

   public final boolean stor(String filename, InputStream in, TransferListener tl) throws IOException
   {
      FtpConnection data = this.pasv();
      this.sendLine("STOR " + filename);
      String line = this.readLine();

      if (this.isResponse(line, 550))
         return false;
      this.checkResponse(line, 150);

      byte[] buf = new byte[8192];

      int bytes;

      if (tl != null)
         tl.transferInitiated(-1);

      while ((bytes = in.read(buf)) != -1)
      {
         data.output.write(buf, 0, bytes);
         if (tl != null)
            tl.transfered(bytes);
      }

      if (tl != null)
         tl.transferFinished(null);

      Streams.safeClose(in);

      data.disconnect();

      this.checkResponse(this.readLine(), 226);

      return true;
   }

   public final boolean retr(String file, OutputStream out) throws IOException
   {
      return this.retr(file, out, null);
   }

   public final boolean retr(String file, OutputStream out, TransferListener tl) throws IOException
   {
      FtpConnection data = this.pasv();
      this.sendLine("RETR " + file);

      String line = this.readLine();

      if (this.isResponse(line, 550))
         return false;
      this.checkResponse(line, 150);

      byte[] buf = new byte[8192];

      if (tl != null)
         tl.transferInitiated(-1);

      int bytes;
      while ((bytes = data.input.read(buf)) != -1)
      {
         if (tl != null)
            tl.transfered(bytes);
         out.write(buf, 0, bytes);
      }

      if (tl != null)
         tl.transferFinished(null);

      data.disconnect();

      this.checkResponse(this.readLine(), 226);

      return true;
   }

   public final byte[] retr(String file) throws IOException
   {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      if (this.retr(file, baos))
         return baos.toByteArray();

      return null;
   }

   public final boolean cwd(String path) throws IOException
   {
      this.sendLine("CWD " + path);
      String line = this.readLine();

      if (this.isResponse(line, 250))
         return true;
      if (this.isResponse(line, 550))
         return false;

      throw new IllegalStateException(line);
   }

   public final boolean mkd(String path) throws IOException
   {
      this.sendLine("MKD " + path);
      String line = this.readLine();

      if (this.isResponse(line, 257))
         return true;
      if (this.isResponse(line, 550))
         return false;

      throw new IllegalStateException(line);
   }

   public final String pwd() throws IOException
   {
      this.sendLine("PWD");
      String line = this.readLine();
      this.checkResponse(line, 257);

      return line.substring(line.indexOf('\"') + 1, line.lastIndexOf('\"'));
   }

   public final boolean dele(String path) throws IOException
   {
      this.sendLine("DELE " + path);
      return this.isResponse(this.readLine(), 250);
   }

   public final boolean rmd(String path) throws IOException
   {
      this.sendLine("RMD " + path);
      return this.isResponse(this.readLine(), 250);
   }

   public final void rename(String from, String to) throws IOException
   {
      this.sendLine("RNFR " + from);
      this.checkResponse(this.readLine(), 350);

      this.sendLine("RNTO " + to);
      this.checkResponse(this.readLine(), 250);
   }

   public final String[] list() throws IOException
   {
      return this.list(null);
   }

   public final String[] list(String path) throws IOException
   {
      FtpConnection data = this.pasv();
      this.sendLine(path != null ? "LIST " + path : "LIST");
      this.readLine();

      List<String> lines = new LinkedList<String>();

      String line;
      while ((line = data.readLine()) != null)
         lines.add(line);

      data.disconnect();
      this.readLine();

      return lines.toArray(new String[lines.size()]);
   }

   private final FtpConnection pasv() throws IOException
   {
      this.sendLine("PASV");
      String line = this.readLine();
      int[] numbers = TextValues.parseInts(Text.split(Text.between(line, "(", ")"), ','));
      byte[] rawHost = { (byte) numbers[0], (byte) numbers[1], (byte) numbers[2], (byte) numbers[3] };
      int rawPort = (numbers[4] << 8) | (numbers[5]);

      InetSocketAddress addr = new InetSocketAddress(InetAddress.getByAddress(rawHost), rawPort);
      Socket sock = new Socket(addr.getAddress(), addr.getPort());
      return new FtpConnection("data", sock, this.verbose, false);
   }

   private final String readLine() throws IOException
   {
      if (verbose)
         System.out.print("FTP <<-- [" + name + "]");

      String line = reader.readLine();

      if (verbose)
         if (line != null)
            System.out.println(": " + line);
         else
            System.out.println(": >EOF<");

      return line;
   }

   private final void sendLine(String line) throws IOException
   {
      if (verbose)
         System.out.print("FTP -->> [" + name + "]");

      writer.write(line + "\r\n");
      writer.flush();

      if (verbose)
         System.out.println(": " + line);
   }

   private final boolean isResponse(String line, int code)
   {
      return line.startsWith(String.valueOf(code) + " ");
   }

   private final void checkResponse(String line, int code)
   {
      if (!this.isResponse(line, code))
         throw new IllegalStateException("unexpected response: " + line);
   }

   public final void disconnect() throws IOException
   {
      reader.close();
      writer.close();
      socket.close();
   }

   // utilities

   public class RemoteFile
   {
      RemoteFile(String path, String name, boolean isDirectory)
      {
         this.path = path;
         this.name0 = name;
         this.isDirectory = isDirectory;
      }

      private final String path;

      public String getPath()
      {
         return path;
      }

      private final String name0;

      public String getName()
      {
         return name0;
      }

      private final boolean isDirectory;

      public boolean isDirectory()
      {
         return isDirectory;
      }
   }

   public final RemoteFile[] listAsUnix() throws IOException
   {
      return this.listAsUnix(null);
   }

   public final RemoteFile[] listAsUnix(String path) throws IOException
   {
      String remPath = (path == null ? this.pwd() : path);

      String[] list = this.list(path);
      RemoteFile[] files = new RemoteFile[list.length];

      int p = 0;
      for (String fileLine : list)
      {
         fileLine = Text.removeDuplicates(fileLine, ' ');
         String[] parts = Text.split(fileLine, ' ');

         boolean isDir = parts[0].charAt(0) == 'd';
         String filename = parts[8];

         files[p++] = new RemoteFile(remPath, filename, isDir);
      }

      return files;
   }
}
