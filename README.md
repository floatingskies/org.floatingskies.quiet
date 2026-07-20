# Quiet

hey, i made an app that blocks scam calls. pretty simple idea honestly — if your number isn't on my list, you don't get through.

i got tired of getting random calls from numbers i've never seen, silent calls that pick up your voice, and just the general anxiety of "who the hell is calling me at 10pm on a tuesday." so i built this thing.

**it's completely free. no ads, no paywall, no tracking, none of that.** just a call blocker that works.

---

## how it works

the concept is dead simple: you keep a **whitelist** of people you actually want to hear from. everyone else? silently dropped. no ring, no missed call notification, no nothing. the caller gets hung up on and you never even know it happened.

think of it like a bouncer for your phone.

### the "paranoid mode" thing

if your whitelist is completely empty, **every single call gets blocked.** emergency numbers are still let through (190, 192, 193, 197, 112, 911) if you have that setting on, but otherwise? nobody gets in. i think it's kind of funny but also genuinely useful if you're having a really bad day and just don't want to talk to anyone.

---

## features

**the important stuff:**
- whitelist-based blocking — only your people get through
- 100% silent blocking on Android 7+ (no ring, no missed call, no notification)
- block hidden/private numbers
- block international numbers (optional, but handy)
- block entire area codes (DDDs) that are known for scams
- night mode — auto-block everything during hours you set
- emergency number bypass — 190, 192, 193, 197, 112, 911 always get through
- frequent scammer detection — if a number tries you 3+ times it gets flagged
- blocked call log so you can see who tried
- export to CSV if you want to file a complaint somewhere

**quality of life:**
- import your whole contact list in one tap
- search your whitelist
- swipe to delete entries
- works on phones and tablets
- dark theme (it looks pretty good actually)
- material design 3

---

## android compatibility

i tested this on a bunch of stuff. here's the honest breakdown:

| android version | what happens |
|---|---|
| **5.0 - 6.0** | works but it's hacky. uses reflection to call `endCall()` internally. you might hear one ring before it drops. not the app's fault, android just didn't have a proper API yet. |
| **7.0 - 9.0** | perfect. uses `CallScreeningService` directly. completely silent. |
| **10 - 15** | perfect. uses `ROLE_CALL_SCREENING` — same approach truecaller and those apps use. your default phone app stays the same, quiet just filters incoming calls in the background. |

### manufacturer weirdness

some android skins are annoying about background apps. if quiet stops working after a while, check these:

- **xiaomi/redmi/poco (MIUI):** enable "auto-start" in settings or the battery manager will absolutely murder the app
- **huawei (EMUI):** disable battery optimization for quiet
- **oppo/realme (ColorOS):** allow background starts
- **samsung (OneUI):** just works, surprisingly
- **stock android (pixel, motorola, etc.):** just works

---

## building it

if you want to compile it yourself (which i'd honestly recommend since it's open source and you shouldn't trust random APKs):

1. grab [android studio](https://developer.android.com/studio)
2. open this project folder
3. let gradle do its thing
4. hit run

or from the terminal:
```bash
./gradlew assembleDebug
```

the APK ends up at `app/build/outputs/apk/debug/app-debug.apk`

for a release build you'll need to set up signing, but that's standard android stuff.

**requirements:**
- android studio (latest stable)
- JDK 17
- android SDK platform 35 (studio installs this automatically)

---

## tech stuff (for the curious)

- **language:** kotlin
- **minSdk:** 21 (android 5.0 — covers like 99% of devices out there)
- **targetSdk:** 35 (android 15)
- **database:** room (for the whitelist and blocked call log)
- **UI:** material design 3 with viewbinding
- **async:** coroutines
- **architecture:** simple. activity + viewmodel-ish pattern + room DAOs. nothing fancy, it's a hobby project.

---

## project structure

```
Quiet/
├── app/src/main/java/org/floatingskies/quiet/
│   ├── App.kt                          # application class, notification channels
│   ├── MainActivity.kt                 # dashboard with toggle and stats
│   ├── data/
│   │   ├── AppDatabase.kt              # room database setup
│   │   ├── WhitelistDao.kt / Entity.kt # whitelist CRUD
│   │   └── BlockedCallDao.kt / Entity.kt # blocked call log
│   ├── service/
│   │   └── CallBlockerService.kt       # the actual call screening logic
│   ├── receiver/
│   │   ├── CallReceiver.kt             # fallback for android 5-6
│   │   └── BootReceiver.kt             # restarts service after reboot
│   ├── ui/
│   │   ├── onboarding/                 # first-run permission setup
│   │   ├── whitelist/                  # manage trusted numbers
│   │   ├── blocked/                    # view blocked call history
│   │   ├── settings/                   # all the toggles
│   │   └── dialer/                     # proxy activity for ROLE_CALL_SCREENING
│   └── util/
│       ├── PhoneUtils.kt               # number normalization & comparison
│       ├── PermissionHelper.kt         # permission checking
│       └── PrefsManager.kt             # shared preferences wrapper
└── app/src/main/res/
    ├── layout/                         # XML layouts (phone + tablet)
    ├── values/                         # colors, strings, dimens, styles
    ├── values-sw600dp/                 # tablet-specific dimensions
    └── values-night/                   # dark theme
```

---

## troubleshooting

| problem | fix |
|---|---|
| calls aren't being blocked | on android 10+, make sure you set quiet as your "call screening app" during onboarding |
| the call screening dialog won't show | tap the button on the onboarding screen that says "call filtering app" |
| blocked call still shows as missed | you're probably on android 5-6. upgrade if you can. on 7+ this doesn't happen. |
| app stops working after a few hours | battery optimization. go to settings and exempt quiet. also check your manufacturer's background restrictions. |
| MIUI keeps killing it | settings > apps > quiet > auto-start > enable. xiaomi is like this with everything. |
| my bank can't call me | add your bank's official number to the whitelist. check their website for the real number — don't trust numbers from random SMS. |

---

## disclaimer

this is a personal tool, not a legal shield. it blocks calls based on phone numbers and that's it. if you're dealing with actual harassment or fraud, please also file proper reports with your local authorities. in brazil that'd be anatel and the civil police.

---

## license

originally created by [floatingskies](https://github.com/floatingskies), now fully open source and free. no paywall, no premium tier, no "unlock pro features" button. it's just an app i wanted to exist.

feel free to fork it, modify it, break it, fix it, whatever. that's the whole point.

---

*if this app saves you from even one scam call, it was worth building.*