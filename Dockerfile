FROM maven:3.3.9-jdk-8

WORKDIR /app/lookup

#RUN apt-get update
#RUN apt-get install -y maven

ADD pom.xml /app/lookup/pom.xml
ADD swagger.yaml /app/lookup/swagger.yaml
ADD index /app/lookup/index
ADD src /lookup/src
RUN mvn install
# Adding source, compile and package into a fat jar
RUN mvn package -Dmaven.test.skip=true

# Expose port 1111
EXPOSE 1111

# define default command as run server
CMD ["mvn", "scala:run", "-Dlauncher=Server", "-DaddArgs=/app/lookup/index"]
