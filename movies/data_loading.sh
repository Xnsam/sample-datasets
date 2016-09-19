TMP=`echo $PWD`
SCHEMA=\'\{\"path\":\"$TMP/schema.gql\"\}\'
ENTS=\'\{\"path\":\"$TMP/1-entities.gql\"\}\'
ASSRTS_3=\'\{\"path\":\"$TMP/2-ternary-relations.gql\"\}\'
ASSRTS_2=\'\{\"path\":\"$TMP/3-binary-relations.gql\"\}\'

### THE FOLLOWING COMMANDS MUST BE EXECUTED IN ORDER!
## Instructions: Uncomment each of the following lines one at a time, in order
## and execute everytime this script from the folder where the Movie .gql files are located
#eval curl -H \"Content-Type: application/json\" -X POST -d `echo $SCHEMA` http://localhost:4567/import/ontology
#eval curl -H \"Content-Type: application/json\" -X POST -d `echo $ENTS` http://localhost:4567/import/batch/data
#eval curl -H \"Content-Type: application/json\" -X POST -d `echo $ASSRTS_3` http://localhost:4567/import/batch/data
#eval curl -H \"Content-Type: application/json\" -X POST -d `echo $ASSRTS_2` http://localhost:4567/import/batch/data
