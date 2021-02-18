package fr.sparna.rdf.shacl.doc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.topbraid.jenax.util.JenaDatatypes;
import org.topbraid.jenax.util.JenaUtil;
import org.topbraid.shacl.arq.functions.HasShapeFunction;
import org.topbraid.shacl.vocabulary.SH;

public class ShaclPropertyReader {

	
	private ConstraintValueReader constraintValueReader = new ConstraintValueReader();
	
	protected String lang;
	protected List<ShaclBox> allBoxes;
	
	public ShaclPropertyReader(String lang, List<ShaclBox> allBoxes) {
		super();
		this.lang = lang;
		this.allBoxes = allBoxes;
	}


	public ShaclProperty read(Resource constraint) {
		
		ShaclProperty shaclProperty = new ShaclProperty(constraint);
		
		shaclProperty.setPath(this.readPath(constraint));
		shaclProperty.setDatatype(this.readDatatype(constraint));
		shaclProperty.setNodeKind(this.readNodeKind(constraint));
		shaclProperty.setCardinality(this.readCardinality(constraint));
		shaclProperty.setNode(this.readNode(constraint));
		shaclProperty.setPattern(this.readPattern(constraint));
		shaclProperty.setClass_node(this.readClass(constraint));
		shaclProperty.setClass_property(this.readClass_property(constraint)); // Returne la valeur de TargetClass
		shaclProperty.setDescription(this.readDescription(constraint));
		shaclProperty.setName(this.readName(constraint));			
		shaclProperty.setShin(this.readShin(constraint));
		shaclProperty.setShValue(this.readShValue(constraint));
		shaclProperty.setShOrder(this.readShOrder(constraint));

		return shaclProperty;
	}

	public String readShValue(Resource constraint) {
		String value = null;
		if(constraint.hasProperty(SH.value)) {
			value = constraint.getProperty(SH.value).getLiteral().getString();
		}
		return value;
	}

	public Integer readShOrder(Resource constraint) {
		Integer value = 0;
		if(constraint.hasProperty(SH.order)) {
			value = Integer.parseInt(constraint.getProperty(SH.order).getLiteral().getString());
		}
		return value;
	}

	public String readShin(Resource constraint) {
		String value = null;
		if (constraint.hasProperty(SH.in)) {
			Resource list = constraint.getProperty(SH.in).getList().asResource();
			RDFList rdflist = list.as(RDFList.class);
			ExtendedIterator<RDFNode> items = rdflist.iterator();
			value = "";
			while (items.hasNext()) {
				RDFNode item = items.next();
				String valueString;
				if(item.isURIResource()) {
					valueString = item.getModel().shortForm(((Resource)item).getURI());
				} else {
					valueString = item.toString();
				}
				
				value += valueString + " ,";
			}
			value.substring(0,(value.length()-2));
		}
		return value;
	}

	public String readName(Resource constraint) {
		String value = null;
		if(constraint.hasProperty(SH.name)) {
			value = constraintValueReader.readValueconstraint(constraint,SH.name,this.lang);
		}
		return value;
	}

	public String readDescription(Resource constraint) {
		String value = null;
		if(constraint.hasProperty(SH.description)) {
			value = constraintValueReader.readValueconstraint(constraint, SH.description, this.lang);
		}
		return value;		
	}

	public String readPath(Resource constraint) {
		return constraintValueReader.readValueconstraint(constraint, SH.path,null);		
	}

	public String readDatatype(Resource constraint) {
		return constraintValueReader.readValueconstraint(constraint, SH.datatype,null);
	}

	public String readNodeKind(Resource constraint) {
		return constraintValueReader.readValueconstraint(constraint, SH.nodeKind,null);
	}

	// Cardinality Constraint Components

	public String readCardinality(Resource constraint) {
		String minCount = "0";
		String maxCount = "*";
		String uml_code = null;
		if (constraint.hasProperty(SH.minCount)) {
			minCount = constraint.getProperty(SH.minCount).getObject().asLiteral().getString();
			if (minCount == "") {
				minCount = "0";
			}
		}
		if (constraint.hasProperty(SH.maxCount)) {
			maxCount = constraint.getProperty(SH.maxCount).getObject().asLiteral().getString();
			;
			if (maxCount == "") {
				maxCount = "*";
			}
		}
		if ((constraint.hasProperty(SH.minCount)) || (constraint.hasProperty(SH.maxCount))) {
			uml_code = minCount + ".." + maxCount;
		} else {
			uml_code = null;
		}

		return uml_code;
	}


	public String readPattern(Resource constraint) {
		return constraintValueReader.readValueconstraint(constraint, SH.pattern,null);
	}

	// Shape-based Constraint Components

	// TODO : devrait retourner un ShaclBox
	public String readNode(Resource constraint) {

		// 1. Lire la valeur de sh:node
		String nodeValue = constraintValueReader.readValueconstraint(constraint, SH.node,null);
		
		return nodeValue;
		
		// 2. Trouver le PlantUmlBox qui a ce nom
//		if(nodeValue != null) {
//			ShaclBox theBox = null;
//			for (ShaclBox plantUmlBox : allBoxes) {
//				if (plantUmlBox.getNameshape().equals(nodeValue)) {
//					theBox = plantUmlBox;
//					break;
//				}
//			}
//
//			if (theBox == null) {
//				// on ne l'a pas trouve, on sort la valeur de sh:node
//				this.node = nodeValue;
//			}	
//		}		
	}

	// TODO : devrait renvoyer une ShaclBox
	public String readClass_property(Resource constraint) {
		String value = null;

		// 1. Lire la valeur de sh:node
		if (constraint.hasProperty(SH.class_)) {
			Resource idclass = constraint.getProperty(SH.class_).getResource();
			List<Resource> nodetargets = constraint.getModel().listResourcesWithProperty(SH.targetClass, idclass)
					.toList();
			for (Resource nodeTarget : nodetargets) {
				if (value != null) {
					System.out.println("Problem !");
				}
				value = nodeTarget.getLocalName();
			}

			if (nodetargets.isEmpty()) {
				if (idclass.hasProperty(RDF.type, RDFS.Class) && idclass.hasProperty(RDF.type, SH.NodeShape)) {
					value = idclass.getLocalName();
				} else { // Section quand il n'y a pas une targetClass
					value = constraint.getProperty(SH.class_).getResource().getLocalName();
				}
			}
		}

		// 2. Trouver le PlantUmlBox qui a ce nom
		ShaclBox theBox = null;
		for (ShaclBox plantUmlBox : allBoxes) {
			if (plantUmlBox.getNameshape().equals(value)) {
				theBox = plantUmlBox;
				break;
			}
		}

		if (theBox == null) {
			// on ne l'a pas trouv�, on sort la valeur de sh:node
			return value;
		}
		
		return null;
	}
	
	
	public String readClass(Resource constraint) {
		String value = null;
		if (constraint.hasProperty(SH.class_)) {
			Resource idclass = constraint.getProperty(SH.class_).getResource();

			List<Resource> nodetarget = constraint.getModel().listResourcesWithProperty(SH.targetClass, idclass)
					.toList();
			for (Resource nodeTarget : nodetarget) {
				value = nodeTarget.getModel().shortForm(constraint.getProperty(SH.class_).getResource().getURI());
			}

		}
		return value;
	}

}
