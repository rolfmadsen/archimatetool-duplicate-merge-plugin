# Archi Duplicate Merge Plugin

[![Java CI with Maven](https://github.com/rolfmadsen/archimatetool-duplicate-merge-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/rolfmadsen/archimatetool-duplicate-merge-plugin/actions/workflows/build.yml)
[![Latest Release](https://img.shields.io/github/v/release/rolfmadsen/archimatetool-duplicate-merge-plugin?label=Latest%20stable%20release&color=blue&include_prereleases)](https://github.com/rolfmadsen/archimatetool-duplicate-merge-plugin/releases/latest/download/duplicate-merge-plugin.archiplugin)
[![Latest Build](https://img.shields.io/badge/Latest_Build-bleeding_edge-orange)](https://github.com/rolfmadsen/archimatetool-duplicate-merge-plugin/actions/workflows/build.yml)

> [!WARNING]
> **Disclaimer: Vibe-Coded Software**
> This plugin is not created by professional Java developers. It has been "vibe-coded" with the help of AI agents. While we strive for correctness (especially regarding model integrity), this software is provided "as is" without any warranties.
>
> **CRITICAL**: Use this plugin at your own risk. Always **backup** your ArchiMate models before performing a merge. The authors are not responsible for any data loss or model corruption.

A plugin for [Archi](https://www.archimatetool.com) that allows you to safely merge duplicate ArchiMate elements, including deep diagram consolidation.

## Features

- **Element Merging**: Consolidates multiple elements of the same type and name into a single target element.
- **Relationship Migration**: Automatically moves all relationships from duplicates to the target element.
- **Diagram Consolidation**: Detects if duplicate elements appear on the same diagram, migrates connections to the target box, and removes the redundant box.
- **Safe Connection Handling**: Uses proper bi-directional updates to prevent model corruption and "hanging" connections.

## 📥 Download & Installation

**1. Download the Plugin:**
You have two options depending on your needs.

*   **Stable Version (Recommended):** [**Download Official Release**](https://github.com/rolfmadsen/archimatetool-duplicate-merge-plugin/releases/latest/download/duplicate-merge-plugin.archiplugin)
*   **Bleeding Edge (Latest Commit):** To get the absolute newest (but potentially unstable) features, click the **Latest Build** badge at the top of this page, select the most recent green checkmark, and download the `.archiplugin` from the **Artifacts** section at the bottom.

**2. Install in Archi:**
* Open Archi.
* Go to **Help -> Manage Plug-ins...**
* Click **Install...** and select the **.archiplugin** file you just downloaded.
* Restart Archi as prompted.

> [!TIP]
> You can also find the latest development build by clicking the **Actions** tab on GitHub, selecting the latest run, and scrolling to the bottom to find the **Artifacts** section!

## Development

### Prerequisites

- **Java**: JDK 17 or 21.
- **Maven**: Version 3.9.0+.

### Building

To build the plugin and create the `.archiplugin` package:

```bash
# Build the entire reactor (includes all dependencies)
mvn clean install -Dmaven.test.skip=true

# The .archiplugin package is created in the root target/package directory during CI
# To create it locally, you can follow the same steps as in .github/workflows/build.yml
mkdir -p target/package
cp com.archimatetool.merge/target/com.archimatetool.merge-*.jar target/package/
touch target/package/archi-plugin
cd target/package && zip -r ../../duplicate-merge-plugin.archiplugin *
```

### How to Release

This project uses GitHub Actions for automated releases. To publish a new version:

1.  Update the version in `pom.xml`.
2.  Commit and push the change.
3.  Create and push a git tag:
    ```bash
    git tag -a v1.0.0 -m "Release version 1.0.0"
    git push origin v1.0.0
    ```
4.  The GitHub Action will automatically build the `.archiplugin` file and attach it to a new GitHub Release.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request or open an issue.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.