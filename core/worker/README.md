Here's how to launch Docker packaging:


jraymond$ mvn docker:package -Ddocker.host=http://192.168.59.103:2375

[INFO] Scanning for projects...
[INFO]                                                                         
[INFO] ------------------------------------------------------------------------
[INFO] Building cdp4aws Request Queueing Example 1.0-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO] 
[INFO] --- docker-maven-plugin:1.3.0:package (default-cli) @ cdp4aws-examples-requestqueueing ---
[INFO] properties filtering supported for [projectVersion, gwt.version, gwt-compile-mode, project.build.finalName, surefire-failsafe-version, project.version, camel-version, appTitle, artigen-version, project.groupId, snapshots-distribution-repo, project.artifactId, project.build.sourceEncoding, maven.build.timestamp.format, distribution.repository, project.description, build-timestamp, source-repo-site, source-repo-scmroot, rootGroupId, archetype-version, project.name, distribution.site, rootArtifactId, test_server_port, project.reporting.outputEncoding, test_server_host, releases-distribution-repo]
[INFO] 1 * Client out-bound request
1 > GET http://192.168.59.103:2375/v1.11/version
1 > Accept: application/json

[INFO] 1 * Client in-bound response
1 < 200
1 < Content-Type: application/json
1 < Job-Name: version
1 < Date: Sun, 29 Jun 2014 22:53:10 GMT
1 < Content-Length: 148
1 < 
{"ApiVersion":"1.12","Arch":"amd64","GitCommit":"990021a","GoVersion":"go1.2.1","KernelVersion":"3.14.1-tinycore64","Os":"linux","Version":"1.0.1"}


[INFO] Docker version 1.0.1
[INFO] package processor
[INFO]  - add /Users/jraymond/projects/disney/cdp4aws/examples/request-queueing/target/cdp4aws-examples-requestqueueing-1.0-SNAPSHOT-jar-with-dependencies.jar
[INFO]  - add /Users/jraymond/projects/disney/cdp4aws/examples/request-queueing/target/classes/AwsCredentials.properties
[INFO] 2 * Client out-bound request
2 > POST http://192.168.59.103:2375/v1.11/build?t=dtss/processor:$%7Bproject.artifactId%7D-$%7Bproject.version%7D
2 > Content-Type: application/tar
2 > Accept: text/plain
Dockerfile0100644 0000000 0000000 00000000467 12365542707 011621 0ustar000000000 0000000 FROM dockerfile/java

ADD cdp4aws-examples-requestqueueing-1.0-SNAPSHOT-jar-with-dependencies.jar /
# TODO Replace the following with ZooKeeper
ADD AwsCredentials.properties ~/.aws/credentials/AwsCredentials.properties

CMD ["java -jar cdp4aws-examples-requestqueueing-1.0-SNAPSHOT-jar-with-dependencies.jar"]

cdp4aws-examples-requestqueueing-1.0-SNAPSHOT-jar-with-dependencies.jar0100644 0000000 0000000 00120734357 12365542211 024644 0ustar000000000 0000000 PK