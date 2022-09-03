# Eclipse Flix Language Support

This project provides an plugin for the Eclipse IDE that provides language support 
for the [Flix programming language](https://flix.dev).

## Build

```
mvn clean verify
```

## Useful links

* [Eclipse Handly Tutorial](https://github.com/pisv/gethandly/wiki)
* [LXTK LSP Eclipse Integration](https://github.com/lxtk-org/lxtk)
* [Flix Visual Studio Code Plugin](https://github.com/flix/vscode-flix)

## Package structure

Under the `de.hetzge.eclipse.flix` package the following packages can be found:

* **client**: The **L**anguage **S**erver **P**rotocol client that bridges the Eclipse IDE with the LSP Server
* **server**: The **L**anguage **S**erver **P**rotocol server implementation that uses the Flix compiler internally to answer language requests
* **editor**: Provides an Eclipse text editor with Flix editing support (Hover, Autocomplete, Highlighting ...)
* **launch**: Allows to run Flix applications. Provide Eclipse UI extensions and commands for this purpose
* **model**: The [Eclipse Handly](https://projects.eclipse.org/projects/technology.handly) language model
* **project**: Extends Eclipse with functions to manage Flix projects (create projects and files)
* **utils**: Collection of useful functions
  
## Ideas and TODOs

* Use `org.eclipse.core.resources.builders` to send deltas to LSP server
* Implement `Project` > `Clean` for Flix projects
	* Fix duplicate language tooling init when new project is created
* Show state of project in footer 
* Fix unpack flix.jar from project
* Progress while downloading flix
* Paramter description in run configuration
* Use FlixLauncher in FlixProjectWizard
* Cmd + , / .

