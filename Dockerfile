FROM maven

WORKDIR /app/lookup

# Prepare by downloading dependencies
ADD pom.xml /app/lookup/pom.xml
ADD swagger.yaml /app/lookup/swagger.yaml
ADD index /app/lookup/index
RUN mvn install

# Adding source, compile and package into a fat jar
ADD src /app/lookup/src
RUN mvn package -Dmaven.test.skip=true

# Expose port 11001  For  Titan Server
EXPOSE 11001

# define default command as run server
CMD ["mvn", "scala:run", "-Dlauncher=Server", "-DaddArgs=/app/lookup/index"]
