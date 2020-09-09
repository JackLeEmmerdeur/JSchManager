## JSchManager

A wrapper library for the SSH library [JSch by JCraft](http://www.jcraft.com/jsch/).

### <ins>Features</ins>

#### SSHChannelShell:

Is capable of issuing a command sequence, which is done via the shell-channel of JSch.

Internally SSHChannelShell reads all available data from the channels inputstream after issuing a command.

The the output of a command can be retrieved as an ArrayList<String> or be appended to a StringBuilder.
	
A missing feature is, that the integer return-value of a command can't be retrieved, therefore you have to analyze the string-output and know what to expect.  

See Example further down

#### SSHChannelExec

This class is able to retrieve the integer return-value of a command but cannot issue a command sequence, to o e.g. `cd` multiple times and then list the contents of a file

See Example further down

#### Other Channel-Types and usages

See [/test/java/de/jackleemmerdeur/JSchManagerTest.java](https://github.com/JackLeEmmerdeur/JSchManager/blob/master/src/test/java/de/jackleemmerdeur/SSHWrapperTest.java)

### <ins>Install library</ins>

git clone https://github.com/JackLeEmmerdeur/JschManager.git

Open the folder with IntelliJ

Open the Maven-tool-window and click install

### <ins>Usage</ins>

Create a new Maven-project.

Create a the classpath de.myorg (replace myorg with your organization) in the src/main/java folder

Add App.java to that classpath

Use dependency and optional maven-shade-plugin from pom.xml below

### <ins>Example</ins>

```java
package de.myorg.test;

import de.jackleemmerdeur.NanoBenchmark;

public class App {
    public static void main(String args[]) {
        try(JSchManager w = new JSchManager(true)) {
            SSHSession s = w.openSession("test", "192.168.178.46", "pi", "raspberry");

            // Use a shell to issue consecutive commands
	    try(SSHChannelShell c = w.openChannelShell(s, 1000)) {
                c.exec("cd .local");

                ArrayList<String> a = c.queryArray("ls -la");
                for(String l: a) {
                    System.out.println(l);
                }

                c.exec("cd ..");

                StringBuilder b = new StringBuilder();
                c.queryBuilder("cat .bashrc", b);
                System.out.println(b);
            }
	    
	    // Use an exec channel to issue a one-time command (or multiple in one go).
	    // No interaction with the SSHChannelExec-Instance is allowed after readAllFromChannelExec() is executed.
	    // The try-with-block ensures resource deallocation and channel-closing.
	    AtomicInteger retcode = new AtomicInteger();
            try(SSHChannelExec e = w.openChannelExec("test", "cd .local && cd share && cd xorg && cat Xorg.0.log")) {
                StringBuilder b = new StringBuilder();
                e.readAllFromChannelExec(b, retcode, 0);
            } finally {
                System.out.println(retcode.get());
            }
	    
        } catch(Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
```

### <ins>pom.xml</ins>

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
		 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<groupId>de.myorg</groupId>
	<artifactId>Test</artifactId>
	<version>1.0-SNAPSHOT</version>
	<properties>
		<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
		<maven.compiler.source>1.8</maven.compiler.source>
		<maven.compiler.target>1.8</maven.compiler.target>
	</properties>
	<build>
		<plugins>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-shade-plugin</artifactId>
				<version>3.2.4</version>
				<executions>
					<execution>
						<phase>package</phase>
						<goals>
							<goal>shade</goal>
						</goals>
						<configuration>
							<transformers>
								<transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
									<mainClass>de.myorg.test.App</mainClass>
								</transformer>
							</transformers>
						</configuration>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
	<dependencies>
		<dependency>
			<groupId>de.jackleemmerdeur</groupId>
			<artifactId>JSchManager</artifactId>
			<version>0.1</version>
		</dependency>
	</dependencies>
</project>
```
