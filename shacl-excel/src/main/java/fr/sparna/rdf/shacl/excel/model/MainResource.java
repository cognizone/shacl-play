package fr.sparna.rdf.shacl.excel.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import fr.sparna.rdf.shacl.excel.CellValues;

public class MainResource {
	
	private Resource mainResource;
	
	public MainResource (Resource r) {  
	    this.mainResource = r;		
	}

	public Resource getMainResource() {
		return mainResource;
	}
	
	public List<String[]> parse() {
		List<String[]> values = new ArrayList<>();
		
		for (Statement aStatement : this.mainResource.listProperties().toList()) {
			ColumnSpecification spec = new ColumnSpecification(aStatement);
			String cellValue = CellValues.toCellValue(aStatement.getObject(), spec);
			String[] pair = new String[] {spec.getHeaderString(), cellValue};

			values.add(pair);
		}
		
		return values;
	}
}
