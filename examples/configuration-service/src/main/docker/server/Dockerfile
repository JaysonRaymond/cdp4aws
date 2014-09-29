FROM dockerfile/java

ADD ${project.artifactId}-${project.version}-jar-with-dependencies.jar /
# TODO Replace the following with ZooKeeper
RUN ["/bin/mkdir", "-p", "/root/.aws/credentials/"]
ADD AwsCredentials.properties /root/.aws/credentials/AwsCredentials.properties

CMD ["/usr/bin/java", "-jar", "${project.artifactId}-${project.version}-jar-with-dependencies.jar"]

EXPOSE 80

