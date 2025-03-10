# Eclipse Flix Language Support

This project implements a plugin for the Eclipse IDE that provides support 
for the [Flix programming language](https://flix.dev).

## Build

```
mvn clean verify
```

## Useful links

* [LXTK LSP Eclipse Integration](https://github.com/lxtk-org/lxtk)
* [Flix Visual Studio Code Plugin](https://github.com/flix/vscode-flix)

## Package structure

Under the `de.hetzge.eclipse.flix` package the following packages can be found:

* **client**: The **L**anguage **S**erver **P**rotocol client that bridges the Eclipse IDE with the LSP Server
* **server**: The **L**anguage **S**erver **P**rotocol server implementation that uses the Flix compiler internally to answer language requests
* **editor**: Provides an Eclipse text editor with Flix editing support (Hover, Autocomplete, Highlighting ...)
* **launch**: Allows to run Flix applications. Provide Eclipse UI extensions and commands for this purpose
* **model**: A model that represents relevant Flix elements (like Flix project and version)
* **project**: Extends Eclipse with functions to manage Flix projects (create projects and files)
* **utils**: Collection of useful functions
* **compiler**: Provide client to interact with the Flix compiler
* **navigator**: Extension to the Eclipse Project Explorer

