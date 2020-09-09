package de.jackleemmerdeur;
import com.jcraft.jsch.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class JSchManager
	implements AutoCloseable
{
	protected static final String ERR_STRINGBUILDER_NOT_INITIALIZED = "Stringbuilder not initialized";
	protected static final String ERR_COMMAND_INVALID = "Command was invalid";
	protected static final String ERR_PARAMETERS_INVALID = "One of the method parameters was invalid";

	protected static final String ERR_SESSION_ID_EXISTS = "The session-id %session already exists";
	protected static final String ERR_SESSION_INVALID = "Session was invalid";
	protected static final String ERR_SESSION_NOT_CONNECTED = "Session %s is not connected";

	protected static final String ERR_CHANNEL_IS_CLOSED = "Channel is closed";
	protected static final String ERR_CHANNEL_IS_NULL = "Channel is invalid";
	protected static final String ERR_CHANNEL_IS_EOF = "Channel is EOF";
	protected static final String ERR_CHANNEL_COULD_NOT_CONNECT = "Channel could not be connected";
	protected static final String ERR_CHANNEL_COULD_NOT_DISCONNECT = "Channel could not be disconnected";

	protected static final String ERR_EXEC_BEFORE_CONNECT = "The passed ChannelExec is not allowed to be connected before calling the method";
	protected static final String ERR_EXEC_INVALID = "No instance of ChannelExec is set to parent";

	protected static final String ERR_SFTP_REMOTEDIR_INVALID = "Remote-Directory invalid";
	protected static final String ERR_SFTP_REMOTEFILES_INVALID = "Remote-File(session) invalid";
	protected static final String ERR_SFTP_LOCAL_FILE_INVALID = "The local file is invalid";

	protected static final String ERR_SHELL_ISTREAM_INVALID = "The shell-input-stream is invalid";
	protected static final String ERR_SHELL_OSTREAM_INVALID = "The shell-output-stream is invalid";
	protected static final String ERR_SHELL_ESTREAM_INVALID = "The shell-error-stream is invalid";
	protected static final String ERR_SHELL_CLOSE_ISTREAM = "Error while closing shell-input-stream: %s";
	protected static final String ERR_SHELL_CLOSE_OSTREAM = "Error while closing shell-output-stream: %s";
	protected static final String ERR_SHELL_CLOSE_ESTREAM = "Error while closing shell-error-stream: %s";
	protected static final String ERR_SHELL_OPENED = "Error after opening shell";

	private final JSch jsch;
	private Map<String,SSHSession> sessions;
	private Map<String, ArrayList<SSHChannel>> channels;
	private boolean debug;

	public JSchManager(boolean debug)
	{
		jsch = new JSch();
		this.debug = debug;
	}

	@Override
	public void close()
		throws Exception
	{
		if (channels != null)
		{
			if (this.debug) System.out.println("closing channels");
			ArrayList<SSHChannel> sessionchannels;
			for (String key: channels.keySet())
			{
				sessionchannels = channels.get(key);
				if (sessionchannels != null)
				{
					for(SSHChannel channel: sessionchannels)
					{
						channel.close();
					}
					sessionchannels.clear();
				}
			}
			channels.clear();
		}

		if (sessions != null)
		{
			if (this.debug) System.out.println("closing sessions");
			for(String key: sessions.keySet())
			{
				SSHSession s = sessions.get(key);
				SSHSession.assertSession(s);
				JSchManager.closeSession(s.session, this.debug);
			}
			sessions.clear();
		}
	}

	protected static void assertDirname(String dir)
		throws Exception
	{
		if (stringIsEmpty(dir))
			throw (new Exception(ERR_SFTP_REMOTEDIR_INVALID));
	}

	protected static void assertFilename(String file)
		throws Exception
	{
		if (stringIsEmpty(file))
			throw (new Exception(ERR_SFTP_REMOTEFILES_INVALID));
	}

	protected static void assertFilenames(String[] files)
		throws Exception
	{
		if (stringsAreEmptyAny(files, false))
			throw (new Exception(ERR_SFTP_REMOTEFILES_INVALID));
	}

	public static boolean stringIsEmpty(String str, boolean preserveWhiteSpace) {
		if (preserveWhiteSpace)
			return str == null || str.length() == 0;
		else
			return str == null || str.trim().length() == 0;
	}

	public static boolean stringIsEmpty(String str) {
		return stringIsEmpty(str, false);
	}

	public static boolean stringsAreEmptyAny(boolean preserveWhiteSpace, String... strs) {
		return stringsAreEmptyAny(strs, preserveWhiteSpace);
	}

	public static boolean stringsAreEmptyAny(String[] strs, boolean preserveWhiteSpace) {
		boolean isEmpty = false;
		if (strs == null || strs.length == 0)
			return true;
		for (String str : strs) {
			if (stringIsEmpty(str, preserveWhiteSpace)) {
				isEmpty = true;
				break;
			}
		}
		return isEmpty;
	}

	private static boolean closeSession(Session session, boolean debug)
	{
		if (session != null && session.isConnected())
		{
			session.disconnect();
			if (debug) System.out.println("session disconnected");
			return true;
		}
		return false;
	}

	public boolean closeSession(String id)
			throws Exception
	{
		if (sessions != null && sessions.containsKey(id))
		{
			SSHSession s = sessions.get(id);
			SSHSession.assertSession(s);
			return JSchManager.closeSession(s.session, this.debug);
		}
		return false;
	}

	private SSHSession getSession(String sessionID)
	{
		if (sessions != null && sessions.containsKey(sessionID))
			return sessions.get(sessionID);
		return null;
	}

	public SSHSession openSession(
		String id,
		String url,
		String user,
		String pass)
			throws JSchException, Exception
	{
		return openSession(id, url, user, pass, true, null, null);
	}

	public SSHSession openSession(
		String id,
		String url,
		String user,
		String pass,
		boolean connectImmediately,
		Integer connectTimeoutMS
	)
		throws JSchException, Exception
	{
		return openSession(id, url, user, pass, connectImmediately, connectTimeoutMS, null);
	}

	public SSHSession openSession(
		String id,
		String url,
		String user,
		String pass,
		boolean connectImmediately,
		Integer connectTimeoutMS,
		Integer port
	)
		throws JSchException, Exception
	{
		if (sessions != null && sessions.containsKey(id))
			throw (new Exception(String.format(ERR_SESSION_ID_EXISTS, id)));
		if (stringIsEmpty(user) || stringIsEmpty(pass))
			throw (new Exception(ERR_PARAMETERS_INVALID));
		Session session = jsch.getSession(user, url, (port == null) ? 22 : port);
		session.setPassword(pass);
		session.setConfig("StrictHostKeyChecking", "no");
		if (connectImmediately) session.connect(SSHChannel.getTimeout(connectTimeoutMS));
		if (sessions == null) sessions = new HashMap<>();
		SSHSession s = new SSHSession(id, session);
		sessions.put(id, s);
		return s;
	}

	private SSHChannel addChannel(
		String sessionID,
		SSHChannel.SSHChannelType type,
		Channel channel,
		boolean removeANSIEscapeSequences,
		boolean trimLines,
		Integer bufferSize,
		Integer pauseAfterConnectMS,
		Integer pauseBetweenReadsMS,
		Integer pauseBetweenWritesMS
	)
	{
		if (channels == null) channels = new HashMap<>();
		ArrayList<SSHChannel> al;
		if (!channels.containsKey(sessionID))
		{
			al = new ArrayList<>();
			channels.put(sessionID, al);
		}
		else
		{
			al = channels.get(sessionID);
		}
		SSHChannel sshChannel = null;
		if (type == SSHChannel.SSHChannelType.Shell) {
			if (bufferSize != null && pauseAfterConnectMS != null && pauseBetweenReadsMS != null && pauseBetweenWritesMS != null)
				sshChannel = new SSHChannelShell((ChannelShell) channel, this.debug, removeANSIEscapeSequences, trimLines, bufferSize, pauseAfterConnectMS, pauseBetweenReadsMS, pauseBetweenWritesMS);
			else
				sshChannel = new SSHChannelShell((ChannelShell) channel, this.debug);
		} else if (type == SSHChannel.SSHChannelType.Exec)
			sshChannel = new SSHChannelExec((ChannelExec)channel, this.debug);
		else if (type == SSHChannel.SSHChannelType.Sftp)
			sshChannel = new SSHChannelSftp((ChannelSftp)channel, this.debug);
		al.add(sshChannel);
		return sshChannel;
	}

	public SSHChannelSftp openChannelSFTP(String sessionID, boolean connectImmediately, Integer connecttimeoutMS)
		throws JSchException, Exception
	{
		return openChannelSFTP(getSession(sessionID), connectImmediately, connecttimeoutMS);
	}

	public SSHChannelSftp openChannelSFTP(SSHSession session, boolean connectImmediately, Integer connecttimeoutMS)
			throws JSchException, Exception
	{
		SSHSession.assertSessionOpen(session);
		ChannelSftp channelSFTP = (ChannelSftp) session.session.openChannel("sftp");
		SSHChannelSftp sftpchannel = (SSHChannelSftp) addChannel(
			session.id, SSHChannel.SSHChannelType.Sftp, channelSFTP, false, false,null, null, null, null
		);
		if(connectImmediately) sftpchannel.connectChannel(SSHChannel.getTimeout(connecttimeoutMS));
		return sftpchannel;
	}

	public SSHChannelShell openChannelShell(
		String sessionID,
		boolean removeANSIEscapeSequences,
		boolean trimLines,
		int connectionTimeout,
		int bufferSize,
		int pauseAfterConnectMS,
		int pauseBetweenReadsMS,
		int pauseBetweenWritesMS
	)
		throws JSchException, Exception
	{
		return openChannelShell(getSession(sessionID), removeANSIEscapeSequences, trimLines, connectionTimeout, bufferSize, pauseAfterConnectMS, pauseBetweenReadsMS, pauseBetweenWritesMS);
	}

	public SSHChannelShell openChannelShell(
		SSHSession session,
		boolean removeANSIEscapeSequences,
		boolean trimLines,
		int connectionTimeout,
		int bufferSize,
		int pauseAfterConnectMS,
		int pauseBetweenReadsMS,
		int pauseBetweenWritesMS
	)
		throws JSchException, Exception
	{
		SSHSession.assertSessionOpen(session);
		ChannelShell channelShell = (ChannelShell) session.session.openChannel("shell");
		channelShell.setInputStream(null);
		channelShell.setOutputStream(null);
		channelShell.setExtOutputStream(System.err);
		SSHChannelShell sshchannel = (SSHChannelShell) addChannel(
			session.id,
			SSHChannel.SSHChannelType.Shell,
			channelShell,
			removeANSIEscapeSequences,
			trimLines,
			bufferSize == -1 ? null : bufferSize,
			pauseAfterConnectMS == -1 ? null : pauseAfterConnectMS,
			pauseBetweenReadsMS == -1 ? null : pauseBetweenReadsMS,
			pauseBetweenWritesMS == -1 ? null : pauseBetweenWritesMS
		);
		sshchannel.connectChannel(SSHChannel.getTimeout(connectionTimeout));
		return sshchannel;
	}

	public SSHChannelShell openChannelShell(String sessionID, int connectionTimeout)
		throws JSchException, Exception
	{
		return openChannelShell(sessionID, true, true, -1, -1, -1, -1, -1);
	}

	public SSHChannelShell openChannelShell(SSHSession session, int connectionTimeout)
			throws JSchException, Exception
	{
		return openChannelShell(session, true, true, -1, -1, -1, -1, -1);
	}

	public SSHChannelExec openChannelExec(String sessionID, String command)
		throws Exception
	{
		return openChannelExec(getSession(sessionID), command);
	}

	public SSHChannelExec openChannelExec(SSHSession session, String command)
			throws Exception
	{
		SSHSession.assertSessionOpen(session);
		if (stringIsEmpty(command)) throw new Exception(JSchManager.ERR_COMMAND_INVALID);
		ChannelExec channelExec = (ChannelExec) session.session.openChannel("exec");
		SSHChannelExec sshchannel = (SSHChannelExec) addChannel(
			session.id, SSHChannel.SSHChannelType.Exec, channelExec, true, true,  null, null, null, null
		);
		if (!stringIsEmpty(command))
			channelExec.setCommand(command);
		return sshchannel;
	}
}
