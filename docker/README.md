## Supported tags and respective Dockerfile links
* latest (DBpedia dump 2015-10)

## How to run

* English    - ```docker run -p 1111:1111 -it dbpedia/lookup java -jar /opt/lookup/dbpedia-lookup-3.1-jar-with-dependencies.jar /opt/lookup/2015-10/``


And then try 

```
http://localhost:1111/api/search/PrefixSearch?QueryClass=&MaxHits=5&QueryString=berl
```

If you are using Docker Compose you can do the same with this minimal Compose file:

```yml
version: '2'
services:
  lookup:
    container_name: lookup
    image: dbpedia/lookup
    ports:
     - "1111:1111"
    command: java -jar /opt/lookup/dbpedia-lookup-3.1-jar-with-dependencies.jar /opt/lookup/2015-10/
```

## Supported Docker versions
This image is officially supported on Docker version 1.9.1.

Please see the [Docker installation documentation] (https://docs.docker.com/installation/) for details on how to upgrade your Docker daemon.


## Issues
If you have any problems with or questions about this image, please contact us through a [GitHub issue](http://github.com/dbpedia/lookup/issues).


## Contributing

First of all, thank you for helping! :) .

Please see [DBpedia Contribute Guide](https://github.com/dbpedia/lookup/wiki/Contributing) for details on how to contribute
