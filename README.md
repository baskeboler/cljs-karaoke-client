# cljs-karaoke-client

[![Netlify Status](https://api.netlify.com/api/v1/badges/8cdcf70f-1a6c-40bb-a3f0-02cb24eda852/deploy-status)](https://app.netlify.com/sites/karaoke-player/deploys)
[![Nyancat Compliance Status](https://img.shields.io/badge/nyancat%20compliance-ridiculous-brightgreen)](https://www.youtube.com/watch?v=QH2-TGUlwu4)

A web karaoke player implemented in clojurescript

## Demo 

[Rolling Stones - All Over Now](https://karaoke-player.netlify.app/songs/Rolling%20Stones-all%20over%20now%20rolling%20stones.html)

## Description 

My try at making a decent karaoke player. 
For years I have tried to get my hands on a good software karaoke solution with little success, the main alternatives out there are either games like Ultrastar, Frets on Fire, etc or putting together a playlist of YouTube karaoke videos. 

Both alternatives offer a broad catalog of songs you can access freely across the internet but with some disadvantages:

- Songs available online for games like Ultrastar more often than not use the original song track with vocals which in my opinion sort of kills the "karaoke experience"
- Lyrics synchronization varies greatly (some just render whole verses at once while others have the lyrics synced at the syllable level) when using YouTube karaoke videos.

The main difficulty was to obtain lyrics with sync information, I found a lot of midi files on the web, apparently used for karaoke machines. These files, contain lyrics synced all the way down to the syllable level. Since there really is no standard on how to store lyrics on midi files for karaoke machines, many have subtle differences in the way they are stored so when parsing these files I made a bunch of gross assumptions and extracted the lyrics to separate files along with the timing data. Most are OK, but many are a bit off by a variable offset

The midi parser is on a separate project: [clj-karaoke](https://github.com/baskeboler/clj-karaoke-lyrics).

Another difficulty was obtaining the song audio track, since the lyrics are synced to the midi audio track, the sanest thing to do was to use that. Initially I intended to use the midi files directly for the audio tracks but when playing the audio, the quality depended greatly on the sound fonts you have installed and setting up a good env for decent results is not a trivial task. Also if I wanted to play these files on a web client, taking all this into account just made things more complicated. So I settled with extracting the lyrics to separate files and rendering the midis to mp3 files using [Timidity](http://timidity.sourceforge.net) which is pretty awesome and I was able to experiment with different sound font collections.

## Features 

- Tons of songs
- Background images for songs are dynamically fetched from the web after a google search during song load (if none is cached)
  - Each user caches their local backgrounds and periodically pushes its urls to a mongo db, google custom search has a daily limit so I try to minimize its use.
  - Initialization data (lyrics offsets, bg images ) improves during builds by consolidating data from users
- Ability to sync lyrics by an offset in miliseconds (either from the control panel or by appending a query string to the url)
- Automatic playlist built from previously synced songs
- Export local song sync information so that it may be merged to the server-side sync data
- Experimental audio input for desktop firefox and chrome with live echo/reverb sound fx (use an external microphone for best results)
- Experimental webcam recording and video export to webm file. The audio channel in the exported video is the result of mixing the microphone input with effects and the song track.
- Remote control. Run the application on a big screen and control playback from a different application instance (for example, from your mobile device!)
- Build pre-renders song pages with SEO tags so you can share links in social networks as pretty looking cards with song name and an image if available.
  - ![Twitter card example](./docs/twittercard.png "example twitter card")
- Lyrics editor for syncing new tracks with lyrics.
  - select an audio file 
  - add text, split it into syllables (or just random chunks )
  - play your track and tap the "sync" button for each piece
  - if you want more accuracy you may slow down the track decreasing the playback rate

## Working on 

- Dynamic font sizes, already optimized font sizes for song titles but I am looking for a way to improve the lyrics display as well.
- A way to capture custom lyrics offsets from all users, integrating these as defaults in subsequent releases
- Same thing with song backgrounds, currently google custom search is used to find images related to the song title, there is a quota for this service and it is pretty low, the less we need to search the better.

## Running locally

Prerequisites: 
* JDK
* shadow-cljs

```bash
$ npm i -g shadow-cljs 
$ npm install
$ shadow-cljs watch app
```

If you want to compile a release build you may running the following: 

```bash 
$ shadow-cljs release app
```

The build will be located in the `/public` directory.

## Key Bindings

- "esc": stop playback
- "l r": load song
- "alt-o": enable optons in playback mode
- "alt-h": enable control panel mode
- "left": audio seek backwards 
- "right" audio seek forward
- "meta-shift-l": loop mode (now currently working) 
- "alt-shift-p": play 
- "shift-right": next song on playlist
- "t t": toasty!
- "h": show cheatsheet 

## Future work

- Raspberry PI image
- Lyrics sync editor
