module org.nasdanika.models.ecore.graph {
	
	requires transitive org.nasdanika.html.model.app.graph;
	requires transitive org.nasdanika.emf;
	requires org.apache.commons.codec;
	requires org.eclipse.emf.codegen.ecore;
	requires org.eclipse.emf.ecore.xmi;
	requires org.nasdanika.html.model.html;
	requires org.nasdanika.ncore;
	requires org.nasdanika.html.model.app.gen;
	requires transitive org.nasdanika.diagram;
	
	exports org.nasdanika.models.ecore.graph;
	exports org.nasdanika.models.ecore.graph.processors;
	
}