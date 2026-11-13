# HC launcher icon family

HMC Duo shares hc-lite's orange device family, derived from HyperChanger's
launcher motif: `ColdP/HyperChanger`,
`app/src/main/res/drawable-nodpi/hc_home_launcher.png`.
The upstream AGPL-3.0 attribution and this project's existing notices remain in
place. The native vectors are independently redrawn; the main color retains the
original HC solid orange `#FF7700`, with a `#FFFDF9` background.

The rounded phone and its simple earpiece/home marks establish the family. Duo's
large play control and separate side capsule distinguish its music/lockscreen
role from Lite's two adjustment tracks and three-panel silhouette. Neither mark
uses letters, clock text, gradients or glow.

- `drawable/duo_launcher_foreground.xml` owns the transparent 108 dp layer.
- `drawable/duo_launcher_monochrome.xml` uses transparent control cutouts so the
  same structure remains visible with system themed-icon colors.
- `drawable/duo_launcher_artwork.xml` renders the same vector for the existing
  About-page resource ID and its existing rounded crop.
- Launcher and round-icon resources share the same layers; Android 13 and newer
  select the monochrome-enabled adaptive resource.

The retired raster artwork and unused legacy launcher background/foreground are
retained in the local development archive. They are absent from active APK
resources. The About page continues using `painterResource`, which supports its
replacement VectorDrawable without changing the UI code.

All foreground geometry fits inside the centered 66 dp circular safe zone.
Host rendering checks cover circular/superellipse masks and 48 px readability;
device-specific launcher masks and themed colors remain native behavior.
