FROM dockerfile/java

ADD ${project.artifactId}-${project.version}-jar-with-dependencies.jar /
# TODO Replace the following with ZooKeeper
ADD ../../../../../../../../.aws/credentials/AwsCredentials.properties /

CMD ["java -jar ${project.artifactId}-${project.version}-jar-with-dependencies.jar"]

