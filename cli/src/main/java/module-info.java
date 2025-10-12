import org.nasdanika.capability.CapabilityFactory;
import org.nasdanika.models.ecore.cli.EcoreHtmlAppGeneratorCommandFactory;

module org.nasdanika.models.ecore.cli {
	
	exports org.nasdanika.models.ecore.cli;

	requires transitive org.nasdanika.models.app.cli;		
	requires org.nasdanika.models.ecore.graph;
	
	opens org.nasdanika.models.ecore.cli to info.picocli, org.nasdanika.cli;
	
	provides CapabilityFactory with	EcoreHtmlAppGeneratorCommandFactory;

}
