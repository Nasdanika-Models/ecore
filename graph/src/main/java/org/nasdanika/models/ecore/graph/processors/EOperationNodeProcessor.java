package org.nasdanika.models.ecore.graph.processors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.TreeMap;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.diagram.plantuml.Link;
import org.nasdanika.diagram.plantuml.clazz.Operation;
import org.nasdanika.diagram.plantuml.clazz.Parameter;
import org.nasdanika.graph.emf.EReferenceConnection;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.graph.processor.OutgoingEndpoint;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.AppFactory;
import org.nasdanika.html.model.app.Label;
import org.nasdanika.html.model.app.gen.DynamicTableBuilder;
import org.nasdanika.html.model.app.graph.WidgetFactory;
import org.nasdanika.html.model.app.graph.emf.OutgoingReferenceBuilder;

public class EOperationNodeProcessor extends ETypedElementNodeProcessor<EOperation> {

	public EOperationNodeProcessor(
			NodeProcessorConfig<WidgetFactory, WidgetFactory> config,
			Context context,
			java.util.function.Function<ProgressMonitor, Action> prototypeProvider) {
		super(config, context, prototypeProvider);
	}	
		
	@Override
	protected boolean isCallOutgoingReferenceLabelsSuppliers(EReference eReference) {
		if (eReference == EcorePackage.Literals.EOPERATION__EPARAMETERS) {
			return true;
		}
		if (eReference == EcorePackage.Literals.EOPERATION__ETYPE_PARAMETERS) {
			return true;
		}
		return super.isCallOutgoingReferenceLabelsSuppliers(eReference);
	}	
	
	private WidgetFactory declaringClassWidgetFactory;
	
	@OutgoingEndpoint("reference.name == 'eContainingClass'")
	public final void setDeclaringClassEndpoint(WidgetFactory declaringClassWidgetFactory) {
		this.declaringClassWidgetFactory = declaringClassWidgetFactory;
	}
	
	private Map<EReferenceConnection,WidgetFactory> eParameterWidgetFactories = Collections.synchronizedMap(new LinkedHashMap<>());
	
	@OutgoingEndpoint("reference.name == 'eParameters'")
	public final void setEParameterEndpoint(EReferenceConnection connection, WidgetFactory eParameterWidgetFactory) {
		eParameterWidgetFactories.put(connection, eParameterWidgetFactory);
	}	
	
	public Map<EReferenceConnection, WidgetFactory> getEParameterWidgetFactories() {
		return eParameterWidgetFactories;
	}
	
	private Map<Integer,WidgetFactory> eTypeParameterWidgetFactories = Collections.synchronizedMap(new TreeMap<>());
	
	@OutgoingEndpoint("reference.name == 'eTypeParameters'")
	public final void setETypeParameterEndpoint(EReferenceConnection connection, WidgetFactory eTypeParameterWidgetFactory) {
		eTypeParameterWidgetFactories.put(connection.getIndex(), eTypeParameterWidgetFactory);
	}	
	
	private Map<EReferenceConnection,WidgetFactory> eGenericExceptionWidgetFactories = Collections.synchronizedMap(new LinkedHashMap<>()); // TODO - a record for value with generic type for reified type
	
	@OutgoingEndpoint("reference.name == 'eGenericExceptions'")
	public final void setEGenericExceptionsEndpoint(EReferenceConnection connection, WidgetFactory eGenericExceptionWidgetFactory) {
		eGenericExceptionWidgetFactories.put(connection, eGenericExceptionWidgetFactory);
	}	
	
	/**
	 * Returns attributes action, creates if necessary. Matches by location.
	 * @param parent
	 * @return
	 */
	protected Action getParametersAction(Action parent) {
		Action pAction = (Action) parent;
		return pAction.getNavigation()
			.stream()
			.filter(e -> e instanceof Action && "parameters.html".equals(((Action) e).getLocation()))
			.findFirst()
			.map(Action.class::cast)
			.orElseGet(() -> {
				Action parametersAction = AppFactory.eINSTANCE.createAction();
				parametersAction.setText("Parameters");
				parametersAction.setIcon("https://cdn.jsdelivr.net/gh/Nasdanika-Models/ecore@master/graph/web-resources/icons/EParameter.gif");
				parametersAction.setLocation("parameters.html");
				pAction.getNavigation().add(parametersAction);
				return parametersAction;
			});
	}
	
	@OutgoingReferenceBuilder(EcorePackage.EOPERATION__EPARAMETERS)
	public void buildParametersOutgoingReference(
			EReference eReference,
			List<Entry<EReferenceConnection, WidgetFactory>> referenceOutgoingEndpoints, 
			Collection<Label> labels,
			Map<EReferenceConnection, Collection<Label>> outgoingLabels, 
			ProgressMonitor progressMonitor) {
		
		// Own parameters 
		for (Label tLabel: labels) {
			if (tLabel instanceof Action) {
				Action parametersAction = getParametersAction((Action) tLabel);
				EList<Action> tAnonymous = parametersAction.getAnonymous();
				for (Entry<EReferenceConnection, Collection<Label>> re: outgoingLabels.entrySet()) {
					for (Label childLabel: re.getValue()) {
						if (childLabel instanceof Action && !((Action) childLabel).getContent().isEmpty()) {
							tAnonymous.add((Action) childLabel);
						}
					}
				}
				
				DynamicTableBuilder<Entry<EReferenceConnection, WidgetFactory>> parametersTableBuilder = new DynamicTableBuilder<>("nsd-ecore-doc-table");
				buildTypedElementColumns(parametersTableBuilder, progressMonitor);
				
				org.nasdanika.html.model.html.Tag operationsTable = parametersTableBuilder.build(
						referenceOutgoingEndpoints,  
						"eoperation-parameters", 
						"parameters-table", 
						progressMonitor);
				parametersAction.getContent().add(operationsTable);				
			}
		}
	}	
	
	/**
	 * Returns attributes action, creates if necessary. Matches by location.
	 * @param parent
	 * @return
	 */
	protected Action getTypeParametersAction(Action parent) {
		Action pAction = (Action) parent;
		return pAction.getNavigation()
			.stream()
			.filter(e -> e instanceof Action && "type-parameters.html".equals(((Action) e).getLocation()))
			.findFirst()
			.map(Action.class::cast)
			.orElseGet(() -> {
				Action typeParametersAction = AppFactory.eINSTANCE.createAction();
				typeParametersAction.setText("Type Parameters");
				typeParametersAction.setIcon("https://cdn.jsdelivr.net/gh/Nasdanika-Models/ecore@master/graph/web-resources/icons/ETypeParameter.gif");
				typeParametersAction.setLocation("type-parameters.html");
				pAction.getNavigation().add(typeParametersAction);
				return typeParametersAction;
			});
	}
	
	@OutgoingReferenceBuilder(EcorePackage.EOPERATION__ETYPE_PARAMETERS)
	public void buildETypeParametersOutgoingReference(
			EReference eReference,
			List<Entry<EReferenceConnection, WidgetFactory>> referenceOutgoingEndpoints, 
			Collection<Label> labels,
			Map<EReferenceConnection, Collection<Label>> outgoingLabels, 
			ProgressMonitor progressMonitor) {
		
		for (Label tLabel: labels) {
			if (tLabel instanceof Action) {
				Action typeParametersAction = getTypeParametersAction((Action) tLabel);
				EList<Action> tAnonymous = typeParametersAction.getAnonymous();
				for (Entry<EReferenceConnection, Collection<Label>> re: outgoingLabels.entrySet()) {
					for (Label childLabel: re.getValue()) {
						if (childLabel instanceof Action && !((Action) childLabel).getContent().isEmpty()) {
							tAnonymous.add((Action) childLabel);
						}
					}
				}
				
				DynamicTableBuilder<Entry<EReferenceConnection, WidgetFactory>> typeParametersTableBuilder = new DynamicTableBuilder<>("nsd-ecore-doc-table");
				buildNamedElementColumns(typeParametersTableBuilder, progressMonitor);
				// TODO - bounds
				
				org.nasdanika.html.model.html.Tag operationsTable = typeParametersTableBuilder.build(
						referenceOutgoingEndpoints,  
						"eoperation-type-parameters", 
						"type-parameters-table", 
						progressMonitor);
				typeParametersAction.getContent().add(operationsTable);
			}
		}
	}					
		
	@Override
	public Object select(Object selector, URI base, ProgressMonitor progressMonitor) {
		if (selector == EcorePackage.Literals.EOPERATION__ECONTAINING_CLASS && declaringClassWidgetFactory != null) {
			return declaringClassWidgetFactory.createLink(base, progressMonitor);
		}
		if (selector instanceof EClassNodeProcessor.ReifiedTypeSelector) {
			EClassNodeProcessor.ReifiedTypeSelector reifiedTypeSelector = (EClassNodeProcessor.ReifiedTypeSelector) selector;
			if (reifiedTypeSelector.getSelector() == EcorePackage.Literals.EOPERATION__EPARAMETERS) {
				if (eParameterWidgetFactories.isEmpty()) {
					return null;
				}
				
				if (eParameterWidgetFactories.size() == 1) {
					List<Object> ret = new ArrayList<>();
					WidgetFactory pwf = eParameterWidgetFactories.values().iterator().next();
					ret.add(pwf.createLink(base, progressMonitor));
					ret.add(" : ");
					ret.add(pwf.select(reifiedTypeSelector.createSelector(EcorePackage.Literals.ETYPED_ELEMENT__EGENERIC_TYPE), base, progressMonitor));
					return ret;
				}
				
				List<Object> ret = new ArrayList<>();
				ret.add("<ol>");
				for (WidgetFactory pwf: eParameterWidgetFactories.entrySet().stream().sorted((a,b) -> a.getKey().getIndex() - b.getKey().getIndex()).map(Map.Entry::getValue).toList()) {
					ret.add("<li>");
					ret.add(pwf.createLink(base, progressMonitor));
					ret.add(" : ");
					ret.add(pwf.select(reifiedTypeSelector.createSelector(EcorePackage.Literals.ETYPED_ELEMENT__EGENERIC_TYPE), base, progressMonitor));
					ret.add("</li>");
				}						
				ret.add("</ol>");
				
				return ret;			
			}		
			if (reifiedTypeSelector.getSelector() == EcorePackage.Literals.EOPERATION__EGENERIC_EXCEPTIONS) {
				if (eGenericExceptionWidgetFactories.isEmpty()) {
					return null;
				}
				
				if (eGenericExceptionWidgetFactories.size() == 1) {
					return eGenericExceptionWidgetFactories.values().iterator().next().createLink(base, progressMonitor); // TODO - reifiedType
				}
				
				List<Object> ret = new ArrayList<>();
				ret.add("<ol>");
				for (WidgetFactory gewf: eGenericExceptionWidgetFactories.entrySet().stream().sorted((a,b) -> a.getKey().getIndex() - b.getKey().getIndex()).map(Map.Entry::getValue).toList()) {
					ret.add("<li>");
					ret.add(gewf.createLink(base, progressMonitor)); // TODO - reifiedType
					ret.add("</li>");
				}						
				ret.add("</ol>");
				
				return ret;			
			}
			if (reifiedTypeSelector.getSelector() == EcorePackage.Literals.EOPERATION__ETYPE_PARAMETERS) {
				if (eTypeParameterWidgetFactories.isEmpty()) {
					return null;
				}
				
				if (eTypeParameterWidgetFactories.size() == 1) {
					return eTypeParameterWidgetFactories.get(0).createLink(base, progressMonitor);
				}
				
				List<Object> ret = new ArrayList<>();
				ret.add("<ol>");
				for (WidgetFactory tpwf: eTypeParameterWidgetFactories.values()) {
					ret.add("<li>");
					ret.add(tpwf.createLink(base, progressMonitor));
					ret.add("</li>");
				}						
				ret.add("</ol>");
				
				return ret;			
			}
		}
		
		return super.select(selector, base, progressMonitor);
	}
	
	public Operation generateOperation(URI base, ProgressMonitor progressMonitor) {
		Operation operation = new Operation();
		operation.getName().add(new Link(getTarget().getName()));
		
		if (genericTypeWidgetFactory != null) {
			Selector<List<Link>> linkSelector = (widgetFactory, sBase, pm) -> {
				return ((EGenericTypeNodeProcessor) widgetFactory).generateDiagramLink(sBase, pm);
			};
			
			List<Link> typeLink = genericTypeWidgetFactory.select(linkSelector, base, progressMonitor);
			if (typeLink != null && !typeLink.isEmpty()) {
				operation.getType().addAll(typeLink);
				String memberCardinality = getMemberMultiplicity();
				if (memberCardinality != null) {
					operation.getType().add(new Link(memberCardinality));
				}
			}
		}
		
		Object link = createLink(base, progressMonitor);
		if (link instanceof Label) {
			operation.setTooltip(((Label) link).getTooltip());
		}
		if (link instanceof org.nasdanika.html.model.app.Link) {
			operation.setLocation(((org.nasdanika.html.model.app.Link) link).getLocation());
		}
				
		Selector<Parameter> parameterSelector = (widgetFactory, sBase, pm) -> {
			return ((EParameterNodeProcessor) widgetFactory).generateParameter(sBase, progressMonitor);
		};		
		
		for (WidgetFactory pwf: eParameterWidgetFactories.entrySet().stream().sorted((a,b) -> a.getKey().getIndex() - b.getKey().getIndex()).map(Map.Entry::getValue).toList()) {
			Parameter param = pwf.select(parameterSelector, base, progressMonitor);
			operation.getParameters().add(param);			
		}						
		
		return operation;
	}		
	
	@Override
	public Collection<EClassifierNodeProcessor<?>> getEClassifierNodeProcessors(int depth, Predicate<WidgetFactory> predicate, ProgressMonitor progressMonitor) {
		Collection<EClassifierNodeProcessor<?>> ret = super.getEClassifierNodeProcessors(depth, predicate, progressMonitor);
		Selector<Collection<EClassifierNodeProcessor<?>>> selector = EClassifierNodeProcessorProvider.createEClassifierNodeProcessorSelector(depth, predicate);
		// parameters
		for (WidgetFactory pwf: eParameterWidgetFactories.values()) {
			ret.addAll(pwf.select(selector, progressMonitor));
		}
		
		// type parameters
		for (WidgetFactory tpwf: eTypeParameterWidgetFactories.values()) {
			ret.addAll(tpwf.select(selector, progressMonitor));
		}
		
		// exceptions
		for (WidgetFactory gewf: eGenericExceptionWidgetFactories.values()) {
			ret.addAll(gewf.select(selector, progressMonitor));
		}

		return ret;
	}
	
}
