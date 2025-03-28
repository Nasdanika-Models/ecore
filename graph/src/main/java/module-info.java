import org.nasdanika.capability.CapabilityFactory;
import org.nasdanika.models.ecore.graph.processors.EcoreNodeProcessorFactoryCapabilityFactory;

module org.nasdanika.models.ecore.graph {
	
	requires transitive org.nasdanika.models.app.graph;
	requires transitive org.nasdanika.emf;
	requires org.apache.commons.codec;
	requires org.eclipse.emf.codegen.ecore;
	requires org.eclipse.emf.ecore.xmi;
	requires org.nasdanika.models.html;
	requires org.nasdanika.ncore;
	requires org.nasdanika.models.app.gen;
	requires transitive org.nasdanika.diagram;
	requires transitive org.nasdanika.models.echarts.graph;
	requires org.jsoup;
	
	exports org.nasdanika.models.ecore.graph;
	exports org.nasdanika.models.ecore.graph.processors;
	
	provides CapabilityFactory with EcoreNodeProcessorFactoryCapabilityFactory;
	
}