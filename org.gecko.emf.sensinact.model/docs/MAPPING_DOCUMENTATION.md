# SensiNact Mapping Framework Documentation

## Table of Contents
1. [Overview](#overview)
2. [Core Concepts](#core-concepts)
3. [Mapping Architecture](#mapping-architecture)
4. [Feature Reference](#feature-reference)
5. [Collection Mapping](#collection-mapping)
6. [Automatic Resource Generation with ReferenceMapping](#automatic-resource-generation-with-referencemapping)
7. [Known Issues](#)
8. [Complete Examples](#complete-examples)
9. [Best Practices](#best-practices)
10. [Troubleshooting](#troubleshooting)

---

## Overview

The SensiNact Mapping Framework provides a **declarative, model-driven approach** to transform domain-specific EMF (Eclipse Modeling Framework) models into SensiNact digital twin providers. Instead of writing imperative transformation code (like QVT transformations), you define mappings in XMI configuration files that specify how data flows from your source model to SensiNact resources.

### Key Benefits

- ✅ **Declarative**: Configuration over code
- ✅ **Hot-reloadable**: Change mappings without recompilation
- ✅ **Type-safe**: Backed by EMF metamodel validation
- ✅ **Reusable**: Same infrastructure for all sensor types
- ✅ **Maintainable**: 71% code reduction vs. imperative transformations
- ✅ **Flexible**: Supports complex collection-based mappings

---

## Core Concepts

### Provider Mapping

A **ProviderMapping** is the root element that defines how a source EMF object is transformed into a SensiNact provider with its services and resources.

```xml
<sensinactMapping:ProviderMapping
    mid="sensor-mapping-id"
    providerTimestamp="false">
  <!-- Configuration here -->
</sensinactMapping:ProviderMapping>
```

**Key Attributes:**
- `mid`: Unique mapping identifier
- `providerTimestamp`: If `true`, all services/resources share the same timestamp

### Feature Paths

Feature paths navigate through EMF object graphs using structural features (attributes and references). They are the core mechanism for value extraction.

```xml
<featurePath xsi:type="ecore:EReference" href="model.ecore#//Class/reference"/>
<featurePath xsi:type="ecore:EAttribute" href="model.ecore#//Class/attribute"/>
```

### Services and Resources

In SensiNact, a provider contains **services**, which contain **resources**:
- **Provider**: Top-level entity (e.g., a weather station)
- **Service**: Logical grouping of related data (e.g., "temperature", "wind")
- **Resource**: Individual data point (e.g., "windSpeed", "windDirection")

---

## Mapping Architecture

### Provider-Level Configuration

#### Provider Name

Defines the unique identifier for the provider:

```xml
<name>
  <featurePath href="model.ecore#//Sensor/deviceId"/>
</name>
```

Or with a static fallback:

```xml
<name name="StaticName">
  <featurePath href="model.ecore#//Sensor/deviceId"/>
</name>
```

#### Provider Classes

Specifies which EMF classes this mapping applies to:

```xml
<providerClasses href="model.ecore#//SensorData"/>
<providerClasses href="model.ecore#//ExtendedSensorData"/>
```

#### Provider Timestamp

Optional timestamp that applies to all services/resources:

```xml
<timestamp strategy="FEATURE">
  <featurePath href="model.ecore#//Sensor/measurementTime"/>
</timestamp>
```

**Timestamp Strategies:**
- `FEATURE`: Extract from feature path
- `NOW`: Use current time
- `FUNCTION`: Use custom function (advanced)

---

## Feature Reference

### Service Mapping

Defines a SensiNact service with its resources:

```xml
<services mid="temperature">
  <name name="Temperature"/>
  <timestamp strategy="FEATURE">
    <featurePath href="model.ecore#//Measurement/timestamp"/>
  </timestamp>

  <!-- Resources go here -->
</services>
```

### Resource Mapping

Defines individual data points within a service:

```xml
<resources
    name="celsius"
    unit="°C"
    mid="celsius">
  <eType xsi:type="ecore:EDataType"
      href="http://www.eclipse.org/emf/2002/Ecore#//EFloat"/>
  <valueFeature href="model.ecore#//TemperatureSensor/value"/>
</resources>
```

**Key Attributes:**
- `name`: Display name
- `unit`: Measurement unit
- `mid`: Unique resource identifier within service
- `eType`: Target data type
- `valueFeature`: Path to source value

### Admin Service

Special service for provider metadata:

```xml
<admin xsi:type="sensinactMapping:AdminMapping" mid="admin">
  <friendlyNameFeature href="model.ecore#//Sensor/displayName"/>
  <providerPackage href="model.ecore#/"/>

  <!-- Location -->
  <latitudeRef href="model.ecore#//Sensor/location"/>
  <latitudeRef href="model.ecore#//Location/lat"/>

  <longitudeRef href="model.ecore#//Sensor/location"/>
  <longitudeRef href="model.ecore#//Location/lon"/>
</admin>
```

---

## Collection Mapping

### Overview

**Collection mapping** enables mapping from EMF objects that contain collections, where different services map from different elements in the collection. This is essential for use cases like:
- Weather forecasts with multiple time periods
- Sensor arrays with multiple measurement points
- Historical data with multiple readings

### Service-Level Collection Navigation

Each service can independently specify which collection element it maps from:

```xml
<!-- Source: WeatherReports containing a list of reports -->
<providerClasses href="weather.ecore#//WeatherReports"/>

<!-- Service 1: Maps from reports[0] (current weather) -->
<services mid="currentWeather" collectionIndex="0">
  <collectionFeature href="weather.ecore#//WeatherReports/reports"/>
  <resources name="windSpeed" .../>
</services>

<!-- Service 2: Maps from reports[1] (3-hour forecast) -->
<services mid="forecast3H" collectionIndex="1">
  <collectionFeature href="weather.ecore#//WeatherReports/reports"/>
  <resources name="windSpeed" .../>
</services>
```

**Key Attributes:**
- `collectionFeature`: Reference to the collection in the source object
- `collectionIndex`: Which element to use (0-based, default=0)
- `collectionFilter`: Optional filter expression (future feature)

### Feature Path Collection Navigation

When feature paths traverse collections, you can specify which index to use:

```xml
<!-- Provider name from first report's station ID -->
<name collectionIndex="0">
  <featurePath href="weather.ecore#//WeatherReports/reports"/>
  <featurePath href="weather.ecore#//WeatherReport/station"/>
  <featurePath href="weather.ecore#//Station/id"/>
</name>
```

This navigates: `WeatherReports.reports[0].station.id`

### Collection Mapping Rules

1. **Service-level collection** (`ServiceMapping.collectionFeature`):
   - Extracts an element from a collection
   - All resources in that service map from the extracted element
   - Different services can use different indices

2. **Feature path collection** (`FeatureMapping.collectionIndex`):
   - Applies when a feature path encounters a collection
   - Uses the specified index (default 0)
   - Useful for provider name, timestamps, and nested navigation

3. **Priority**: Service-level collection extraction happens **first**, then feature paths are evaluated relative to the extracted element

### Validation

The framework validates collection mappings at both configuration and runtime:

```java
// At runtime, validates:
// - Collection exists and is not empty
// - Index is within bounds
// - Collection elements are EObjects
```

Validation errors are reported with clear messages:
```
Collection index 1 out of bounds for collection 'reports' of size 1 (service: forecast3H)
```

---

## Automatic Resource Generation with ReferenceMapping

### Overview

**ReferenceMapping** is a powerful feature that automatically generates ResourceMappings from all attributes of a referenced EClass, eliminating the need to manually define each resource in your mapping XMI. This is particularly useful when:
- You have types with many attributes (e.g., weather reports with 50+ fields)
- You want all or most attributes mapped as resources
- The attribute structure may change over time
- You're dealing with inheritance hierarchies where concrete types vary at runtime

### How It Works

When you configure a `referencedResource` on a `ServiceMapping`, the framework:

1. **At Model Registration Time** (ProviderModelSensinactMapper):
   - Analyzes the target EClass specified in the ReferenceMapping
   - Generates a ResourceMapping for each attribute (respecting filter/exclude rules)
   - Creates SensinactResource instances in the service model
   - Stores generated ResourceMappings in the service's `temporaryResources` list

2. **At Runtime** (ValueMapperImpl):
   - Maps values from the source instance to the auto-generated resources
   - Uses the complete feature paths built during model registration
   - Applies collection indices for navigating to the correct element

### Basic Configuration

```xml
<services mid="currentWeather">
  <name name="Current Weather"/>

  <!-- ReferenceMapping: auto-generates resources from all attributes -->
  <referencedResource
      xsi:type="sensinactMapping:ReferenceMapping"
      collectionIndex="0"
      exclude="true">

    <!-- Feature path to the collection/reference -->
    <featurePath
        xsi:type="ecore:EReference"
        href="weather.ecore#//WeatherReports/reports"/>

    <!-- Optional: Filter attributes to exclude -->
    <filter
        xsi:type="ecore:EAttribute"
        href="weather.ecore#//WeatherReport/id"/>
    <filter
        xsi:type="ecore:EAttribute"
        href="weather.ecore#//WeatherReport/timestamp"/>
  </referencedResource>
</services>
```

**This configuration will:**
- Navigate to `WeatherReports.reports[0]` (first element due to `collectionIndex="0"`)
- Generate a ResourceMapping for every attribute in `WeatherReport`
- Exclude `id` and `timestamp` attributes (because `exclude="true"`)
- Create resources named after the attributes (e.g., `windSpeed`, `temperature`, etc.)

### Filter Configuration

The `filter` and `exclude` attributes work together:

| exclude | filter | Result |
|---------|--------|--------|
| `true` (default) | Empty | Map **all** attributes |
| `true` | Specified | Map all attributes **except** those in filter |
| `false` | Specified | Map **only** attributes in filter |
| `false` | Empty | Map **nothing** (empty service) |

**Example: Include only specific attributes**
```xml
<referencedResource
    xsi:type="sensinactMapping:ReferenceMapping"
    exclude="false">

  <featurePath href="weather.ecore#//WeatherReports/reports"/>

  <!-- Only map these two attributes -->
  <filter href="weather.ecore#//WeatherReport/temperature"/>
  <filter href="weather.ecore#//WeatherReport/windSpeed"/>
</referencedResource>
```

### Handling Inheritance with targetEClass

**Problem:** When your feature path points to a base type, but runtime instances are of a specific subtype, you need to specify which concrete EClass to use for resource generation.

**Example Scenario:**
```
WeatherReport (base type)
├── id: String
├── timestamp: Date
└── temperature: Float

MOSMIXSWeatherReport extends WeatherReport
├── (inherits all from WeatherReport)
├── windSpeed: Float
├── dewPoint: Float
└── significantWeather6Hours: W1W2 (reference)
```

If `WeatherReports.reports` is declared as `List<WeatherReport>`, but you know instances will be `MOSMIXSWeatherReport`, use `targetEClass`:

```xml
<referencedResource
    xsi:type="sensinactMapping:ReferenceMapping"
    collectionIndex="0"
    exclude="true">

  <featurePath href="weather.ecore#//WeatherReports/reports"/>

  <!-- Explicitly specify concrete subtype -->
  <targetEClass href="weather.ecore#//MOSMIXSWeatherReport"/>

  <filter href="weather.ecore#//WeatherReport/id"/>
  <filter href="weather.ecore#//WeatherReport/timestamp"/>
</referencedResource>
```

**This generates resources from MOSMIXSWeatherReport attributes:**
- `temperature` (inherited)
- `windSpeed` (from MOSMIXSWeatherReport)
- `dewPoint` (from MOSMIXSWeatherReport)
- etc.

**Without targetEClass**, only base `WeatherReport` attributes would be mapped.

### Nested Reference Mapping

You can map attributes from nested references using `referenceMappings`:

```xml
<referencedResource
    xsi:type="sensinactMapping:ReferenceMapping"
    collectionIndex="0"
    exclude="true">

  <featurePath href="weather.ecore#//WeatherReports/reports"/>
  <targetEClass href="weather.ecore#//MOSMIXSWeatherReport"/>

  <filter href="weather.ecore#//WeatherReport/id"/>

  <!-- Nested mapping for significantWeather6Hours reference -->
  <referenceMappings
      xsi:type="sensinactMapping:ReferenceMapping">

    <featurePath
        xsi:type="ecore:EReference"
        href="weather.ecore#//MOSMIXSWeatherReport/significantWeather6Hours"/>

    <!-- Optional: specify concrete type for nested reference -->
    <targetEClass href="weather.ecore#//W1W2"/>
  </referenceMappings>
</referencedResource>
```

**Result:** Resources will be created for:
1. All attributes from `MOSMIXSWeatherReport` (except `id`)
2. All attributes from `W1W2` with prefix `significantWeather6Hours` (e.g., `significantWeather6HoursWeatherCode`)

### Temporary Resources

Generated ResourceMappings are stored in `ServiceMapping.temporaryResources`:

```java
// Generated ResourceMappings during model registration
serviceMapping.getTemporaryResources().forEach(resource -> {
    System.out.println("Auto-generated: " + resource.getMid());
});

// Manually defined resources (from XMI)
serviceMapping.getResources().forEach(resource -> {
    System.out.println("Manual: " + resource.getMid());
});
```

**Why "temporary"?**
- They're transient (not saved back to XMI)
- Generated fresh each time the mapping is registered
- Kept separate from manually-defined resources

**Both lists are processed identically** during value mapping.

### Complete Example: Weather Reports

**Source Model:**
```
WeatherReports
├── id: String
└── reports: List<WeatherReport>
    └── [runtime type: MOSMIXSWeatherReport]
        ├── id: String
        ├── timestamp: Date
        ├── temperature: Float
        ├── windSpeed: Float
        ├── dewPoint: Float
        ├── cloudCover: Float
        └── significantWeather6Hours: W1W2
            ├── weatherCode: Int
            └── intensity: Int
```

**Mapping:**
```xml
<sensinactMapping:ProviderMapping
    mid="dwd-weather-reports"
    providerTimestamp="false">

  <!-- Provider name from first report's station -->
  <name>
    <featurePath href="weather.ecore#//WeatherReports/reports"/>
    <featurePath href="weather.ecore#//WeatherReport/weatherStation"/>
    <featurePath href="weather.ecore#//WeatherStation/id"/>
  </name>

  <providerClasses href="weather.ecore#//WeatherReports"/>

  <!-- Current Weather service: auto-generate from reports[0] -->
  <services mid="currentWeather">
    <timestamp strategy="FEATURE">
      <featurePath href="weather.ecore#//WeatherReport/timestamp"/>
    </timestamp>
    <name name="Current Weather"/>

    <referencedResource
        xsi:type="sensinactMapping:ReferenceMapping"
        collectionIndex="0"
        exclude="true">

      <featurePath href="weather.ecore#//WeatherReports/reports"/>

      <!-- Use concrete subtype -->
      <targetEClass href="weather.ecore#//MOSMIXSWeatherReport"/>

      <!-- Exclude ID and timestamp -->
      <filter href="weather.ecore#//WeatherReport/id"/>
      <filter href="weather.ecore#//WeatherReport/timestamp"/>

      <!-- Include nested weather code reference -->
      <referenceMappings
          xsi:type="sensinactMapping:ReferenceMapping">
        <featurePath href="weather.ecore#//MOSMIXSWeatherReport/significantWeather6Hours"/>
      </referenceMappings>
    </referencedResource>
  </services>

  <!-- 3H Forecast service: auto-generate from reports[1] -->
  <services mid="forecast3H">
    <timestamp strategy="FEATURE">
      <featurePath href="weather.ecore#//WeatherReport/timestamp"/>
    </timestamp>
    <name name="3H Forecast"/>

    <referencedResource
        xsi:type="sensinactMapping:ReferenceMapping"
        collectionIndex="1"
        exclude="true">

      <featurePath href="weather.ecore#//WeatherReports/reports"/>
      <targetEClass href="weather.ecore#//MOSMIXSWeatherReport"/>

      <filter href="weather.ecore#//WeatherReport/id"/>
      <filter href="weather.ecore#//WeatherReport/timestamp"/>

      <referenceMappings
          xsi:type="sensinactMapping:ReferenceMapping">
        <featurePath href="weather.ecore#//MOSMIXSWeatherReport/significantWeather6Hours"/>
      </referenceMappings>
    </referencedResource>
  </services>

  <!-- Admin service -->
  <admin xsi:type="sensinactMapping:AdminMapping" mid="admin">
    <timestamp strategy="FEATURE">
      <featurePath href="weather.ecore#//WeatherReports/reports"/>
      <featurePath href="weather.ecore#//WeatherReport/timestamp"/>
    </timestamp>
    <name name="Admin"/>

    <friendlyNameFeature href="weather.ecore#//WeatherReports/reports"/>
    <friendlyNameFeature href="weather.ecore#//WeatherReport/station"/>
    <friendlyNameFeature href="weather.ecore#//Station/name"/>

    <providerPackage href="weather.ecore#/"/>

    <latitudeRef href="weather.ecore#//WeatherReports/reports"/>
    <latitudeRef href="weather.ecore#//WeatherReport/station"/>
    <latitudeRef href="weather.ecore#//Station/location"/>
    <latitudeRef href="weather.ecore#//GeoPosition/latitude"/>

    <longitudeRef href="weather.ecore#//WeatherReports/reports"/>
    <longitudeRef href="weather.ecore#//WeatherReport/station"/>
    <longitudeRef href="weather.ecore#//Station/location"/>
    <longitudeRef href="weather.ecore#//GeoPosition/longitude"/>
  </admin>
</sensinactMapping:ProviderMapping>
```

**Generated Resources for `currentWeather` service:**
- `temperature` (from MOSMIXSWeatherReport)
- `windSpeed` (from MOSMIXSWeatherReport)
- `dewPoint` (from MOSMIXSWeatherReport)
- `cloudCover` (from MOSMIXSWeatherReport)
- `significantWeather6HoursWeatherCode` (from nested W1W2)
- `significantWeather6HoursIntensity` (from nested W1W2)

### Benefits

**Code Reduction:**
- Manual: ~10 lines per resource × 50 resources = 500 lines
- Auto-generated: ~20 lines total for ReferenceMapping
- **96% reduction in mapping configuration**

**Maintainability:**
- Model changes automatically reflected in resources
- No need to update XMI when attributes are added/removed
- Consistent resource naming

**Flexibility:**
- Combine auto-generated with manually-defined resources
- Override specific resources if needed
- Filter attributes using include/exclude patterns

### Limitations

1. **Metadata**: Auto-generated resources copy EAnnotations from the Ecore model attributes. You can customize metadata by adding annotations to your Ecore attributes using:
   - Annotation source `"sensinact.mapping"` with detail keys:
     - `"sensinact.mapping.unit"` for units
     - `"sensinact.mapping.description"` for descriptions
   - Annotation source `"sensinact.mapping.metadata"` for additional custom metadata
   - GenModel documentation annotations (`http://www.eclipse.org/emf/2002/GenModel` with key `"documentation"`)

   Only if you need metadata that differs from the source attribute's annotations would you need to define resources manually.

2. **Complex Types**: Only EAttribute types are auto-mapped. EReferences require explicit nested ReferenceMapping
3. **Naming**: Resource names match attribute names. Custom naming requires manual definition

### Customizing Metadata with Ecore Annotations

Auto-generated resources automatically extract metadata from Ecore attribute annotations. This is the **recommended approach** for customizing metadata:

**Ecore Model with Annotations:**
```xml
<!-- In your .ecore file -->
<eStructuralFeatures xsi:type="ecore:EAttribute" name="temperature" eType="ecore:EDataType ...">
  <!-- Unit and description metadata -->
  <eAnnotations source="sensinact.mapping">
    <details key="sensinact.mapping.unit" value="°C"/>
    <details key="sensinact.mapping.description" value="Temperature in degrees Celsius"/>
  </eAnnotations>

  <!-- Additional metadata -->
  <eAnnotations source="sensinact.mapping.metadata">
    <details key="displayFormat" value="%.1f"/>
    <details key="min" value="-50"/>
    <details key="max" value="50"/>
  </eAnnotations>

  <!-- GenModel documentation (fallback for description) -->
  <eAnnotations source="http://www.eclipse.org/emf/2002/GenModel">
    <details key="documentation" value="Ambient temperature measurement"/>
  </eAnnotations>
</eStructuralFeatures>
```

**Mapping (no manual resource definition needed):**
```xml
<services mid="weather">
  <!-- Auto-generate all resources - metadata comes from Ecore annotations -->
  <referencedResource xsi:type="sensinactMapping:ReferenceMapping">
    <featurePath href="weather.ecore#//WeatherReports/reports"/>
    <targetEClass href="weather.ecore#//MOSMIXSWeatherReport"/>
  </referencedResource>
</services>
```

The generated `temperature` resource will automatically have the unit, description, and extra metadata from the Ecore annotations.

### Combining Auto-Generated and Manual Resources

You can also mix both approaches when you need resource-specific overrides:

```xml
<services mid="weather">
  <!-- Auto-generate most resources -->
  <referencedResource xsi:type="sensinactMapping:ReferenceMapping">
    <featurePath href="weather.ecore#//WeatherReports/reports"/>
    <targetEClass href="weather.ecore#//MOSMIXSWeatherReport"/>

    <!-- Exclude this one - we'll define it manually with different metadata -->
    <filter href="weather.ecore#//MOSMIXSWeatherReport/temperature"/>
  </referencedResource>

  <!-- Manually define temperature with mapping-specific metadata -->
  <resources name="Temperature in Celsius" unit="°C" mid="temperature">
    <eType href="http://www.eclipse.org/emf/2002/Ecore#//EFloat"/>
    <valueFeature href="weather.ecore#//WeatherReports/reports"/>
    <valueFeature href="weather.ecore#//MOSMIXSWeatherReport/temperature"/>
    <extraMetadata key="displayFormat" value="%.1f"/>
    <extraMetadata key="min" value="-50"/>
    <extraMetadata key="max" value="50"/>
  </resources>
</services>
```

---

## Known Issues

### Admin Service not properly mapped

The Admin service is not really mapped followed the xmi mapping instructions.

The resource value is properly found in the `ValueMapperImpl`, but when you try to read it, you do not get the right value. 

There is a failing (and yet disabled for now) test that shows this behavior in the ''ValueMapperTest#providerMapping_admin`:

```java
@Test
    @Disabled("This is currently not working because the admin service is treated differently in sensinact")
    @DisplayName("Provider mapping: should map admin service")
    void providerMapping_admin() throws Exception {
        // Load weather mapping
        URL mappingUrl = getClass().getResource(WEATHER_MAPPING_URI_NO_RESOURCES);
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
                    SensinactService adminService = provider.getServices().get("admin");
                    assertNotNull(adminService, "adminService service should exist");

                    SensinactResource friendlyName = adminService.getResources().get("friendlyName");
                    assertNotNull(friendlyName, "friendlyName resource should exist");

                    Promise<TimedValue<?>> valuePromise = friendlyName.getValue();
                    TimedValue<?> timedValue = valuePromise.getValue();
                    assertEquals("GERA", timedValue.getValue(), "friendlyName should match reports[0]"); // fails here!!

                    return pf.resolved(true);
                } catch (Exception e) {
                    return pf.failed(e);
                }
            }
        });
     
        assertTrue(result.getValue(), "currentWeather mapping from reports[0] should succeed");
    }
```





## Complete Examples

### Example 1: Simple Sensor Mapping

**Source Model:**
```
SensorData
├── deviceId: String
├── timestamp: Date
└── batteryLevel: Double
```

**Mapping:**
```xml
<sensinactMapping:ProviderMapping mid="battery-sensor">
  <name>
    <featurePath href="sensor.ecore#//SensorData/deviceId"/>
  </name>

  <providerClasses href="sensor.ecore#//SensorData"/>

  <services mid="battery">
    <name name="Battery"/>
    <timestamp strategy="FEATURE">
      <featurePath href="sensor.ecore#//SensorData/timestamp"/>
    </timestamp>

    <resources name="level" unit="%" mid="level">
      <eType href="http://www.eclipse.org/emf/2002/Ecore#//EDouble"/>
      <valueFeature href="sensor.ecore#//SensorData/batteryLevel"/>
    </resources>
  </services>
</sensinactMapping:ProviderMapping>
```

### Example 2: Nested Object Navigation

**Source Model:**
```
UplinkMessage
├── deviceInfo
│   └── deviceName: String
└── object (M5MoistureUplink)
    └── object (M5MoistureSensorData)
        └── moisture (M5MoistureReading)
            ├── percent: Int
            └── raw: Int
```

**Mapping:**
```xml
<sensinactMapping:ProviderMapping mid="moisture-sensor">
  <!-- Provider name from nested reference -->
  <name>
    <featurePath href="lorawan.ecore#//UplinkMessage/deviceInfo"/>
    <featurePath href="lorawan.ecore#//DeviceInfo/deviceName"/>
  </name>

  <providerClasses href="moisture.ecore#//M5MoistureUplink"/>

  <services mid="soilmoisture">
    <name name="Soil Moisture"/>

    <!-- Navigate deep into nested structure -->
    <resources name="moisturePercent" unit="%" mid="moisture-percent">
      <eType href="http://www.eclipse.org/emf/2002/Ecore#//EInt"/>
      <valueFeature href="moisture.ecore#//M5MoistureUplink/object"/>
      <valueFeature href="moisture.ecore#//M5MoistureSensorData/moisture"/>
      <valueFeature href="moisture.ecore#//M5MoistureReading/percent"/>
    </resources>
  </services>
</sensinactMapping:ProviderMapping>
```

### Example 3: Collection-Based Weather Mapping

**Source Model:**
```
WeatherReports
├── id: String
└── reports: List<WeatherReport>
    ├── [0] (current weather)
    ├── [1] (3H forecast)
    └── [2] (6H forecast)
```

**Mapping:**
```xml
<sensinactMapping:ProviderMapping mid="weather-provider">
  <!-- Provider name from first report -->
  <name collectionIndex="0">
    <featurePath href="weather.ecore#//WeatherReports/reports"/>
    <featurePath href="weather.ecore#//WeatherReport/weatherStation"/>
    <featurePath href="weather.ecore#//WeatherStation/id"/>
  </name>

  <providerClasses href="weather.ecore#//WeatherReports"/>

  <!-- Current Weather: reports[0] -->
  <services mid="currentWeather" collectionIndex="0">
    <name name="Current Weather"/>
    <collectionFeature href="weather.ecore#//WeatherReports/reports"/>

    <resources name="windSpeed" unit="m/s" mid="windSpeed">
      <eType href="http://www.eclipse.org/emf/2002/Ecore#//EFloat"/>
      <valueFeature href="weather.ecore#//WeatherReport/windSpeed"/>
    </resources>

    <resources name="temperature" unit="K" mid="temperature">
      <eType href="http://www.eclipse.org/emf/2002/Ecore#//EFloat"/>
      <valueFeature href="weather.ecore#//WeatherReport/tempAboveSurface5"/>
    </resources>
  </services>

  <!-- 3H Forecast: reports[1] -->
  <services mid="forecast3H" collectionIndex="1">
    <name name="3H Forecast"/>
    <collectionFeature href="weather.ecore#//WeatherReports/reports"/>

    <!-- Same resources as currentWeather, different source -->
    <resources name="windSpeed" unit="m/s" mid="windSpeed">
      <eType href="http://www.eclipse.org/emf/2002/Ecore#//EFloat"/>
      <valueFeature href="weather.ecore#//WeatherReport/windSpeed"/>
    </resources>
  </services>

  <!-- Admin: metadata from reports[0] -->
  <admin xsi:type="sensinactMapping:AdminMapping" mid="admin" collectionIndex="0">
    <collectionFeature href="weather.ecore#//WeatherReports/reports"/>
    <friendlyNameFeature href="weather.ecore#//WeatherReport/station"/>
    <friendlyNameFeature href="weather.ecore#//Station/name"/>
    <providerPackage href="weather.ecore#/"/>
  </admin>
</sensinactMapping:ProviderMapping>
```

---

## Best Practices

### 1. Mapping Organization

**DO:**
- Use descriptive `mid` values: `"temperature-sensor"` not `"ts1"`
- Group related resources in logical services
- Add XML comments to explain complex mappings

**DON'T:**
- Mix unrelated resources in one service
- Use generic names like "service1", "resource1"

### 2. Collection Mapping

**DO:**
- Validate that your source data always has the required collection size
- Use explicit `collectionIndex` even when default (0) for clarity
- Test with edge cases (empty collections, single-element collections)

**DON'T:**
- Assume collections will always have data
- Use the same service name for different collection elements
- Forget to update validation when adding services with new indices

### 3. Type Conversion

**DO:**
- Specify explicit `eType` for all resources
- Use appropriate EMF data types (EFloat, EInt, EString, etc.)
- Test with real data to ensure conversions work

**DON'T:**
- Rely on implicit type conversion
- Mix incompatible types (e.g., String to Float without validation)

### 4. Timestamp Handling

**DO:**
- Prefer resource-level timestamps for granular updates
- Use provider-level timestamp only when all data is truly synchronized
- Ensure timestamp format matches your source data

**DON'T:**
- Mix provider and resource timestamps unnecessarily
- Use `NOW` strategy if source data has timestamps

### 5. Performance

**DO:**
- Keep feature paths as short as possible
- Validate mappings at startup to catch errors early
- Use the `ValidationResult` API before production mapping

**DON'T:**
- Create deeply nested feature paths unnecessarily
- Ignore validation warnings

---

## Troubleshooting

### Common Issues

#### 1. "Instance type does not match expected provider classes"

**Cause:** The source object's EClass doesn't match any `providerClasses` in the mapping.

**Solution:**
```xml
<!-- Ensure providerClasses includes all possible types -->
<providerClasses href="model.ecore#//BaseClass"/>
<providerClasses href="model.ecore#//ExtendedClass"/>
```

#### 2. "Collection index X out of bounds"

**Cause:** The collection doesn't have enough elements for the specified index.

**Solution:**
- Validate input data before mapping
- Adjust `collectionIndex` to match available data
- Add validation in your data source

```java
// Validate before mapping
if (weatherReports.getReports().size() < 2) {
    throw new IllegalArgumentException("Need at least 2 weather reports");
}
```

#### 3. "Feature path returns no value"

**Cause:** Navigation through feature path failed (null reference or empty collection).

**Solution:**
- Check that all intermediate references are populated
- Verify feature paths using EMF reflection
- Add null checks in your data preparation

```java
// Debug feature path navigation
EObject current = sourceObject;
for (EStructuralFeature feature : featurePath) {
    Object value = current.eGet(feature);
    System.out.println(feature.getName() + " = " + value);
    if (value instanceof EObject) {
        current = (EObject) value;
    }
}
```

#### 4. "Service not found"

**Cause:** Service `mid` doesn't match registered provider model.

**Solution:**
- Ensure mapping is registered: `mappingRegistry.registerModelMapping(mapping)`
- Check service `mid` values match mapping definition
- Verify provider model was created correctly

### Debugging Tips

#### Enable Debug Logging

```java
// Set log level for ValueMapperImpl
Logger logger = Logger.getLogger("org.gecko.emf.sensinact.model.impl");
logger.setLevel(Level.DEBUG);
```

#### Validate Before Mapping

```java
ValueMapper mapper = ValueMapperFactory.createValueMapper(twin, mapping);
ValueMapper.ValidationResult result = mapper.validateInstance(sourceObject);

if (!result.isValid()) {
    System.err.println("Validation errors:");
    result.getErrors().forEach(System.err::println);
}

if (!result.getWarnings().isEmpty()) {
    System.out.println("Validation warnings:");
    result.getWarnings().forEach(System.out::println);
}
```

#### Test Feature Paths Independently

```java
// Test feature path extraction
NameMapping nameMapping = mapping.getName();
Optional<Object> value = getRawValue(sourceObject, nameMapping);
System.out.println("Provider name: " + value.orElse("NOT FOUND"));
```

---

## API Reference

### Core Classes

#### `ValueMapper`
Main interface for mapping operations.

```java
void mapInstance(EObject sourceInstance) throws ValueMappingException;
Map<String, Object> mapResourceValues(EObject sourceInstance);
ValidationResult validateInstance(EObject sourceInstance);
```

#### `ValueMapperFactory`
Factory for creating mappers.

```java
static ValueMapper createValueMapper(
    SensinactDigitalTwin twin,
    ProviderMapping mapping
);
```

#### `ProviderMappingRegistry`
Registry for managing mappings.

```java
void registerModelMapping(ProviderMapping mapping);
List<ProviderMapping> getProviderMapping(EClass eClass);
void dispose();
```

#### `ValidationResult`
Validation result container.

```java
boolean isValid();
List<String> getErrors();
List<String> getWarnings();
```

### Usage Example

```java
// 1. Load mapping
Resource mappingResource = resourceSet.createResource(
    URI.createURI("file:///path/to/mapping.xmi"));
mappingResource.load(null);
ProviderMapping mapping = (ProviderMapping) mappingResource.getContents().get(0);

// 2. Register mapping
mappingRegistry.registerModelMapping(mapping);

// 3. Execute mapping in SensiNact gateway thread
gatewayThread.execute(new AbstractSensinactCommand<Boolean>() {
    @Override
    protected Promise<Boolean> call(
            SensinactDigitalTwin twin,
            SensinactModelManager modelManager,
            PromiseFactory pf) {
        try {
            // Create mapper
            ValueMapper mapper = ValueMapperFactory.createValueMapper(twin, mapping);

            // Validate (optional but recommended)
            ValidationResult validation = mapper.validateInstance(sourceData);
            if (!validation.isValid()) {
                throw new ValueMappingException("Validation failed: " +
                    validation.getErrors());
            }

            // Map to SensiNact
            mapper.mapInstance(sourceData);

            return pf.resolved(true);
        } catch (Exception e) {
            return pf.failed(e);
        }
    }
});
```

---

## Migration from QVT

If you're migrating from QVT transformations to declarative mappings:

### QVT vs. Mapping Comparison

| Aspect | QVT Transformation | Declarative Mapping |
|--------|-------------------|---------------------|
| **Definition** | Imperative code (QVT-O) | Declarative XMI configuration |
| **Hot Reload** | ❌ No (requires recompilation) | ✅ Yes |
| **Code Size** | 150+ lines typical | 50-100 lines XMI |
| **Maintenance** | Code changes + rebuild | Edit XMI file |
| **Debugging** | QVT debugger required | Standard EMF validation |
| **Reusability** | Limited | High (same infrastructure) |
| **Learning Curve** | QVT-O language | XML + EMF basics |

### Migration Steps

1. **Identify Source and Target Models**
   - Source: Your domain EMF model
   - Target: SensiNact provider structure

2. **Map QVT Transformations to Services**
   ```qvto
   // QVT
   currentWeather := self.reports->at(1).map toWeatherService();
   ```

   ```xml
   <!-- Mapping -->
   <services mid="currentWeather" collectionIndex="0">
     <collectionFeature href="...WeatherReports/reports"/>
   </services>
   ```

3. **Convert Field Mappings to Resources**
   ```qvto
   // QVT
   windSpeed := self.windSpeed;
   ```

   ```xml
   <!-- Mapping -->
   <resources name="windSpeed" unit="m/s" mid="windSpeed">
     <valueFeature href="...#//WeatherReport/windSpeed"/>
   </resources>
   ```

4. **Test Incrementally**
   - Start with simple services
   - Add validation tests
   - Verify against QVT output

---

## Version History

### Version 1.2 (Current)
- ✅ Added **ReferenceMapping** for automatic resource generation
- ✅ **temporaryResources** support for auto-generated ResourceMappings
- ✅ **targetEClass** attribute to handle inheritance hierarchies
- ✅ Nested reference mapping with referenceMappings
- ✅ Filter configuration (include/exclude patterns)
- ✅ 96% reduction in mapping XMI size for complex types
- ✅ Comprehensive documentation and examples

### Version 1.1
- ✅ Added collection mapping support
- ✅ Service-level collection navigation
- ✅ Feature path collection indices
- ✅ Enhanced validation with collection bounds checking
- ✅ Comprehensive test coverage

### Version 1.0
- ✅ Basic provider mapping
- ✅ Service and resource mapping
- ✅ Feature path navigation
- ✅ Admin service support
- ✅ Timestamp strategies
- ✅ Type conversion

---

## Additional Resources

- **EMF Documentation**: https://www.eclipse.org/modeling/emf/
- **SensiNact Documentation**: https://eclipse.org/sensinact/
- **Example Mappings**: See `org.gecko.emf.sensinact.tests/data/` directory
- **Test Cases**: See `org.gecko.emf.sensinact.tests/src/.../ValueMapperTest.java`
- **Weather Mapping Example**: See `WeatherReportsProviderMapping.xmi` in test data

---

## Support

For issues, questions, or contributions:
- **Email**: support@data-in-motion.biz

---

*Last Updated: 2025-01-11*
*Version: 1.2.0*
