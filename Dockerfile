FROM maven:3.3.9-jdk-8

ADD lookup /app/lookup
WORKDIR /app/lookup

RUN mvn install
RUN mvn package -Dmaven.test.skip=true

ADD 2015-04 /app/lookup/index/2015-04
ADD Dataset_de /app/lookup/dataset
ADD run_de.sh /app/lookup/index.sh
RUN chmod a+x /app/lookup/index.sh

# Expose port 1111
EXPOSE 1111

VOLUME ["/app/lookup/lookup_index_dir"]

# define default command as run server
#CMD ["./index.sh"]
CMD ["mvn", "scala:run", "-Dlauncher=Server", "-DaddArgs=/app/lookup/index/2015-04"]
