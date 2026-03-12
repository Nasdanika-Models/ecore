package org.nasdanika.models.ecore.cli;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.nasdanika.capability.CapabilityLoader;
import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.capability.ServiceCapabilityFactory.Requirement;
import org.nasdanika.capability.emf.EPackageResourceSetContributor;
import org.nasdanika.capability.emf.ResourceSetContributor;
import org.nasdanika.cli.CommandGroup;
import org.nasdanika.cli.ParentCommands;
import org.nasdanika.common.Context;
import org.nasdanika.common.DefaultConverter;
import org.nasdanika.common.Diagnostic;
import org.nasdanika.common.EObjectSupplier;
import org.nasdanika.common.MutableContext;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.common.Util;
import org.nasdanika.emf.persistence.MarkerFactory;
import org.nasdanika.exec.content.ContentFactory;
import org.nasdanika.exec.content.Interpolator;
import org.nasdanika.exec.content.Markdown;
import org.nasdanika.exec.content.Text;
import org.nasdanika.models.app.Action;
import org.nasdanika.models.app.AppFactory;
import org.nasdanika.models.app.Label;
import org.nasdanika.models.ecore.graph.processors.EcoreHtmlAppGenerator;
import org.nasdanika.models.ecore.graph.processors.EcoreNodeProcessorFactory;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;


@Command(
		description = {
				"Generates html application model from an ecore model"
		},
		versionProvider = ModuleVersionProvider.class,		
		mixinStandardHelpOptions = true,
		name = "ecore-doc")
@ParentCommands(EObjectSupplier.class)
//@Description()
public class EcoreDocGeneratorCommand extends CommandGroup implements EObjectSupplier<EObject> {
		
	private static final URI README_MD_URI = URI.createURI("readme.md");

	public EcoreDocGeneratorCommand() {
		super();
	}

	public EcoreDocGeneratorCommand(CapabilityLoader capabilityLoader) {
		super(capabilityLoader);
	}
		
	@ParentCommand
	EObjectSupplier<EObject> eObjectSupplier;
	
	@Option(
			names = "--diagram", 
			description = "Diagram file to generate")	
	private File diagramFile;
	
	@Option(
			names = "--doc-stubs", 
			description = "Create documentation stubs")	
	private boolean docStubs;
	
	@Option(
			names = "--doc-dir", 
			description = "Documentation directory")	
	private File docDir;		
	
	@Option(
			names = "--layout-width", 
			description = {
				"Width for auto-layout of the generated diagram",
				"Defaults to ${DEFAULT-VALUE}"
			},
			defaultValue = "1400")
	private double layoutWidth;
	
	@Option(
			names = "--layout-height", 
			description = {
					"Height for auto-layout of the generated diagram",
					"Defaults to ${DEFAULT-VALUE}"
				},
			defaultValue = "800")	
	private double layoutHeight;	

	@Override
	public Collection<EObject> getEObjects(ProgressMonitor progressMonitor) {
		Collection<EObject> ecoreObjects = eObjectSupplier.getEObjects(progressMonitor);
		URIConverter uriConverter = ecoreObjects
			.stream()
			.map(EObject::eResource)
			.filter(Objects::nonNull)
			.map(Resource::getResourceSet)
			.filter(Objects::nonNull)
			.map(ResourceSet::getURIConverter)
			.findAny()
			.orElse(null);
		
		URI baseURI = ecoreObjects
				.stream()
				.filter(EPackage.class::isInstance)
				.map(EPackage.class::cast)
				.map(EPackage::getNsURI)
				.filter(Objects::nonNull)
				.sorted((a,b) -> Integer.compare(a.length(), b.length()))
				.findFirst()
				.map(URI::createURI)
				.orElse(null);		
		
		MutableContext context = Context.EMPTY_CONTEXT.fork();
		Consumer<Diagnostic> diagnosticConsumer = d -> d.dump(System.out, 0);
		List<Function<URI,Action>> actionProviders = new ArrayList<>();		

		if (uriConverter != null && docDir != null && baseURI != null) {
			actionProviders.add(uri -> getAction(uri, baseURI, uriConverter));
		}
		
		EcoreGenProcessorsFactory ecoreGenProcessorFactory = new EcoreGenProcessorsFactory(
				diagramFile == null ? null : URI.createFileURI(diagramFile.getAbsolutePath()),
				uriConverter,
				layoutWidth,
				layoutHeight,
				context);	
		
		EcoreNodeProcessorFactory ecoreNodeProcessorFactory = new EcoreNodeProcessorFactory(
				context, 
				(uri, pm) -> {
					for (Function<URI, Action> ap: actionProviders) {
						Action prototype = ap.apply(uri);
						if (prototype != null) {
							return prototype;
						}
					}
					return null;
				},
				diagnosticConsumer,
				ecoreGenProcessorFactory);
		
		Predicate<ResourceSetContributor> contributorPredicate = contributor -> contributor instanceof EPackageResourceSetContributor;		
		Requirement<Predicate<ResourceSetContributor>, ResourceSetContributor> contributorRequirement = ServiceCapabilityFactory.createRequirement(
				ResourceSetContributor.class, 
				null,
				contributorPredicate);

		Map<EPackage, URI> references = new IdentityHashMap<EPackage, URI>();
		for (CapabilityProvider<Object> resourceSetContributorProvider: capabilityLoader.load(contributorRequirement, progressMonitor)) {
			resourceSetContributorProvider.getPublisher().filter(Objects::nonNull).collectList().block().forEach(contributor -> {
				if (contributor instanceof EPackageResourceSetContributor) {
					EPackageResourceSetContributor ePackageResourceSetContributor = (EPackageResourceSetContributor) contributor;
					URI docURI = ePackageResourceSetContributor.getDocumentationURI();
					if (docURI != null) {
						references.put(ePackageResourceSetContributor.getEPackage(), docURI);
					}
				}
			});
		}
		
		Map<EObject,URI> tmpURIs = new HashMap<>();
		for (EObject eObj: ecoreObjects) {
			tmpURIs.put(eObj, URI.createURI("tmp://" + UUID.randomUUID() + "/" + UUID.randomUUID() + "/"));
		}
				
		Function<? super EObject, URI> uriResolver = eObj -> {
			return tmpURIs.computeIfAbsent(eObj, references::get);
		};
				
		EcoreHtmlAppGenerator eCoreHtmlAppGenerator = new EcoreHtmlAppGenerator(
				ecoreObjects, 
				references.keySet(), // Verify 
				uriResolver,
				ecoreNodeProcessorFactory);
		
		Map<EObject, Collection<Label>> labelMap = eCoreHtmlAppGenerator.generateHtmlAppModel(diagnosticConsumer, progressMonitor);
		return labelMap
				.values()
				.stream()
				.flatMap(Collection::stream)
				.map(EObject.class::cast)
				.toList();
	}
	
	protected Action getAction(URI uri, URI baseURI, URIConverter uriConverter) {
		if (uri == null) {
			return null;
		}
		
		if (uri.equals(baseURI)) {
			return getAction(null, uriConverter);
		}
		
		URI relative = uri.deresolve(baseURI.appendSegment(""), true, true, true);
		return getAction(relative.appendSegment(""), uriConverter);
	}
	
	protected Action getAction(URI relativeDocURI, URIConverter uriConverter) {
		URI docDirURI = URI.createFileURI(docDir.getAbsolutePath()).appendSegment("");
		if (relativeDocURI == null) {
			return getDocAction(README_MD_URI.resolve(docDirURI), uriConverter);
		}
		
		if (relativeDocURI.isRelative()) {
			URI actionBaseURI = relativeDocURI.resolve(docDirURI);
			return getDocAction(README_MD_URI.resolve(actionBaseURI), uriConverter);
		}
		
		return null;		
	} 
	
	protected Action getDocAction(URI docURI, URIConverter uriConverter) {
		if (uriConverter.exists(docURI, null)) {		
			try {
				String documentation = DefaultConverter.INSTANCE.toString(uriConverter.createInputStream(docURI));
				if (Util.isBlank(documentation)) {
					return null;
				}
				Markdown markdown = ContentFactory.eINSTANCE.createMarkdown();
				Interpolator interpolator = ContentFactory.eINSTANCE.createInterpolator();
				Text text = ContentFactory.eINSTANCE.createText();
				text.setContent(documentation);
				interpolator.setSource(text);
				markdown.setSource(interpolator);
				markdown.setStyle(true);
				
				org.nasdanika.ncore.Marker marker = MarkerFactory.INSTANCE.createMarker(docURI.toString(), null);
				markdown.getMarkers().add(marker); 
				Action ret = AppFactory.eINSTANCE.createAction();
				ret.getContent().add(markdown);
				return ret;
			} catch (IOException e) {
				return createErrorAction("Error reading documentation from " + docURI + ": " + e);
			}
		} else if (docStubs) {				
			try (Writer writer = new OutputStreamWriter(uriConverter.createOutputStream(docURI))) {
				writer.write("");
				return null;
			} catch (IOException e) {
				return createErrorAction("Error creating documentation stub at " + docURI + ": " + e);
			}				
		}
		
		return null;
	} 

	protected Action createErrorAction(String errorMessage) {
		Action errorAction = AppFactory.eINSTANCE.createAction();				
		Text text = ContentFactory.eINSTANCE.createText(); // Interpolate with element properties?
		text.setContent("<div class=\"nsd-error\">" + errorMessage +  "</div>");
		errorAction.getContent().add(text);
		return errorAction;
	}
	
}
