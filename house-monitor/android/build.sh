#!/bin/sh
# Builds "House-Monitor.apk" (Android 7+) with the monitor page inside: by default the website version
# (../web/index.html, the same page as monitorsim.vercel.app, with its icons); HM_PAGE picks another page.
# Needs the Android SDK (platforms;android-34, build-tools;34.0.0) in ANDROID_HOME and the signing key:
#   HM_KEYSTORE (path) and HM_KS_PASS (its password). The key is not kept in this public repo: the same key must sign
#   every build, or Android refuses to install the new APK over the old one.
set -e
cd "$(dirname "$0")"
SDK=${ANDROID_HOME:-/opt/android-sdk}; BT=$SDK/build-tools/34.0.0; JAR=$SDK/platforms/android-34/android.jar
: "${HM_KEYSTORE:?set HM_KEYSTORE}"; : "${HM_KS_PASS:?set HM_KS_PASS}"
OUT=build; rm -rf $OUT; mkdir -p $OUT/assets $OUT/classes $OUT/res
PAGE=${HM_PAGE:-../web/index.html}
cp "$PAGE" $OUT/assets/index.html
for f in icon-192.png icon-512.png manifest.webmanifest; do [ -f "$(dirname "$PAGE")/$f" ] && cp "$(dirname "$PAGE")/$f" $OUT/assets/; done
$BT/aapt2 compile --dir res -o $OUT/res.zip
$BT/aapt2 link -o $OUT/base.apk -I $JAR --manifest AndroidManifest.xml -A $OUT/assets --java $OUT/gen $OUT/res.zip
javac -g -source 8 -target 8 -nowarn -encoding UTF-8 -bootclasspath $JAR -classpath $JAR -d $OUT/classes \
  $(find src $OUT/gen -name '*.java')
(cd $OUT/classes && jar cf ../classes.jar .)
D8=${D8:-$SDK/cmdline-tools/latest/bin/d8}; [ -x "$D8" ] || D8=$BT/d8
$D8 --release --min-api 24 --lib $JAR --output $OUT $OUT/classes.jar
cp $OUT/base.apk $OUT/unsigned.apk
(cd $OUT && zip -q -j unsigned.apk classes.dex)
$BT/zipalign -f -p 4 $OUT/unsigned.apk $OUT/aligned.apk
$BT/apksigner sign --ks "$HM_KEYSTORE" --ks-pass env:HM_KS_PASS --out ../House-Monitor.apk $OUT/aligned.apk
$BT/apksigner verify ../House-Monitor.apk
rm -rf $OUT
echo "built ../House-Monitor.apk"
