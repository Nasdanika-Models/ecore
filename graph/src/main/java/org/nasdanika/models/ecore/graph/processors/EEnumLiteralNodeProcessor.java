package org.nasdanika.models.ecore.graph.processors;

import org.eclipse.emf.ecore.EEnumLiteral;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.graph.WidgetFactory;

public class EEnumLiteralNodeProcessor extends EModelElementNodeProcessor<EEnumLiteral> {

	public EEnumLiteralNodeProcessor(
			NodeProcessorConfig<WidgetFactory, WidgetFactory> config,
			Context context,
			java.util.function.Function<ProgressMonitor, Action> prototypeProvider) {
		super(config, context, prototypeProvider);
	}	

}
