This is SNOMED-CT 2 Grakn migrator. 

To run the migrator you can use the following command:

./snomed2grakn.sh filename.owl

where "filename.owl" is the name of the OWL file, placed in the project's directory, that contains the SNOMED-CT ontology. 

Currently, two samples of SNOMED-CT ontology are supplied with the project for testing purposes:
- snomedSample.owl, containing 400+ named classes and 282 object properties;
- snomedSample2.owl, containing 4K+ named classes and 284 object properties. 

The full version of SNOMED-CT OWL ontology with 300K+ named classes and 294 object properties (including three additionally inserted inverse properties, also present in the sample files) is available at:
https://www.dropbox.com/s/obgjayguwueo7sd/snomed_ct_full_inv.owl?dl=0

