#Draw SVG Demo

This app demonstrates how a user could generate SVG paths using the touch screen of a mobile device.  Basic drawing features are implemented:

* Drawing with single finger
* Undo last path drawn
* Clear entire canvas
* Share canvas as SVG attached in email

##Installation

Best way to install is via [Google Play Store](https://play.google.com/store/apps/details?id=joe.amrhein.drawsvgdemo)

##Caveats

* Demo currently does not support screen rotations.  Canvas will reset on rotation
* Demo does not attempt to close paths or assist user in drawing
* Demo does not attempt to scale the SVG.  Output SVG is a 1:1 match to the pixel dimensions of the device
* Demo uses Bezier curves for interpolating between touch sample points, but only uses ```lineto``` option in SVG output file


##Acknowledgements

Special thanks to the following resources for inspiration

* [Android-signaturepad](https://github.com/gcacace/android-signaturepad)
* [Smooth signatures](https://corner.squareup.com/2010/07/smooth-signatures.html) blog post by Square, Inc
* [Smoother signatures](https://corner.squareup.com/2012/07/smoother-signatures.html) blog post by Square, Inc