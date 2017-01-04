#!/bin/bash

FIRST_ARGUMENT="$1"

mvn exec:java -Dexec.mainClass="ai.grakn.snomed2grakn.migrator.Main" -Dexec.args="$FIRST_ARGUMENT"