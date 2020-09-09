package de.jackleemmerdeur;

import com.jcraft.jsch.ChannelShell;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class SSHChannelShell extends SSHChannel implements Iterator<String> {
    protected static final int DEFAULT_BUFFER_SIZE = 1024;
    protected static final int DEFAULT_PAUSE_BETWEEN_READS_MS = 1000;
    protected static final int DEFAULT_PAUSE_BETWEEN_WRITES_MS = 500;
    protected static final int DEFAULT_PAUSE_AFTER_CONNECT_MS = 500;

    private int bufferSize;
    private int pauseBetweenReadsMS;
    private int pauseBetweenWritesMS;
    private int pauseAfterConnectMS;
    private boolean removeANSITerminalSequences;
    private boolean trimLines;
    private ArrayList<String> lines = new ArrayList<>();
    private StringBuilder linebuffer;

    protected SSHChannelShell(
            ChannelShell c,
            boolean debug,
            boolean removeANSITerminalSequences,
            boolean trimLines,
            int bufferSize,
            int pauseAfterConnectMS,
            int pauseBetweenReadsMS,
            int pauseBetweenWritesMS
    ) {
        super(SSHChannelType.Shell, c, debug);
        this.removeANSITerminalSequences = removeANSITerminalSequences;
        this.trimLines = trimLines;
        this.bufferSize = bufferSize;
        this.pauseBetweenReadsMS = pauseBetweenReadsMS;
        this.pauseBetweenWritesMS = pauseBetweenWritesMS;
        this.pauseAfterConnectMS = pauseAfterConnectMS;
    }

    protected SSHChannelShell(ChannelShell c, boolean debug) {
        super(SSHChannelType.Shell, c, debug);
        this.removeANSITerminalSequences = true;
        this.trimLines = true;
        this.bufferSize = DEFAULT_BUFFER_SIZE;
        this.pauseBetweenReadsMS = DEFAULT_PAUSE_BETWEEN_READS_MS;
        this.pauseBetweenWritesMS = DEFAULT_PAUSE_BETWEEN_WRITES_MS;
        this.pauseAfterConnectMS = DEFAULT_PAUSE_AFTER_CONNECT_MS;
    }

    public void resetStreams() {
        try {
            c.setInputStream(null);
            c.setOutputStream(null);
            super.shellin = c.getInputStream();
            super.shellout = c.getOutputStream();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void connectedEvent(boolean debug) {

        boolean err = false;
        try {
            resetStreams();
            c.setExtOutputStream(System.err);
            super.shellerr = c.getExtInputStream();
            Thread.sleep(this.pauseAfterConnectMS);
        } catch (Exception e) {
            System.err.format(JSchManager.ERR_SHELL_OPENED, e.getMessage());
            err = true;
        } finally {
            if (!err) {
                try {
                    this.readIn(null, null, null, false, false);
                } catch (Exception e) {
                    System.err.format(JSchManager.ERR_SHELL_OPENED, e.getMessage());
                    err = true;
                }
            }
        }
        if (debug) {
            if (!err)
                System.out.println("Success connected event: shell");
            else
                System.out.println("Failed connected event: shell");
        }
    }

    @Override
    public void disconnectedEvent(boolean debug, Object userdata) {
        if (debug) System.out.println("disconnected event: shell");
    }

    public static String cleanConsoleLine(String line, boolean afterTrim) {
        if (JSchManager.stringIsEmpty(line)) return "";
        // https://stackoverrun.com/de/q/8369993
        String linenew = line.replaceAll("(\\u001b\\[.*?[@-~])|(\\u001b=)", "");
        if (afterTrim)
            linenew = linenew.trim();
        return linenew;
    }

    public void exec(String command)
            throws Exception {
        this.queryBuilder(command, null, true, null, null);
    }

    public List<String> queryArray(String command)
            throws Exception {
        return queryArray(command, true, null, null);
    }

    public List<String> queryArray(String command, boolean readInput, Integer buffersizeBytes, Integer pauseBetweenReadsMS)
            throws Exception {

        super.assertChannel();
        super.assertShellStreams();

        super.shellout.write((command + "\n").getBytes());
        super.shellout.flush();

        List<String> list = null;

        if (this.pauseBetweenWritesMS > -1)
            Thread.sleep(this.pauseBetweenWritesMS);

        if (readInput) {
            StringBuilder b = new StringBuilder();
            readIn(b, buffersizeBytes, pauseBetweenReadsMS, false, false);
            if (b.length() > 0)
                list = readLines(b, buffersizeBytes, false);
        }

        return list;
    }

    public void queryBuilder(String command, StringBuilder b)
            throws Exception {
        queryBuilder(command, b, true, null, null);
    }

    public void queryBuilder(String command, StringBuilder b, boolean readInput, Integer buffersizeBytes, Integer pauseBetweenReadsMS)
            throws Exception {
        super.assertChannel();
        super.assertShellStreams();

        super.shellout.write((command + "\n").getBytes());
        super.shellout.flush();

        if (this.pauseBetweenWritesMS > -1)
            Thread.sleep(this.pauseBetweenWritesMS);

        if (readInput)
            readIn(b, buffersizeBytes, pauseBetweenReadsMS, false, false);
    }

    private int readIn(StringBuilder b, Integer buffersizeBytes, Integer pauseBetweenReadsMS, boolean doPause, boolean onlyReadBufferSize)
            throws Exception {
        int dpauseBetweenReads = (pauseBetweenReadsMS != null && pauseBetweenReadsMS > 100) ? pauseBetweenReadsMS : this.pauseBetweenReadsMS;
        int dbuffersize = (buffersizeBytes != null && buffersizeBytes > 100) ? buffersizeBytes : this.bufferSize;
        byte[] tmp = new byte[dbuffersize];
        boolean available;
        super.assertChannel();
        available = super.shellin.available() > 0;
        int n = 0;
        while (available) {
            int i = super.shellin.read(tmp, 0, dbuffersize);
            n += i;
            if (i < 0) break;
            if (b != null)
                b.append(new String(tmp, 0, i));
            if (onlyReadBufferSize && n >= dbuffersize)
                break;
            available = super.shellin.available() > 0;
        }
        if (doPause)
            Thread.sleep(dpauseBetweenReads);
        return n;
    }

    public void resetIterator() {
        if (lines != null)
            lines.clear();
        if (linebuffer != null)
            linebuffer.delete(0, linebuffer.length());
    }

    public void breakIterator() throws Exception {
        resetIterator();
        readIn(null, 5000, null, false, false);
    }

    @Override
    public boolean hasNext() {
        if (lines != null && !lines.isEmpty())
            return true;
        readLines(null, 512, true);
        return (lines != null && !lines.isEmpty());
    }

    @Override
    public String next()
            throws NoSuchElementException {
        String s;
        if (hasNext()) {
            s = lines.get(0);
            lines.remove(0);
        } else {
            throw (new NoSuchElementException());
        }
        return s;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("SSHChannelShell iterator is read-only");
    }

    private List<String> readLines(StringBuilder tempBuilder, Integer bufferSize, boolean onlyReadBufferSize) {
        StringBuilder usedBuilder;
        boolean useTempBuilder = false;

        if (tempBuilder != null) {
            usedBuilder = tempBuilder;
            useTempBuilder = true;
        } else {
            if (linebuffer == null)
                linebuffer = new StringBuilder();
            usedBuilder = linebuffer;
        }
        boolean hasLine = false;

        ArrayList<String> usedArray;

        if (useTempBuilder) {
            usedArray = new ArrayList<>();
        } else {
            usedArray = lines;
        }

        while (true) {
            if (usedBuilder.length() > 0 && usedBuilder.indexOf("\n") > -1)
                hasLine = true;

            if (!hasLine) {
                try {
                    int n = readIn(usedBuilder, (bufferSize != null && bufferSize > 100) ? bufferSize : 1024, null, false, onlyReadBufferSize);
                    if (usedBuilder.length() == 0) {
                        break;
                    } else if (n == 0) {
                        hasLine = true;
                    }
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                    break;
                }
            }
            if (hasLine)
                break;
        }

        if (hasLine) {
            String line;
            while (true) {
                if (usedBuilder.length() > 0) {
                    int lbpos = usedBuilder.indexOf("\n");

                    if (lbpos > -1) {
                        line = usedBuilder.substring(0, lbpos);
                        if (removeANSITerminalSequences)
                            line = cleanConsoleLine(line, trimLines);
                        usedArray.add(line);
                        usedBuilder.delete(0, lbpos + 1);
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        return usedArray;
    }
}

