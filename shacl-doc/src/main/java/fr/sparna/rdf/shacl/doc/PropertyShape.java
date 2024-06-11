package fr.sparna.rdf.shacl.doc;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.topbraid.shacl.vocabulary.SH;

public class PropertyShape {

	private Resource resource;
	
	// can be a URI or a blank node corresponding to a property path
	
	
	
	public PropertyShape(Resource resource) {
		super();
		this.resource = resource;
	}
	
	public Resource getResource() {
		return resource;
	}
	
	/**
	 * Returns the short form of the property or the property path already shortened
	 * @return
	 */
	public String getShPathAsString() {
		return (this.getShPath().isURIResource())?ModelRenderingUtils.render(this.getShPath(), false):ModelRenderingUtils.renderShaclPropertyPath(this.getShPath());
	}
	
	public String getShNameAsString(String lang) {
		return ModelRenderingUtils.render(this.getShName(lang), true);
	}
	
	public String getDisplayLabel(Model owlModel, String lang) {
		String shName = ModelRenderingUtils.render(this.getShName(lang), true); //property shape's sh:name
		String dctTitle = ModelRenderingUtils.render(ModelReadingUtils.readLiteralInLang(this.getResource(), DCTerms.title, lang), true); //property shape's dct:title

		String result = Stream.of(shName, dctTitle)
					.filter(Objects::nonNull)
					.reduce((s1, s2) -> s1 + " (" + s2 + ")")
					.orElse(shName);
		
		if(result == null && this.getShPath().isURIResource()) {
			// otherwise if we have skos:prefLabel on the property, take it
			result = ModelRenderingUtils.render(ModelReadingUtils.readLiteralInLang(owlModel.getResource(this.getShPath().getURI()), SKOS.prefLabel, lang), true);
		}
		
		if(result == null && this.getShPath().isURIResource()) {
			// otherwise if we have rdfs:label on the property, take it
			result = ModelRenderingUtils.render(ModelReadingUtils.readLiteralInLang(owlModel.getResource(this.getShPath().getURI()), RDFS.label, lang), true);
		}
		
		// otherwise return empty string, never null (for sorting)
		if(result == null) {
			result = "";
		}
		
		return result;
	}
	
	public String getDisplayDescription(Model owlModel, String lang) {
		String shDescription = ModelRenderingUtils.render(this.getShDescription(lang), true); //property shape's sh:description
		String dctDescription =ModelRenderingUtils.render(ModelReadingUtils.readLiteralInLang(this.getResource(), DCTerms.description, lang), true); //property shape's dct:description

		String result = Stream.of(shDescription, dctDescription)
													.filter(Objects::nonNull)
													.collect(Collectors.joining("<br/> - <br/>"));
		
		if(result == null && this.getShPath().isURIResource()) {
			// otherwise if we have skos:definition on the property, take it
			result = ModelRenderingUtils.render(ModelReadingUtils.readLiteralInLang(owlModel.getResource(this.getShPath().getURI()), SKOS.definition, lang), true);
		}
		
		if(result == null && this.getShPath().isURIResource()) {
			// otherwise if we have rdfs:comment on the property, take it
			result = ModelRenderingUtils.render(ModelReadingUtils.readLiteralInLang(owlModel.getResource(this.getShPath().getURI()), RDFS.comment, lang), true);
		}
		
		return result;
	}

	public List<Resource> getShOr() {
		if (this.resource.hasProperty(SH.or)) {			
			Resource list = this.resource.getProperty(SH.or).getList().asResource();
			List<RDFNode> rdflist = list.as(RDFList.class).asJavaList();

			List<Resource> result = new ArrayList<Resource>();
			rdflist.stream().forEach(item -> {
				if(item.isResource()) {
					if(item.asResource().hasProperty(SH.node)) {
						result.add(item.asResource().getPropertyResourceValue(SH.node));
					} else if(item.asResource().hasProperty(SH.class_)) {
						result.add(item.asResource().getPropertyResourceValue(SH.class_));
					} else if(item.asResource().hasProperty(SH.datatype)) {
						result.add(item.asResource().getPropertyResourceValue(SH.datatype));
					} else if(item.asResource().hasProperty(SH.or)) {
						// actually, this would be a NodeShape theoretically...
						PropertyShape recursiveShape = new PropertyShape(item.asResource());
						result.addAll(recursiveShape.getShOr());
					} else if(item.asResource().hasProperty(SH.nodeKind)) {
						result.add(item.asResource().getPropertyResourceValue(SH.nodeKind));
					} 
				} 
			});
			return result;
		} else {
			return null;
		}
	}

	public RDFNode getShValue() {
		return Optional.ofNullable(this.resource.getProperty(SH.hasValue)).map(s -> s.getObject()).orElse(null);
	}

	public Double getShOrder() {
		return Optional.ofNullable(this.resource.getProperty(SH.order)).map(s -> s.getDouble()).orElse(null);
	}

	public List<RDFNode> getShIn() {
		if (this.resource.hasProperty(SH.in)) {
			Resource list = this.resource.getProperty(SH.in).getList().asResource();
			return list.as(RDFList.class).asJavaList();
		} else {
			return null;
		}
	}

	public List<Literal> getShName(String lang) {
		return ModelReadingUtils.readLiteralInLang(this.resource, SH.name, lang);
	}

	public List<Literal> getShDescription(String lang) {
		return ModelReadingUtils.readLiteralInLang(this.resource, SH.description, lang);
	}

	public Resource getShPath() {
		return Optional.ofNullable(this.resource.getProperty(SH.path)).map(s -> s.getResource()).orElse(null);
	}

	public Resource getShDatatype() {
		return Optional.ofNullable(this.resource.getProperty(SH.datatype)).map(s -> s.getResource()).orElse(null);
	}

	public Resource getShNodeKind() {		
		return Optional.ofNullable(this.resource.getProperty(SH.nodeKind)).map(s -> s.getResource()).orElse(null);
	}
	
	public Integer getShMinCount() {
		return Optional.ofNullable(this.resource.getProperty(SH.minCount)).map(s -> Integer.parseInt(s.getString())).orElse(null);
	}
	
	public Integer getShMaxCount() {
		return Optional.ofNullable(this.resource.getProperty(SH.maxCount)).map(s -> Integer.parseInt(s.getString())).orElse(null);
	}

	public Literal getShPattern() {
		return Optional.ofNullable(this.resource.getProperty(SH.pattern)).map(s -> s.getLiteral()).orElse(null);
	}

	// TODO : devrait retourner un ShaclBox
	public Resource getShNode() {
		return Optional.ofNullable(this.resource.getProperty(SH.node)).map(s -> s.getResource()).orElse(null);
	}

	public Resource getShClass() {
		return Optional.ofNullable(this.resource.getProperty(SH.class_)).map(s -> s.getResource()).orElse(null);
	}
	
}
