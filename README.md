
# TRIfAma - More Appeal, same Tox

Now with 73% more "oh, that's actually nice."

TRIfA represents a comprehensive engineering effort to bring P2P
communication to mobile platforms, balancing security, performance,
and usability while maintaining full compatibility with the broader
Tox ecosystem.

The codebase demonstrates advanced Android development
techniques including JNI integration, battery optimization, and secure
storage implementation.

Status
=
[![Android CI](https://github.com/zoff99/trifa_ma/workflows/Android%20startup/badge.svg)](https://github.com/zoff99/trifa_ma/actions?query=workflow%3A%22Android+startup%22)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0.en.html)
[![Liberapay](https://img.shields.io/liberapay/goal/zoff.svg?logo=liberapay)](https://liberapay.com/zoff/donate)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/zoff99/trifa_ma)

Latest Automated Screenshots
=
<img src="https://github.com/zoff99/trifa_ma/releases/download/nightly/screen_shot_android_29_02.png" width="150">&nbsp;<img src="https://github.com/zoff99/trifa_ma/releases/download/nightly/screen_shot_android_29_03.png" width="150">&nbsp;<img src="https://github.com/zoff99/trifa_ma/releases/download/nightly/screen_shot_android_29_11.png" width="150">&nbsp;
<br>
Startup Test Automated Screenshots
<br>

<img src="https://github.com/zoff99/trifa_ma/releases/download/nightly/android_screen01_21.png" width="120">&nbsp;<img src="https://github.com/zoff99/trifa_ma/releases/download/nightly/android_screen01_29.png" width="120">&nbsp;<img src="https://github.com/zoff99/trifa_ma/releases/download/nightly/android_screen01_33.png" width="120">&nbsp;<img src="https://github.com/zoff99/trifa_ma/releases/download/nightly/android_screen01_35.png" width="120">

Push Notifications
=
TRIfAma supports push wake-ups via ntfy.sh and the Tox ma Notify
companion app. This lets the app sleep in battery saving mode and wake
when a message arrives, while the Tox message content is still fetched
over Tox. See [PUSH_NOTIFICATION.md](PUSH_NOTIFICATION.md) for setup
and privacy notes.

Offline Messages
=
With ToxProxy, an always-on relay you run yourself, TRIfAma can
receive messages sent while it is offline or asleep. ToxProxy buffers
messages and can send push wake-ups. See
[OFFLINE_MESSAGES.md](OFFLINE_MESSAGES.md) for setup and privacy notes.
