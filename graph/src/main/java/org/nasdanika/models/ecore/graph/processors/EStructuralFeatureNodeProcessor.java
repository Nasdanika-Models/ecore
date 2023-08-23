package org.nasdanika.models.ecore.graph.processors;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.graph.processor.OutgoingEndpoint;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.graph.WidgetFactory;

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
	public Object createWidget(Object selector, URI base, ProgressMonitor progressMonitor) {
		if (selector == EcorePackage.Literals.ESTRUCTURAL_FEATURE__ECONTAINING_CLASS && declaringClassWidgetFactory != null) {
			return declaringClassWidgetFactory.createLink(base, progressMonitor);
		}
		return super.createWidget(selector, base, progressMonitor);
	}
	
	public abstract org.nasdanika.diagram.plantuml.clazz.Member generateMember(URI base, ProgressMonitor progressMonitor); 	
	
}
