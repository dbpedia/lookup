FROM java:8

# Install maven
RUN apt-get update
RUN apt-get install -y maven

WORKDIR /Users/Kunal/workspace/lookup

# Prepare by downloading dependencies
ADD pom.xml /Users/Kunal/workspace/lookup/pom.xml
RUN ["mvn", "dependency:resolve"]
RUN ["mvn", "verify"]

#Need to download datasets

# Adding source, compile and package into a fat jar
ADD src /lookup/src
RUN ["mvn", "package"]

EXPOSE 1111
CMD ["/usr/lib/jvm/java-8-openjdk-amd64/bin/java", "-jar", "target/dbpedia-lookup-3.1-jar-with-dependencies.jar"]
