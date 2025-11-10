/**
 * Copyright (c) 2012 - 2025 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package org.gecko.emf.sensinact.model.impl;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.sensinact.core.emf.model.EMFModel;
import org.eclipse.sensinact.core.emf.model.SensinactEMFModelManager;
import org.eclipse.sensinact.core.emf.twin.SensinactEMFDigitalTwin;
import org.eclipse.sensinact.core.model.Model;
import org.eclipse.sensinact.core.model.Resource;
import org.eclipse.sensinact.core.model.ResourceBuilder;
import org.eclipse.sensinact.core.model.ResourceType;
import org.eclipse.sensinact.core.model.Service;
import org.eclipse.sensinact.core.twin.SensinactProvider;
import org.eclipse.sensinact.mapping.AdminMapping;
import org.eclipse.sensinact.mapping.Mapping;
import org.eclipse.sensinact.mapping.MappingProfile;
import org.eclipse.sensinact.mapping.ProviderMapping;
import org.eclipse.sensinact.mapping.ProviderStrategy;
import org.eclipse.sensinact.mapping.ResourceMapping;
import org.eclipse.sensinact.mapping.ServiceMapping;
import org.gecko.emf.sensinact.model.MappingProfileRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This mapper translates the {@link Mapping} instances into the sensinact core model
 * @author Mark Hoffmann
 * @since 16.07.2025
 */
public class ProviderModelSensinactMapper {

	private static final Logger logger = LoggerFactory.getLogger(ProviderModelSensinactMapper.class);
	private final SensinactEMFModelManager modelManager;
	private final SensinactEMFDigitalTwin twin;
	private final MappingProfileRegistry profileRegistry;

	/**
	 * Factory to create mapping instance
	 */
	public static class Factory {

		private MappingProfileRegistry profileRegistry;

		/**
		 * Creates a new instance.
		 */
		public Factory(final MappingProfileRegistry profileRegistry) {
			this.profileRegistry = profileRegistry;
		}

		public ProviderModelSensinactMapper createMapper(final SensinactEMFDigitalTwin twin, final SensinactEMFModelManager modelManager) {
			return new ProviderModelSensinactMapper(twin, modelManager, profileRegistry);
		}
	}

	private ProviderModelSensinactMapper(final SensinactEMFDigitalTwin twin, final SensinactEMFModelManager modelManager, 
			final MappingProfileRegistry profileRegistry) {
		this.twin = twin;
		this.modelManager = modelManager;
		this.profileRegistry = profileRegistry;
	}

	/**
	 * Registers a {@link ProviderMapping} into sensinact
	 * @param providerMapping the {@link ProviderMapping}
	 */
	public void registerModelMapping(ProviderMapping providerMapping) {
		if (isNull(providerMapping)) {
			return;
		}

		// Validate against profile if one is referenced
		if (nonNull(providerMapping.getProfile()) && nonNull(profileRegistry)) {
			MappingProfileRegistry.ValidationResult validationResult = profileRegistry.validateMapping(providerMapping);
			if (!validationResult.isValid()) {
				StringBuilder errorMessage = new StringBuilder("Mapping validation failed for provider '")
						.append(providerMapping.getName() != null ? providerMapping.getName().getName() : providerMapping.getMid())
						.append("':\n");

				for (String error : validationResult.getErrors()) {
					errorMessage.append("  - ").append(error).append("\n");
				}

				if (!validationResult.getWarnings().isEmpty()) {
					errorMessage.append("Warnings:\n");
					for (String warning : validationResult.getWarnings()) {
						errorMessage.append("  - ").append(warning).append("\n");
					}
				}

				throw new IllegalArgumentException(errorMessage.toString());
			}

			// Log warnings if any
			if (!validationResult.getWarnings().isEmpty()) {
				String providerName = providerMapping.getName() != null ? providerMapping.getName().getName() : providerMapping.getMid();
				logger.warn("Mapping validation warnings for provider '{}':", providerName);
				for (String warning : validationResult.getWarnings()) {
					logger.warn("  - {}", warning);
				}
			}
		}

		mapProvider(providerMapping);

		MappingProfile profile = providerMapping.getProfile();
		String actualProviderId = determineProviderId(providerMapping);
		String profileInfo = profile != null ? 
				" (using profile: " + profile.getProfileId() + " v" + profile.getVersion() + 
				", strategy: " + profile.getProviderStrategy() + ")" : "";

		String providerName = providerMapping.getName() != null ? providerMapping.getName().getName() : providerMapping.getMid();
		logger.info("Model for provider '{}' → '{}' successfully registered{}.", providerName, actualProviderId, profileInfo);
	}

	/**
	 * Un-registers a {@link ProviderMapping} from sensinact
	 * @param providerMapping the {@link ProviderMapping}
	 */
	public void unregisterModelMapping(ProviderMapping providerMapping) {
		if (nonNull(providerMapping)) {
			String actualProviderId = determineProviderId(providerMapping);
			modelManager.deleteModel(actualProviderId);
			logger.info("Model successfully unregistered for '{}'.", actualProviderId);
		}
	}

	static Class<?> boxClass(Class<?> unboxed) {
		if (unboxed.isPrimitive()) {
			return switch (unboxed.getName()) {
				case "float": {
					yield Float.class;
				}
				case "byte": {
					yield Byte.class;
				}
				case "char": {
					yield Character.class;
				}
				case "short": {
					yield Short.class;
				}
				case "int": {
					yield Integer.class;
				}
				case "double": {
					yield Double.class;
				}
				case "boolean": {
					yield Boolean.class;
				}
				case "long": {
					yield Long.class;
				}
				default:
					throw new IllegalArgumentException("Unexpected value: " + unboxed.getName());
				};
		}
		return unboxed;
	}

	/**
	 * Maps a provider
	 * @param providerMapping the {@link ProviderMapping}
	 */
	private void mapProvider(ProviderMapping providerMapping) {
		// Determine the actual provider ID based on the strategy
		String actualProviderId = determineProviderId(providerMapping);

		SensinactProvider provider = twin.getProvider(actualProviderId);
		if (isNull(provider)) {
			AdminMapping adminMapping = providerMapping.getAdmin();
			requireNonNull(adminMapping);
//			EPackage providerPackage = adminMapping.getProviderPackage();
			EMFModel model = modelManager.getModel(actualProviderId);
			if (isNull(model)) {
				model = modelManager.createModel(actualProviderId).build();
			}

//			mapAdminService(model, adminMapping);

			for (ServiceMapping serviceMapping : providerMapping.getServices()) {
				mapService(model, serviceMapping);
			}
//			twin.createProvider(providerPackage.getNsURI(), providerPackage.getName(), providerMapping.getMid());
		}
	}

	/**
	 * Determines the provider ID to use based on the mapping strategy.
	 * For UNIFIED strategy, uses the profile's providerId.
	 * For SEPARATE strategy, uses the mapping's own ID.
	 * 
	 * @param providerMapping the provider mapping
	 * @return the provider ID to use
	 */
	private String determineProviderId(ProviderMapping providerMapping) {
		MappingProfile profile = providerMapping.getProfile();
		if (profile != null && profile.getProviderStrategy() == ProviderStrategy.UNIFIED) {
			// Use the profile's providerId for unified strategy
			return profile.getProvider().getProviderId();
		} else {
			// Use the mapping's own ID for separate strategy (default)
			return providerMapping.getFqMid();
		}
	}

//	/**
//	 * Maps the admin service
//	 * @param model the {@link Model}
//	 * @param adminMapping the {@link AdminMapping}
//	 */
//	private Service mapAdminService(EMFModel model, AdminMapping adminMapping) {
//		requireNonNull(adminMapping);
//		final Instant timestamp = Instant.now();
//		Service admin = mapService(model, adminMapping);
//		EClass adminClass = (EClass) model.getModelEClass().getEStructuralFeature("admin").getEType();
//		EAttribute friendlyNameAttr = (EAttribute) adminClass.getEStructuralFeature("friendlyName");
//		friendlyNameAttr.setDefaultValueLiteral("test");
//		if (nonNull(adminMapping.getFriendlyName()) && model instanceof EMFModel em) {
//			admin.createResource("friendlyName").
//			withResourceType(ResourceType.PROPERTY).
//			withType(String.class).
//			withInitialValue(adminMapping.getFriendlyName(), timestamp).
//			build();
//		}
//		final GeoJsonObject location = lookupLocation(adminMapping);
//		if (nonNull(location)) {
//			EAttribute locationAttr = (EAttribute) adminClass.getEStructuralFeature("location");
//			locationAttr.setDefaultValue(location);
//			admin.createResource("location").
//			withResourceType(ResourceType.PROPERTY).
//			withType(GeoJsonObject.class).
//			withInitialValue(location, timestamp).
//			build();
//		}
//		return admin;
//	}

	/**
	 * Maps into a sensinact {@link Service}
	 * @param model the {@link Model}
	 * @param serviceMapping the {@link ServiceMapping}
	 */
	private Service mapService(Model model, ServiceMapping serviceMapping) {
		Service service = model.getServices().get(serviceMapping.getMid());
		if (isNull(service)) {
			service = model.createService(serviceMapping.getMid()).build();
		}
		for (ResourceMapping resourceMapping : serviceMapping.getResources()) {
			mapResource(service, resourceMapping);
		}
		return service;
	}

	/**
	 * Maps a resource for a sensinact service
	 * @param service the sensinact {@link Service}
	 * @param resourceMapping the {@link ResourceMapping}
	 */
	@SuppressWarnings({ "unchecked" })
	private Resource mapResource(Service service, ResourceMapping resourceMapping) {
		Resource resource = service.getResources().get(resourceMapping.getMid());
		if (isNull(resource)) {
			ResourceBuilder<Resource, Object> resourceBuilder = service.createResource(resourceMapping.getMid()).
					withResourceType(ResourceType.SENSOR);
			EDataType valueType = resourceMapping.getEAttributeType();
			Class<?> type = valueType.getInstanceClass();
			if (nonNull(type)) {
				resourceBuilder = resourceBuilder.withType((Class<Object>) boxClass(type));
			}
			if (nonNull(resourceMapping.getDefaultValue())) {
				Object defaultValue = EcoreUtil.createFromString(valueType, resourceMapping.getDefaultValue().toString());
				resourceBuilder = resourceBuilder.withInitialValue(defaultValue);
			}
			Map<String, Object> metadata = new HashMap<>();
			if (nonNull(resourceMapping.getUnit())) {
				metadata.put("unit", resourceMapping.getUnit());
			}
			if (nonNull(resourceMapping.getName()) ) {
				metadata.put("friendlyName", resourceMapping.getName());
			}
			if (nonNull(resourceMapping.getDescription()) ) {
				metadata.put("description", resourceMapping.getDescription());
			}
			if(!resourceMapping.getExtraMetadata().isEmpty()) {
				resourceMapping.getExtraMetadata().forEach(e -> metadata.put(e.getKey(), e.getValue()));
			}
			if (!metadata.isEmpty()) {
				resourceBuilder = resourceBuilder.withDefaultMetadata(metadata);
			}
			resource = resourceBuilder.build();
		}
		return resource;
	}

}