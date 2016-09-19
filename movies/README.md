Movies Sample Dataset
=====================

This sample dataset containing a small subset of the data behind the [Moogi](http://moogi.co) movie search engine.
It contains information on about 7300 movies and about 20000 people.

After having downloaded the files and started the MindmapsDB engine, the .gql files must be loaded into the engine in a specific order. _Notice that this can take easily take more than 30 minutes, depending on your computer._

First the schema file must be loaded, by running the following command into your favourite shell
```
curl -H "Content-Type: application/json" -X POST -d '{"path":"/PATH/TO/schema.gql"}' http://localhost:4567/import/ontology
```
After that, the data has to be loaded. Load the entities
```
curl -H "Content-Type: application/json" -X POST -d '{"path":"/PATH/TO/1-entities.gql"}' http://localhost:4567/import/batch/data
```
Then the ternary relations
```
curl -H "Content-Type: application/json" -X POST -d '{"path":"/PATH/TO/2-ternary-relations.gql"}' http://localhost:4567/import/batch/data
```
And finally the binary relations
```
curl -H "Content-Type: application/json" -X POST -d '{"path":"/PATH/TO/3-binary-relations.gql"}' http://localhost:4567/import/batch/data

```

If everything went well, you will be able to query you new dataset.

![Example Query](img/movie_query.png)
