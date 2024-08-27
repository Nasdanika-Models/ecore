import org.nasdanika.capability.CapabilityFactory;
import org.nasdanika.models.ecore.cli.EcoreHtmlAppGeneratorCommandFactory;

module org.nasdanika.models.ecore.cli {
	
	exports org.nasdanika.models.ecore.cli;

	requires transitive org.nasdanika.html.model.app.gen.cli;		
	requires org.nasdanika.models.ecore.graph;
	
	opens org.nasdanika.models.ecore.cli to info.picocli;
	
	provides CapabilityFactory with	EcoreHtmlAppGeneratorCommandFactory;

}
