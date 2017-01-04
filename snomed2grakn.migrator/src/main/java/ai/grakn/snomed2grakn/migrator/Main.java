package ai.grakn.snomed2grakn.migrator;

import java.io.File;
import java.util.Arrays;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import ai.grakn.Grakn;
import ai.grakn.GraknGraph;
import ai.grakn.engine.loader.client.LoaderClient;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

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
	static String keyspace = "grakn";
	public static GraknGraph graknGraph;
	public static LoaderClient loaderClient;
	
    public static void main( String[] args )
    {
    	Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME); 
    	logger.setLevel(Level.OFF);
    	
    	if (args.length==0) {
    		System.out.println("You must provide the name of the OWL file containing SNOMED-CT.");
    		System.exit(0);
    	}
    	
    	File input = new File(args[0]);
		
		if (!input.exists()) {
			System.out.println("Could not find the file: " + input.getAbsoluteFile());
			System.exit(0);
		}
    	
    	graknGraph = Grakn.factory(Grakn.DEFAULT_URI, keyspace).getGraph();
    	loaderClient = new LoaderClient(keyspace, Arrays.asList(Grakn.DEFAULT_URI));
		
		try{
			System.out.println("Loading SNOMED...");
			OWLOntology ontology = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(input);
			Migrator.migrateSNOMED(ontology, graknGraph);
			
			graknGraph.close();			
		}
		catch (OWLOntologyCreationException e) {
			System.out.println("Could not load ontology: " + e.getMessage());
		} 
	}
    
    public static void commitGraph()
    {
    	try {
			graknGraph.commit();
		}
		catch (Exception ex) {
			System.out.println(ex);
		};
    }
}
