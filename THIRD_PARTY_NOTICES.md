# Third-party notices

## OpenCC dictionaries

- Project: Open Chinese Convert (OpenCC), BYVoid and contributors.
- Source tag: `ver.1.1.9`.
- Upstream: https://github.com/BYVoid/OpenCC
- Files: `data/dictionary/TSCharacters.txt`, `data/dictionary/TSPhrases.txt`.
- License: Apache License 2.0; full text in `THIRD_PARTY_LICENSES/OpenCC-LICENSE.txt` and packaged under `assets/licenses/`.
- These files are included as UTF-8 text, with no dictionary-entry edits. A trailing blank line may differ from upstream. Conversion code is an independent implementation, not the OpenCC C++ engine.

Bundled SHA-256:

```text
dc1ec208bebca2e9fcaea5962cf33e277fd5c67f2d9327f9cc0d0accb4730f49  TSCharacters.txt
74a151e9b8fd2191d7a3d4932907ad5c88e1cd738a67b0d74ddf5b7f7b1b0493  TSPhrases.txt
```

## Gradle Wrapper

Gradle Wrapper scripts follow the LNR plugin template; wrapper JAR comes from the Gradle `v9.6.1` source repository. Gradle is licensed under Apache-2.0.

https://github.com/gradle/gradle/tree/v9.6.1/gradle/wrapper

## LightNovelReader API

`io.nightfish.lightnovelreader:api:0.3` (1.2.3d / API 3) and `api:0.4-SNAPSHOT` (dev / API 4) are supplied by the corresponding host and referenced as compile-time dependencies. Neither APK contains a second copy of its host API classes.

https://github.com/dmzz-yyhyy/LightNovelReader
