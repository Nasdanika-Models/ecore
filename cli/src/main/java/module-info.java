import org.nasdanika.capability.CapabilityFactory;
import org.nasdanika.models.ecore.cli.ModelCommandFactory;

module org.nasdanika.models.ecore.cli {
	
	exports org.nasdanika.models.ecore.cli;

	requires transitive org.nasdanika.html.model.app.gen.cli;		
	requires transitive org.nasdanika.cli;
	requires spring.core;
	requires org.eclipse.emf.ecore.xmi;
	requires org.nasdanika.html.model.app.graph;
	requires org.nasdanika.models.ecore.graph;
	
	opens org.nasdanika.models.ecore.cli to info.picocli;
	
	provides CapabilityFactory with	ModelCommandFactory;

}
