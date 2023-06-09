module org.nasdanika.models.ecore.processors.doc {
	
	exports org.nasdanika.models.ecore.processors.doc;
	opens org.nasdanika.models.ecore.processors.doc;
	
	requires transitive org.nasdanika.html.model.app;
	requires transitive org.nasdanika.models.ecore.graph;
		
}