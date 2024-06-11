package fr.sparna.rdf.shacl.doc.read;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;

import fr.sparna.rdf.shacl.doc.ConstraintValueReader;
import fr.sparna.rdf.shacl.doc.NodeShape;
import fr.sparna.rdf.shacl.doc.PropertyShape;
import fr.sparna.rdf.shacl.doc.model.PropertyShapeDocumentation;

import fr.sparna.rdf.shacl.doc.model.Link;


public class PropertyShapeDocumentationBuilder {

	public static PropertyShapeDocumentation build(
			PropertyShape propertyShape,
			List<NodeShape> allNodeShapes,
			Model shaclGraph,
			Model owlGraph,
			String lang) {
		// Start building final structure
		PropertyShapeDocumentation proprieteDoc = new PropertyShapeDocumentation();
		proprieteDoc.setLabel(selectLabel(propertyShape, shaclGraph.union(owlGraph), lang));
		
		if(propertyShape.getShNode() != null) {
			for(NodeShape aBox : allNodeShapes) {
				if(aBox.getNodeShape().getURI().equals(propertyShape.getShNode().getURI())) {
					proprieteDoc.setLinkNodeShapeUri(aBox.getShortForm());
					if(aBox.getRdfsLabel() == null) {
						proprieteDoc.setLinkNodeShape(aBox.getShortForm());
					}else {
						proprieteDoc.setLinkNodeShape(aBox.getRdfsLabel());
					}							
				}
			}
		}
		
		//proprieteDoc.setShortForm(propertyShape.getShPathAsString());
		//proprieteDoc.setPropertyUri(propertyShape.getShPath().isURIResource()?propertyShape.getShPath().getURI():null);				
		
		// URI in the raport
		proprieteDoc.setPropertyUri(buildPathLink(propertyShape));
		
		proprieteDoc.setExpectedValueLabel(selectExpectedValueLabel(
				propertyShape.getShClass(),
				propertyShape.getShNode(),
				propertyShape.getShDatatype(),
				propertyShape.getShNodeKind(),
				propertyShape.getShValue()
		));
		
		if(propertyShape.getShClass() != null) {
			for(NodeShape aNodeShape : allNodeShapes) {
				if(aNodeShape.getShTargetClass() != null && aNodeShape.getShTargetClass().getURI().equals(propertyShape.getShClass().getURI())) {
					proprieteDoc.setLinkNodeShapeUri(aNodeShape.getShortForm()); //aName.getShortForm().getLocalName()
					if(aNodeShape.getRdfsLabel() == null) {
						proprieteDoc.setLinkNodeShape(aNodeShape.getShortForm());
					}else {
						proprieteDoc.setLinkNodeShape(aNodeShape.getRdfsLabel());
					}
					break;
				// checks that the URI of the NodeShape is itself equal to the sh:class
				} else if (aNodeShape.getNodeShape().getURI().equals(propertyShape.getShClass().getURI())) {
					proprieteDoc.setLinkNodeShapeUri(aNodeShape.getShortForm()); //aName.getShortForm().getLocalName()
					if(aNodeShape.getRdfsLabel() == null) {
						proprieteDoc.setLinkNodeShape(aNodeShape.getShortForm());
					}else {
						proprieteDoc.setLinkNodeShape(aNodeShape.getRdfsLabel());
					}
					break;
				}
			}					
		}

		proprieteDoc.setExpectedValueAdditionnalInfoIn(render(propertyShape.getShIn(), false));
		proprieteDoc.setCardinalite(renderCardinalities(propertyShape.getShMinCount(), propertyShape.getShMaxCount()));
		proprieteDoc.setDescription(selectDescription(propertyShape, shaclGraph.union(owlGraph), lang));
		
		// create a String of comma-separated short forms
		proprieteDoc.setOr(render(propertyShape.getShOr(), false));
		
		return proprieteDoc;
	}
	
	public static String selectLabel(PropertyShape prop, Model owlModel, String lang) {
		String shName = render(prop.getShName(), true); //property shape's sh:name
		String rdfsLabel = render(ConstraintValueReader.readLiteralInLang(owlModel.getResource(prop.getShPath().getURI()), RDFS.label, lang), true); //property label from ontology (if provided)
		String dctTitle = render(ConstraintValueReader.readLiteralInLang(prop.getResource(), DCTerms.title, lang), true); //property shape's dct:title

		String additionalLabel = Stream.of(rdfsLabel, dctTitle)
																	 .filter(Objects::nonNull)
																	 .collect(Collectors.joining(", "));

		return additionalLabel.isEmpty() ? shName : shName + " (" + additionalLabel + ")";
	}
	
	public static String selectDescription(PropertyShape prop, Model owlModel, String lang) {
		String shDescription = render(prop.getShDescription(), true); //property shape's sh:description
		String rdfsComment = render(ConstraintValueReader.readLiteralInLang(owlModel.getResource(prop.getShPath().getURI()), RDFS.comment, lang), true); //property description from ontology (if provided)
		String dctDescription = render(ConstraintValueReader.readLiteralInLang(prop.getResource(), DCTerms.description, lang), true); //property shape's dct:description

		return Stream.of(shDescription, rdfsComment, dctDescription)
								 .filter(Objects::nonNull)
								 .collect(Collectors.joining("<br/> - <br/>"));
	}
	
	public static String render(List<? extends RDFNode> list, boolean plainString) {
		if(list == null) {
			return null;
		}
		
		return list.stream().map(item -> {
			return render(item, plainString);
		}).collect(Collectors.joining(", "));
	}
	
	public static Link buildPathLink(PropertyShape prop) {			
		if(prop.getShPath().isURIResource()) {
			return new Link(
					prop.getShPath().getURI(),
					prop.getShPathAsString()
			);			
		} else {
			return new Link(
					null,
					prop.getShPathAsString()
			);
		}
	}
	
	public static String render(RDFNode node, boolean plainString) {
		if(node == null) {
			return null;
		}
		
		if(node.isURIResource()) {
			return node.getModel().shortForm(node.asResource().getURI());
		} else if(node.isAnon()) {
			return node.toString();
		} else if(node.isLiteral()) {
			// if we asked for a plain string, just return the literal string
			if(plainString) {				
				
				try {
					if(node.asLiteral().getDatatypeURI().equals(XSD.date.getURI())) {
						SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");					
						Date date = formatter.parse(node.asLiteral().getLexicalForm());
						return formatter.format(date);
					} else if (node.asLiteral().getDatatypeURI().equals(XSD.dateTime.getURI())) {
						SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
						SimpleDateFormat outputformatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						Date date = formatter.parse(node.asLiteral().getLexicalForm());
						return outputformatter.format(date);
					} else {
						return node.asLiteral().getLexicalForm();
					}
				} catch (ParseException e) {
					e.printStackTrace();
					node.asLiteral().getLexicalForm();
				}
			}
			
			if (node.asLiteral().getDatatype() != null && !node.asLiteral().getDatatypeURI().equals(RDF.langString.getURI())) {
				// nicely prints datatypes with their short form
				return "\"" + node.asLiteral().getLexicalForm() + "\"<sup>^^"
						+ node.getModel().shortForm(node.asLiteral().getDatatype().getURI())+"</sup>";
			} else if (node.asLiteral().getLanguage() != null) {
				return "\"" + node.asLiteral().getLexicalForm() + "\"<sup>@"
						+ node.asLiteral().getLanguage()+"</sup>";
			} else {
				return node.toString();
			}
		} else {
			// default, should never get there
			return node.toString();
		}
	}	
	
	public static String renderCardinalities(Integer min, Integer max) {
		String minCount = "0";
		String maxCount = "*";
		
		if (min != null) {
			minCount = min.toString();
		}
		if (max != null) {
			maxCount = max.toString();
		}
		
		return minCount + ".." + maxCount;
	}
	
	public static String selectExpectedValueLabel(
			Resource shClass,
			Resource shNode,
			Resource value_datatype,
			Resource value_nodeKind,
			RDFNode value_shValue
	) {
		String value = null;

		if (value_shValue != null) {
			value = render(value_shValue, false);
		} else if (shClass != null) {
			value = render(shClass, false);
		} else if (shNode != null) {
			value = render(shNode, false);
		} else if (value_datatype != null) {
			value = render(value_datatype, false);
		} else if (value_nodeKind != null) {
			value = renderNodeKind(value_nodeKind);
		}
		
		return value;
	}
	
	public static String renderNodeKind(Resource nodeKind) {
		if(nodeKind == null) {
			return null;
		}
		
		String rendered = render(nodeKind, false);
		if (rendered.startsWith("sh:")) {
			return rendered.split(":")[1];	
		} else {
			return rendered;
		}
	}
	
}
