package org.nasdanika.models.ecore.graph.processors;

import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EcorePackage;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.diagram.plantuml.Link;
import org.nasdanika.diagram.plantuml.clazz.Parameter;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.graph.processor.OutgoingEndpoint;
import org.nasdanika.models.app.Action;
import org.nasdanika.models.app.Label;
import org.nasdanika.models.app.graph.WidgetFactory;

public class EParameterNodeProcessor extends ETypedElementNodeProcessor<EParameter> {

	public EParameterNodeProcessor(
			NodeProcessorConfig<WidgetFactory, WidgetFactory> config,
			Context context,
			java.util.function.Function<ProgressMonitor, Action> prototypeProvider) {
		super(config, context, prototypeProvider);
	}	
	
	private WidgetFactory operationWidgetFactory;
	
	@OutgoingEndpoint("reference.name == 'eOperation'")
	public final void setDeclaringClassEndpoint(WidgetFactory operationWidgetFactory) {
		this.operationWidgetFactory = operationWidgetFactory;
	}
	
	@Override
	public Object select(Object selector, URI base, ProgressMonitor progressMonitor) {
		if (selector == EcorePackage.Literals.EPARAMETER__EOPERATION && operationWidgetFactory != null) {
			return operationWidgetFactory.createLink(base, progressMonitor);
		}
		return super.select(selector, base, progressMonitor);
	}	
	
	public Parameter generateParameter(URI base, ProgressMonitor progressMonitor) {
		Parameter parameter = new Parameter();
		parameter.getName().add(new Link(getTarget().getName()));
		
		if (genericTypeWidgetFactory != null) {
			Selector<List<Link>> linkSelector = (widgetFactory, sBase, pm) -> {
				return ((EGenericTypeNodeProcessor) widgetFactory).generateDiagramLink(sBase, pm);
			};
			
			List<Link> typeLink = genericTypeWidgetFactory.select(linkSelector, base, progressMonitor);
			if (typeLink != null && !typeLink.isEmpty()) {
				parameter.getType().addAll(typeLink);
				String memberCardinality = getMemberMultiplicity();
				if (memberCardinality != null) {
					parameter.getType().add(new Link(memberCardinality));
				}
			}
		}
		
		Object link = createLink(base, progressMonitor);
		if (link instanceof Label) {
			parameter.setTooltip(((Label) link).getTooltip());
		}
		if (link instanceof org.nasdanika.models.app.Link) {
			parameter.setLocation(((org.nasdanika.models.app.Link) link).getLocation());
		}
		return parameter;
	}		
	
}
