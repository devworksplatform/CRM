import com.jcraft.jsch.*;
import java.io.*;
import java.util.Properties;
import java.util.regex.*;

public class ssh {

    private final String keyPath;
    private final String remoteHost;
    private final String remoteUser;
    private String currentDir;
    private final String serverCmdPrefix;
    private final Callbacker callbacker;

    public interface Callbacker {
        void onOutput(String output);
        void onError(String error);
    }

    public ssh(String keyPath, String remoteHost, String remoteUser, String initialDir, Callbacker callbacker) {
        this.keyPath = keyPath;
        this.remoteHost = remoteHost;
        this.remoteUser = remoteUser;
        this.currentDir = initialDir;
        this.serverCmdPrefix = "source venv/bin/activate && python3 serv.py";
        this.callbacker = callbacker;
    }

    private Session createSession() throws JSchException {
        JSch jsch = new JSch();
        jsch.addIdentity(keyPath);
        Session session = jsch.getSession(remoteUser, remoteHost, 22);
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();
        return session;
    }

    private void closeSession(Session session) {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    public void runCommandWithCallback(final String command) {
        new Thread(new Runnable() {
            public void run() {
                Session session = null;
                ChannelExec channel = null;
                InputStream in = null;
                InputStream err = null;
                try {
                    session = createSession();
                    channel = (ChannelExec) session.openChannel("exec");

                    if (command.startsWith("cd ")) {
                        Matcher cdMatcher = Pattern.compile("^\\s*cd\\s+(.+)$").matcher(command);
                        if (cdMatcher.find()) {
                            String path = cdMatcher.group(1).trim();
                            String newPath = (path.startsWith("/") || path.startsWith("~")) ? path : currentDir + "/" + path;
                            channel.setCommand("cd " + newPath + " && pwd");
                            in = channel.getInputStream();
                            err = channel.getErrStream();
                            channel.connect();
                            String result = getOutput(in);
                            String error = getOutput(err);
                            if (channel.getExitStatus() == 0 && result != null) {
                                currentDir = result.trim();
                                callbacker.onOutput("Changed directory to: " + currentDir);
                            } else if (error != null && !error.isEmpty()) {
                                callbacker.onError("Error changing directory: " + error.trim());
                            }
                            return;
                        }
                    }

                    String fullCommand = "cd " + currentDir + " && " + command;
                    channel.setCommand(fullCommand);
                    in = channel.getInputStream();
                    err = channel.getErrStream();
                    channel.connect();
                    String result = getOutput(in);
                    String error = getOutput(err);

                    if (channel.getExitStatus() == 0 && result != null) {
                        callbacker.onOutput(result.trim());
                    } else if (error != null && !error.isEmpty()) {
                        callbacker.onError("Error executing command: " + error.trim());
                    }

} catch (JSchException e) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    callbacker.onError("JSchException: " + e.getMessage() + "\nStack Trace:\n" + sw.toString());
                } catch (IOException e) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    callbacker.onError("IOException: " + e.getMessage() + "\nStack Trace:\n" + sw.toString());
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                    if (err != null) {
                        try {
                            err.close();
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                    if (channel != null && channel.isConnected()) {
                        channel.disconnect();
                    }
                    if (session != null && session.isConnected()) {
                        closeSession(session);
                    }
                }
            }
        }).start();
    }

    private String getOutput(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        return output.toString();
    }
}
