package de.jackleemmerdeur;

import org.junit.*;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Ignore
public class JSchManagerTest {
    JSchManager w = null;

    @BeforeClass
    public static void setUpClass() {

    }

    @AfterClass
    public static void tearDownClass() {

    }

    @Before
    public void beforeTest() {

    }

    @After
    public void afterTest() {

    }

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
