# Archi Duplicate Merge Plugin

![Build Status](https://github.com/rolfmadsen/archimatetool-duplicate-merge-plugin/actions/workflows/build.yml/badge.svg)

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

## Installation

1. Download the latest `.archiplugin` file from the [Releases](../../releases) page.
2. Open Archi.
3. Go to **Help > Manage Archi Plug-ins...**
4. Click **Install New...** and select the downloaded file.
5. Restart Archi as prompted.

## Development

### Prerequisites

- **Java**: JDK 17 or 21.
- **Maven**: Version 3.9.0+.

### Building

To build the plugin and create the `.archiplugin` package:

```bash
# Build the JAR
mvn clean install -Dmaven.test.skip=true

# Create the package
mkdir -p target/package
cp target/com.archimatetool.merge-0.1.0-SNAPSHOT.jar target/package/
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

This project is licensed under the GNU General Public License v3 - see the [LICENSE](LICENSE) file for details.