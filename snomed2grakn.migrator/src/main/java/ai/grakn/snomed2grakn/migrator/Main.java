package ai.grakn.snomed2grakn.migrator;

import java.io.File;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import ai.grakn.Grakn;
import ai.grakn.GraknGraph;

/**
 * <p>
 * Main program to migrate SNOMED-CT OWL ontology into a Grakn knowledge graph. 
 * Note that currently the input file is fixed to snomedSample.owl which should be
 * included in the project directory. 
 * </p>
 * 
 * @author Szymon Klarman
 *
 */


public class Main 
{	
	public static GraknGraph graknGraph = Grakn.factory(Grakn.IN_MEMORY, "grakn").getGraph();
	
    public static void main( String[] args )
    {
    	File input = new File("snomedSample.owl"); 
		System.out.println(input.getAbsoluteFile());
		try{
			System.out.println("Loading SNOMED...");
			OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(input);
			Migrator.migrateSNOMED(ontology, graknGraph);
		} 
		catch (OWLOntologyCreationException e) {
			System.out.println("Could not load ontology: " + e.getMessage());
		} 
	}
}
