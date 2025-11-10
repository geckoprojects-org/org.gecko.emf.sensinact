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
 *     Mark Hoffmann - initial API and implementation
 */
package org.gecko.emf.sensinact.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.time.Instant;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.sensinact.core.command.AbstractSensinactCommand;
import org.eclipse.sensinact.core.command.GatewayThread;
import org.eclipse.sensinact.core.model.SensinactModelManager;
import org.eclipse.sensinact.core.twin.SensinactDigitalTwin;
import org.eclipse.sensinact.core.twin.SensinactProvider;
import org.eclipse.sensinact.core.twin.SensinactResource;
import org.eclipse.sensinact.core.twin.SensinactService;
import org.eclipse.sensinact.core.twin.TimedValue;
import org.eclipse.sensinact.mapping.ProviderMapping;
import org.gecko.emf.osgi.annotation.require.RequireEMF;
import org.gecko.emf.sensinact.model.ProviderMappingRegistry;
import org.gecko.emf.sensinact.model.ValueMapper;
import org.gecko.emf.sensinact.model.ValueMapperFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.service.typedevent.annotations.RequireTypedEvent;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

import org.gecko.weather.model.weather.MOSMIXSWeatherReport;
import org.gecko.weather.model.weather.Station;
import org.gecko.weather.model.weather.WeatherFactory;
import org.gecko.weather.model.weather.WeatherReports;
import org.gecko.weather.model.weather.WeatherStation;
import org.gecko.weather.model.weather.GeoPosition;

import dragino.DraginoLSE01Uplink;

/**
 * OSGi integration tests for ValueMapper functionality.
 * Tests real value extraction, conversion, and mapping to SensiNact resources
 * using actual EMF models and SensiNact services.
 */
@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
@RequireEMF
@RequireTypedEvent
public class ValueMapperTest {

	private static final String DRAGINO_MAPPING_URI = "/data/battery/dragino-battery-mapping_no-profile.xmi";
	private static final String DRAGINO_EXAMPLE_URI = "/data/dragino-example.dragino";
	private static final String DRAGINO_MODEL = "dragino-battery-sensor";

	private static final String WEATHER_MAPPING_URI = "/data/WeatherReportsProviderMapping.xmi";
	private static final String WEATHER_MODEL = "dwd-weather-reports";

    @InjectService
    GatewayThread gatewayThread;

    @InjectService
    ProviderMappingRegistry mappingRegistry;

    @InjectService
    ResourceSet resourceSet;

    private ProviderMapping batteryMapping;
    private DraginoLSE01Uplink draginoExample;

    @BeforeEach
    void setUp() throws IOException {
        // Load the Dragino battery mapping configuration
    	URL mappingUrl = getClass().getResource(DRAGINO_MAPPING_URI);
        Resource mappingResource = resourceSet.createResource(URI.createURI(mappingUrl.toString()));
        mappingResource.load(null);
        batteryMapping = (ProviderMapping) mappingResource.getContents().get(0);
        
        // Load the Dragino example data
        URL exampleUrl = getClass().getResource(DRAGINO_EXAMPLE_URI);
        Resource exampleResource = resourceSet.createResource(URI.createURI(exampleUrl.toString()));
        exampleResource.load(null);
        draginoExample = (DraginoLSE01Uplink) exampleResource.getContents().get(0);
        
        // Register the mapping to create the provider model structure
        mappingRegistry.registerModelMapping(batteryMapping);
    }

    @AfterEach
    void tearDown() {
    	mappingRegistry.dispose();
    }

    @Test
    @DisplayName("ValueMapper should extract resource values from Dragino example")
    void mapResourceValues_withDraginoExample_returnsValues() throws Exception {
        // Execute in SensiNact gateway thread
        Promise<Map<String, Object>> result = gatewayThread.execute(new AbstractSensinactCommand<Map<String, Object>>() {
            @Override
            protected Promise<Map<String, Object>> call(SensinactDigitalTwin twin, SensinactModelManager modelManager, PromiseFactory pf) {
                try {
                    ValueMapper mapper = ValueMapperFactory.createValueMapper(twin, batteryMapping);
                    Map<String, Object> values = mapper.mapResourceValues(draginoExample);
                    return pf.resolved(values);
                } catch (Exception e) {
                    return pf.failed(e);
                }
            }
        });
        
        Map<String, Object> values = result.getValue();
        
        // Verify values were extracted
        assertNotNull(values, "Values should not be null");
        assertFalse(values.isEmpty(), "Values should not be empty");
        
        // Check for expected battery resource
        assertTrue(values.containsKey("battery.level"), "Battery level should be extracted");
        Object batteryLevel = values.get("battery.level");
        assertNotNull(batteryLevel, "Battery level value should not be null");
        assertTrue(batteryLevel instanceof Double, "Battery level should be Double");
    }

    @Test
    @DisplayName("ValueMapper should validate Dragino example instance")
    void validateInstance_withDraginoExample_returnsValidResult() throws Exception {
        // Execute validation in SensiNact gateway thread
        Promise<ValueMapper.ValidationResult> result = gatewayThread.execute(new AbstractSensinactCommand<ValueMapper.ValidationResult>() {
            @Override
            protected Promise<ValueMapper.ValidationResult> call(SensinactDigitalTwin twin, SensinactModelManager modelManager, PromiseFactory pf) {
                try {
                    ValueMapper mapper = ValueMapperFactory.createValueMapper(twin, batteryMapping);
                    ValueMapper.ValidationResult validationResult = mapper.validateInstance(draginoExample);
                    return pf.resolved(validationResult);
                } catch (Exception e) {
                    return pf.failed(e);
                }
            }
        });
        
        ValueMapper.ValidationResult validationResult = result.getValue();
        
        // Verify validation result
        assertNotNull(validationResult, "Validation result should not be null");
        assertTrue(validationResult.isValid(), "Dragino example should be valid");
        assertTrue(validationResult.getErrors().isEmpty(), "No validation errors should be present");
    }

    @Test
    @DisplayName("ValueMapper should map Dragino instance to SensiNact provider")
    void mapInstance_withDraginoExample_createsProvider() throws Exception {
    	DraginoLSE01Uplink dragino = EcoreUtil.copy(draginoExample);
    	dragino.setDeduplicationId("test");
    	
    	// Set timestamp 5 minutes in the future to ensure it's newer than provider creation
    	Instant futureTimestamp = Instant.now().plusSeconds(300); // 300 seconds = 5 minutes
    	String futureTimestampString = futureTimestamp.toString();
    	// Update the first rxInfo's time attribute
    	if (!dragino.getRxInfo().isEmpty()) {
    		dragino.getRxInfo().get(0).setTime(futureTimestampString);
    	}
        // Execute mapping in SensiNact gateway thread
        Promise<Boolean> result = gatewayThread.execute(new AbstractSensinactCommand<Boolean>() {
            @Override
            protected Promise<Boolean> call(SensinactDigitalTwin twin, SensinactModelManager modelManager, PromiseFactory pf) {
                try {
                	assertNull(twin.getProvider(DRAGINO_MODEL, "test"), "No provider exists yet");
                	
                    ValueMapper mapper = ValueMapperFactory.createValueMapper(twin, batteryMapping);
                    // Map the instance
                    mapper.mapInstance(dragino);
                    SensinactProvider provider = twin.getProvider(DRAGINO_MODEL, "test");
                    assertNotNull(provider, "Dragino provider should not be null");
                    SensinactService adminService = provider.getServices().get("admin");
                    assertNotNull(adminService, "Admin service should ne be null");
                    SensinactResource fnResource = adminService.getResources().get("friendlyName");
                    assertNotNull(fnResource, "Friendly name should be set");
                    Promise<TimedValue<?>> value = fnResource.getValue();
                    TimedValue<?> timedValue = value.getValue();
                    // Verify timestamp from mapping configuration (should be the future timestamp we set)
                    assertEquals(futureTimestamp, timedValue.getTimestamp(), 
                    		"Admin friendlyName should use timestamp from provider mapping");
                    assertEquals("Dragino_LSE01", timedValue.getValue().toString());
                    
                    return pf.resolved(true);
                } catch (Exception e) {
                    return pf.failed(e);
                }
            }
        });
        
        Boolean success = result.getValue();
        assertTrue(success, "Mapping should complete successfully");
    }

    @Test
    @DisplayName("ValueMapper should handle null EObject parameter")
    void mapInstance_withNullEObject_throwsException() throws Exception {
        // Execute in SensiNact gateway thread and expect exception
        Promise<Boolean> result = gatewayThread.execute(new AbstractSensinactCommand<Boolean>() {
            @Override
            protected Promise<Boolean> call(SensinactDigitalTwin twin, SensinactModelManager modelManager, PromiseFactory pf) {
                try {
                    ValueMapper mapper = ValueMapperFactory.createValueMapper(twin, batteryMapping);
                    mapper.mapInstance(null);
                    return pf.resolved(true);
                } catch (Exception e) {
                    return pf.failed(e);
                }
            }
        });
        
        // Verify the promise failed with NullPointerException
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> result.getValue());
        assertTrue(exception.getCause() instanceof NullPointerException, 
        		"Should throw NullPointerException for null EObject");
    }

    @Test
    @DisplayName("ValueMapper should reject null parameters")
    void createValueMapper_withNullParameters_throwsException() throws Exception {
        // Execute in SensiNact gateway thread
        Promise<Boolean> result = gatewayThread.execute(new AbstractSensinactCommand<Boolean>() {
            @Override
            protected Promise<Boolean> call(SensinactDigitalTwin twin, SensinactModelManager modelManager, PromiseFactory pf) {
                try {
                    ValueMapperFactory.createValueMapper(null, batteryMapping);
                    return pf.resolved(true);
                } catch (Exception e) {
                    return pf.failed(e);
                }
            }
        });
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> result.getValue());
        assertTrue(exception.getCause() instanceof NullPointerException,
                  "Should throw NullPointerException for null twin");
    }

    // ========== Collection Mapping Tests ==========

    @Test
    @DisplayName("Collection mapping: should map provider name from collection index 0")
    void collectionMapping_providerName_usesFirstReport() throws Exception {
        // Load weather mapping
        URL mappingUrl = getClass().getResource(WEATHER_MAPPING_URI);
        Resource mappingResource = resourceSet.createResource(URI.createURI(mappingUrl.toString()));
        mappingResource.load(null);
        ProviderMapping weatherMapping = (ProviderMapping) mappingResource.getContents().get(0);

        // Create test data with WeatherReports containing multiple reports
        WeatherReports weatherReports = createWeatherReportsTestData();

        // Register mapping
        mappingRegistry.registerModelMapping(weatherMapping);

        // Execute mapping in SensiNact gateway thread
        Promise<Boolean> result = gatewayThread.execute(new AbstractSensinactCommand<Boolean>() {
            @Override
            protected Promise<Boolean> call(SensinactDigitalTwin twin, SensinactModelManager modelManager, PromiseFactory pf) {
                try {
                    ValueMapper mapper = ValueMapperFactory.createValueMapper(twin, weatherMapping);
                    mapper.mapInstance(weatherReports);

                    // Verify provider was created with ID from first report
                    SensinactProvider provider = twin.getProvider(WEATHER_MODEL, "10567");
                    assertNotNull(provider, "Weather provider should be created");

                    return pf.resolved(true);
                } catch (Exception e) {
                    return pf.failed(e);
                }
            }
        });

        assertTrue(result.getValue(), "Provider name mapping from collection should succeed");
    }

    @Test
    @DisplayName("Collection mapping: should map currentWeather service from reports[0]")
    void collectionMapping_currentWeather_usesReportAtIndex0() throws Exception {
        // Load weather mapping
        URL mappingUrl = getClass().getResource(WEATHER_MAPPING_URI);
        Resource mappingResource = resourceSet.createResource(URI.createURI(mappingUrl.toString()));
        mappingResource.load(null);
        ProviderMapping weatherMapping = (ProviderMapping) mappingResource.getContents().get(0);

        // Create test data
        WeatherReports weatherReports = createWeatherReportsTestData();

        // Register mapping
        mappingRegistry.registerModelMapping(weatherMapping);

        // Execute mapping
        Promise<Boolean> result = gatewayThread.execute(new AbstractSensinactCommand<Boolean>() {
            @Override
            protected Promise<Boolean> call(SensinactDigitalTwin twin, SensinactModelManager modelManager, PromiseFactory pf) {
                try {
                    ValueMapper mapper = ValueMapperFactory.createValueMapper(twin, weatherMapping);
                    mapper.mapInstance(weatherReports);

                    SensinactProvider provider = twin.getProvider(WEATHER_MODEL, "10567");
                    assertNotNull(provider, "Provider should exist");

                    // Check currentWeather service exists
                    SensinactService currentWeatherService = provider.getServices().get("currentWeather");
                    assertNotNull(currentWeatherService, "currentWeather service should exist");

                    // Verify data comes from reports[0] (windSpeed = 5.0)
                    SensinactResource windSpeedResource = currentWeatherService.getResources().get("windSpeed");
                    assertNotNull(windSpeedResource, "windSpeed resource should exist");

                    Promise<TimedValue<?>> valuePromise = windSpeedResource.getValue();
                    TimedValue<?> timedValue = valuePromise.getValue();
                    assertEquals(5.0f, timedValue.getValue(), "windSpeed should match reports[0]");

                    return pf.resolved(true);
                } catch (Exception e) {
                    return pf.failed(e);
                }
            }
        });

        assertTrue(result.getValue(), "currentWeather mapping from reports[0] should succeed");
    }

    @Test
    @DisplayName("Collection mapping: should map forecast3H service from reports[1]")
    void collectionMapping_forecast3H_usesReportAtIndex1() throws Exception {
        // Load weather mapping
        URL mappingUrl = getClass().getResource(WEATHER_MAPPING_URI);
        Resource mappingResource = resourceSet.createResource(URI.createURI(mappingUrl.toString()));
        mappingResource.load(null);
        ProviderMapping weatherMapping = (ProviderMapping) mappingResource.getContents().get(0);

        // Create test data
        WeatherReports weatherReports = createWeatherReportsTestData();

        // Register mapping
        mappingRegistry.registerModelMapping(weatherMapping);

        // Execute mapping
        Promise<Boolean> result = gatewayThread.execute(new AbstractSensinactCommand<Boolean>() {
            @Override
            protected Promise<Boolean> call(SensinactDigitalTwin twin, SensinactModelManager modelManager, PromiseFactory pf) {
                try {
                    ValueMapper mapper = ValueMapperFactory.createValueMapper(twin, weatherMapping);
                    mapper.mapInstance(weatherReports);

                    SensinactProvider provider = twin.getProvider(WEATHER_MODEL, "10567");
                    assertNotNull(provider, "Provider should exist");

                    // Check forecast3H service exists
                    SensinactService forecast3HService = provider.getServices().get("forecast3H");
                    assertNotNull(forecast3HService, "forecast3H service should exist");

                    // Verify data comes from reports[1] (windSpeed = 7.5)
                    SensinactResource windSpeedResource = forecast3HService.getResources().get("windSpeed");
                    assertNotNull(windSpeedResource, "windSpeed resource should exist");

                    Promise<TimedValue<?>> valuePromise = windSpeedResource.getValue();
                    TimedValue<?> timedValue = valuePromise.getValue();
                    assertEquals(7.5f, timedValue.getValue(), "windSpeed should match reports[1]");

                    return pf.resolved(true);
                } catch (Exception e) {
                    return pf.failed(e);
                }
            }
        });

        assertTrue(result.getValue(), "forecast3H mapping from reports[1] should succeed");
    }

    @Test
    @DisplayName("Collection mapping: validation should fail if collection index out of bounds")
    void collectionMapping_validation_failsForOutOfBoundsIndex() throws Exception {
        // Load weather mapping
        URL mappingUrl = getClass().getResource(WEATHER_MAPPING_URI);
        Resource mappingResource = resourceSet.createResource(URI.createURI(mappingUrl.toString()));
        mappingResource.load(null);
        ProviderMapping weatherMapping = (ProviderMapping) mappingResource.getContents().get(0);

        // Create test data with only ONE report (but mapping expects index 0 AND 1)
        WeatherReports weatherReports = WeatherFactory.eINSTANCE.createWeatherReports();
        weatherReports.setId("station-10567");
        MOSMIXSWeatherReport report = WeatherFactory.eINSTANCE.createMOSMIXSWeatherReport();
        report.setId("report-0");
        weatherReports.getReports().add(report);

        // Register mapping
        mappingRegistry.registerModelMapping(weatherMapping);

        // Execute validation
        Promise<ValueMapper.ValidationResult> result = gatewayThread.execute(new AbstractSensinactCommand<ValueMapper.ValidationResult>() {
            @Override
            protected Promise<ValueMapper.ValidationResult> call(SensinactDigitalTwin twin, SensinactModelManager modelManager, PromiseFactory pf) {
                try {
                    ValueMapper mapper = ValueMapperFactory.createValueMapper(twin, weatherMapping);
                    ValueMapper.ValidationResult validation = mapper.validateInstance(weatherReports);
                    return pf.resolved(validation);
                } catch (Exception e) {
                    return pf.failed(e);
                }
            }
        });

        ValueMapper.ValidationResult validation = result.getValue();
        assertFalse(validation.isValid(), "Validation should fail when collection index 1 is out of bounds");
        assertFalse(validation.getErrors().isEmpty(), "Should have validation errors");
    }

    /**
     * Helper method to create test WeatherReports data with multiple reports.
     */
    private WeatherReports createWeatherReportsTestData() {
        WeatherFactory factory = WeatherFactory.eINSTANCE;

        // Create WeatherReports container
        WeatherReports weatherReports = factory.createWeatherReports();
        weatherReports.setId("station-10567");

        // Create weather station
        WeatherStation weatherStation = factory.createWeatherStation();
        weatherStation.setId("10567");
        weatherStation.setName("GERA");
        GeoPosition location = factory.createGeoPosition();
        location.setLatitude(50.88);
        location.setLongitude(12.13);
        location.setElevation(311);
        weatherStation.setLocation(location);

        // Create first report (current weather) - index 0
        MOSMIXSWeatherReport report0 = factory.createMOSMIXSWeatherReport();
        report0.setId("report-0");
        report0.setTimestamp(new java.util.Date());
        report0.setWeatherStation(weatherStation);

        Station station0 = factory.createStation();
        station0.setName("GERA");
        station0.setLocation(location);
        report0.setStation(station0);

        report0.setWindSpeed(5.0f);
        report0.setWindDirection(180.0f);
        report0.setTempAboveSurface5(288.15f); // 15°C in Kelvin
        report0.setCloudCoverTotal(50.0f);
        report0.setSurfacePressure(101325.0f);

        weatherReports.getReports().add(report0);

        // Create second report (3H forecast) - index 1
        MOSMIXSWeatherReport report1 = factory.createMOSMIXSWeatherReport();
        report1.setId("report-1");
        report1.setTimestamp(new java.util.Date(System.currentTimeMillis() + 10800000)); // +3 hours
        report1.setWeatherStation(weatherStation);

        Station station1 = factory.createStation();
        station1.setName("GERA");
        station1.setLocation(location);
        report1.setStation(station1);

        report1.setWindSpeed(7.5f); // Different value for forecast
        report1.setWindDirection(200.0f);
        report1.setTempAboveSurface5(290.15f); // 17°C in Kelvin
        report1.setCloudCoverTotal(70.0f);
        report1.setSurfacePressure(101200.0f);

        weatherReports.getReports().add(report1);

        return weatherReports;
    }
}