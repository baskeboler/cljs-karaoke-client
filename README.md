# cljs-karaoke-client

[![Netlify Status](https://api.netlify.com/api/v1/badges/8cdcf70f-1a6c-40bb-a3f0-02cb24eda852/deploy-status)](https://app.netlify.com/sites/karaoke-player/deploys)

A web karaoke player implemented in clojurescript

## Demo

[Rolling Stones - All Over Now](https://karaoke.uyuyuy.xyz/#/songs/Rolling%20Stones-all%20over%20now%20rolling%20stones?offset=0)

## Description 

My try at making a decent karaoke player. 
For years I have tried to get my hands on a good software karaoke solution with little success, the main alternatives out there are either games like Ultrastar, Frets on Fire, etc or putting together a playlist of YouTube karaoke videos. 

Both alternatives offer a broad catalog of songs you can access freely across the internet but with some disadvantages:

- Songs available online for games like Ultrastar more often than not use the original song track with vocals which in my opinion sort of kills the "karaoke experience"
- Lyrics synchronization varies greatly (some just render whole verses at once while others have the lyrics synced at the syllable level) when using YouTube karaoke videos.

The main difficulty was to obtain lyrics with sync information, I found a lot of midi files on the web, apparently used for karaoke machines. These files, contain lyrics synced all the way down to the syllable level. Since there really is no standard on how to store lyrics on midi files for karaoke machines, many have subtle differences in the way they are stored so when parsing these files I made a bunch of gross assumptions and extracted the lyrics to separate files along with the timing data. Most are OK, but many are a bit off by a variable offset

The midi parser is on a separate project: [clj-karaoke](https://github.com/baskeboler/clj-karaoke-lyrics).

Another difficulty was obtaining the song audio track, since the lyrics are synced to the midi audio track, the sanest thing to do was to use that. Initially I intended to use the midi files directly for the audio tracks but when playing the audio, the quality depended greatly on the sound fonts you have installed and setting up a good env for decent results is not a trivial task. Also if I wanted to play these files on a web client, taking all this into account just made things more complicated. So I settled with extracting the lyrics to separate files and rendering the midis to mp3 files using [Timidity](http://timidity.sourceforge.net) which is pretty awesome and I was able to experiment with different sound font collections.

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

## Future work

- Audio input 
- Raspberry PI image
- Lyrics sync editor
