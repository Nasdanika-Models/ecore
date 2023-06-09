package org.nasdanika.models.ecore.graph.processors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.TreeMap;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.nasdanika.common.Context;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.graph.emf.EReferenceConnection;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.graph.processor.OutgoingEndpoint;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.AppFactory;
import org.nasdanika.html.model.app.Label;
import org.nasdanika.html.model.app.gen.DynamicTableBuilder;
import org.nasdanika.html.model.app.graph.Registry;
import org.nasdanika.html.model.app.graph.WidgetFactory;
import org.nasdanika.html.model.app.graph.emf.OutgoingReferenceBuilder;

public class EOperationNodeProcessor extends ETypedElementNodeProcessor<EOperation> {

	public EOperationNodeProcessor(
			NodeProcessorConfig<Object, WidgetFactory, WidgetFactory, Registry<URI>> config,
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
	
	private Map<EReferenceConnection,WidgetFactory> eParameterWidgetFactories = new LinkedHashMap<>();
	
	@OutgoingEndpoint("reference.name == 'eParameters'")
	public final void setEParameterEndpoint(EReferenceConnection connection, WidgetFactory eParameterWidgetFactory) {
		eParameterWidgetFactories.put(connection, eParameterWidgetFactory);
	}	
	
	public Map<EReferenceConnection, WidgetFactory> getEParameterWidgetFactories() {
		return eParameterWidgetFactories;
	}
	
	private Map<Integer,WidgetFactory> eTypeParametersWidgetFactories = new TreeMap<>();
	
	@OutgoingEndpoint("reference.name == 'eTypeParameters'")
	public final void setETypeParameterEndpoint(EReferenceConnection connection, WidgetFactory eTypeParameterWidgetFactory) {
		eTypeParametersWidgetFactories.put(connection.getIndex(), eTypeParameterWidgetFactory);
	}	
	
	private Map<EReferenceConnection,WidgetFactory> eGenericExceptionsWidgetFactories = new LinkedHashMap<>(); // TODO - a record for value with generic type for reified type
	
	@OutgoingEndpoint("reference.name == 'eGenericExceptions'")
	public final void setEGenericExceptionsEndpoint(EReferenceConnection connection, WidgetFactory eGenericExceptionWidgetFactory) {
		eGenericExceptionsWidgetFactories.put(connection, eGenericExceptionWidgetFactory);
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
	public Object createWidget(Object selector, URI base, ProgressMonitor progressMonitor) {
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
					ret.add(pwf.createWidget(reifiedTypeSelector.createSelector(EcorePackage.Literals.ETYPED_ELEMENT__EGENERIC_TYPE), base, progressMonitor));
					return ret;
				}
				
				List<Object> ret = new ArrayList<>();
				ret.add("<ol>");
				for (WidgetFactory pwf: eParameterWidgetFactories.entrySet().stream().sorted((a,b) -> a.getKey().getIndex() - b.getKey().getIndex()).map(Map.Entry::getValue).collect(Collectors.toList())) {
					ret.add("<li>");
					ret.add(pwf.createLink(base, progressMonitor));
					ret.add(" : ");
					ret.add(pwf.createWidget(reifiedTypeSelector.createSelector(EcorePackage.Literals.ETYPED_ELEMENT__EGENERIC_TYPE), base, progressMonitor));
					ret.add("</li>");
				}						
				ret.add("</ol>");
				
				return ret;			
			}		
			if (reifiedTypeSelector.getSelector() == EcorePackage.Literals.EOPERATION__EGENERIC_EXCEPTIONS) {
				if (eGenericExceptionsWidgetFactories.isEmpty()) {
					return null;
				}
				
				if (eGenericExceptionsWidgetFactories.size() == 1) {
					return eGenericExceptionsWidgetFactories.values().iterator().next().createLink(base, progressMonitor); // TODO - reifiedType
				}
				
				List<Object> ret = new ArrayList<>();
				ret.add("<ol>");
				for (WidgetFactory gewf: eGenericExceptionsWidgetFactories.entrySet().stream().sorted((a,b) -> a.getKey().getIndex() - b.getKey().getIndex()).map(Map.Entry::getValue).collect(Collectors.toList())) {
					ret.add("<li>");
					ret.add(gewf.createLink(base, progressMonitor)); // TODO - reifiedType
					ret.add("</li>");
				}						
				ret.add("</ol>");
				
				return ret;			
			}
			if (reifiedTypeSelector.getSelector() == EcorePackage.Literals.EOPERATION__ETYPE_PARAMETERS) {
				if (eTypeParametersWidgetFactories.isEmpty()) {
					return null;
				}
				
				if (eTypeParametersWidgetFactories.size() == 1) {
					return eTypeParametersWidgetFactories.get(0).createLink(base, progressMonitor);
				}
				
				List<Object> ret = new ArrayList<>();
				ret.add("<ol>");
				for (WidgetFactory tpwf: eTypeParametersWidgetFactories.values()) {
					ret.add("<li>");
					ret.add(tpwf.createLink(base, progressMonitor));
					ret.add("</li>");
				}						
				ret.add("</ol>");
				
				return ret;			
			}
		}
		
		return super.createWidget(selector, base, progressMonitor);
	}

}
