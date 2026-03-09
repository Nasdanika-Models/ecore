package org.nasdanika.models.ecore.cli;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
			
		return CompletableFuture.completedStage(new EcoreDocGeneratorCommand(loader.getCapabilityLoader()));
	}

}
