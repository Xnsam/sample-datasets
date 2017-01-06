This is SNOMED-CT 2 Grakn migrator. 

To run the migrator you can use the following command:

./snomed2grakn.sh filename.owl

where "filename.owl" is the name of the OWL file, placed in the project's directory, that contains the SNOMED-CT ontology. 

Currently, two samples of SNOMED-CT ontology are supplied with the project for testing purposes:
- snomedSample.owl, containing 400+ named classes and 282 object properties;
- snomedSample2.owl, containing 4K+ named classes and 284 object properties. 

The full version of SNOMED-CT OWL ontology with 300K+ named classes and 294 object properties (including three additionally inserted inverse properties, also present in the sample files) is available at:
https://www.dropbox.com/s/obgjayguwueo7sd/snomed_ct_full_inv.owl?dl=0



TRANSLATION:
------------

The migrator implements a structural translation of OWL EL subclass axioms into a Grakn graph. Additionally property axioms are translated into Grakn subtype statements or rules. Basic reasoning tasks supported by OWL EL semantics are captured by corresponding Grakn rules. The details of the translation are outlined below.


Top level Grakn ontology
------------------------

owl-class sub entity
	plays-role subclass
	plays-role superclass
	has-resource snomed-uri
	has-resource label;

named-class sub owl-class;

intersection-class sub owl-class;

existential-class sub owl-class;

owl-property sub relation is-abstract;

subclassing sub relation
	has-role subclass
	has-role superclass;

subclass sub role;
superclass sub role;


Fixed rules
-----------
%subclass traversal

lhs {{
(subclass: $x, superclass: $y) isa subclassing; 
(subclass: $y, superclass: $z) isa subclassing;
}} 
rhs { 
(subclass: $x, superclass: $z) isa subclassing 
}; 


%inhertiance of properties from superclasses

lhs {{
(subclass: $x, superclass: $y) isa subclassing; 
($rel-role-1: $y, $rel-role-2: $z) isa $rel;
$rel sub owl-property; 
}} 
rhs { 
isa $rel ($rel-role-1: $x, $rel-role-2: $z) 
}; 



SNOMED properties
-----------------

Every object property is translated into a Grakn relation with two roles (called <owl-property name>-from and <owl-property name>-to) denoting the source and the target of the property, respectively, e.g.:

SNOMED:
<owl:ObjectProperty rdf:about="id/246112005">
    <rdfs:label xml:lang="en">Severity (attribute)</rdfs:label>
</owl:ObjectProperty>

Grakn:
"Severity-(attribute)" sub relation
	has-role "Severity-(attribute)-from"
	has-role "Severity-(attribute)-to";

owl-class plays-role "Severity-(attribute)-from";
owl-class plays-role "Severity-(attribute)-to";

"Severity-(attribute)-from" sub role;
"Severity-(attribute)-to" sub role;


SNOMED property axioms
----------------------

Property axioms are translated into equivalent Grakn subtype statements or rules, as follows:


- subproperties

SNOMED:
<owl:ObjectProperty rdf:about="property1">
    <rdfs:subPropertyOf rdf:resource="property2"/>
</owl:ObjectProperty>

Grakn:
<property1-name> sub <property2-name>;
<property1-name-from> sub <property2-name-from>;
<property1-name-to> sub <property2-name-to>;


- inverse properties

SNOMED:
<owl:ObjectProperty rdf:about="property1">
    <owl:inverseOf rdf:resource="property2" />
</owl:ObjectProperty>

Grakn:
lhs { 
(<property1-name-from>: $x, <property1-name-to>: $y) isa <property1-name>;
} 
rhs { 
(<property2-name-from>: $y, <property2-name-to>: $x) isa <property2-name> 
}; 

lhs { 
(<property2-name-from>: $x, <property2-name-to>: $y) isa <property2-name>;
} 
rhs { 
(<property1-name-from>: $y, <property1-name-to>: $x) isa <property1-name> 
}; 


- property chains

SNOMED:
<rdf:Description>
   <rdfs:subPropertyOf rdf:resource="property1"/>
   <owl:propertyChain rdf:parseType="Collection">
      <rdf:Description rdf:about="property2"/>
      <rdf:Description rdf:about="property3"/>
   </owl:propertyChain>
</rdf:Description>


Grakn:
lhs { 
(<property2-name-from>: $x, <property2-name-to>: $y) isa <property2-name>;
(<property3-name-from>: $x, <property3-name-to>: $y) isa <property3-name>;
} 
rhs { 
(<property1-name-from>: $y, <property1-name-to>: $x) isa <property1-name> 
}; 


SNOMED classes
--------------

Every OWL class is translated into a Grakn entity, with complex classes being translated recursively, according to the following rules. 


- named classes

A named class is translated into a fresh instance of the `named-class` type in Grakn, with the `snomed-uri` and `label` resources defined as expected, e.g.:

SNOMED:
<owl:Class rdf:about="id/100014000">
   <rdfs:label xml:lang="en">BLUE SHAMPOO (product)</rdfs:label>
</owl:Class>

Grakn:
$x isa named-class, has snomed-uri "snomed:100014000", has label "BLUE-SHAMPOO-(product)"; 


- existential restriction

An existential restriction is translated into a fresh instance of the `existential-class` type in Grakn and related to its role filler via a corresponding Grakn relation, e.g.:

SNOMED:
<owl:Restriction>
	<owl:onProperty rdf:resource="property"/>
	<owl:someValuesFrom rdf:resource="filler-class"/>
</owl:Restriction>

Grakn:
$x isa existential-class;
(<property-name-from>: $x, <property-name-to>: $y) isa <property-name>;
$y id <filler-class-id>;


- class intersection

A class intersection is translated into a fresh instance of the `intersection-class` type in Grakn and related to all classes in the intersection via `subclassing` relation, e.g.:

SNOMED:
<owl:Class>
<owl:intersectionOf rdf:parseType="Collection">
       <owl:Class rdf:about="class1"/>
       <owl:Class rdf:about="class2"/>
       ...
</owl:intersectionOf>
</owl:Class>

$x isa intersection-class;
(subclass: $x, superclass: $y1) isa subclassing;
$y1 id <class1-id>;
(subclass: $x, superclass: $y2) isa subclassing;
$y2 id <class2-id>;
...


Grakn:
$x isa existential-class;
(<property-name-from>: $x, <property-name-to>: $y) isa <property-name>;
$y id <filler-class-id>;


SNOMED class axioms
-------------------

Subclass/equivalence axioms are translated into instances of relation subclassing between the corresponding classes, e.g.:

SNOMED:
<owl:Class rdf:about="class1">
    <owl:equivalentClass>
       <owl:Class rdf:about="class2"/>
    </owl:equivalentClass>
</owl:Class>

Grakn:
(subclass: $x, superclass: $y) isa subclassing;
(subclass: $y, superclass: $z) isa subclassing;
$x id <class1-id>;
$y id <class2-id>;


