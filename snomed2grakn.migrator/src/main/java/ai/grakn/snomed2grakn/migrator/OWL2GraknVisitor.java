package ai.grakn.snomed2grakn.migrator;

import static ai.grakn.graql.Graql.match;
import static ai.grakn.graql.Graql.var;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.OWLAxiomVisitor;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;

import ai.grakn.concept.RuleType;
import ai.grakn.graql.Graql;
import ai.grakn.graql.Pattern;
import ai.grakn.graql.Var;


/**
 * <p>
 * This is the class processing OWL axioms and complex expressions. Note that the named OWL classes and object 
 * properties are first registered by the Migrator.  
 * </p>
 * 
 * @author Szymon Klarman
 *
 */


class OWL2GraknAxiomVisitor implements OWLAxiomVisitor {
	
	
    public void visit(OWLSubClassOfAxiom ax) {
		//System.out.println("Axiom: " + ax);
    	OWLClassExpression subClass = ax.getSubClass();
    	OWLClassExpression superClass = ax.getSuperClass();
    	
    	OWL2GraknExpressionVisitor visitor = new OWL2GraknExpressionVisitor();
    	
    	Snomed2GraknPattern subPattern = subClass.accept(visitor);
    	Snomed2GraknPattern superPattern = superClass.accept(visitor);
		
    	List<Pattern> matchPatternList = Snomed2GraknPattern.patternConcat(subPattern.matchPatternList, superPattern.matchPatternList);
    	Pattern[] matchPattern = matchPatternList.toArray(new Pattern[matchPatternList.size()]);
    	   	
    	Var relationPattern = var().rel("subclass", var(subPattern.var)).rel("superclass", var(superPattern.var)).isa("subclassing");
    	
    	List<Var> insertPatternList = Snomed2GraknPattern.varConcat(Snomed2GraknPattern.varConcat(subPattern.insertPatternList, superPattern.insertPatternList), relationPattern);
    	Var[] insertPattern = insertPatternList.toArray(new Var[insertPatternList.size()]);
    	
    	//System.out.println(match(matchPattern).insert(insertPattern).toString()); System.out.println();
    	Main.loaderClient.add(match(matchPattern).insert(insertPattern));
   }
    
    public void visit(OWLEquivalentClassesAxiom ax) {
		//System.out.println("Axiom: " + ax);
    	List<OWLClassExpression> classes = ax.classExpressions().collect(Collectors.toList());
     	
    	OWL2GraknExpressionVisitor visitor = new OWL2GraknExpressionVisitor();
    	
    	Snomed2GraknPattern entity1 = classes.get(0).accept(visitor);
    	Snomed2GraknPattern entity2 = classes.get(1).accept(visitor);
    	
    	List<Pattern> matchPatternList = Snomed2GraknPattern.patternConcat(entity1.matchPatternList, entity2.matchPatternList);
    	Pattern[] matchPattern = matchPatternList.toArray(new Pattern[matchPatternList.size()]);
    	   	
    	Var relationPattern1 = var().rel("subclass", var(entity1.var)).rel("superclass", var(entity2.var)).isa("subclassing");
    	Var relationPattern2 = var().rel("subclass", var(entity2.var)).rel("superclass", var(entity1.var)).isa("subclassing");
    	
     	List<Var> insertPatternList = Snomed2GraknPattern.varConcat(Snomed2GraknPattern.varConcat(Snomed2GraknPattern.varConcat(entity1.insertPatternList, entity2.insertPatternList), relationPattern1), relationPattern2);
      	Var[] insertPattern = insertPatternList.toArray(new Var[insertPatternList.size()]);
    	
    	//System.out.println(match(matchPattern).insert(insertPattern).toString()); System.out.println();
    	Main.loaderClient.add(match(matchPattern).insert(insertPattern));
    }
    
    public void visit(OWLSubObjectPropertyOfAxiom ax) {
		OWLObjectProperty subProperty = (OWLObjectProperty) ax.getSubProperty();
		String[] subRelationInfo = Migrator.relationTypes.get(subProperty);
		OWLObjectProperty superProperty = (OWLObjectProperty) ax.getSuperProperty();
		String[] superRelationInfo = Migrator.relationTypes.get(superProperty);
		Main.graknGraph.getRelationType(subRelationInfo[0]).superType(Main.graknGraph.getRelationType(superRelationInfo[0]));
		Main.graknGraph.getRoleType(subRelationInfo[1]).superType(Main.graknGraph.getRoleType(superRelationInfo[1]));
		Main.graknGraph.getRoleType(subRelationInfo[2]).superType(Main.graknGraph.getRoleType(superRelationInfo[2]));
	}
	
	public void visit(OWLSubPropertyChainOfAxiom ax) {
		List<OWLObjectPropertyExpression> subProperties = ax.getPropertyChain();
		if (subProperties.size()!=2) return;
		String[] superRelationInfo = Migrator.relationTypes.get(ax.getSuperProperty().asOWLObjectProperty());
		
		RuleType ruleType = Main.graknGraph.getRuleType("property-chain");
		String[] leftSubRelationInfo = Migrator.relationTypes.get(subProperties.get(0).asOWLObjectProperty());
		String[] rightSubRelationInfo = Migrator.relationTypes.get(subProperties.get(1).asOWLObjectProperty());
		
		Pattern leftSub = Graql.var().isa(leftSubRelationInfo[0]).rel(leftSubRelationInfo[1], "x").rel(leftSubRelationInfo[2], "y");
	    Pattern rightSub = Graql.var().isa(rightSubRelationInfo[0]).rel(rightSubRelationInfo[1], "y").rel(rightSubRelationInfo[2], "z");
	    Pattern body = Graql.and(leftSub, rightSub);
	    Pattern head = Graql.var().isa(superRelationInfo[0]).rel(superRelationInfo[1], "x").rel(superRelationInfo[2], "z");
	    ruleType.addRule(body, head);
	}	
	
	public void visit(OWLInverseObjectPropertiesAxiom ax) {
		System.out.println("Axiom: " + ax);
		String[] firstRelationInfo = Migrator.relationTypes.get(ax.getFirstProperty());
		String[] secondRelationInfo = Migrator.relationTypes.get(ax.getSecondProperty());
		RuleType ruleType = Main.graknGraph.getRuleType("property-inverse");
		
		Pattern body = Graql.var().isa(secondRelationInfo[0]).rel(secondRelationInfo[1], "x").rel(secondRelationInfo[2], "y");
		Pattern head = Graql.var().isa(firstRelationInfo[0]).rel(firstRelationInfo[1], "y").rel(firstRelationInfo[2], "x");
		ruleType.addRule(body, head);
	}
	
}

class OWL2GraknExpressionVisitor implements OWLClassExpressionVisitorEx<Snomed2GraknPattern> {
  
	
	public static int varNo = 0;
	
	public static String getNewVarName() {
		varNo++;
		return "var"+varNo;
	}
	
	public Snomed2GraknPattern visit(OWLClass exp) {
		String snomedUri = Migrator.entities.get(exp);
		String classVar = getNewVarName();
		Pattern matchPattern = var(classVar).has("snomed-uri", snomedUri);
		
		return new Snomed2GraknPattern(matchPattern, classVar);
	}
	
	public Snomed2GraknPattern visit(OWLObjectIntersectionOf exp) {
		String conjunctionVar = getNewVarName();
		Var conjunctionEntityPattern = var(conjunctionVar).isa("intersection-class");
		
		List<Pattern> matchPatternList = Arrays.asList();
		List<Var> insertPatternList = Arrays.asList(conjunctionEntityPattern);

		for (OWLClassExpression conjunctEntity : exp.asConjunctSet()) {
			Snomed2GraknPattern conjunctEntityPattern = conjunctEntity.accept(this);
			matchPatternList = Snomed2GraknPattern.patternConcat(matchPatternList, conjunctEntityPattern.matchPatternList);
			insertPatternList = Snomed2GraknPattern.varConcat(insertPatternList, conjunctEntityPattern.insertPatternList);
			Var relationPattern = var().rel("subclass", var(conjunctionVar)).rel("superclass", var(conjunctEntityPattern.var)).isa("subclassing");
			insertPatternList.add(relationPattern);
		}
		
		return new Snomed2GraknPattern(matchPatternList, insertPatternList, conjunctionVar);
	}
	
	public Snomed2GraknPattern visit(OWLObjectSomeValuesFrom exp) {
		String existentialVar = getNewVarName();
		
		OWLObjectProperty property = (OWLObjectProperty) exp.getProperty();
		String[] relationInfo = Migrator.relationTypes.get(property);
				
		OWLClassExpression fillerExpression = exp.getFiller();
		Snomed2GraknPattern fillerEntityPattern = fillerExpression.accept(this);
		
		List<Var> insertPatternList = fillerEntityPattern.insertPatternList;
		
		Var existentialEntityPattern = var(existentialVar).isa("existential-class");
		Var relationPattern = var().rel(relationInfo[1], var(existentialVar)).rel(relationInfo[2], var(fillerEntityPattern.var)).isa(relationInfo[0]);
		
		insertPatternList = Snomed2GraknPattern.varConcat(insertPatternList, existentialEntityPattern);
		insertPatternList = Snomed2GraknPattern.varConcat(insertPatternList, relationPattern);

		return new Snomed2GraknPattern(fillerEntityPattern.matchPatternList, insertPatternList, existentialVar);
	}
	
}

class Snomed2GraknPattern{
	List<Pattern> matchPatternList;
	List<Var> insertPatternList;
	String var;
	
	Snomed2GraknPattern(List<Pattern> matchList, List<Var> insertList, String var) {
		this.matchPatternList = matchList;
		this.insertPatternList = insertList;
		this.var = var;
	}
	
	Snomed2GraknPattern(Pattern match, String var) {
		this.matchPatternList = Arrays.asList(match);
		this.insertPatternList = Arrays.asList();
		this.var = var;
	}
	
	public static List<Pattern> patternConcat(List<Pattern> firstList, List<Pattern> secondList) {
		return Stream.concat(firstList.stream(), secondList.stream()).collect(Collectors.toList());
	}
	
	public static List<Pattern> patternConcat(List<Pattern> patternList, Pattern pattern) {
		return Stream.concat(patternList.stream(), Arrays.asList(pattern).stream()).collect(Collectors.toList());
	}
	
	public static List<Var> varConcat(List<Var> firstList, List<Var> secondList) {
		return Stream.concat(firstList.stream(), secondList.stream()).collect(Collectors.toList());
	}
	
	public static List<Var> varConcat(List<Var> patternList, Var pattern) {
		return Stream.concat(patternList.stream(), Arrays.asList(pattern).stream()).collect(Collectors.toList());
	}
	
}