package de.jackleemmerdeur;

import com.jcraft.jsch.Session;

public class SSHSession {
    Session session;
    String id;

    protected SSHSession(String id, Session s) {
        this.id = id;
        this.session = s;
    }

    public Session getRawSession() {
        return session;
    }

    public String getID() {
        return id;
    }

    public static void assertSession(SSHSession sess)
            throws Exception {
        if (sess == null || sess.session == null) throw (new Exception(JSchManager.ERR_SESSION_INVALID));
    }

    public static void assertSessionOpen(SSHSession sess)
            throws Exception {
        SSHSession.assertSession(sess);
        if (!sess.session.isConnected())
            throw (new Exception(String.format(JSchManager.ERR_SESSION_NOT_CONNECTED, sess.id)));
    }
}
