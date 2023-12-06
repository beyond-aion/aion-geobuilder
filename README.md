# Aion GeoBuilder

Aion GeoBuilder filters relevant level data from the Aion game client and converts it into compact files for the server emulator. It is a rewrite of GeoDataBuilderJ and parses the following data:
- Terrain heightmaps
- Terrain surface materials
- Static entities: [brushes](https://web.archive.org/web/20150131204653/http://docs.cryengine.com/display/SDKDOC2/Brushes) and objects (vegetation)
- Dynamic entities: doors, town objects, event objects and spawnable objects with entity IDs (called "static_id" in most Aion server emulators)
- Mesh materials/collision intentions

Currently not supported:
- Animated entities other than doors (elevators, air ships, etc.)
- Navmesh generation


## Prerequisites

- Java 21 or later
- Maven


## Usage

```sh
mvn package
java -jar target/geobuilder.jar "path/to/the/game/client"
```
Optional arguments control what data will be processed. Run the program without arguments to see all options.


## Info resources

- [PyFFI + QSkope (*.cgf/cga file analyzer)](https://github.com/niftools/pyffi) 
- [CryEngine asset converter](https://github.com/Markemp/Cryengine-Converter) (partly based on PyFFI's [cgf.xml](https://github.com/niftools/pyffi/blob/develop/pyffi/formats/cgf/cgf.xml))
- [CryEngine source code](https://github.com/MergHQ/CRYENGINE)
- [C# port of GeoDataBuilderJ, client file viewer and Monono2 (level viewer)](https://github.com/zzsort/monono2)
- [GeoDataBuilderJ_src_20120911.zip](https://app.assembla.com/spaces/aion-ger-emulator/subversion/source/HEAD/trunk/Tools/GeoDataBuilderJ/GeoDataBuilderJ_src_20120911.zip?_format=raw) (original repo is lost in time)