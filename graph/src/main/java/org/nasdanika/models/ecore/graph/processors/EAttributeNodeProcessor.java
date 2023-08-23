package org.nasdanika.models.ecore.graph.processors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.diagram.plantuml.Link;
import org.nasdanika.diagram.plantuml.clazz.Attribute;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.graph.WidgetFactory;

public class EAttributeNodeProcessor extends EStructuralFeatureNodeProcessor<EAttribute> {

	public EAttributeNodeProcessor(
			NodeProcessorConfig<WidgetFactory, WidgetFactory> config,
			Context context,
			java.util.function.Function<ProgressMonitor, Action> prototypeProvider) {
		super(config, context, prototypeProvider);
	}	
	
	@Override
	public Attribute generateMember(URI base, ProgressMonitor progressMonitor) {
		Attribute attribute = new Attribute();
		attribute.getName().add(new Link(getTarget().getName()));
		
		return attribute;
	}	
	
	
}
