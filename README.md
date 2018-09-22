# Movie search using a unidirectional state flow pattern

This is an attempt at coming up with a unidirectional state flow pattern that uses the concepts
of patterns like Redux, Cycle.js, MVI etc.

I wanted to achieve the benefits of this pattern without necessarily introducing any new library
(aside from Rx) and keeping the boilerplate to an absolute minimum.

This is a simple movie searching app. Clicking the movie result populates a history list. The idea
is to try and add a somewhat reasonably meaty app with business logic content that demonstrates this
 pattern outside of a simple Hello World use case.

We use the wonderful [OMDB api](http://www.omdbapi.com) to fetch movie information.

## Setting up your OMDB API KEY

There are quotas on this api, so please don't use mine :)

1. Get an [api key for OMDB here](http://www.omdbapi.com/apikey.aspx)
2. Run this in a terminal like application

```
touch $HOME/.gradle/gradle.properties
echo "omdb_apikey=\"<API_KEY_GOES_HERE>\"" >> $HOME/.gradle/gradle.properties
```

You can read [this post for instructions](https://medium.com/code-better/hiding-api-keys-from-your-android-repository-b23f5598b906) on this private api
setting up process.

For great movie recommendations, ping me [@kaushikgopal](https://twitter.com/kaushikgopal) (seriously, I watch a love of movies).
