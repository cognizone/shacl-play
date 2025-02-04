package fr.sparna.rdf.shacl.diagram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shacl.vocabulary.SHACLM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.sparna.rdf.jena.ModelRenderingUtils;

public class PlantUmlRenderer {

	private Logger log = LoggerFactory.getLogger(this.getClass().getName());
	
	protected boolean generateAnchorHyperlink = false;
	protected boolean displayPatterns = false;
	protected boolean avoidArrowsToEmptyBoxes = true;
	protected boolean includeSubclassLinks = true;
	protected boolean hideProperties = false;
	
	protected List<String> inverseList = new ArrayList<String>();
	
	protected transient PlantUmlDiagram diagram;
	
	public PlantUmlRenderer() {
		super();
	}

	public PlantUmlRenderer(boolean generateAnchorHyperlink, boolean displayPatterns, boolean avoidArrowsToEmptyBoxes, boolean includeSubclassLinks, boolean hideProperties) {
		super();
		this.generateAnchorHyperlink = generateAnchorHyperlink;
		this.displayPatterns = displayPatterns;
		this.avoidArrowsToEmptyBoxes = avoidArrowsToEmptyBoxes;
		this.includeSubclassLinks = includeSubclassLinks;
		this.hideProperties = hideProperties;
	}

	private String render(
			PlantUmlProperty property,
			PlantUmlBox box,
			boolean renderAsDatatypeProperty,
			Map<String, String> collectRelationProperties
	) {
		
		//get the color for the arrow drawn
		String colorArrowProperty = "";
		if(property.getColorString() != null) {
			colorArrowProperty = "[bold,#"+property.getColorString()+"]";
		}
				
		if (property.getShNode().isPresent()) {
			String getCodeUML = renderAsNodeReference(property, box, renderAsDatatypeProperty, colorArrowProperty, collectRelationProperties);
			return (getCodeUML.contains("->")) ? "" : getCodeUML;
		} else if (property.getShClass().isPresent()) {
			String getCodeUML = renderAsClassReference(property, box, renderAsDatatypeProperty, collectRelationProperties); 
			return (getCodeUML.contains("->")) ? "" : getCodeUML;
		} else if (property.getShQualifiedValueShape().isPresent()) {
			String getCodeUML = renderAsQualifiedShapeReference(property, box,colorArrowProperty,collectRelationProperties); 
			return (getCodeUML.contains("->")) ? "" : getCodeUML;
		} else if (property.hasShOrShClassOrShNode()) {
			return renderAsOr(property, box,colorArrowProperty);
		} else {
			return renderDefault(property, box);
		}
	}

	// uml_shape+ " --> " +"\""+uml_node+"\""+" : "+uml_path+uml_datatype+"
	// "+uml_literal+" "+uml_pattern+" "+uml_nodekind(uml_nodekind)+"\n";
	private String renderAsNodeReference(PlantUmlProperty property, PlantUmlBox box, Boolean renderAsDatatypeProperty, String colorArrow, Map<String, String> collectRelationProperties) {

		// find in property if has attribut
		String output = null;
		String ctrlnodeOrigen = null;
		String ctrlnodeDest = null;
		
		String nodeReference = this.resolveShNodeReference(property.getShNode().get());

		if (renderAsDatatypeProperty) {				
			output = box.getPlantUmlQuotedBoxName() + " : +" + property.getPathAsSparql() + " : " + nodeReference;	

			if (property.getPlantUmlCardinalityString() != null) {
				output += " " + property.getPlantUmlCardinalityString() + " ";
			}
			if (property.getShPattern().isPresent() && this.displayPatterns) {
				output += "(" + ModelRenderingUtils.render(property.getShPattern().get()) + ")" + " ";
			}
			if (property.getShNodeKind().isPresent() && !property.getShNodeKind().get().equals(SHACLM.IRI)) {
				output += ModelRenderingUtils.render(property.getShNodeKind().get()) + " " ;
			}				
		} else if(property.getShNode().isPresent() && diagram.findBoxByResource(property.getShNode().get()).getProperties().size() > 0) {

			output = box.getPlantUmlQuotedBoxName() + " -"+colorArrow+"-> \"" + nodeReference + "\" : " + property.getPathAsSparql();
			
			if (property.getPlantUmlCardinalityString() != null) {
				output += " " + property.getPlantUmlCardinalityString() + " ";
			}
			if (property.getShPattern().isPresent() && this.displayPatterns) {
				output += "(" + ModelRenderingUtils.render(property.getShPattern().get()) + ")" + " ";
			}
			
			// merge the arrow
			String option = "";
			if (property.getPlantUmlCardinalityString() != null) {
				option += "<U+00A0>" + property.getPlantUmlCardinalityString() + " ";
			}				
			if (property.getShPattern().isPresent() && this.displayPatterns) {
				option += "(" + ModelRenderingUtils.render(property.getShPattern().get()) + ")" + " ";
			}
			
			// Merge all arrow 
			collectData(
					// key
					box.getPlantUmlQuotedBoxName() + " -"+colorArrow+"-> \"" + nodeReference+ "\" : ",
					// Values
					property.getPathAsSparql() + option,
					// Record
					collectRelationProperties
					);				
			
		} else {
			
			//output = boxName + " -[bold]-> \"" + property.getValue_node().getLabel() + "\" : " + property.getValue_path();
			output = box.getPlantUmlQuotedBoxName() + " -"+colorArrow+"-> \"" + property.getShNodeLabel() + "\" : " + property.getPathAsSparql();
			
			String option = "";
			if (property.getPlantUmlCardinalityString() != null) {
				output += " " + property.getPlantUmlCardinalityString() + " ";
				option += "<U+00A0>" + property.getPlantUmlCardinalityString() + " ";
			}
			if (property.getShPattern().isPresent() && this.displayPatterns) {
				output += "(" + ModelRenderingUtils.render(property.getShPattern().get()) + ")" + " ";
				option += "(" + ModelRenderingUtils.render(property.getShPattern().get()) + ")" + " ";
			}
			
			// function for Merge all arrow what point to same class
			collectData(
					// key
					box.getPlantUmlQuotedBoxName() + " -"+colorArrow+"-> \"" + nodeReference + "\" : ",
					// Values
					property.getPathAsSparql()+option,
					// Record
					collectRelationProperties
					);
			
		}

		if (ctrlnodeOrigen != null & ctrlnodeDest != null) {
			if (inverseList.contains("node" + '-' + ctrlnodeOrigen + '-' + ctrlnodeDest)
					|| inverseList.contains("node" + '-' + ctrlnodeDest + '-' + ctrlnodeOrigen)) {
				output = "";
			}
			inverseList.add("node" + '-' + ctrlnodeOrigen + '-' + ctrlnodeDest);
		}

		output += "\n";
		return output;
	}

	// value = uml_shape+" --> "+"\""+uml_or;
	private String renderAsOr(PlantUmlProperty property, PlantUmlBox box, String colorArrow) {
		// use property local name to garantee unicity of diamond
		String nodeShapeLocalName = box.getNodeShape().getLocalName();
		String nodeshapeId = nodeShapeLocalName.contains(":")?nodeShapeLocalName.split(":")[1]:nodeShapeLocalName;
		String localName = property.getPropertyShape().getLocalName();
		if (localName == null) {
		    localName = property.getPropertyShape().getId().getLabelString();
		}
		String sNameDiamond = "diamond_" + nodeshapeId.replace("-", "_") + "_" + localName.replace("-", "_");
		// diamond declaration
		String output = "<> " + sNameDiamond + "\n";

		// link between box and diamond
		//output += boxName + " -[bold]-> \"" + sNameDiamond + "\" : " + property.getValue_path();
		output += box.getPlantUmlQuotedBoxName() + " -"+colorArrow+"-> \"" + sNameDiamond + "\" : " + property.getPathAsSparql();

		// added information on link
		if (property.getPlantUmlCardinalityString() != null) {
			output += " " + property.getPlantUmlCardinalityString() + " ";
		}
		if (property.getShPattern().isPresent() && this.displayPatterns) {
			output += "(" + ModelRenderingUtils.render(property.getShPattern().get()) + ")" + " ";
		}
		output += "\n";

		// now link diamond to each value in the sh:or
		if(property.getShOrShClass() != null) {
			for (Resource shOrShClass : property.getShOrShClass() ) {
				output += sNameDiamond + " .. " + " \""+this.resolveShClassReference(shOrShClass)+ "\"" + "\n";
			}			
		}
		if(property.getShOrShNode() != null) {
			for (Resource shOrShNode : property.getShOrShNode() ) {
				output += sNameDiamond + " .. " + " \""+this.resolveShNodeReference(shOrShNode)+ "\"" + "\n";
			}
			
		}
		
		return output;
	}

	// value = uml_shape+ " --> " +"\""+uml_qualifiedvalueshape+"\""+" :
	// "+uml_path+uml_datatype+" "+uml_qualifiedMinMaxCount+"\n";
	private String renderAsQualifiedShapeReference(PlantUmlProperty property, PlantUmlBox box, String colorArrow, Map<String, String> collectRelationProperties) {
		String output = box.getPlantUmlQuotedBoxName() + " -"+colorArrow+"-> \"" + property.getShQualifiedValueShapeLabel() + "\" : "
				+ property.getPathAsSparql();

		String option="";
		if (property.getPlantUmlQualifiedCardinalityString() != null) {
			output += " " + property.getPlantUmlQualifiedCardinalityString() + " ";
			option = " " + property.getPlantUmlQualifiedCardinalityString() + " ";
		}
		
		collectData(
				//codeKey
				box.getPlantUmlQuotedBoxName() + " -"+colorArrow+"-> \"" + property.getShQualifiedValueShapeLabel() + "\" : ",
				//data value
				property.getPathAsSparql()+option, 
				collectRelationProperties);

		output += "\n";

		return output;
	}

	// value = uml_shape+" --> "+"\""+uml_class_property+"\""+" :
	// "+uml_path+uml_literal+" "+uml_pattern+" "+uml_nodekind+"\n";
	private String renderAsClassReference(PlantUmlProperty property, PlantUmlBox box, boolean renderAsDatatypeProperty, Map<String,String> collectRelationProperties) {

		String output = "";
		
		String classReference = this.resolveShClassReference(property.getShClass().get());
		
		if (renderAsDatatypeProperty) {
			
			output = box.getPlantUmlQuotedBoxName() + " : +" + property.getPathAsSparql() + " : " + classReference;	
			
			if (property.getPlantUmlCardinalityString() != null) {
				output += " " + property.getPlantUmlCardinalityString() + " ";
			}
			if (property.getShPattern().isPresent() && this.displayPatterns) {
				output += "(" + ModelRenderingUtils.render(property.getShPattern().get()) + ")" + " ";
			}
			if (property.getShNodeKind().isPresent() && !property.getShNodeKind().equals(SHACLM.IRI)) {
				output += ModelRenderingUtils.render(property.getShNodeKind().get()) + " " ;
			}
			
		} else {
			String labelColor = "";
			String labelColorClose = "";
			if(property.getColorString() != null) {
				labelColor = "<color:"+property.getColorString()+">"+" ";
				labelColorClose = "</color>";
			}

			output = box.getPlantUmlQuotedBoxName() + " --> \""+""+labelColor+ classReference + "\" : " + property.getPathAsSparql()+" ";
					
			String option = "";
			if (property.getPlantUmlCardinalityString() != null) {
				output += " " + property.getPlantUmlCardinalityString() + " ";
				option += "<U+00A0>" + property.getPlantUmlCardinalityString() + " ";
			}
			if (property.getShPattern().isPresent() && this.displayPatterns) {
				output += "(" + ModelRenderingUtils.render(property.getShPattern().get()) + ")" + " ";
				option += "(" + ModelRenderingUtils.render(property.getShPattern().get()) + ")" + " ";
			}

			collectData(
				// Key
				box.getPlantUmlQuotedBoxName() + " --> \""+""+labelColor+ classReference + "\" : ",
				// data Value
				property.getPathAsSparql() + option,
				collectRelationProperties
			);
			
			output += labelColorClose;
		}
		
		output += " \n";
		
		return output;
	}

	private String renderDefault(PlantUmlProperty property, PlantUmlBox box) {
		
		String labelColor = "";
		String labelColorClose = "";
		if(property.getColorString() != null) {
			labelColor = "<color:"+property.getColorString()+">"+" ";
			labelColorClose = "</color>";
		}
		
		
		String output = box.getPlantUmlQuotedBoxName() + " : "+""+labelColor+ property.getPathAsSparql() + " ";
		
		// if  sh:or value is of kind of datatype , for each property concat with or word .. eg. xsd:string or rdf:langString
		String shOr_Datatype = "";
		if(property.getShOrShDatatype() != null) {
			shOr_Datatype += property.getShOrShDatatype().stream().map(r -> ModelRenderingUtils.render(r)).collect(Collectors.joining(" or "));
		}
		if(property.getShOrShNodeKind() != null) {
			shOr_Datatype += property.getShOrShNodeKind().stream().map(r -> ModelRenderingUtils.render(r)).collect(Collectors.joining(" or "));
		}
		
		if (property.getShDatatype().isPresent()) {
			output += " : " + ModelRenderingUtils.render(property.getShDatatype().get()) + " ";
		} else if (property.getShDatatype().isEmpty() && !shOr_Datatype.equals("")) {
			output += " : " +shOr_Datatype+ " ";
		}
		
		
		
		if (property.getPlantUmlCardinalityString() != null) {
			output += " " + property.getPlantUmlCardinalityString() + " ";
		}
		if (property.getShPattern().isPresent() && this.displayPatterns) {
			output += "{field}" + " " + "(" + ModelRenderingUtils.render(property.getShPattern().get()) + ")" + " ";
		}
		if (property.isUniqueLang()) {
			output += "uniqueLang" + " ";
		}
		if (property.getShHasValue().isPresent()) {
			output += " = " + ModelRenderingUtils.render(property.getShHasValue().get()) + " ";
		}
		
		
		if (
			 (!property.getShHasValue().isPresent())
			 &&
			 (!property.getShClass().isPresent())
			 &&
			 (!property.getShDatatype().isPresent())
			 &&
			 (!property.getShNode().isPresent())
			 &&					
			 (property.getPlantUmlCardinalityString() == null)
			 &&
			 (property.getShNodeKind().isPresent()) 
			) {
			String nameNodeKind = ModelRenderingUtils.render(property.getShNodeKind().get()); 
			output += " :  "+nameNodeKind.split(":")[1];
		}
				
		output += labelColorClose+" \n";
		
		return output;
	}
	
	public String renderDiagram(PlantUmlDiagram diagram) {
		this.diagram = diagram;
		
		StringBuffer sourceuml = new StringBuffer();	
		sourceuml.append("@startuml\n");
		// this allows to have dots in unescaped classes names, to avoid that are interpreted as namespaces
		// see https://github.com/sparna-git/shacl-play/issues/122
		// see https://forum.plantuml.net/221/dots-in-class-names
		sourceuml.append("set namespaceSeparator none\n");
		sourceuml.append("skinparam classFontSize 14"+"\n");
		sourceuml.append("!define LIGHTORANGE\n");
		sourceuml.append("skinparam componentStyle uml2\n");
		sourceuml.append("skinparam wrapMessageWidth 100\n");
		sourceuml.append("skinparam ArrowColor #Maroon\n");
		sourceuml.append("set namespaceSeparator none \n"); // Command for not create an package uml
		
		// retrieve all package declaration
		for (PlantUmlBox plantUmlBox : diagram.getBoxes()) {
			sourceuml.append(this.renderNodeShape(plantUmlBox,this.avoidArrowsToEmptyBoxes));
		}
		
		sourceuml.append("hide circle\n");
		sourceuml.append("hide methods\n");
		sourceuml.append("hide empty members\n");
		
		if (this.hideProperties) {
			sourceuml.append("hide fields\n");
		}
		
		sourceuml.append("@enduml\n");
		
		// return output
		return sourceuml.toString();
	}

	public String renderNodeShape(PlantUmlBox box, boolean avoidArrowsToEmptyBoxes) {
		// String declaration = "Class"+"
		// "+"\""+box.getNameshape()+"\""+((box.getNametargetclass() != null)?"
		// "+"<"+box.getNametargetclass()+">":"");
		String declaration = "";

		String colorBackGround = "";
		if (box.getBackgroundColorString() != null) {
			colorBackGround = "#back:"+box.getBackgroundColorString();			
		}else {
			colorBackGround = "";
		}
		
		String labelColorClass = "";
		if(box.getColorString() != null) {
			if(!colorBackGround.equals("")) {
				labelColorClass += ";";
			} else {
				labelColorClass += "#";
			}
			labelColorClass += "text:"+box.getColorString();
		}
		
		// resolve subclasses only if we were asked for it
		List<PlantUmlBox> superClassesBoxes = new ArrayList<>();
		if(this.includeSubclassLinks) {
			superClassesBoxes = box.getRdfsSubClassOf().stream().map(sc -> this.diagram.findBoxByResource(sc)).filter(b -> b != null).collect(Collectors.toList());
		}
		
		if (box.getProperties().size() > 0 || superClassesBoxes.size() > 0) {
			if (box.getNodeShape().isAnon()) {
				// give it an empty label
				declaration = "Class" + " " + box.getLabel() +" as " +"\""+" \"";
			} else {
				declaration = "Class" + " " + "\"" + box.getLabel() + "\"";
			}

			declaration += (this.generateAnchorHyperlink) ? " [[#" + box.getNodeShape().getModel().shortForm(box.getNodeShape().getURI()) +"}]]" : "";
			declaration += " " + colorBackGround+labelColorClass + "\n";
			
			if (superClassesBoxes != null) {
				for (PlantUmlBox aSuperClass : superClassesBoxes) {
					// generate an "up" arrow - bolder and gray to distinguish it from other arrows
					declaration += "\""+box.getLabel()+"\"" + " -up[#gray,bold]-|> " + "\""+aSuperClass.getLabel()+"\"" + "\n";
				}
			}
			
			String declarationPropertes = "";
			Map<String,String> collectRelationProperties = new HashMap<>();
			for (PlantUmlProperty plantUmlproperty : box.getProperties()) {
				boolean displayAsDatatypeProperty = false;
				
				// if we want to avoid arrows to empty boxes...
				// note that this behavior is triggered only if the diagram has a certain size
				if (avoidArrowsToEmptyBoxes && diagram.getBoxes().size() > 8) {
					// then see if there is a reference to a box
					String arrowReference = diagram.resolvePropertyShapeShNodeOrShClass(plantUmlproperty);
					PlantUmlBox boxReference = diagram.findBoxById(arrowReference);
					
					// if the box is empty...
					if (
							arrowReference != null
							&&
							(
									// no box : the reference is an sh:class pointing to a URI
									// that does not correspond to a NodeShape
									boxReference == null
									||
									(
											// points to an existing NodeShape, but with no property
											boxReference.getProperties().size() == 0
											&&
											// and does not have a super class
											superClassesBoxes.size() == 0
									)
							)							
					) {
						// count number of times it is used
						int nCount = 0;
						for (PlantUmlBox plantumlbox : diagram.getBoxes()) {
							nCount += plantumlbox.countShNodeOrShClassReferencesTo(arrowReference, diagram);
						}
						
						// if used more than once, then display the box, otherwise don't display it
						if (nCount <= 1) {
							displayAsDatatypeProperty = true;
						}
					}
				}
				
				
				String codePropertyPlantUml = this.render(
						plantUmlproperty, 
						box,
						displayAsDatatypeProperty,
						collectRelationProperties
				);
				
				
				if (codePropertyPlantUml!="") {
					declarationPropertes += codePropertyPlantUml; 
				}
				//declaration += this.render(plantUmlproperty, "\"" + box.getLabel() + "\"", displayAsDatatypeProperty,box.getLabel());
			}
			
			if (collectRelationProperties.size() > 0) {
				for (Map.Entry<String, String> entry : collectRelationProperties.entrySet()) {
					String outputData = entry.getKey() + entry.getValue()+" \n";;
					declarationPropertes +=  outputData;
					
				}
			}
			declaration += declarationPropertes;
		}
		
		return declaration;
	}
	
	public String resolveShClassReference(Resource shClassReference) {
		PlantUmlBox b = this.diagram.findBoxByTargetClass(shClassReference);
		if(b != null) {
			return b.getLabel();
		} else {
			if (shClassReference.isURIResource()) { 
				return shClassReference.getModel().shortForm(shClassReference.getURI());
			} else {
				log.warn("Found a blank sh:class reference on a shape with sh:path "+shClassReference+", cannot handle it");
				return null;
			}
		}
	}
	
	public String resolveShNodeReference(Resource shNodeReference) {
		PlantUmlBox b = this.diagram.findBoxByResource(shNodeReference);
		if(b != null) {
			return b.getLabel();
		} else {
			return ModelRenderingUtils.render(shNodeReference, true);

		}
	}
	
	/*
	 * function for Merge arrows what point to same class.
	 * codeKey - property key
	 * dataValue - data values or additional values
	 * collectRelationProperties - save all properties and if the property is included in the map, generate a list of values.
	 */
	public void collectData(String codeKey,String dataValue ,Map<String,String> collectRelationProperties) {
		
		if (!linkPropertyExist(codeKey, collectRelationProperties)) {
			// 
			collectRelationProperties.put(
					// key
					codeKey,
					// Values
					dataValue);
			
		} else {
			//update record
			updatePropertyRelacionValue(
					// key
					codeKey,
					//data values
					dataValue ,
					//Map
					collectRelationProperties
					);
		}
		
	}	
	
	public boolean linkPropertyExist(String codeKey, Map<String,String> collectRelationProperties) {
		return collectRelationProperties.entrySet().stream().filter(f -> f.getKey().equals(codeKey)).findAny().isPresent();
	}
	
	/* Merge for each input data values in the same property */
	public void updatePropertyRelacionValue(String codeKey,String newValue ,Map<String,String> collectRelationProperties) {
		
		String recordInMap = collectRelationProperties.entrySet().stream().filter(f -> f.getKey().equals(codeKey)).findFirst().get().getValue();
		collectRelationProperties.computeIfPresent(codeKey, (k,v) -> v = recordInMap+" \\l"+newValue);
	}
	
	
	public boolean isGenerateAnchorHyperlink() {
		return generateAnchorHyperlink;
	}

	public void setGenerateAnchorHyperlink(boolean generateAnchorHyperlink) {
		this.generateAnchorHyperlink = generateAnchorHyperlink;
	}

	public boolean isDisplayPatterns() {
		return displayPatterns;
	}

	public void setDisplayPatterns(boolean displayPatterns) {
		this.displayPatterns = displayPatterns;
	}

	public boolean isAvoidArrowsToEmptyBoxes() {
		return avoidArrowsToEmptyBoxes;
	}

	public void setAvoidArrowsToEmptyBoxes(boolean avoidArrowsToEmptyBoxes) {
		this.avoidArrowsToEmptyBoxes = avoidArrowsToEmptyBoxes;
	}

	public boolean isIncludeSubclassLinks() {
		return includeSubclassLinks;
	}

	public void setIncludeSubclassLinks(boolean includeSubclassLinks) {
		this.includeSubclassLinks = includeSubclassLinks;
	}

	public boolean isHideProperties() {
		return hideProperties;
	}

	public void setHideProperties(boolean hideProperties) {
		this.hideProperties = hideProperties;
	}

}
