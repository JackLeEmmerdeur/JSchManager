# JSchManager

## App.java

```java
package de.myorg.test;

import de.jackleemmerdeur.NanoBenchmark;

public class App {
    public static void main(String args[]) {
        try(JSchManager w = new JSchManager(true)) {
            SSHSession s = w.openSession("test", "192.168.178.30", "pi", "raspberry");
            try(SSHChannelShell c = w.openChannelShell(s, 1000)) {
                c.exec("cd .local");
                ArrayList<String> a = c.queryArray("ls -la");
                for(String l: a) {
                    System.out.println(l);
                }
            }
        } catch(Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
```

## pom.xml

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
