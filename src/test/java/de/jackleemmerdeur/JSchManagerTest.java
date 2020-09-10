package de.jackleemmerdeur;

import org.junit.*;
//import org.mockito.AdditionalAnswers;
//import org.mockito.invocation.InvocationOnMock;
//import org.mockito.stubbing.Answer;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.*;

import java.io.*;
import java.nio.file.Paths;
import java.util.List;

@Ignore
public class JSchManagerTest {
    JSchManager w = null;

// ===============================================================================
// General rule: unit-tests should not depend on outside services.
// This is not achievable with the tests below that depend a live ssh-connection.
// Those test merely function as examples.
//
// And now for a completely failed try to mock parts of the classes for unit-tests.
// If I understood correctly Mockito works best with classes that utilize
// dependency-injection.
// For JSchManager it would need to be redesigned so it would receive instances of
// SSHChannelExec, SSHChannelSftp, SSHSession etc. via dependency injection.
// All of these injected objects would be mocked and e.g.
// SSHChannelShell.queryBuilder("ls", ...) would be moved to
// the main JSchManager instance, which is a real instance that would talk
// to the injected mocked instances.
// Is not feasible atm.
// ===============================================================================

//    JSchManager mockedManager = null;
//    SSHChannelExec mockedChannel = null;
//    StringBuilder mockBuilder = null;

//    @Before
//    public void beforeTest() {
//        try {
//            mockedManager = mock(JSchManager.class);
//            mockBuilder = new StringBuilder();
//            SSHSession sampleSession = mock(SSHSession.class);
//            sampleSession.session = mock(Session.class);
//            sampleSession.id = "test";
//            List<String> sampleList = new ArrayList<>();
//            sampleList.add("-rw-r--r--  1 pi   pi      33 Mar 19  2019 .nanorc\n");
//            sampleList.add("drwxr-xr-x  2 pi   pi    4096 Mar 18  2019 .picodrive\n");
//            sampleList.add("-rw-r--r--  1 pi   pi     675 Mar 13  2018 .profile\n");
//            SSHChannelShell sampleShell = mock(SSHChannelShell.class);
//            sampleShell.c = mock(Channel.class);
//            sampleShell.debug = false;
//            sampleShell.t = SSHChannel.SSHChannelType.Shell;
//            sampleShell.channelClosed = true;
//            when(mockedManager.openSession("test", "", "", "")).thenReturn(sampleSession);
//            when(mockedManager.openChannelShell("test", 0)).thenReturn(sampleShell);
//            when(sampleShell.queryArray("ls")).thenReturn(sampleList);
//            doAnswer(invokationOnMock -> {
//                StringBuilder b = (StringBuilder)invokationOnMock.getArguments()[1];
//                for (String line: sampleList) {
//                    b.append(line);
//                }
//                return b;
//            }).when(sampleShell).queryBuilder("ls", mockBuilder);
//            doAnswer(invokationOnMock -> SSHChannel.SSHChannelType.Shell).when(sampleShell).getChannelType();
//        } catch(Exception e) {
//            System.err.println(e.getMessage());
//        }
//    }

//    @Test
//    public void testMock() {
//        try {
//            SSHSession s = mockedManager.openSession("test", "", "", "");
//            if (s != null) {
//                SSHChannelShell e = mockedManager.openChannelShell("test", 0);
//                System.out.println(e.getChannelType());
//                e.queryBuilder("ls", mockBuilder);
//                System.out.println(mockBuilder.toString());
//            }
//        }catch (Exception e) { }
//    }

    @Before
    public void setUp() {
        w = new JSchManager(false);
    }

    @After
    public void tearDown() {
        try {
            if (w != null)
                w.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Ignore
    @Test
    public void testCleanLine() {
        String s = "less testfile\n" +
                "\u001B[?1h\u001B=\n" +
                "The man in black fled across the desert.\n" +
                "\u001B[7mtestfile (END)\u001B[m\u001B[K";
        s = SSHChannelShell.cleanConsoleLine(s, true);
        String so = "less testfile\n" +
                "\n" +
                "The man in black fled across the desert.\n" +
                "testfile (END)";
        Assert.assertEquals(so, s);
    }

    @Ignore
    @Test
    public void testExecChannel() {
        try {
            SSHSession s = w.openSession("test", "192.168.178.46", "pi", "raspberry", true, 2000);
            try(SSHChannelExec c = w.openChannelExec(s, "less .nanorc")) {
                StringBuilder b = new StringBuilder();
                c.readAllFromChannelExec(b, null, 0);
                System.out.println(b.toString());
                String expected = "set constantshow\nset linenumbers\n";
                Assert.assertEquals(expected, b.toString());
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Ignore
    @Test
    public void testSftpPut() {
        try {
            SSHSession s = w.openSession("test", "192.168.178.46", "pi", "raspberry", true, 2000);
            try(SSHChannelSftp c = w.openChannelSFTP(s, true, 1000)) {
                c.putFile("/home/pi", new File("/home/buccaneersdan/.bashrc"), "test");
                Assert.assertTrue(c.fileExistsSFTP("/home/pi", "test"));
                c.deleteFileSFTP("/home/pi", "test");
                Assert.assertFalse(c.fileExistsSFTP("/home/pi", "test"));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Ignore
    @Test
    public void testSftpReadFile() {
        try {
            String filecontents = "test123";
            String localdir = "/home/buccaneersdan";
            String localfile = "testfile";

            String remotedir = "/home/pi";
            String remotefile = "testfile";

            File f = Paths.get(localdir, localfile).toFile();

            if (!f.exists() && !f.createNewFile())
                throw(new Exception("Could not create " + localfile));

            try (FileOutputStream fo = new FileOutputStream(f)) {
                fo.write(filecontents.getBytes());
            } finally {
                SSHSession s = w.openSession("test", "192.168.178.46", "pi", "raspberry", true, 2000);
                try(SSHChannelSftp c = w.openChannelSFTP(s, true, 1000)) {
                    c.putFile(remotedir, f, remotefile);
                    Assert.assertTrue(c.fileExistsSFTP(remotedir, remotefile));
                    String expFilecontents = c.readFileSFTP(remotedir, remotefile, "utf-8", 1024, 1);
                    Assert.assertEquals(expFilecontents, filecontents);
                    c.deleteFileSFTP(remotedir, remotefile);
                    Assert.assertFalse(c.fileExistsSFTP(remotedir, remotefile));
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            File f = new File("/home/buccaneersdan/test");
            if (f.exists())
                Assert.assertTrue(f.delete());
        }
    }

    @Ignore
    @Test
    public void testShell() {
        try {
            SSHSession sess = w.openSession("test", "192.168.178.46", "pi", "raspberry", true, 2000);
            try(SSHChannelShell shell = w.openChannelShell(sess, 1000)) {
                shell.exec("cd .local");

                shell.exec("rm testfile");

                shell.exec("echo 'Cowabunga!' >> testfile");
                shell.exec("echo \"It's Pizza time.\" >> testfile");

                System.out.println("=== Read into List<String>");

                List<String> lines = shell.queryArray("cat testfile");
                for(String line: lines) {
                    System.out.println(line);
                }

                System.out.println("=== Read into StringBuilder");

                StringBuilder b = new StringBuilder();
                shell.queryBuilder("cat testfile", b);
                System.out.println(b);

                System.out.println("=== Read with Iterator");

                shell.exec("cd share");
                shell.exec("cd xorg");

                shell.queryBuilder("cat Xorg.0.log", null, false, null, null);
                shell.resetIterator();
                while(shell.hasNext()) {
                    String s = shell.next();
                    System.out.println(s);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
