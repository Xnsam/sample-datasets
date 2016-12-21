package ai.grakn.snomed2grakn.migrator;

import java.util.List;
import java.util.stream.Collectors;

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

import ai.grakn.concept.Concept;
import ai.grakn.concept.Entity;
import ai.grakn.concept.Relation;
import ai.grakn.concept.RuleType;
import ai.grakn.graql.Graql;
import ai.grakn.graql.Pattern;



/**
 * <p>
 * This is the class processing OWL axioms and complex expressions. Note that the named OWL classes and object 
 * properties are first registered by the Migrator.  
 * </p>
 * 
 * @author Szymon Klarman
 *
 */


class OWL2GraknVisitor implements OWLAxiomVisitor {
	
    public void visit(OWLSubClassOfAxiom ax) {
    	OWLClassExpression subClass = ax.getSubClass();
    	OWLClassExpression superClass = ax.getSuperClass();
    	
    	OWLClassExpression2GraknVisitor visitor = new OWLClassExpression2GraknVisitor();
    	
    	Entity subEntity = subClass.accept(visitor);
		Entity superEntity = superClass.accept(visitor);
		
		Relation newRelation = Main.graknGraph.getRelationType("subclassing").addRelation();
		newRelation.putRolePlayer(Main.graknGraph.getRoleType("subclass"), subEntity);
		newRelation.putRolePlayer(Main.graknGraph.getRoleType("superclass"), superEntity);	
	}
    
    public void visit(OWLEquivalentClassesAxiom ax) {

    	List<OWLClassExpression> classes = ax.classExpressions().collect(Collectors.toList());
     	
    	OWLClassExpression2GraknVisitor visitor = new OWLClassExpression2GraknVisitor();
    	
    	Entity entity1 = classes.get(0).accept(visitor);
		Entity entity2 = classes.get(1).accept(visitor);
		
		Relation newRelation;
		
		newRelation = Main.graknGraph.getRelationType("subclassing").addRelation();
		newRelation.putRolePlayer(Main.graknGraph.getRoleType("subclass"), entity1);
		newRelation.putRolePlayer(Main.graknGraph.getRoleType("superclass"), entity2);
		
		newRelation = Main.graknGraph.getRelationType("subclassing").addRelation();
		newRelation.putRolePlayer(Main.graknGraph.getRoleType("subclass"), entity2);
		newRelation.putRolePlayer(Main.graknGraph.getRoleType("superclass"), entity1);	
	}
	
	public void visit(OWLSubObjectPropertyOfAxiom ax) {
		OWLObjectProperty subProperty = (OWLObjectProperty) ax.getSubProperty();
		Concept[] subRelationInfo = Migrator.relationTypes.get(subProperty);
		OWLObjectProperty superProperty = (OWLObjectProperty) ax.getSuperProperty();
		Concept[] superRelationInfo = Migrator.relationTypes.get(superProperty);
		subRelationInfo[0].asRelationType().superType(superRelationInfo[0].asRelationType());
		subRelationInfo[1].asRoleType().superType(superRelationInfo[1].asRoleType());
		subRelationInfo[2].asRoleType().superType(superRelationInfo[2].asRoleType());
	}
	
	public void visit(OWLSubPropertyChainOfAxiom ax) {
		List<OWLObjectPropertyExpression> subProperties = ax.getPropertyChain();
		if (subProperties.size()!=2) return;
		Concept[] superRelationInfo = Migrator.relationTypes.get(ax.getSuperProperty().asOWLObjectProperty());
		
		RuleType ruleType = Main.graknGraph.getRuleType("property-chain");
		Concept[] leftSubRelationInfo = Migrator.relationTypes.get(subProperties.get(0).asOWLObjectProperty());
		Concept[] rightSubRelationInfo = Migrator.relationTypes.get(subProperties.get(1).asOWLObjectProperty());
		
		Pattern leftSub = Graql.var().isa(leftSubRelationInfo[0].asRelationType().getName()).rel(leftSubRelationInfo[1].asRoleType().getName(), "x").rel(leftSubRelationInfo[2].asRoleType().getName(), "y");
	    Pattern rightSub = Graql.var().isa(rightSubRelationInfo[0].asRelationType().getName()).rel(rightSubRelationInfo[1].asRoleType().getName(), "y").rel(rightSubRelationInfo[2].asRoleType().getName(), "z");
	    Pattern body = Graql.and(leftSub, rightSub);
	    Pattern head = Graql.var().isa(superRelationInfo[0].asRelationType().getName()).rel(superRelationInfo[1].asRoleType().getName(), "x").rel(superRelationInfo[2].asRoleType().getName(), "z");
	    ruleType.addRule(body, head);
	}	
	
	public void  visit(OWLInverseObjectPropertiesAxiom ax) {
		Concept[] firstRelationInfo = Migrator.relationTypes.get(ax.getFirstProperty());
		Concept[] secondRelationInfo = Migrator.relationTypes.get(ax.getSecondProperty());
		RuleType ruleType = Main.graknGraph.getRuleType("property-inverse");
		
		Pattern body = Graql.var().isa(secondRelationInfo[0].asRelationType().getName()).rel(secondRelationInfo[1].asRoleType().getName(), "x").rel(secondRelationInfo[2].asRoleType().getName(), "y");
		Pattern head = Graql.var().isa(firstRelationInfo[0].asRelationType().getName()).rel(firstRelationInfo[1].asRoleType().getName(), "y").rel(firstRelationInfo[2].asRoleType().getName(), "x");
		ruleType.addRule(body, head);
	}
}



class OWLClassExpression2GraknVisitor implements OWLClassExpressionVisitorEx<Entity> {
	
	public Entity visit(OWLClass exp) {
		return Migrator.entities.get(exp);
	}
	
	public Entity visit(OWLObjectIntersectionOf exp) {
		Entity conjunctionEntity = Main.graknGraph.getEntityType("snomed-class").addEntity();
		conjunctionEntity.hasResource(Main.graknGraph.getResourceType("label").putResource("Intersection node"));
		OWLClassExpression2GraknVisitor visitor = new OWLClassExpression2GraknVisitor();
		exp.asConjunctSet().forEach(conj -> {
			Entity conjunctEntity = conj.accept(visitor);
			Relation newRelation = Main.graknGraph.getRelationType("subclassing").addRelation();
			newRelation.putRolePlayer(Main.graknGraph.getRoleType("subclass"), conjunctionEntity);
			newRelation.putRolePlayer(Main.graknGraph.getRoleType("superclass"), conjunctEntity);	
		});
		return conjunctionEntity;
	}
	
	public Entity visit(OWLObjectSomeValuesFrom exp) {
		Entity existentialEntity = Main.graknGraph.getEntityType("snomed-class").addEntity();
		existentialEntity.hasResource(Main.graknGraph.getResourceType("label").putResource("Existential node"));
		
		OWLObjectProperty property = (OWLObjectProperty) exp.getProperty();
		Concept[] relationInfo = Migrator.relationTypes.get(property);
				
		OWLClassExpression fillerExpression = exp.getFiller();
		OWLClassExpression2GraknVisitor visitor = new OWLClassExpression2GraknVisitor();
		Entity fillerEntity = fillerExpression.accept(visitor);
		
		Relation newRelation = relationInfo[0].asRelationType().addRelation();
		newRelation.putRolePlayer(relationInfo[1].asRoleType(), existentialEntity);
		newRelation.putRolePlayer(relationInfo[2].asRoleType(), fillerEntity);	
	
		return existentialEntity;
	}
	
}