FROM maven

WORKDIR /app/lookup

# Prepare by downloading dependencies
ADD pom.xml /app/lookup/pom.xml
ADD swagger.json /app/lookup/swagger.json
RUN mvn install

# Adding source, compile and package into a fat jar
ADD src /app/lookup/src
RUN mvn package -Dmaven.test.skip=true

# Expose port 1111
EXPOSE 1111

# define default command as run server
CMD ["mvn", "scala:run", "-Dlauncher=Server", "-DaddArgs=/app/lookup/index"]
