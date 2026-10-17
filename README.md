# Clinical Sim (BETA)

> **Clinical Sim is in beta.** It is built and tested by one person, and there are bugs I haven't found yet.
> **PLEASE report bugs**, anything medically wrong, and anything confusing — I can't find them all myself, and every report gets read and fixed.
>
> **Email: [ClinicalSimSupport@Gmail.com](mailto:ClinicalSimSupport@Gmail.com)**
> or in the app: **Menu → Help & learning → Email the developer** (it writes the message, with your app version and device, for you).

A realistic patient simulator: a Philips IntelliVue MX800-style bedside monitor, real drugs at real doses, patients who arrive the way real patients do, and a training course with a license at the end.

**A simulation for interest and general learning — not a medical course or qualification, not medical advice, not a medical device, and not for use with real patients.** Read the full **[Legal notice, privacy and disclaimer](LEGAL.md)** before using it. In an emergency call 911.

## Install (Android)
Download **[ClinicalSim.apk](ClinicalSim.apk)** and open it on your phone (allow installs from your browser if asked).

**Windows PC / laptop:** download **[ClinicalSim-PC.zip](ClinicalSim-PC.zip)**, right-click it → Extract All, then open **ClinicalSim.exe** in the extracted folder. If Windows says "Windows protected your PC", click More info → Run anyway.
After that the app checks this page for updates every time it opens and asks before installing.

## What helps in a bug report
- What you did, what you expected, and what happened instead
- The app version (Menu → Settings → Version)
- A screenshot if you can

## Files here
- `ClinicalSim.apk` — the Android app
- `ClinicalSim-PC.zip` — the Windows app
- `update.json` + `clinicalsim-<version>.html` — the update channel the app reads (checked against a SHA-256)

## B450 monitor simulator (Windows)
A separate, standalone CARESCAPE B450-style bedside monitor simulator: waveforms, alarm area and alarm light, the B450 main-menu keys, NIBP, trends, alarm history, and an instructor panel to drive the patient.

Download **[B450-Simulator-PC.zip](B450-Simulator-PC.zip)**, extract it, and double-click **Start B450 Simulator.bat**. It opens in its own window using Microsoft Edge, which is built into Windows. Source: [`b450-sim/`](b450-sim/). Not affiliated with GE HealthCare; for training and hobby use only.
