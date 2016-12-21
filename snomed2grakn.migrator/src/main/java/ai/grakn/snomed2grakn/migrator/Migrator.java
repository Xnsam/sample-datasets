package ai.grakn.snomed2grakn.migrator;

import java.util.HashMap;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

import ai.grakn.GraknGraph;
import ai.grakn.concept.Concept;
import ai.grakn.concept.Entity;
import ai.grakn.concept.EntityType;
import ai.grakn.concept.RelationType;
import ai.grakn.concept.ResourceType;
import ai.grakn.concept.RoleType;
import ai.grakn.graql.Graql;
import ai.grakn.graql.Pattern;


/**
 * 
 * <p>
 * The Migrator is the main driver of the SNOMED_CT migration process.
 * </p>
 *
 * @author Szymon Klarman
 *
 */

public class Migrator {
	
	public static HashMap<OWLObjectProperty, Concept[]> relationTypes = new HashMap<OWLObjectProperty, Concept[]>();
	public static HashMap<OWLClass, Entity> entities = new HashMap<OWLClass, Entity>();
	
	
static void migrateSNOMED (OWLOntology snomed, GraknGraph graknGraph) {
		
		//registering some top-level predicates 
		
		EntityType snomedClass = graknGraph.putEntityType("snomed-class");
		graknGraph.putResourceTypeUnique("snomed-uri", ResourceType.DataType.STRING);
		graknGraph.putResourceType("label", ResourceType.DataType.STRING);
		graknGraph.getEntityType("snomed-class").hasResource(graknGraph.getResourceType("label"));
		graknGraph.getEntityType("snomed-class").hasResource(graknGraph.getResourceType("snomed-uri"));
		RelationType subclassing = graknGraph.putRelationType("subclassing");
		RoleType subclass = graknGraph.putRoleType("subclass");
		RoleType superclass = graknGraph.putRoleType("superclass");
		subclassing.hasRole(subclass);
		subclassing.hasRole(superclass);
		snomedClass.playsRole(subclass);
		snomedClass.playsRole(superclass);
		graknGraph.putRuleType("property-chain");
		graknGraph.putRuleType("property-inverse");
		graknGraph.putRuleType("subclass-traversing");
		
		Pattern leftSub = Graql.var().isa("subclassing").rel("subclass", "x").rel("superclass", "y");
		Pattern rightSub = Graql.var().isa(Graql.var("rel")).rel(Graql.var("rel-role-1"), "y").rel(Graql.var("rel-role-2"), "z");
		Pattern body = Graql.and(leftSub, rightSub);
		Pattern head = Graql.var().isa(Graql.var("rel")).rel(Graql.var("rel-role-1"), "x").rel(Graql.var("rel-role-2"), "z");
		graknGraph.getRuleType("subclass-traversing").addRule(body, head);

		
		//registering named OWL classes in SNOMED as entities 
		
		System.out.println("Registering classes and properties...");
		
		snomed.classesInSignature().forEach(snomedEntity -> {
			String snomedUri = "snomed:" + shortName(snomedEntity);
			Entity newEntity = graknGraph.getEntityType("snomed-class").addEntity();
			newEntity.hasResource(graknGraph.getResourceType("snomed-uri").putResource(snomedUri));
			newEntity.hasResource(graknGraph.getResourceType("label").putResource(getRdfsLabel(snomedEntity, snomed)));
			entities.put(snomedEntity, newEntity);
		});
		
		//registering named OWL properties in SNOMED as relations
		
		snomed.objectPropertiesInSignature().forEach(snomedProperty -> {
			String propertyName = getRdfsLabel(snomedProperty, snomed);
			RelationType relation = graknGraph.putRelationType(propertyName);
			RoleType from = graknGraph.putRoleType(propertyName + "-from");
			RoleType to = graknGraph.putRoleType(propertyName + "-to");
			relation.hasRole(from);
			relation.hasRole(to);
			snomedClass.playsRole(from);
			snomedClass.playsRole(to);
			Concept[] relationInfo = {relation, from, to}; 
			relationTypes.put(snomedProperty, relationInfo);
		});
		
		//Extracting and structuring information from OWL axioms in SNOMED
		
		System.out.println("Migrating Snomed axioms...");
		OWL2GraknVisitor visitor = new OWL2GraknVisitor();
		snomed.axioms().forEach(ax ->  ax.accept(visitor));
	
		
		System.out.println("Migration completed.");
	
	}
    
public static String shortName(OWLEntity id) {
    SimpleShortFormProvider shortform = new SimpleShortFormProvider();
    return shortform.getShortForm(id); 
}

public static String getRdfsLabel(OWLEntity id, OWLOntology ontology) {
	OWLDataFactory df = OWLManager.createOWLOntologyManager().getOWLDataFactory();
	OWLAnnotationProperty labProp = df.getRDFSLabel();
	OWLAnnotationAssertionAxiom annAx = ontology.annotationAssertionAxioms((OWLAnnotationSubject) id.getIRI()).filter(ann -> ann.getAnnotation().getProperty().equals(labProp)).findFirst().orElse(null);
	if (annAx!=null) return annAx.getAnnotation().getValue().toString().split("\"")[1].replace(" ", "-"); 
	else return shortName(id); 
}
}
