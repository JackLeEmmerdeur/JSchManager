package de.jackleemmerdeur;

import com.jcraft.jsch.ChannelExec;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

public class SSHChannelExec extends SSHChannel {

    protected static final int DEFAULT_BUFFER_SIZE = 4096;

    protected SSHChannelExec(ChannelExec c, boolean debug) {
        super(SSHChannelType.Exec, c, debug);
    }

    @Override
    public void connectedEvent(boolean debug) {

        if (debug) System.out.println("connected event: exec");
    }

    @Override
    public void disconnectedEvent(boolean debug, Object userdata) {
        if (userdata instanceof AtomicInteger && super.c != null) {
            try {
                AtomicInteger i = (AtomicInteger) userdata;
                i.set(super.c.getExitStatus());
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
        if (debug) System.out.println("disconnected event: exec");
    }

    /**
     * @param builder                     The StringBuilder into which the output of the command
     *                                    running on the the execChannel, will be written
     * @param returnCode                  If null the return-code won't be set otherwise
     *                                    the internal value of the AtomicInteger will be the
     *                                    final command integer return value
     * @param breakExitCodesOtherThanThis When the status of the exit-code of
     *                                    the command does not equal breakExitCodesOtherThanThis the
     *                                    output-read-loop will break
     * @throws Exception Throws if StringBuilder or the channel is invalid or already connected
     */
    public void readAllFromChannelExec(StringBuilder builder, AtomicInteger returnCode, Integer breakExitCodesOtherThanThis)
            throws Exception {
        readAllFromChannelExec(builder, returnCode, null, null, null, breakExitCodesOtherThanThis);
    }

    /**
     * @param builder                     The StringBuilder into which the output of the command
     *                                    running on the the execChannel, will be written
     * @param returnCode                  If null the return-code won't be set otherwise
     *                                    the internal value of the AtomicInteger will be the
     *                                    final command integer return value
     * @param connectTimeout              The timeout in milliseconds, when trying to connect
     *                                    to the ssh-execution-channel Defaults to 1000 if 0 was passed
     * @param initialSleep                The initial time in milliseconds between connecting
     *                                    and the start of reading (workaround for a bug in JSch-lib) Defaults to
     *                                    1000 if 0 was passed
     * @param pauseBetweenBufferFill      The time in milliseconds the buffer was
     *                                    completely filled Defaults to 1000 if 0 was passed
     * @param breakExitCodesOtherThanThis When the status of the exit-code of
     *                                    the command does not equal breakExitCodesOtherThanThis the
     *                                    output-read-loop will break
     * @throws Exception Throws if StringBuilder or the channel is invalid or already connected
     */
    public void readAllFromChannelExec(
            StringBuilder builder,
            AtomicInteger returnCode,
            Integer connectTimeout,
            Integer initialSleep,
            Integer pauseBetweenBufferFill,
            Integer breakExitCodesOtherThanThis
    )
            throws Exception {
        ChannelExec cexec = null;
        try {
            if (builder == null)
                throw (new Exception(JSchManager.ERR_STRINGBUILDER_NOT_INITIALIZED));

            super.assertChannel();

            cexec = (ChannelExec) super.c;
            if (cexec == null)
                throw (new Exception(JSchManager.ERR_EXEC_INVALID));

            if (cexec.isConnected())
                throw (new Exception(JSchManager.ERR_EXEC_BEFORE_CONNECT));

            cexec.setInputStream(null);
            cexec.setErrStream(System.err);

            InputStream in = cexec.getInputStream();

            if (!cexec.isConnected()) {
                super.connectChannel(connectTimeout);
                Thread.sleep((initialSleep == null || initialSleep == 0) ? 1000 : initialSleep);
            }

            int bufsize = 4096;
            byte[] bytebuf = new byte[bufsize];
            boolean isclosed;
            int exitstatus;
            int i;
            int pbbf = (pauseBetweenBufferFill == null) ? 20 : pauseBetweenBufferFill;

            while (true) {
                while (in.available() > 0) {
                    i = in.read(bytebuf, 0, bufsize);
                    if (i < 0) break;
                    builder.append(new String(bytebuf, 0, i));
                }

                isclosed = cexec.isClosed();
                exitstatus = cexec.getExitStatus();
                if (isclosed) break;
                if (breakExitCodesOtherThanThis != null &&
                        breakExitCodesOtherThanThis != exitstatus)
                    break;
                if (pbbf > 0)
                    Thread.sleep(pbbf);
            }
        } finally {
            if (cexec != null)
                super.disconnectChannel(returnCode);
        }
    }
}

