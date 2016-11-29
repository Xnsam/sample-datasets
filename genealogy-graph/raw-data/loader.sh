#!/bin/bash

if [ -n "$1" ]
then
	echo "Migrating people"
	$1/migration.sh csv -i $PWD/people.csv -t $PWD/migrators/people-migrator.gql
	echo "Migrating births"
	$1/migration.sh csv -i $PWD/births.csv -t $PWD/migrators/births-migrator.gql
	echo "Migrating weddings"
	$1/migration.sh csv -i $PWD/weddings.csv -t $PWD/migrators/weddings-migrator.gql
	echo "Migrating documents"
	$1/migration.sh csv -i $PWD/documents.csv -t $PWD/migrators/documents-migrator.gql
	echo "Migrating evidences"
	$1/migration.sh csv -i $PWD/evidences.csv -t $PWD/migrators/evidences-migrator.gql
	echo "Done migrating data"
else
	echo "Usage: ./loader.sh <Grakn-bin-directory>"
fi