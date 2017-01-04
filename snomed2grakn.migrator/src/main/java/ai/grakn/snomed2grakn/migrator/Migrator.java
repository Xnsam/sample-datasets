package ai.grakn.snomed2grakn.migrator;

import static ai.grakn.graql.Graql.insert;
import static ai.grakn.graql.Graql.var;

import java.time.Duration;
import java.time.Instant;
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
import ai.grakn.concept.EntityType;
import ai.grakn.concept.RelationType;
import ai.grakn.concept.ResourceType;
import ai.grakn.concept.RoleType;
import ai.grakn.graql.Graql;
import ai.grakn.graql.Pattern;
import ai.grakn.graql.Var;


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
	
	public static HashMap<OWLObjectProperty, String[]> relationTypes = new HashMap<OWLObjectProperty, String[]>();
	public static HashMap<OWLClass, String> entities = new HashMap<OWLClass, String>();
	public static int counter=0;
	
static void migrateSNOMED (OWLOntology snomed, GraknGraph graknGraph) {
		
	
		Instant start = Instant.now();

	
		//registering some top-level predicates 
		
		EntityType owlClass = graknGraph.putEntityType("owl-class");
		EntityType snomedClass = graknGraph.putEntityType("named-class");
		EntityType intersectionClass = graknGraph.putEntityType("intersection-class");
		EntityType existentialClass = graknGraph.putEntityType("existential-class");
		snomedClass.superType(owlClass);
		intersectionClass.superType(owlClass);
		existentialClass.superType(owlClass);
		ResourceType<String> snomedUriRes = graknGraph.putResourceType("snomed-uri", ResourceType.DataType.STRING);
		ResourceType<String> snomedLabelRes = graknGraph.putResourceType("label", ResourceType.DataType.STRING);
		owlClass.hasResource(snomedUriRes);
		owlClass.hasResource(snomedLabelRes);
		RelationType subclassing = graknGraph.putRelationType("subclassing");
		RoleType subclass = graknGraph.putRoleType("subclass");
		RoleType superclass = graknGraph.putRoleType("superclass");
		subclassing.hasRole(subclass);
		subclassing.hasRole(superclass);
		owlClass.playsRole(subclass);
		owlClass.playsRole(superclass);
		graknGraph.putRuleType("property-chain");
		graknGraph.putRuleType("property-inverse");
		graknGraph.putRuleType("subclass-traversing");
		
		Pattern leftSub = Graql.var().isa("subclassing").rel("subclass", "x").rel("superclass", "y");
		Pattern rightSub = Graql.var().isa(Graql.var("rel")).rel(Graql.var("rel-role-1"), "y").rel(Graql.var("rel-role-2"), "z");
		Pattern body = Graql.and(leftSub, rightSub);
		Pattern head = Graql.var().isa(Graql.var("rel")).rel(Graql.var("rel-role-1"), "x").rel(Graql.var("rel-role-2"), "z");
		graknGraph.getRuleType("subclass-traversing").addRule(body, head);
		
		
		//registering named OWL properties in SNOMED as relations
		System.out.println("\nRegistering properties...");
	
		snomed.objectPropertiesInSignature().forEach(snomedProperty -> {
			count();
			String relationName = getLabel(snomedProperty, snomed);
			String fromRoleName = relationName + "-from";
			String toRoleName = relationName + "-to";
			RelationType relation = graknGraph.putRelationType(relationName);
			RoleType from = graknGraph.putRoleType(fromRoleName);
			RoleType to = graknGraph.putRoleType(toRoleName);
			relation.hasRole(from);
			relation.hasRole(to);
			owlClass.playsRole(from);
			owlClass.playsRole(to);
			String[] relationInfo = {relationName, fromRoleName, toRoleName}; 
			relationTypes.put(snomedProperty, relationInfo);
		});
		
		System.out.println("\nProperties registered: " + counter);
		
		Main.commitGraph();
		
		
		//registering named OWL classes in SNOMED as entities 
		System.out.println("\nRegistering classes...");
		counter=0;
		snomed.classesInSignature().forEach(snomedNamedClass -> {
			count();
			String snomedUri = "snomed:" + shortName(snomedNamedClass);
			String snomedLabel = getLabel(snomedNamedClass, snomed);
			Var entityPattern = var().isa("named-class").has("snomed-uri", snomedUri).has("label", snomedLabel);
			Main.loaderClient.add(insert(entityPattern));
			entities.put(snomedNamedClass, snomedUri);
		});
		
		Main.loaderClient.waitToFinish();
		Main.commitGraph();
		
		System.out.println("\nClasses registered: " + counter);
		
		//Extracting and structuring information from OWL axioms in SNOMED
		System.out.println("\nMigrating SNOMED axioms...");
		counter=0;
		
		OWL2GraknAxiomVisitor visitor = new OWL2GraknAxiomVisitor();
		snomed.axioms().forEach(ax ->  {
			count();
			ax.accept(visitor);
			});
		Main.loaderClient.waitToFinish();
		Main.commitGraph();
		System.out.println("\nAxioms migrated: " + counter);
		Instant end = Instant.now();
		System.out.println("\nMigration finished in: " + Duration.between(start, end));
	}
    
public static String shortName(OWLEntity id) {
    SimpleShortFormProvider shortform = new SimpleShortFormProvider();
    return shortform.getShortForm(id); 
}

public static String getLabel(OWLEntity id, OWLOntology ontology) {
	OWLDataFactory df = OWLManager.createOWLOntologyManager().getOWLDataFactory();
	OWLAnnotationProperty labProp = df.getRDFSLabel();
	OWLAnnotationAssertionAxiom annAx = ontology.annotationAssertionAxioms((OWLAnnotationSubject) id.getIRI()).filter(ann -> ann.getAnnotation().getProperty().equals(labProp)).findFirst().orElse(null);
	if (annAx!=null) return annAx.getAnnotation().getValue().toString().split("\"")[1].replace(" ", "-"); 
	else return shortName(id); 
}

public static void count() {
	counter++;
	if (counter % 1000 == 0) {
		System.out.print(counter/1000 + "K.. ");
	}
}
}
