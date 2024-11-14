package org.nasdanika.models.ecore.graph.processors;

import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.diagram.plantuml.Link;
import org.nasdanika.diagram.plantuml.clazz.Attribute;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.graph.processor.OutgoingEndpoint;
import org.nasdanika.models.app.Action;
import org.nasdanika.models.app.Label;
import org.nasdanika.models.app.graph.WidgetFactory;

public abstract class EStructuralFeatureNodeProcessor<T extends EStructuralFeature> extends ETypedElementNodeProcessor<T> {

	public EStructuralFeatureNodeProcessor(
			NodeProcessorConfig<WidgetFactory, WidgetFactory> config,
			Context context,
			java.util.function.Function<ProgressMonitor, Action> prototypeProvider) {
		super(config, context, prototypeProvider);
	}	
	
	private WidgetFactory declaringClassWidgetFactory;
	
	@OutgoingEndpoint("reference.name == 'eContainingClass'")
	public final void setDeclaringClassEndpoint(WidgetFactory declaringClassWidgetFactory) {
		this.declaringClassWidgetFactory = declaringClassWidgetFactory;
	}
	
	public WidgetFactory getDeclaringClassWidgetFactory() {
		return declaringClassWidgetFactory;
	}
	
	@Override
	public Object select(Object selector, URI base, ProgressMonitor progressMonitor) {
		if (selector == EcorePackage.Literals.ESTRUCTURAL_FEATURE__ECONTAINING_CLASS && declaringClassWidgetFactory != null) {
			return declaringClassWidgetFactory.createLink(base, progressMonitor);
		}
		return super.select(selector, base, progressMonitor);
	}
	
	public Attribute generateMember(URI base, ProgressMonitor progressMonitor) {
		Attribute attribute = new Attribute();
		attribute.getName().add(new Link(getTarget().getName()));
		
		if (genericTypeWidgetFactory != null) {
			Selector<List<Link>> linkSelector = (widgetFactory, sBase, pm) -> {
				return ((EGenericTypeNodeProcessor) widgetFactory).generateDiagramLink(sBase, pm);
			};
			
			List<Link> typeLink = genericTypeWidgetFactory.select(linkSelector, base, progressMonitor);
			if (typeLink != null && !typeLink.isEmpty()) {
				attribute.getType().addAll(typeLink);
				String memberCardinality = getMemberMultiplicity();
				if (memberCardinality != null) {
					attribute.getType().add(new Link(memberCardinality));
				}
			}
		}
		
		Object link = createLink(base, progressMonitor);
		if (link instanceof Label) {
			attribute.setTooltip(((Label) link).getTooltip());
		}
		if (link instanceof org.nasdanika.models.app.Link) {
			attribute.setLocation(((org.nasdanika.models.app.Link) link).getLocation());
		}
		return attribute;
	}	
	
}
