package org.nasdanika.models.ecore.graph.processors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.diagram.plantuml.Link;
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
	public Object createWidget(Object selector, URI base, ProgressMonitor progressMonitor) {
		if (selector == org.nasdanika.diagram.plantuml.clazz.Attribute.class) {
			org.nasdanika.diagram.plantuml.clazz.Attribute ret = new org.nasdanika.diagram.plantuml.clazz.Attribute();
			ret.getName().add(new Link(getTarget().getName()));
//			ret.getName().add(new org.nasdanika.diagram.plantuml.Link(getTarget().getName()));
			// TODO - type
			return ret;
		}
		return super.createWidget(selector, base, progressMonitor);
	}
	
	@Override
	public org.nasdanika.diagram.plantuml.clazz.Attribute generateMember(URI base, ProgressMonitor progressMonitor) {
		throw new UnsupportedOperationException();
	}	
	
	
}
