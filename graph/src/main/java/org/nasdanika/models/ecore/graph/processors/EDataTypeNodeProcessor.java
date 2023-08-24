package org.nasdanika.models.ecore.graph.processors;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EModelElement;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.diagram.plantuml.clazz.DataType;
import org.nasdanika.diagram.plantuml.clazz.DiagramElement;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.graph.WidgetFactory;

public class EDataTypeNodeProcessor<T extends EDataType> extends EClassifierNodeProcessor<T> {

	public EDataTypeNodeProcessor(
			NodeProcessorConfig<WidgetFactory, WidgetFactory> config,
			Context context,
			java.util.function.Function<ProgressMonitor, Action> prototypeProvider) {
		super(config, context, prototypeProvider);
	}	
	
	@Override
	public DataType generateDiagramElement(
			URI base,
			Function<EModelElement, CompletionStage<DiagramElement>> diagramElementProvider,
			ProgressMonitor progressMonitor) {
		throw new UnsupportedOperationException();
	}

}
