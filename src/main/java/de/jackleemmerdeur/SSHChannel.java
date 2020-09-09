package de.jackleemmerdeur;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class SSHChannel implements AutoCloseable {
    public enum SSHChannelType {
        Exec,
        Sftp,
        Shell,
        None
    }

	protected static final int DEFAULT_TIMEOUT = 500;
    Channel c;
    SSHChannelType t;
    InputStream shellin = null;
    OutputStream shellout = null;
    InputStream shellerr = null;
    boolean autoCloseDone = false;
    boolean channelClosed = false;
    boolean debug;

    protected SSHChannel(SSHChannelType t, Channel c, boolean debug) {
        this.c = c;
        this.t = t;
        this.debug = debug;
    }

    protected SSHChannel(SSHChannelType t, Channel c) {
        this.c = c;
        this.t = t;
        this.debug = false;
    }

    @Override
    public void close() throws Exception {
        if (!autoCloseDone) {
            if (this.t == SSHChannelType.Shell) {
                try {
                    this.shellin.close();
                    if (this.debug) System.out.println("shellin closed");
                } catch (IOException e) {
                    System.err.format(JSchManager.ERR_SHELL_CLOSE_ISTREAM, e.getMessage());
                }

                try {
                    this.shellout.close();
                    if (this.debug) System.out.println("shellout closed");
                } catch (IOException e) {
                    System.err.format(JSchManager.ERR_SHELL_CLOSE_OSTREAM, e.getMessage());
                }

                try {
                    this.shellerr.close();
                    if (this.debug) System.out.println("shellerr closed");
                } catch (IOException e) {
                    System.err.format(JSchManager.ERR_SHELL_CLOSE_ESTREAM, e.getMessage());
                }
            }
            this.closeChannel();
        }
        this.autoCloseDone = true;
    }

    abstract void connectedEvent(boolean debug);

    abstract void disconnectedEvent(boolean debug);

    public void assertChannel() throws Exception {
        if (c == null) throw (new Exception(JSchManager.ERR_CHANNEL_IS_NULL));
        if (c.isClosed()) throw (new Exception(JSchManager.ERR_CHANNEL_IS_CLOSED));
        if (c.isEOF()) throw (new Exception(JSchManager.ERR_CHANNEL_IS_EOF));
    }

    public void assertShellStreams() throws Exception {
        if (shellin == null) throw (new Exception(JSchManager.ERR_SHELL_ISTREAM_INVALID));
        if (shellout == null) throw (new Exception(JSchManager.ERR_SHELL_OSTREAM_INVALID));
        if (shellerr == null) throw (new Exception(JSchManager.ERR_SHELL_ESTREAM_INVALID));
    }

    protected static int getTimeout(Integer timeoutMS)
	{
		return (timeoutMS == null || timeoutMS < DEFAULT_TIMEOUT) ? DEFAULT_TIMEOUT : timeoutMS;
	}

    public SSHChannelType getChannelType() {
        return t;
    }

    public Channel getChannel() {
        return c;
    }

    private  void closeChannel() throws Exception {
        if (c != null && !channelClosed) {
            if (t == SSHChannelType.Sftp)
                ((ChannelSftp)c).exit();
            this.disconnectChannel();
            channelClosed = true;
            if (this.debug) System.out.println("channel closed: " + this.t.toString());
        }
    }

    public void connectChannel(Integer connecttimeoutMS) throws Exception {
        if (c == null) throw (new Exception(JSchManager.ERR_CHANNEL_IS_NULL));
        if (!c.isConnected()) c.connect(SSHChannel.getTimeout(connecttimeoutMS));
        if (!c.isConnected()) throw (new Exception(JSchManager.ERR_CHANNEL_COULD_NOT_CONNECT));
        this.connectedEvent(this.debug);
    }

    public void disconnectChannel() throws Exception {
        if (c == null) throw (new Exception(JSchManager.ERR_CHANNEL_IS_NULL));
        if (c.isConnected()) c.disconnect();
        if (c.isConnected()) throw (new Exception(JSchManager.ERR_CHANNEL_COULD_NOT_DISCONNECT));
        this.disconnectedEvent(this.debug);
    }
}
