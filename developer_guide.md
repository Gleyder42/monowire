# Developer Guide

## Package naming

Base naming `com.github.gleyder42.monowire.<project>`.
Where `<project>` should be replaced with the Gradle subproject name.
E.g. when developing something in `common` the package would be `com.github.gleyder42.monowire.common`


## Declaring dependencies

All dependencies should be declared in the gradle version catalogue that 
is located [here](gradle/libs.versions.toml)