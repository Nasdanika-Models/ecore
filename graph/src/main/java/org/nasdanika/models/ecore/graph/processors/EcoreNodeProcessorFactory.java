package org.nasdanika.models.ecore.graph.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EParameter;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypeParameter;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.nasdanika.common.Context;
import org.nasdanika.common.Diagnostic;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Reflector;
import org.nasdanika.common.Util;
import org.nasdanika.emf.persistence.MarkerFactory;
import org.nasdanika.exec.content.ContentFactory;
import org.nasdanika.exec.content.Interpolator;
import org.nasdanika.exec.content.Markdown;
import org.nasdanika.exec.content.Text;
import org.nasdanika.graph.Node;
import org.nasdanika.graph.emf.EObjectNode;
import org.nasdanika.graph.processor.NodeProcessorConfig;
import org.nasdanika.graph.processor.emf.EObjectNodeProcessor;
import org.nasdanika.html.model.app.Action;
import org.nasdanika.html.model.app.AppFactory;
import org.nasdanika.html.model.app.Label;
import org.nasdanika.html.model.app.graph.Registry;
import org.nasdanika.html.model.app.graph.WidgetFactory;
import org.nasdanika.html.model.app.graph.emf.EObjectReflectiveProcessorFactory;
import org.nasdanika.html.model.app.util.AppObjectLoaderSupplier;
import org.nasdanika.ncore.ModelElement;
import org.nasdanika.ncore.util.NcoreUtil;
import org.nasdanika.persistence.ObjectLoaderResource;

/**
 * Node processor factory to use with {@link EObjectReflectiveProcessorFactory}.
 * @author Pavel
 *
 */
public class EcoreNodeProcessorFactory extends Reflector {
			
	private Context context;
	private Consumer<Diagnostic> diagnosticConsumer;
	private java.util.function.BiFunction<URI, ProgressMonitor, Action> prototypeProvider;
	
	protected java.util.function.Function<ProgressMonitor, Action> getPrototypeProvider(NodeProcessorConfig<Object, WidgetFactory, WidgetFactory, Registry<URI>> config) {
		return progressMonitor -> {
			if (prototypeProvider != null) {
				for (URI identifier: NcoreUtil.getIdentifiers(((EObjectNode) config.getElement()).getTarget())) {
					Action prototype = prototypeProvider.apply(identifier, progressMonitor);
					if (prototype != null) {
						return prototype;
					}				
				}			
			}
			return AppFactory.eINSTANCE.createAction();
		};		
	}
	
	protected List<AnnotatedElementRecord> annotatedElementRecords = new ArrayList<>();

	/**
	 * 
	 * @param context
	 * @param reflectiveFactories Objects with annotated methods for creating processors. 
	 */
	public EcoreNodeProcessorFactory(
			Context context, 
			java.util.function.BiFunction<URI, ProgressMonitor, Action> prototypeProvider,
			Consumer<Diagnostic> diagnosticConsumer,
			Object... targets)  {
		this.context = context;
		this.prototypeProvider = prototypeProvider;
		this.diagnosticConsumer = diagnosticConsumer;
		for (Object target: targets) {
			getAnnotatedElementRecords(target).forEach(annotatedElementRecords::add);
		}
	}
	
	protected Action loadActionPrototype(
			String spec,
			String specRef, 
			URI base, 
			ProgressMonitor progressMonitor) {

		if (!Util.isBlank(spec) && !Util.isBlank(specRef)) {
			throw new IllegalArgumentException("actionPrototype and actionPrototypeRef are mutually exclusive");
		}
		
		URI specURI;		
		if (Util.isBlank(spec)) {
			if (Util.isBlank(specRef)) {
				return null;
			}
			
			specURI = URI.createURI(specRef);
			if (base != null) {
				specURI = specURI.resolve(base);
			}
		} else {
			specURI = ObjectLoaderResource.encode(spec, "YAML", base, null);
		}
		return (Action) AppObjectLoaderSupplier.loadObject(specURI, diagnosticConsumer, context, progressMonitor);
	}
	
	@Override
	protected Predicate<Object> getTargetPredicate(Object target) {
		EPackageNodeProcessorFactory pnfa = target.getClass().getAnnotation(EPackageNodeProcessorFactory.class);
		EClassifierNodeProcessorFactory cnfa = target.getClass().getAnnotation(EClassifierNodeProcessorFactory.class);		
		return obj -> {
			EObject eObj = ((EObjectNode) obj).getTarget();
			if (pnfa != null) {
				EPackage ePackage = null;
				if (eObj instanceof EPackage) {
					ePackage = (EPackage) eObj;
				} else if (eObj instanceof EClassifier) {
					ePackage = ((EClassifier) eObj).getEPackage();
				} else if (eObj instanceof EStructuralFeature) {
					ePackage = ((EStructuralFeature) eObj).getEContainingClass().getEPackage();
				} else if (eObj instanceof EOperation) {
					ePackage = ((EOperation) eObj).getEContainingClass().getEPackage();
				} else if (eObj instanceof EParameter) {
					ePackage = ((EParameter) eObj).getEOperation().getEContainingClass().getEPackage();
				} else if (eObj instanceof EEnumLiteral) {
					ePackage = ((EEnumLiteral) eObj).getEEnum().getEPackage();					
				}
								
				if (ePackage != null && !pnfa.nsURI().equals(ePackage.getNsURI())) {
					return false;
				}
			}
			
			if (cnfa != null) {
				EClassifier eClassifier = null;
				if (eObj instanceof EClassifier) {
					eClassifier = (EClassifier) eObj;
				} else if (eObj instanceof EStructuralFeature) {
					eClassifier = ((EStructuralFeature) eObj).getEContainingClass();
				} else if (eObj instanceof EOperation) {
					eClassifier = ((EOperation) eObj).getEContainingClass();
				} else if (eObj instanceof EParameter) {
					eClassifier = ((EParameter) eObj).getEOperation().getEContainingClass();
				} else if (eObj instanceof EEnumLiteral) {
					eClassifier = ((EEnumLiteral) eObj).getEEnum();
				}
				
				if (eClassifier != null) {
					if (!Util.isBlank(cnfa.nsURI()) && !cnfa.nsURI().equals(eClassifier.getEPackage().getNsURI())) {
						return false;
					}
					if (cnfa.classifierID() != -1 && cnfa.classifierID() != eClassifier.getClassifierID()) {
						return false;
					}
				}				
			}
			
			return true;			
		};
	}
	
	protected Function<ProgressMonitor, Action> getPrototypeProvider(
			NodeProcessorConfig<Object, WidgetFactory, WidgetFactory, Registry<URI>> config,
			URI baseURI,
			String actionPrototypeSpec,
			String actionPrototypeRef,
			String documentation,
			ProgressMonitor progressMonitor) {
		
		Action actionPrototype = loadActionPrototype(
				actionPrototypeSpec,
				actionPrototypeRef,
				baseURI, 
				progressMonitor);
		
		return pm -> {
			Action ret;
			if (actionPrototype == null) {
				ret = EcoreNodeProcessorFactory.this.getPrototypeProvider(config).apply(pm);
			} else {
				ret = EcoreUtil.copy(actionPrototype);
				ret.setUuid(UUID.randomUUID().toString());
				TreeIterator<EObject> cit = ret.eAllContents();
				while (cit.hasNext()) {
					EObject next = cit.next();
					if (next instanceof ModelElement) {
						((ModelElement) next).setUuid(UUID.randomUUID().toString());
					}
				}
			}
			
			if (!Util.isBlank(documentation)) {
				Markdown markdown = ContentFactory.eINSTANCE.createMarkdown();
				Interpolator interpolator = ContentFactory.eINSTANCE.createInterpolator();
				Text text = ContentFactory.eINSTANCE.createText();
				text.setContent(documentation);
				interpolator.setSource(text);
				markdown.setSource(interpolator);
				markdown.setStyle(true);
				
				// Creating a marker with EObject resource location for resource resolution in Markdown
				if (baseURI != null) {
					org.nasdanika.ncore.Marker marker = context.get(MarkerFactory.class, MarkerFactory.INSTANCE).createMarker(baseURI.toString(), progressMonitor);
					markdown.getMarkers().add(marker); 
				}
				ret.getContent().add(markdown);
			}
			
			return ret;
		};
	}
	
	protected BiConsumer<Label, ProgressMonitor> getLabelConfigurator(
			String label,
			String icon,
			String description,
			ProgressMonitor progressMonitor) {
		
		return (lbl, pm) -> {
			if (!Util.isBlank(label)) {
				lbl.setText(label);
			}
			if (!Util.isBlank(icon)) {
				lbl.setIcon(icon);
			}
			if (!Util.isBlank(description)) {
				lbl.setTooltip(description);
			}					
		};
	}
	
		
	@EObjectNodeProcessor(type = EPackage.class)
	public Object createEPackageNodeProcessor(NodeProcessorConfig<Object, WidgetFactory, WidgetFactory, Registry<URI>> config, ProgressMonitor progressMonitor) {
		Optional<AnnotatedElementRecord> fo = annotatedElementRecords
			.stream()
			.filter(aer -> aer.test(config.getElement()))
			.filter(aer -> {
				EPackageNodeProcessorFactory ann = aer.getAnnotation(EPackageNodeProcessorFactory.class);
				return ann != null && (Util.isBlank(ann.nsURI()) || ann.nsURI().equals(((EPackage) ((EObjectNode) config.getElement()).getTarget()).getNsURI()));
			}).findFirst();
		
		if (fo.isPresent()) {
			AnnotatedElementRecord annotatedElementRecord = fo.get();
			EPackageNodeProcessorFactory ann = annotatedElementRecord.getAnnotation(EPackageNodeProcessorFactory.class);
			
			return annotatedElementRecord.invoke(
					config, 
					getPrototypeProvider(
							config, 
							annotatedElementRecord.getBaseURI(),
							ann.actionPrototype(), 
							ann.actionPrototypeRef(),
							ann.documentation(),
							progressMonitor),
					getLabelConfigurator(
							ann.label(),
							ann.icon(), 
							ann.description(),
							progressMonitor),
					progressMonitor);
		}
		
		return new EPackageNodeProcessor(config, context, getPrototypeProvider(config));
	}	
	
	@EObjectNodeProcessor(type = EClass.class)
	public Object createEClassNodeProcessor(NodeProcessorConfig<Object, WidgetFactory, WidgetFactory, Registry<URI>> config, ProgressMonitor progressMonitor) {
		return createEClassifierNodeProcessor(config, () -> new EClassNodeProcessor(config, context, getPrototypeProvider(config)), progressMonitor);
	}

	protected Object createEClassifierNodeProcessor(
			NodeProcessorConfig<Object, WidgetFactory, WidgetFactory, Registry<URI>> config,
			Supplier<Object> fallback,
			ProgressMonitor progressMonitor) {
		Optional<AnnotatedElementRecord> fo = annotatedElementRecords
				.stream()
				.filter(aer -> aer.test(config.getElement()))
				.filter(aer -> {
					EClassifierNodeProcessorFactory ann = aer.getAnnotation(EClassifierNodeProcessorFactory.class);
					if (ann == null) {
						return false;
					}
					EClassifier target = (EClassifier) ((EObjectNode) config.getElement()).getTarget();
					int id = ann.classifierID();
					if (id != -1 && id != target.getClassifierID()) {
						return false;
					}
					String nsURI = ann.nsURI();
					return Util.isBlank(nsURI) || nsURI.equals(target.getEPackage().getNsURI());
				}).findFirst();
		
		if (fo.isEmpty()) {
			return fallback.get();
		}
		
		AnnotatedElementRecord annotatedElementRecord = fo.get();
		EClassifierNodeProcessorFactory ann = annotatedElementRecord.getAnnotation(EClassifierNodeProcessorFactory.class);
		String label = ann.label();
		Node node = config.getElement();
		if (node instanceof EObjectNode) {
			EObject target = ((EObjectNode) node).getTarget();
			if (target instanceof EClass && ((EClass) target).isAbstract()) {
				label = "<i>" + label + "</i>";
			}
		}
		return annotatedElementRecord.invoke(
				config, 
				getPrototypeProvider(
						config, 
						annotatedElementRecord.getBaseURI(),
						ann.actionPrototype(), 
						ann.actionPrototypeRef(),
						ann.documentation(),
						progressMonitor),
				getLabelConfigurator(
						label,
						ann.icon(), 
						ann.description(),
						progressMonitor),
				progressMonitor);
	}	
	
	@EObjectNodeProcessor(type = EDataType.class)
	public Object createEDataTypeNodeProcessor(NodeProcessorConfig<Object, WidgetFactory, WidgetFactory, Registry<URI>> config, ProgressMonitor progressMonitor) {
		return createEClassifierNodeProcessor(config, () -> new EDataTypeNodeProcessor<EDataType>(config, context, getPrototypeProvider(config)), progressMonitor);
	}
	
	@EObjectNodeProcessor(type = EEnum.class)
	public Object createEEnumNodeProcessor(NodeProcessorConfig<Object, WidgetFactory, WidgetFactory, Registry<URI>> config, ProgressMonitor progressMonitor) {
		return createEClassifierNodeProcessor(config, () -> new EEnumNodeProcessor(config, context, getPrototypeProvider(config)), progressMonitor);		
	}	

	// --- EClass members ---
	
	protected Object createEStructuralFeatureNodeProcessor(
			NodeProcessorConfig<Object, WidgetFactory, WidgetFactory, Registry<URI>> config,
			Supplier<Object> fallback,
			ProgressMonitor progressMonitor) {
		Optional<AnnotatedElementRecord> fo = annotatedElementRecords
				.stream()
				.filter(aer -> aer.test(config.getElement()))
				.filter(aer -> {
					EStructuralFeatureNodeProcessorFactory ann = aer.getAnnotation(EStructuralFeatureNodeProcessorFactory.class);
					if (ann == null) {
						return false;
					}
					EStructuralFeature target = (EStructuralFeature) ((EObjectNode) config.getElement()).getTarget();
					if (target.getFeatureID() != ann.featureID()) {
						return false;
					}
					int classID = ann.classID();
					if (classID != -1  &&  classID != target.getEContainingClass().getClassifierID()) {
						return false;
					}
					
					String nsURI = ann.nsURI();
					return Util.isBlank(nsURI) || nsURI.equals(target.getEContainingClass().getEPackage().getNsURI());
				}).findFirst();
		
		if (fo.isEmpty()) {
			return fallback.get();
		}
		
		AnnotatedElementRecord annotatedElementRecord = fo.get();
		EStructuralFeatureNodeProcessorFactory ann = annotatedElementRecord.getAnnotation(EStructuralFeatureNodeProcessorFactory.class);
		return annotatedElementRecord.invoke(
				config, 
				getPrototypeProvider(
						config, 
						annotatedElementRecord.getBaseURI(),
						ann.actionPrototype(), 
						ann.actionPrototypeRef(),
						ann.documentation(),
						progressMonitor),
				getLabelConfigurator(
						ann.label(),
						ann.icon(), 
						ann.description(),
						progressMonitor),
				progressMonitor);
	}	
		
	@EObjectNodeProcessor(type = EAttribute.class)
	public Object createEAttributeNodeProcessor(NodeProcessorConfig<Object, WidgetFactory, WidgetFactory, Registry<URI>> config, ProgressMonitor progressMonitor) {
		return createEStructuralFeatureNodeProcessor(config, () -> new EAttributeNodeProcessor(config, context, getPrototypeProvider(config)), progressMonitor);		
	}	
	
	@EObjectNodeProcessor(type = EReference.class)
	public Object createEReferenceNodeProcessor(NodeProcessorConfig<Object, WidgetFactory, WidgetFactory, Registry<URI>> config, ProgressMonitor progressMonitor) {
		return createEStructuralFeatureNodeProcessor(config, () -> new EReferenceNodeProcessor(config, context, getPrototypeProvider(config)), progressMonitor);		
	}
	
	@EObjectNodeProcessor(type = EOperation.class)
	public Object createEOperationNodeProcessor(NodeProcessorConfig<Object, WidgetFactory, WidgetFactory, Registry<URI>> config, ProgressMonitor progressMonitor) {
		Optional<AnnotatedElementRecord> fo = annotatedElementRecords
				.stream()
				.filter(aer -> aer.test(config.getElement()))
				.filter(aer -> {
					EOperationNodeProcessorFactory ann = aer.getAnnotation(EOperationNodeProcessorFactory.class);
					if (ann == null) {
						return false;
					}
					EOperation target = (EOperation) ((EObjectNode) config.getElement()).getTarget();
					if (target.getOperationID() != ann.operationID()) {
						return false;
					}
					int classID = ann.classID();
					if (classID != -1 && classID != target.getEContainingClass().getClassifierID()) {
						return false;
					}
					
					String nsURI = ann.nsURI();
					return Util.isBlank(nsURI) || nsURI.equals(target.getEContainingClass().getEPackage().getNsURI());
				}).findFirst();
		
		if (fo.isPresent()) {
			AnnotatedElementRecord annotatedElementRecord = fo.get();
			EOperationNodeProcessorFactory ann = annotatedElementRecord.getAnnotation(EOperationNodeProcessorFactory.class);
			return annotatedElementRecord.invoke(
					config, 
					getPrototypeProvider(
							config, 
							annotatedElementRecord.getBaseURI(),
							ann.actionPrototype(), 
							ann.actionPrototypeRef(),
							ann.documentation(),
							progressMonitor),
					getLabelConfigurator(
							ann.label(),
							ann.icon(), 
							ann.description(),
							progressMonitor),
					progressMonitor);
		}				
		
		return new EOperationNodeProcessor(config, context, getPrototypeProvider(config));
	}	
	
	@EObjectNodeProcessor(type = EParameter.class)
	public Object createEParameterNodeProcessor(NodeProcessorConfig<Object, WidgetFactory, WidgetFactory, Registry<URI>> config, ProgressMonitor progressMonitor) {
		Optional<AnnotatedElementRecord> fo = annotatedElementRecords
				.stream()
				.filter(aer -> aer.test(config.getElement()))
				.filter(aer -> {
					EParameterNodeProcessorFactory ann = aer.getAnnotation(EParameterNodeProcessorFactory.class);
					if (ann == null) {
						return false;
					}					
					EParameter target = (EParameter) ((EObjectNode) config.getElement()).getTarget();
					if (!target.getName().equals(ann.name())) {
						return false;
					}
					if (target.getEOperation().getOperationID() != ann.operationID()) {
						return false;
					}
					int classID = ann.classID();
					if (classID != -1 && classID != target.getEOperation().getEContainingClass().getClassifierID()) {
						return false;
					}
					
					String nsURI = ann.nsURI();
					return Util.isBlank(nsURI) || nsURI.equals(target.getEOperation().getEContainingClass().getEPackage().getNsURI());
				}).findFirst();
		
		if (fo.isPresent()) {
			AnnotatedElementRecord annotatedElementRecord = fo.get();
			EParameterNodeProcessorFactory ann = annotatedElementRecord.getAnnotation(EParameterNodeProcessorFactory.class);
			return annotatedElementRecord.invoke(
					config, 
					getPrototypeProvider(
							config, 
							annotatedElementRecord.getBaseURI(),
							ann.actionPrototype(), 
							ann.actionPrototypeRef(),
							ann.documentation(),
							progressMonitor),
					getLabelConfigurator(
							ann.label(),
							ann.icon(), 
							ann.description(),
							progressMonitor),
					progressMonitor);
		}
		
		return new EParameterNodeProcessor(config, context, getPrototypeProvider(config));
	}	

	@EObjectNodeProcessor(type = EEnumLiteral.class)
	public Object createEEnumLiteralNodeProcessor(NodeProcessorConfig<Object, WidgetFactory, WidgetFactory, Registry<URI>> config, ProgressMonitor progressMonitor) {
		Optional<AnnotatedElementRecord> fo = annotatedElementRecords
				.stream()
				.filter(aer -> aer.test(config.getElement()))
				.filter(aer -> {
					EEnumLiteralNodeProcessorFactory ann = aer.getAnnotation(EEnumLiteralNodeProcessorFactory.class);
					if (ann == null) {
						return false;
					}
					EEnumLiteral target = (EEnumLiteral) ((EObjectNode) config.getElement()).getTarget();
					if (!ann.literal().equals(target.getLiteral())) {
						return false;
					}
					int enumID = ann.enumID();
					if (enumID != -1 && enumID != target.getEEnum().getClassifierID()) {
						return false;
					}
					
					String nsURI = ann.nsURI();
					return Util.isBlank(nsURI) || nsURI.equals(target.getEEnum().getEPackage().getNsURI());
				}).findFirst();
		
		if (fo.isPresent()) {
			AnnotatedElementRecord annotatedElementRecord = fo.get();
			EEnumLiteralNodeProcessorFactory ann = annotatedElementRecord.getAnnotation(EEnumLiteralNodeProcessorFactory.class);
			return annotatedElementRecord.invoke(
					config, 
					getPrototypeProvider(
							config, 
							annotatedElementRecord.getBaseURI(),
							ann.actionPrototype(), 
							ann.actionPrototypeRef(),
							ann.documentation(),
							progressMonitor),
					getLabelConfigurator(
							ann.label(),
							ann.icon(), 
							ann.description(),
							progressMonitor),
					progressMonitor);
		}
		
		return new EEnumLiteralNodeProcessor(config, context, getPrototypeProvider(config));
	}	
	
	@EObjectNodeProcessor(type = ETypeParameter.class)
	public Object createETypeParameterNodeProcessor(NodeProcessorConfig<Object, WidgetFactory, WidgetFactory, Registry<URI>> config, ProgressMonitor progressMonitor) {
		Optional<AnnotatedElementRecord> fo = annotatedElementRecords
				.stream()
				.filter(aer -> aer.test(config.getElement()))
				.filter(aer -> {
					ETypeParameterNodeProcessorFactory ann = aer.getAnnotation(ETypeParameterNodeProcessorFactory.class);
					if (ann == null) {
						return false;
					}					
					ETypeParameter target = (ETypeParameter) ((EObjectNode) config.getElement()).getTarget();
					if (!target.getName().equals(ann.name())) {
						return false;
					}
					if (ann.operationID() == -1) {
						if (!(target.eContainer() instanceof EClassifier)) {
							return false;
						}
					} else {
						if (!(target.eContainer() instanceof EOperation)) {
							return false;
						}
						
						if (((EOperation) target.eContainer()).getOperationID() != ann.operationID()) {
							return false;
						}
					}
					
					int classifierID = ann.classifierID();
					EClassifier eClassifier = target.eContainer() instanceof EOperation ? ((EOperation) target.eContainer()).getEContainingClass() : (EClassifier) target.eContainer();  
					if (classifierID != -1 && classifierID != eClassifier.getClassifierID()) {
						return false;
					}
					
					String nsURI = ann.nsURI();
					return Util.isBlank(nsURI) || nsURI.equals(eClassifier.getEPackage().getNsURI());
				}).findFirst();
		
		if (fo.isPresent()) {
			AnnotatedElementRecord annotatedElementRecord = fo.get();
			ETypeParameterNodeProcessorFactory ann = annotatedElementRecord.getAnnotation(ETypeParameterNodeProcessorFactory.class);
			return annotatedElementRecord.invoke(
					config, 
					getPrototypeProvider(
							config, 
							annotatedElementRecord.getBaseURI(),
							ann.actionPrototype(), 
							ann.actionPrototypeRef(),
							ann.documentation(),
							progressMonitor),
					getLabelConfigurator(
							ann.label(),
							ann.icon(), 
							ann.description(),
							progressMonitor),
					progressMonitor);
		}
		
		return new ETypeParameterNodeProcessor(config, context, getPrototypeProvider(config));
	}	
	
	@EObjectNodeProcessor(type = EGenericType.class)
	public Object createEGenericTypeNodeProcessor(NodeProcessorConfig<Object, WidgetFactory, WidgetFactory, Registry<URI>> config, ProgressMonitor progressMonitor) {
		Optional<AnnotatedElementRecord> fo = annotatedElementRecords
				.stream()
				.filter(aer -> aer.test(config.getElement()))
				.filter(aer -> {
					ETypeParameterBoundNodeProcessorFactory ann = aer.getAnnotation(ETypeParameterBoundNodeProcessorFactory.class);
					if (ann == null) {
						return false;
					}
					
					EGenericType target = (EGenericType) ((EObjectNode) config.getElement()).getTarget();
					EObject targetContainer = target.eContainer();
					if (!(targetContainer instanceof ETypeParameter)) {
						return false;
					}
					int index = ((List<?>) targetContainer.eGet(target.eContainmentFeature())).indexOf(target);					
					if (index == -1 || index != ann.index()) {
						return false;
					}
					ETypeParameter eTypeParameter = (ETypeParameter) targetContainer; 					
					
					if (!eTypeParameter.getName().equals(ann.typeParameterName())) {
						return false;
					}
					EObject typeParameterContainer = eTypeParameter.eContainer();
					if (ann.operationID() == -1) {
						if (!(typeParameterContainer instanceof EClassifier)) {
							return false;
						}
					} else {
						if (!(typeParameterContainer instanceof EOperation)) {
							return false;
						}
						
						if (((EOperation) typeParameterContainer).getOperationID() != ann.operationID()) {
							return false;
						}
					}
					
					int classifierID = ann.classifierID();
					EClassifier eClassifier = typeParameterContainer instanceof EOperation ? ((EOperation) typeParameterContainer).getEContainingClass() : (EClassifier) typeParameterContainer;  
					if (classifierID != -1 && classifierID != eClassifier.getClassifierID()) {
						return false;
					}
					
					String nsURI = ann.nsURI();
					return Util.isBlank(nsURI) || nsURI.equals(eClassifier.getEPackage().getNsURI());
				}).findFirst();
		
		if (fo.isPresent()) {
			AnnotatedElementRecord annotatedElementRecord = fo.get();
			ETypeParameterBoundNodeProcessorFactory ann = annotatedElementRecord.getAnnotation(ETypeParameterBoundNodeProcessorFactory.class);
			return annotatedElementRecord.invoke(
					config, 
					getPrototypeProvider(
							config, 
							annotatedElementRecord.getBaseURI(),
							ann.actionPrototype(), 
							ann.actionPrototypeRef(),
							ann.documentation(),
							progressMonitor),
					getLabelConfigurator(
							ann.label(),
							ann.icon(), 
							ann.description(),
							progressMonitor),
					progressMonitor);
		}
		
		return new EGenericTypeNodeProcessor(config, context, getPrototypeProvider(config));
	}	
	
	// --- TODO ~~~
	
	@EObjectNodeProcessor(type = EAnnotation.class)
	public EAnnotationNodeProcessor createEAnnotationNodeProcessor(NodeProcessorConfig<Object, WidgetFactory, WidgetFactory, Registry<URI>> config, ProgressMonitor progressMonitor) {
		return new EAnnotationNodeProcessor(config, context, getPrototypeProvider(config));
	}	

}
