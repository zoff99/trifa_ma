#! /bin/sh
url_two_friend_list='https://github.com/zoff99/trifa_ma/releases/download/nightly/screen_shot_android_29_02.png'
url_one_on_one_chat_with_image='https://github.com/zoff99/trifa_ma/releases/download/nightly/screen_shot_android_29_03.png'
url_video_call_test='https://github.com/zoff99/trifa_ma/releases/download/nightly/screen_shot_android_29_11.png'

_HOME2_=$(dirname $0)
export _HOME2_
_HOME_=$(cd $_HOME2_;pwd)
export _HOME_

basedir="$_HOME_""/../"

cd "$basedir"
wget "$url_two_friend_list" -O ./fastlane/metadata/android/en-US/images/phoneScreenshots/105.png
wget "$url_one_on_one_chat_with_image" -O ./fastlane/metadata/android/en-US/images/phoneScreenshots/106.png
wget "$url_video_call_test" -O ./fastlane/metadata/android/en-US/images/phoneScreenshots/107.png



