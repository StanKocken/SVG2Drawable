SVG2Drawable
============

Use a jar executable to create a Drawable class to display a SVG on Android.

## Why?

I want to use a scalable image format.

A SVG sounds really good but it's a bit long to load (mainly the time to parse the XML) and I didn't find any library that support the Hardware acceleration.

## How does it work?

My main goal is to have a [Drawable](http://developer.android.com/reference/android/graphics/drawable/Drawable.html) class:

* Made a fork of the [svg-android](https://code.google.com/p/svg-android/) library.
* Replaced any instructions to create a [Picture](http://developer.android.com/reference/android/graphics/Picture.html) to collect them into a Logger.
* Print this Logger.
* Save it to a Drawable java class.

## How to use it?

You can read the code but I'm not proud of it. I started to do a POC for a basic usage and now I need to transform the code to make it more readable and robust.

You can simply use the [jar provided into this repo](https://github.com/StanKocken/SVG2Drawable/blob/master/svg_converter.jar) by specify:

* the SVG file
* the package where you want to put the Drawable
* the drawable name

You can also specify the output of the script to create a file. By adding "> filename.java".


    java -jar svg_converter.jar example.svg com.skocken.svg.example.drawable ExampleDrawable > ExampleDrawable.java

Now, copy this "ExampleDrawable.java" into your Android project, into the package "com.skocken.svg.example.drawable" (from this example), and add it to your ImageView into your code with:

    imageview.setImageDrawable(new ExampleDrawable());

## License

* [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

## Contributing

If you found a SVG with a problem, you can open an issue. Please, if possible, let me know if this same SVG work with the library [svg-android](https://code.google.com/p/svg-android/).

Please fork this repository and contribute back using
[pull requests](https://github.com/StanKocken/SVG2Drawable/pulls).

Any contributions, large or small, major features, bug fixes, additional
language translations, unit/integration tests are welcomed and appreciated
but will be thoroughly reviewed and discussed.
