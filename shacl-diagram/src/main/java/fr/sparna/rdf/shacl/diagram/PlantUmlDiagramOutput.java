package fr.sparna.rdf.shacl.diagram;

public class PlantUmlDiagramOutput {

	private String plantUmlString;
	private String diagramUri;
	private String diagramTitle;
	private String diagramDescription;
	
	public PlantUmlDiagramOutput(PlantUmlDiagram d, PlantUmlRenderer renderer) {
		super();
		this.plantUmlString = renderer.renderDiagram(d);
		if(d.getResource() != null) {
			this.diagramUri = d.getResource().getURI();
		}
		this.diagramTitle = d.getTitle();
		this.diagramDescription = d.getDescription();
	}
	
	public PlantUmlDiagramOutput(String plantUmlString) {
		super();
		this.plantUmlString = plantUmlString;
	}

	public PlantUmlDiagramOutput(String plantUmlString, String diagramUri) {
		super();
		this.plantUmlString = plantUmlString;
		this.diagramUri = diagramUri;
	}
	
	public String getDisplayTitle() {
		if(this.diagramTitle != null) {
			return diagramTitle;
		} else {
			return getLocalName(this.diagramUri);
		}
	}
	
	private static String getLocalName(String uri) {
		if(uri.contains("#")) {
			return uri.substring(uri.lastIndexOf('#')+1);
		} else {
			return uri.substring(uri.lastIndexOf('/')+1);
		}
	}

	public String getDiagramUri() {
		return diagramUri;
	}

	public void setDiagramUri(String diagramUri) {
		this.diagramUri = diagramUri;
	}

	public String getDiagramTitle() {
		return diagramTitle;
	}

	public void setDiagramTitle(String diagramTitle) {
		this.diagramTitle = diagramTitle;
	}

	public String getDiagramDescription() {
		return diagramDescription;
	}

	public void setDiagramDescription(String diagramDescription) {
		this.diagramDescription = diagramDescription;
	}

	public String getPlantUmlString() {
		return plantUmlString;
	}
	
}
