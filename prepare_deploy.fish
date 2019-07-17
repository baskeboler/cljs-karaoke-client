#!/usr/bin/env fish

shadow-cljs release app
rm -rf public/js/cljs-runtime
git branch -f gh-pages
git checkout gh-pages
git reset --hard origin/master
mkdir tmp
cd tmp
mv ../* .
cd ..
mv tmp/public/* .


# git checkout gh-pages
# cp -rf build/Release/* .

echo "Built project and copied build into root of gh-pages, ready to commit."
