# GazeAR

GazeAR utilizes gaze tracking technology to apply augmented reality to a user's field of vision. The software works by tracking user's eye movements and using that data to determine where in the user's field of view to overlay digital information. This information could include text, graphics or, as in the example, 3D models. 

The project includes code for both tha gaze tracking software and the augmented reality overlay, futhermore uses both the front and the back camera of a smartphone. 
Developers can use this code as starting point for creating their own gaze tracking augmented reality applications. 

GazeAR has the potential to be used ina variety of application, from gaming and entertainment to education and training. For example, a game could use the technology to create an immersive experience where the player must use their gaze to interact with the game world.

Overall, GazeAR is an exciting project that demonstrates the power of combining gaze tracking technology with augmented reality.

## External Dependencies

In the current version, all the dependecies are already installed.

## Hardware Dependencies

Some hardwares are not capable enough to give previews from both front and rear cameras simultaneously. Visit https://proandroiddev.com/can-we-use-the-front-back-camera-at-the-same-time-on-android-3e6558c1b12a for more information.

## Technologies

Name  | Link
------------- | -------------
SeeSo | https://seeso.io/
Camera2  | https://developer.android.com/reference/android/hardware/camera2/package-summary
ARCore | https://github.com/google-ar/arcore-android-sdk/tree/master/samples/hello_ar_java

## Project Setup

Clone this repository and import into **Android Studio**.

```bash
  git clone https://github.com/Plettrus/GazeAR
```
Get a license key from https://manage.seeso.io and copy your key to [`GazeTrackerManager.java`](https://github.com/Plettrus/GazeAR/blob/main/app/src/main/java/com/google/ar/core/examples/java/services/GazeTrackerManager.java "services/GazeTrackerManager.java").

```java
  // TODO: change licence key
String SEESO_LICENSE_KEY = "your license key";
```
You can find more information about this step in https://github.com/visualcamp/seeso-sample-android.
 
