package org.nasdanika.models.ecore.cli;

import java.util.List;
import java.util.concurrent.CompletionStage;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.capability.emf.ResourceSetRequirement;
import org.nasdanika.cli.SubCommandCapabilityFactory;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.models.app.util.LabelSupplier;

import picocli.CommandLine;

public class EcoreDocGeneratorCommandFactory extends SubCommandCapabilityFactory<EcoreDocGeneratorCommand> {

	@Override
	protected Class<EcoreDocGeneratorCommand> getCommandType() {
		return EcoreDocGeneratorCommand.class;
	}
	
	@Override
	protected CompletionStage<EcoreDocGeneratorCommand> doCreateCommand(
			List<CommandLine> parentPath,
			Loader loader,
			ProgressMonitor progressMonitor) {
		
		// Do not bind to LabelSuppliers or self - would be an infinite loop
		if (!parentPath.isEmpty()) {
			CommandLine lastCommand = parentPath.get(parentPath.size() - 1);
			Object userObject = lastCommand.getCommandSpec().userObject();
			if (userObject instanceof LabelSupplier || userObject instanceof EcoreDocGeneratorCommand) {
				return null;
			}
		}
		
		Requirement<ResourceSetRequirement, ResourceSet> resourceSetRequirement = ServiceCapabilityFactory.createRequirement(ResourceSet.class);		
		CompletionStage<ResourceSet> resourceSetCS = loader.loadOne(resourceSetRequirement, progressMonitor);		
		return resourceSetCS.thenApply(rs -> new EcoreDocGeneratorCommand(loader.getCapabilityLoader(), rs)); 		
	}

}
