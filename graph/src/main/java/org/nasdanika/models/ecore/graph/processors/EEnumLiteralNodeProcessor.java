package org.nasdanika.models.ecore.graph.processors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EObject;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.diagram.plantuml.Link;
import org.nasdanika.diagram.plantuml.clazz.EnumLiteral;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.models.app.Action;
import org.nasdanika.models.app.Label;
import org.nasdanika.models.app.graph.WidgetFactory;

public class EEnumLiteralNodeProcessor extends EModelElementNodeProcessor<EEnumLiteral> {

	public EEnumLiteralNodeProcessor(
			NodeProcessorConfig<WidgetFactory, WidgetFactory> config,
			Context context,
			java.util.function.BiFunction<EObject, ProgressMonitor, Action> prototypeProvider) {
		super(config, context, prototypeProvider);
	}	

	public EnumLiteral generateLiteral(URI base, ProgressMonitor progressMonitor) {
		EnumLiteral litaral = new EnumLiteral();
		litaral.getName().add(new Link(getTarget().getName()));
		
		Object link = createLink(base, progressMonitor);
		if (link instanceof Label) {
			litaral.setTooltip(((Label) link).getTooltip());
		}
		if (link instanceof org.nasdanika.models.app.Link) {
			litaral.setLocation(((org.nasdanika.models.app.Link) link).getLocation());
		}
		return litaral;
	}	
	
}
