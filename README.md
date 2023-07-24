# Movie search using a unidirectional state flow pattern

This is an attempt at coming up with a unidirectional state flow pattern that uses the concepts
of patterns like Redux, Cycle.js, MVI etc.

Many of my contemporaries have already done amazing work in this area and I've drawn a lot of lessons and learnings from their work already:

* [The state of managing state with RxJava](https://jakewharton.com/the-state-of-managing-state-with-rxjava/) - [JakeWharton](https://twitter.com/JakeWharton)
* [MVI patterns with Hannes Dorfmann](http://fragmentedpodcast.com/episodes/103/) - [Hannes Dorfmann](https://twitter.com/sockeqwe)
* [LCE: Modeling Data Loading in RxJava](https://tech.instacart.com/lce-modeling-data-loading-in-rxjava-b798ac98d80) - [Laimonas](https://twitter.com/ThatLime)

I wanted to achieve the benefits of this pattern without introducing any new libraries
or a new framework. How would one familiar with an MVVM model today leverage the principles/benefits of a unidirectional state/data flow? I hope to demo those concepts with this app.

![usf_animation.gif "picture showing the USF animation"](usf_animation.gif)


The app is a simple movie search app. Clicking the movie result populates a history list. While this is not an extremely complex app, it isn't a silly Hello World one either, so the hope is that it'll cover regular use cases for a basic application.

I've also started meaninful test cases in the repo.

## Setting up your OMDB API KEY

_We use the wonderful [OMDB api](http://www.omdbapi.com) to fetch movie information._

There are quotas on this api, so please don't use mine :)

1. Get an [api key for OMDB here](http://www.omdbapi.com/apikey.aspx)
2. Add it to you local.properties file (which shouldn't be checked in to a VCS) like so:

```
# local.properties
OMDB_API_KEY="<API_KEY_GOES_HERE>"
```

For great movie recommendations, ping me [@kau.sh](https://kau.sh) (seriously, I watch a lot of movies).

I gave a talk at [MBLT}Dev 2018](https://twitter.com/mbltdev) on how I went about building this app. [Slides can be found here](https://speakerdeck.com/kaushikgopal/unidirectional-state-flow-patterns-a-refactoring-story).

## Getting Started

This project now uses [ksp](https://kotlinlang.org/docs/ksp-overview.html) to reduce the boilerplate in wiring up a new feature. [This PR](https://github.com/kaushikgopal/movies-usf-android/pull/32) has the details for how this change was made.

All that's needed is writing the implementation of your ViewModel so `MyFeatureViewModelImpl: UsfViewModelImpl<E, R, VS, VE>` and adding the `@UsfViewModel` annotation. Your ViewModel boilerplate code will be auto-generated.

Take a look at [MSMovieViewModelImpl](https://github.com/kaushikgopal/movies-usf-android/blob/master/app/src/main/java/co/kaush/msusf/movies/MSMovieViewModelImpl.kt) for the View Model logic and [MSMovieActivity](https://github.com/kaushikgopal/movies-usf-android/blob/master/app/src/main/java/co/kaush/msusf/movies/MSMovieActivity.kt) to see how the `viewModel` is invoked.

# iOS app

I gave another talk at [Mobilization IX](https://twitter.com/mobilizationpl/status/1184008559157219328?s=20) showing how we can use the same concepts on iOS too and wrote [my first iOS app to demonstrate these concepts - You can check that out here](https://github.com/kaushikgopal/movies-usf-ios).
