# Demosystem Use-Cases

* 2 verschiedene Kontexte
  * SmartCity/Home like auf Golfplatz mit Sensorik [bis Barcelona]
  * Business Data mit Anbindung Drittsystemen

## SmartCity / -Home  [bis Barcelona]

* Bodenfeuchte für Büropflanzen/Werte aus Büropflanzen + Position Golfplatz [bis Barcelona]
* (Temperatur + CO2 für Büroräume)
* Beleuchtung auf Golfplatz - Wege (Shelly)
* Daanse Dahsboard mit Golfplatzplan und Positionierung Pflanzen  [bis Barcelona]
  * igolf zu ogc features transformieren [bis Barcelona]
* Wetterdaten mit Widget (s. Bodenfeuchte) integrieren  [bis Barcelona]
* Idee ist die Modelle neuer Sensor Hersteller automatisch per AI generieren und einbinden  [bis Barcelona]
* ggfs. Git Workflow etablieren und vor Freigabe das Sensinact Mapping per UI erstellen [bis Barcelona]

## Business Data

* Anbindung Toggle
* Anbindung Google Kalender
* Urlaubskalender auslesen und Arbeitszeiten für MA in Toggle automatisiert eintragen
* Krankenstand nachtragen im Google Kalender
* Feiertagskalender berücksichtigen
* Backend mit MA individuellen bevorzugten Arbeitszeiten, Urlaubstage und Arbeitstage lt. Vertrag
* Problem: Soll verpflichtende Arbeitszeit dokumentation automatisieren + Förderprojektabrechnungsgrundlage im Toggle
  * Verbessert unser KPI’s
  * Daanse Würfel erstellen um KPI zu visualisieren


## Nice To Have

* KPI Dashboard im Daanse für unserer Weekly + Dev-Weekly KPI’s
* Automatische privacy Anylse des Modells (Privacy Analysis Tooling / MDP)

## System

* ModelAtlas
* Sensinact
* Datenbank
* Daanse Dashboard
* Daanse Würfel -> BI für KPI
* MessageBroker

## Barcelona?!

* Jürgen Vorschlag:
* UI Dashboard für Büro, Temperatur + Pflanzen
* @Markus UI für Timetracking in Dashboard darstellbar?
* Ggfs Sensorik faken
* Ggfs. UI für Model Freigabe und Sensinact Mapping

## Workflow Neues Modell

* AI generiert Ecore anahand von empfangen Json (SensiNact)
* Backend erstellt neuen lokalen Branch (Data Atlas)
* Ecore wird in Branch gelegt und gepusht (Data Atlas)
* UI schaut in das Repo remote (Branch selection)
  *  UI macht fetch und schaut sich Brnaches an
  *  Solange es einen offen Branch gibt ist es ein Draft
  *  Branches werden nach merge gelöscht
* Anpassungen / REfinement in UI
  *  EAnnotatio0n Genmodel Documentation
  *  Codec - Type Key
  *  ggfs. im Hintergrund ModelAtlas Configuration anlegen /
  *  Commit und Push in den Branch
* UI hat Freigabe UI
  *  darüber triggert man den MErge
  *  PR erstellen, approven und mergen
* Git Webhook sagt ModelAtlas Bescheid
  *  Model wird registriert
* UI bekommt Event vom ModelAtlas über neues Modell
  *  UI hat Modell
  *  Mapping Modell wird erzeugt
  *  neuer Branch, Review, PR, Merge
* Sensinact bekommt webhook von Github für das Mapping
  *  REgistriert Mapping
  *  Daten werden gemappt
* UI bekommt über STA die Info das neue Thing da ist
  *  UI kann dann STA daten anzeigen
  * Golfplatz könnte STA oder plain Geojson sein
  * Daanse hat Composer das aus GeoJson als CSV per RESTR ausliefern / oder Plätze liegen als Geojson DAtei und sind über Endpount erreichbar
* RESET über UI, in dem Mapping-Modell, Modell wieder entfernt werden.

* Optional: UI Konfigruation der UI beim Mapping mitmachen

## IoT Broker Model Generation Workflow

### Problem Statement
Der IoT Broker empfängt JSON-Daten von unbekannten Sensoren/Geräten. Für diese Daten existiert noch kein EPackage zum Deserialisieren. Ein AI-Tool kann EPackages aus JSON-Samples generieren, aber es gibt eine Race Condition: Während der Modelgenerierung können weitere Daten des gleichen Formats ankommen.

### Requirements
* **Data Handling**: IoT Broker verwirft unbekannte JSON-Daten und triggert Modelgenerierung
* **Race Condition Prevention**: Mehrere JSON-Daten desselben Formats sollen nicht mehrfache Modelgenerierung auslösen
* **Generation Cache**: System muss erkennen, ob für ein JSON-Format bereits eine Generierung läuft
* **Release-based Processing**: Nur freigegebene (released) Modelle können im IoT Broker zur Deserialisierung verwendet werden
* **Deployment Flexibility**: Lokale OSGi-Calls oder REST API je nach Deployment-Szenario

### Detailed Workflow

#### 1. Unknown JSON arrives at IoT Broker
```
IoT Broker receives JSON → Check if EPackage exists for format
├── EPackage exists and RELEASED → Use for deserialization
├── EPackage exists but IN_PROGRESS → Drop data, wait for release
└── No EPackage → Trigger generation request
```

#### 2. Model Generation State Machine
* **GENERATION_REQUESTED** - Anfrage für AI-Generierung gestellt
* **GENERATION_IN_PROGRESS** - AI arbeitet an Modellerstellung  
* **READY_FOR_REVIEW** - Generiert, aber noch nicht approved/released
* **RELEASED** - Verfügbar für IoT Broker Deserialisierung
* **FAILED** - Generierung fehlgeschlagen

#### 3. JSON Pattern Recognition
* **Fingerprinting**: JSON-Struktur wird zu eindeutigem Identifier umgewandelt
* **Cache Lookup**: `JSON Fingerprint → Generation Status`
* **Duplicate Prevention**: Gleiche JSON-Struktur triggert nicht mehrfache Generierung

#### 4. Integration Scenarios

##### Local Deployment (Same JVM)
```java
// Direct OSGi service call
EPackageWorkflowService service;
service.findPackageByJsonPattern(jsonSample, sourceChannel)
  .then(registration -> processWithModel(registration))
  .recover(notFound -> requestGeneration(jsonSample));
```

##### Remote Deployment (Separate Systems)  
```http
POST /packages/check-pattern
Content-Type: application/json
X-Source-Channel: sensor-hub-1

{
  "jsonSample": "{ \"temperature\": 25.3, \"humidity\": 60 }",
  "similarity": 0.8
}

Response:
- 200: EPackage found and released
- 202: Generation in progress  
- 404: No matching pattern, trigger generation
```

#### 5. Cache Management
* **Fingerprint Generation**: Struktureller Hash der JSON-Eigenschaften
* **TTL**: Cache-Einträge mit Timeout für failed generations
* **Cleanup**: Entfernung von Cache-Einträgen nach successful release
* **Persistence**: Cache sollte Service-Restarts überleben

#### 6. Event Notifications
* **Generation Started**: IoT Broker wird über gestartete Generierung informiert
* **Model Released**: IoT Broker wird über verfügbares neues Modell benachrichtigt
* **Generation Failed**: IoT Broker wird über fehlgeschlagene Generierung informiert

### API Extensions Required

#### New Operations for EPackageWorkflowService
```java
Promise<ObjectMetadata> findPackageByJsonPattern(String jsonSample, String sourceChannel);
Promise<GenerationRequest> requestGenerationIfNeeded(String jsonSample, String sourceChannel, String requestingUser);
Promise<GenerationStatus> getGenerationStatus(String requestId);
Promise<List<ObjectMetadata>> searchPackagesByPattern(String jsonPattern, double similarity);
```

#### New Data Entities
* **GenerationRequest**: Request-ID, JSON sample, status, timestamps
* **GenerationStatus**: Current state, progress info, error messages  
* **JsonFingerprint**: Structural hash, original sample, metadata

### Technical Considerations
* **Performance**: Fingerprint-Berechnung muss schnell sein (< 10ms)
* **Accuracy**: Pattern-Matching muss false positives vermeiden
* **Scalability**: Cache muss für hohe IoT-Datenraten skalieren
* **Reliability**: Robuste Behandlung von Generation failures