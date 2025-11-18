package org.nasdanika.models.ecore.graph.processors;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.diagram.plantuml.clazz.DataType;
import org.nasdanika.diagram.plantuml.clazz.DiagramElement;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.models.app.Action;
import org.nasdanika.models.app.Label;
import org.nasdanika.models.app.graph.WidgetFactory;

public class EDataTypeNodeProcessor<T extends EDataType> extends EClassifierNodeProcessor<T> {

	public EDataTypeNodeProcessor(
			NodeProcessorConfig<WidgetFactory, WidgetFactory> config,
			Context context,
			java.util.function.BiFunction<EObject, ProgressMonitor, Action> prototypeProvider) {
		super(config, context, prototypeProvider);
	}	
	
	@Override
	public DataType generateDiagramElement(
			URI base,
			Function<EClassifier, CompletionStage<DiagramElement>> diagramElementProvider,
			ProgressMonitor progressMonitor) {

		DataType dataType = new DataType(getTarget().getName());
	
		Object link = createLink(base, progressMonitor);
		if (link instanceof Label) {
			dataType.setTooltip(((Label) link).getTooltip());
		}
		if (link instanceof org.nasdanika.models.app.Link) {
			dataType.setLocation(((org.nasdanika.models.app.Link) link).getLocation());
		}
		
		return dataType;
	}
	
}
