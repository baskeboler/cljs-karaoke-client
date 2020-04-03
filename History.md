
n.n.n / 2020-04-03
==================



1.4.0 / 2020-04-02
==================

  * Merge branch 'master' of https://github.com/baskeboler/cljs-karaoke-client
  * readme and canonical tag update
  * Merge pull request #20 from baskeboler/develop
  * styles
  * playback controls styles
  * dynamic text size for song titles
  * Merge pull request #19 from baskeboler/mongo
  * remove commented code
  * new urls redirect in prerrenders
  * added redirects file
  * remove comments
  * initial refactor of components into modules, new routing
  * updates and mongo client
  * Merge branch 'master' of https://github.com/baskeboler/cljs-karaoke-client
  * Merge branch 'develop'
  * toasty on mobile
  * pageloader
  * mobile tweaks and page loader fix
  * update sitemap
  * changed canonical
  * added canonical header to seo pages
  * disable remote control
  * Merge pull request #18 from baskeboler/develop
  * Merge pull request #17 from baskeboler/develop
  * Merge pull request #13 from baskeboler/develop
  * Merge pull request #12 from baskeboler/develop
  * Merge pull request #11 from baskeboler/add-license-1
  * Create LICENSE
  * Merge pull request #10 from baskeboler/develop
  * Merge pull request #9 from baskeboler/develop
  * Merge pull request #8 from baskeboler/develop
  * Merge pull request #7 from baskeboler/develop

v1.3.0 / 2020-03-18
===================

  * updated package.json deps
  * styles fixes
  * update deps
  * added share link in playback mode
  * covers updates
  * update build script
  * added cover images and updated bgs
  * deps updates
  * editor commits
  * dep updates
  * added sogn info modal to menu
  * added delay select modal to control panel menu
  * menu items
  * delays
  * build seo pages
  * added custom seo card images
  * changed sync dispatches to async
  * menus
  * Merge pull request #16 from baskeboler/sass-migration
  * bing
  * added sitemap to build
  * remove comments
  * migrated css to sass, all styles are now bundled together.
  * fix seo pages with offsets and new events
  * a little editor cleanup
  * editor stats subs
  * delays
  * superpancho
  * add editor screenshot
  * control panel layout
  * Merge pull request #15 from baskeboler/editor
  * icons
  * Merge pull request #14 from baskeboler/editor
  * hide some buttons in playback mode. move song bg list to this codebase
  * rudimentary lyrics editor with import/export capabilites. addes new song with lyrics created in builtin editor.
  * file select
  * editor
  * editor WIP
  * update re-frame-10x dep
  * refactor out some component from app.cljs
  * lyrics editor
  * lyrics editor work in progress
  * change og:type meta tag to website
  * seo
  * seo
  * add songs dir
  * added seo pages to build
  * fix loader css
  * fix build error
  * replaced loader logo
  * changed build
  * remove debug code
  * added lyrics sync progress indicator
  * improve lyrics display. show frames a little bit before so you have time to read
  * song name display
  * removed google analytics from index.html. configured in deployment in netlify
  * moved delays file into the project for simpler updates, added remove song from default playlist in playlist view
  * improved transitions when navigating to songs, added billboards to display announcements, introductions, possible ads for limited time spans.
  * save synced lyrics automatically to localstorage
  * added kb shortcuts for lyrics sync'ing and user info in state
  * play button z-index and bg
  * added frame positions sub
  * updated http-relay endpoint and trying out new pageloader animations
  * added failback background image when song bg fails to load
  * removed specter dependency.
  * fix pagination bug
  * externalized song list to edn file, added delay to page loader exit animation, added event for loading song list at initialization and other minor tweaks
  * removed logo animation
  * removed bulma extensiosn
  * playlist load fix
  * removed bulma extensions and built custom loader
  * added more css animations
  * fix lyrics highlighting animation
  * cleanup, remove/commented dead code
  * simplified lyrics highlighting logic, no longer using core.async for that.
  * Merge branch 'develop' of https://github.com/baskeboler/cljs-karaoke-client into develop
  * responsive styles
  * Merge branch 'master' into develop
  * disable search when it fails
  * fix styles and enable iamge search
  * fixed next track icon and replaced load song with play icon

v1.2.0 / 2019-08-13
===================

  * 1.2.0
  * Merge pull request #6 from baskeboler/develop
  * Update README.md
  * get full waveform buffer
  * ios fix
  * anims
  * object alt text
  * remove dev code
  * animate  lazily
  * doall on the song list
  * webaudio work
  * fix ios
  * fix alt text
  * fix event
  * ios

v1.1.4 / 2019-08-08
===================

  * 1.1.4
  * alt text for some imagealt text for some imagess

v1.1.3 / 2019-08-08
===================

  * 1.1.3
  * ios fix

v1.1.2 / 2019-08-08
===================

  * 1.1.2
  * fix header logo on mobile

v1.1.1 / 2019-08-08
===================

  * 1.1.1
  * audio mix

v1.1.0 / 2019-08-08
===================

  * 1.1.0
  * audio track mix song and mic for recording
  * added webcam video/audio recording
  * webcam support
  * fixanimation bugs
  * btn for camera
  * added styles to video
  * anims
  * animation tweaks
  * transform interpolations
  * interpolate protocol for colors
  * ani
  * animations
  * anims
  * delays
  * animation changes
  * animations
  * playlist ops
  * header logo
  * logo animation with bardo
  * Merge branch 'master' of https://github.com/baskeboler/cljs-karaoke-client into develop
  * playlist view and embedded logo for animations
  * added playlist view and contextual transitions events
  * css tweak
  * Merge pull request #5 from baskeboler/develop
  * fix audio input for firefox
  * css
  * css fix
  * add song to current playlist when sync offset is set
  * added smooth transition to spetrograph bars
  * fix incorrect localstorage merge with remote data
  * viz stuff
  * Merge branch 'master' of https://github.com/baskeboler/cljs-karaoke-client into develop
  * fix bad merge
  * merge
  * merge
  * viz stuff
  * fix bad ref
  * cleanup
  * async
  * spectrograph
  * robots.txt
  * Merge branch 'develop'
  * images for docs
  * Merge pull request #4 from baskeboler/develop
  * cleanup
  * remove dead code
  * Merge branch 'develop' of https://github.com/baskeboler/cljs-karaoke-client into develop
  * migrated audio input logic to re-frame events and async-flow added interval fx for analyser refresh
  * Update README.md
  * voice spectrogram
  * added notification of success after audio input init
  * added audio input button
  * moved audio input code to separate file
  * audio input reverb effect
  * added remote ctrl to ctrl panel
  * missing files
  * added notifications
  * clean up remote ctrl settings
  * Merge pull request #3 from baskeboler/develop
  * remote play
  * remote control
  * initial remote control code with httprelay.io
  * Update README.md
  * Merge pull request #2 from baskeboler/develop
  * ios sync
  * sync playback in ios
  * refactor lyrics loading to separate ns
  * fix toasty ios
  * deploy dev
  * background handling flow refactor
  * switched to jpg images for ios support
  * Merge branch 'develop'
  * Merge branch 'master' of https://github.com/baskeboler/cljs-karaoke-client
  * Merge branch 'master' of https://github.com/baskeboler/cljs-karaoke-client into develop
  * font change
  * Merge pull request #1 from baskeboler/develop
  * clean up code
  * key bindings in readme
  * Merge branch 'master' into develop
  * trigger toasty when shaking device
  * refactor
  * demo url
  * forward declaration
  * attempt to fix playback in ios
  * changes js Audio obj to audio html elements changed images to webp format
  * seek button components
  * added seek buttons with hotspots on right/left screen borders to show/hide
  * spellcheck
  * added readme and package-lock.json
  * added logo to page loader
  * fix seek
  * add travis config
  * remove generated files
  * initial commit
