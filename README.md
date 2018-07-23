[![Circle CI](https://circleci.com/gh/SmartThingsOSS/ratpack-liquibase.svg?style=svg)](https://circleci.com/gh/SmartThingsOSS/ratpack-liquibase) [![codecov.io](https://codecov.io/github/SmartThingsOSS/ratpack-liquibase/coverage.svg?branch=master)](https://codecov.io/github/SmartThingsOSS/ratpack-liquibase?branch=master) [ ![Download](https://api.bintray.com/packages/smartthingsoss/maven/smartthings.ratpack-liquibase/images/download.svg) ](https://bintray.com/smartthingsoss/maven/smartthings.ratpack-liquibase/_latestVersion)
# Ratpack Liquibase


## Enabling the Ratpack Liquibase module
1) Add ratpack Liquibase dependency to Gradle
```
    compile "smartthings:ratpack-liquibase:1.0.0"
```

2) Add module binding to Ratpack main.  Your usage may vary depending on your configuration strategy, 
but it would look something like:
```
    bindings.moduleConfig(LiquibaseModule.class,
            configData.get("/liquibase",
                    LiquibaseModule.Config.class)
```

3) Add config properties to your project's .yml file, something like:
```
liquibase:
  migrationFile: migrations.xml
  autoMigrate: true
```

4) Finally create `migrations.xml` with migrations. An empty migrations file is provided below.
See the [Liquibase documentation](http://www.liquibase.org/documentation/xml_format.html)
for more details on creating migrations.
```
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
		xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.0.xsd">
</databaseChangeLog>
```
