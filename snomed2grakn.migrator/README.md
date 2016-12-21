This is SNOMED-CT 2 Grakn migrator. 

Currently, it is fixed to use the file named "snomedSample.owl", which should be placed in the project's directory. 
It contains a sample of 400+ (out of 300K+) SNOMED-CT classes and all 200+ object properties. 

The full version of SNOMED-CT OWL ontology is available at:
https://www.dropbox.com/s/obgjayguwueo7sd/snomed_ct_full_inv.owl?dl=0

TODO:

The Main class must be extended:
- to allow the file name as a parameter 
- to use the default graph provided by MigrationCLI instead of the IN_MEMORY one
- to commit/close the graph on completing the migration