package org.nasdanika.models.ecore.graph.processors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.EModelElement;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.diagram.plantuml.clazz.DiagramElement;
import org.nasdanika.graph.emf.EReferenceConnection;
import org.nasdanika.graph.processor.IncomingEndpoint;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.graph.processor.OutgoingEndpoint;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.Label;
import org.nasdanika.html.model.app.graph.WidgetFactory;
import org.nasdanika.html.model.app.graph.emf.EObjectNodeProcessor;

public class EGenericTypeNodeProcessor extends EObjectNodeProcessor<EGenericType> {
	
	public record SubtypeSelector(EClass subType) {};

	public EGenericTypeNodeProcessor(
			NodeProcessorConfig<WidgetFactory, WidgetFactory> config,
			Context context,
			java.util.function.Function<ProgressMonitor, Action> prototypeProvider) {
		super(config, context, prototypeProvider);
	}
	
	//	getEClassifier()
	private WidgetFactory eClassifierWidgetFactory;
	
	@OutgoingEndpoint("reference.name == 'eClassifier'")
	public final void setEClassifierEndpoint(WidgetFactory eClassifierWidgetFactory) {
		this.eClassifierWidgetFactory = eClassifierWidgetFactory;
	}
	
	//	getETypeParameter()
	private WidgetFactory eTypeParameterWidgetFactory;
	
	@OutgoingEndpoint("reference.name == 'eTypeParameter'")
	public final void setETypeParameterEndpoint(WidgetFactory eTypeParameterWidgetFactory) {
		this.eTypeParameterWidgetFactory = eTypeParameterWidgetFactory;
	}
	
	// getELowerBound()
	private WidgetFactory eLowerBoundWidgetFactory;
	
	@OutgoingEndpoint("reference.name == 'eLowerBound'")
	public final void setELowerBoundEndpoint(WidgetFactory eLowerBoundWidgetFactory) {
		this.eLowerBoundWidgetFactory = eLowerBoundWidgetFactory;
	}
	
	// getETypeArguments()
	private Map<Integer,WidgetFactory> eTypeArgumentWidgetFactories = new TreeMap<>();
	
	@OutgoingEndpoint("reference.name == 'eTypeArguments'")
	public final void setETypeArgumentEndpoint(EReferenceConnection connection, WidgetFactory eTypeArgumentWidgetFactory) {
		eTypeArgumentWidgetFactories.put(connection.getIndex(), eTypeArgumentWidgetFactory);
	}
	
	// getEUpperBound()
	private WidgetFactory eUpperBoundWidgetFactory;
	
	@OutgoingEndpoint("reference.name == 'eUpperBound'")
	public final void setEUpperBoundEndpoint(WidgetFactory eUpperBoundWidgetFactory) {
		this.eUpperBoundWidgetFactory = eUpperBoundWidgetFactory;
	}
	
	// EClass.getEGenericSupertypes()
	private Map<EClass,WidgetFactory> subTypeWidgetFactories = Collections.synchronizedMap(new HashMap<>());
	
	@IncomingEndpoint("reference.name == 'eAllGenericSuperTypes'")
	public final void setEAllGenericSuperTypesIncomingEndpoint(EReferenceConnection connection, WidgetFactory subtypeWidgetFactory) {
		subTypeWidgetFactories.put((EClass) connection.getSource().get(), subtypeWidgetFactory);
	}	
	
	public Map<EClass, WidgetFactory> getSubTypeWidgetFactories() {
		return subTypeWidgetFactories;
	}

	@Override
	public Object createLink(URI base, ProgressMonitor progressMonitor) {
		List<Object> ret = new ArrayList<>();
				
		if (eTypeParameterWidgetFactory != null) {
			ret.add(eTypeParameterWidgetFactory.createLink(base, progressMonitor));
		} else if (eClassifierWidgetFactory != null) {
			ret.add(eClassifierWidgetFactory.createLink(base, progressMonitor));
			ret.addAll(genericTypeArguments(base, progressMonitor));
		} else {
			ret.add("?");
			if (eLowerBoundWidgetFactory != null) {
				ret.add(" super ");
				ret.add(eLowerBoundWidgetFactory.createLink(base, progressMonitor));
			} else if (eUpperBoundWidgetFactory != null) {
				ret.add(" extends ");
				ret.add(eUpperBoundWidgetFactory.createLink(base, progressMonitor));
			}
		}
		return ret;
	}
	
	protected List<Object> genericTypeArguments(URI base, ProgressMonitor progressMonitor) {
		List<Object> ret = new ArrayList<>();
		Iterator<WidgetFactory> it = eTypeArgumentWidgetFactories.values().iterator();
		if (it.hasNext()) {
			ret.add("<");
			while (it.hasNext()) {
				WidgetFactory typeArgumentWidgetFactory = it.next();
				ret.add(typeArgumentWidgetFactory.createLink(base, progressMonitor));
				if (it.hasNext()) {
					ret.add(",");
				}
			}
			ret.add(">");
		}
		return ret;
	}
	
	// --- Diagram generation methods ---
	public List<org.nasdanika.diagram.plantuml.Link> generateDiagramLink(URI base, ProgressMonitor progressMonitor) {
		return generateDiagramLink(createLink(base, progressMonitor), base, progressMonitor);
	}	
	
	protected List<org.nasdanika.diagram.plantuml.Link> generateDiagramLink(Object link, URI base, ProgressMonitor progressMonitor) {
		List<org.nasdanika.diagram.plantuml.Link> ret = new ArrayList<>();
		if (link instanceof Label) {
			Label label = (Label) link;
			org.nasdanika.diagram.plantuml.Link dLink = new org.nasdanika.diagram.plantuml.Link(label.getText());
			dLink.setTooltip(label.getTooltip());
			if (label instanceof org.nasdanika.html.model.app.Link) {
				dLink.setLocation(((org.nasdanika.html.model.app.Link) label).getLocation());
			}
			ret.add(dLink);
		} else if (link instanceof Collection) {
			for (Object le: (Collection<?>) link) {
				ret.addAll(generateDiagramLink(le, base, progressMonitor));
			}
		} else if (link != null) {
			ret.add(new org.nasdanika.diagram.plantuml.Link(link.toString()));
		}
		return ret;
	}
	
	public DiagramElement generateEClassifierDiagramElement(
			URI base, 
			Function<EModelElement, CompletionStage<DiagramElement>> diagramElementProvider,
			ProgressMonitor progressMonitor) {		
		
		Selector<DiagramElement> diagramElementSelector = (widgetFactory, sBase, pm) -> {
			return ((EClassifierNodeProcessor<?>) widgetFactory).generateDiagramElement(sBase, diagramElementProvider, pm);
		};
		
		return eClassifierWidgetFactory.createWidget(diagramElementSelector, base, progressMonitor);
	}
	
}
