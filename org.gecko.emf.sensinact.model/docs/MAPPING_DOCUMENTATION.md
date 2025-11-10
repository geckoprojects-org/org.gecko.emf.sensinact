# SensiNact Mapping Framework Documentation

## Table of Contents
1. [Overview](#overview)
2. [Core Concepts](#core-concepts)
3. [Mapping Architecture](#mapping-architecture)
4. [Feature Reference](#feature-reference)
5. [Collection Mapping](#collection-mapping)
6. [Complete Examples](#complete-examples)
7. [Best Practices](#best-practices)
8. [Troubleshooting](#troubleshooting)

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

### Version 1.1 (Current)
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
- **Model Atlas Project**: See ARCHITECTURE.md in project root
- **Example Mappings**: See `org.gecko.mac.sensor.mapping/mapping/` directory
- **Test Cases**: See `org.gecko.emf.sensinact.tests/src/.../ValueMapperTest.java`

---

## Support

For issues, questions, or contributions:
- **Project Repository**: https://github.com/geckoprojects-org/model-atlas-cloud
- **Issue Tracker**: GitHub Issues
- **Email**: support@data-in-motion.biz

---

*Last Updated: 2025-11-07*
*Version: 1.1.0*
