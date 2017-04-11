
# react-native-rigo-rfid

## Getting started

`$ npm install react-native-rigo-rfid --save`

### Mostly automatic installation

`$ react-native link react-native-rigo-rfid`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-rigo-rfid` and add `RNRigoRfid.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNRigoRfid.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.rigoiot.RNRigoRfidPackage;` to the imports at the top of the file
  - Add `new RNRigoRfidPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-rigo-rfid'
  	project(':react-native-rigo-rfid').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-rigo-rfid/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-rigo-rfid')
  	```

#### Windows
[Read it! :D](https://github.com/ReactWindows/react-native)

1. In Visual Studio add the `RNRigoRfid.sln` in `node_modules/react-native-rigo-rfid/windows/RNRigoRfid.sln` folder to their solution, reference from their app.
2. Open up your `MainPage.cs` app
  - Add `using Cl.Json.RNRigoRfid;` to the usings at the top of the file
  - Add `new RNRigoRfidPackage()` to the `List<IReactPackage>` returned by the `Packages` method


## Usage
```javascript
import RNRigoRfid from 'react-native-rigo-rfid';

// TODO: What do with the module?
RNRigoRfid;
```
  