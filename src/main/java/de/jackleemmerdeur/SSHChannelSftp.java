package de.jackleemmerdeur;

import com.jcraft.jsch.ChannelSftp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class SSHChannelSftp extends SSHChannel {

    protected SSHChannelSftp(ChannelSftp c, boolean debug) {
        super(SSHChannelType.Sftp, c, debug);
    }

    @Override
    public void connectedEvent(boolean debug) {
        if (debug) System.out.println("connected event: sftp");
    }

    @Override
    public void disconnectedEvent(boolean debug, Object userdata) {
        if (debug) System.out.println("disconnected event: sftp");
    }

    public void putFile(String dirname, File f, String newRemoteFilename)
            throws Exception {
        super.assertChannel();
        JSchManager.assertDirname(dirname);
        JSchManager.assertFilename(newRemoteFilename);
        if (f == null || !f.exists())
            throw (new Exception(JSchManager.ERR_SFTP_LOCAL_FILE_INVALID));
        ChannelSftp csftp = (ChannelSftp) super.c;
        csftp.cd(dirname);
        csftp.put(new FileInputStream(f), newRemoteFilename, ChannelSftp.OVERWRITE);
    }

    public boolean fileExistsSFTP(String dirname, String filename)
            throws
            Exception {
        super.assertChannel();
        JSchManager.assertDirname(dirname);
        JSchManager.assertFilename(filename);
        SSHCustomLsEntrySelector sel = new SSHCustomLsEntrySelector(filename);
        ChannelSftp csftp = (ChannelSftp) super.c;
        csftp.ls(dirname, sel);
        return sel.exists;
    }

    public boolean deleteFileSFTP(
            String dirname,
            String filename
    )
            throws Exception {
        super.assertChannel();
        JSchManager.assertDirname(dirname);
        JSchManager.assertFilename(filename);
        if (fileExistsSFTP(dirname, filename)) {
            String file = dirname + (dirname.charAt(dirname.length() - 1) == '/' ? "" : "/") + filename;
            ChannelSftp csftp = (ChannelSftp) super.c;
            csftp.rm(file);
            return true;
        }
        return false;
    }

    public boolean deleteFilesSFTP(
            String dirname,
            String[] filenames
    )
            throws Exception {
        super.assertChannel();
        JSchManager.assertDirname(dirname);
        JSchManager.assertFilenames(filenames);
        int deleted = 0;
        for (String filename : filenames) {
            if (fileExistsSFTP(dirname, filename)) {
                String file = dirname + (dirname.charAt(dirname.length() - 1) == '/' ? "" : "/") + filename;
                ChannelSftp csftp = (ChannelSftp) super.c;
                csftp.rm(file);
                deleted++;
            }
        }
        return (deleted == filenames.length);
    }

    private InputStreamReader readFileFromChannelSFTP(
            String dirname,
            String filename,
            String encoding
    )
            throws Exception {
        super.assertChannel();
        JSchManager.assertDirname(dirname);
        JSchManager.assertFilename(filename);
        ChannelSftp csftp = (ChannelSftp) super.c;
        csftp.cd(dirname);
        return (new InputStreamReader(csftp.get(filename), JSchManager.stringIsEmpty(encoding) ? "UTF-8" : encoding));
    }

    public String readFileSFTP(
            String dirname,
            String filename,
            String encoding,
            Integer bufferSize,
            Integer maxMB
    )
            throws Exception {
        super.assertChannel();
        JSchManager.assertDirname(dirname);
        JSchManager.assertFilename(filename);

        int bs = (bufferSize == null) ? SSHChannelExec.DEFAULT_BUFFER_SIZE : bufferSize;
        int paraBreak = (maxMB == null) ? 0 : (maxMB * 1000000) / bs;

        try (InputStreamReader r = readFileFromChannelSFTP(dirname, filename, encoding)) {
            BufferedReader b = new BufferedReader(r);
            char[] chunk = new char[bs];
            StringBuilder sb = new StringBuilder();
            int i = 0;
            int n;

            while ((n = b.read(chunk)) > -1) {
                sb.append(chunk, 0, n);
                i++;
                if (paraBreak > 0 && i > paraBreak) {
                    // Paranoid android - This file is approximately bigger
                    // than maxMB Megabytes, stop here.
                    break;
                }
            }
            return sb.toString();
        }
    }
}

