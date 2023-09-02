package org.nasdanika.models.ecore.graph.processors;

import org.eclipse.emf.ecore.EReference;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.graph.processor.OutgoingEndpoint;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.graph.WidgetFactory;
import org.nasdanika.models.ecore.graph.OppositeReferenceConnection;

public class EReferenceNodeProcessor extends EStructuralFeatureNodeProcessor<EReference> {
		
	private WidgetFactory oppositeReferenceWidgetFactory;
	
	@OutgoingEndpoint()
	public final void setOppositeReferenceEndpoint(OppositeReferenceConnection connection, WidgetFactory oppositeReferenceWidgetFactory) {
		this.oppositeReferenceWidgetFactory = oppositeReferenceWidgetFactory;
	}
	
	public WidgetFactory getOppositeReferenceWidgetFactory() {
		return oppositeReferenceWidgetFactory;
	}
	
	public EReferenceNodeProcessor(
			NodeProcessorConfig<WidgetFactory, WidgetFactory> config,
			Context context,
			java.util.function.Function<ProgressMonitor, Action> prototypeProvider) {
		super(config, context, prototypeProvider);
	}	

}
