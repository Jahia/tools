## module-provider available versions :

- 1.1.0
- 1.1.1
- 1.2.0
- 2.3.8

## Cases that should handle all upgrades :

### module-dependent-case10

maven dependency to module-provider 1.1.0, no jahia depends, no import package no version

**MANIFEST elements:**  
Import-Package: org.external.modules.provider;version="[1.1,2)",org.apache.naming.java,org.jahia.defaults.config.spring,org.jahia.exceptions,org.jahia.services    
Require-Capability: (...) osgi.service;filter:="(objectClass=org.external.modules.provider.ProvidedComponent)";effective:=active (...)  

#### module-dependent-case11 

maven dependency to module-provider 1.1.0, no jahia depends, import package no version

**MANIFEST elements:**  
Import-Package: org.external.modules.provider;version="[1.1,2)"  
Require-Capability: (...) osgi.service;filter:="(objectClass=org.external.modules.provider.ProvidedComponent)";effective:=active  (...)

## Cases that should handle an upgrade of module-provider to all version under 2.x :

#### module-dependent-case21

maven dependency to module-provider 1.1.0, jahia-depends to module-provider no-version, no import package

**MANIFEST elements:**  
Jahia-Depends: module-provider  
Import-Package: org.external.modules.provider;version="[1.1,2)",org.apache.naming.java,org.jahia.defaults.config.spring,org.jahia.exceptions,org.jahia.services  
Require-Capability: (...) com.jahia.modules.dependencies;filter:="(moduleIdentifier=module-provider)" (...) osgi.service;filter:="(objectClass=org.external.modules.provider.ProvidedComponent)";effective:=active  (...)

#### module-dependent-case22

maven dependency to module-provider 1.1.0, jahia-depends to module-provider no-version optional, no import package  

**MANIFEST elements:**  
Jahia-Depends: module-provider=optional  
Import-Package: org.external.modules.provider;version="[1.1,2)",org.apache.naming.java,org.jahia.defaults.config.spring,org.jahia.exceptions,org.jahia.services
Require-Capability: (...) com.jahia.modules.dependencies;filter:="(moduleIdentifier=module-provider)";resolution:=optional (...) osgi.service;filter:="(objectClass=org.external.modules.provider.ProvidedComponent)";effective:=active (...)

#### module-dependent-case23

maven dependency to module-provider 1.1.0, jahia-depends to module-provider version 1.1.0, no import package  

**MANIFEST elements:**  
Jahia-Depends: module-provider=1.1.0  
Import-Package: org.external.modules.provider;version="[1.1,2)",org.apache.naming.java,org.jahia.defaults.config.spring,org.jahia.exceptions,org.jahia.services  
Require-Capability: (...) com.jahia.modules.dependencies;filter:="(&(moduleIdentifier=module-provider)(moduleVersion>=1.1.0))" (...) osgi.service;filter:="(objectClass=org.external.modules.provider.ProvidedComponent)";effective:=active (...)

#### module-dependent-case24

maven dependency to module-provider 1.1.0, jahia-depends to module-provider version 1.1.0 optional, no import package  
**MANIFEST elements:**  
Jahia-Depends: module-provider=1.1.0;optional  
Import-Package: org.external.modules.provider;version="[1.1,2)",org.apache.naming.java,org.jahia.defaults.config.spring,org.jahia.exceptions,org.jahia.services  
Require-Capability: (...) com.jahia.modules.dependencies;filter:="(&(moduleIdentifier=module-provider)(moduleVersion>=1.1.0))";resolution:=optional (...) osgi.service;filter:="(objectClass=org.external.modules.provider.ProvidedComponent)";effective:=active (...)

#### module-dependent-case25

maven dependency to module-provider 1.1.0, jahia-depends to module-provider version 1.1.1 optional, no import package  
**MANIFEST elements:**  
Jahia-Depends: module-provider=1.1.1;optional  
Import-Package: org.external.modules.provider;version="[1.1,2)",org.apache.naming.java,org.jahia.defaults.config.spring,org.jahia.exceptions,org.jahia.services  
Require-Capability: (...) com.jahia.modules.dependencies;filter:="(&(moduleIdentifier=module-provider)(moduleVersion>=1.1.1))";resolution:=optional (...) osgi.service;filter:="(objectClass=org.external.modules.provider.ProvidedComponent)";effective:=active (...)

#### module-dependent-case26

maven dependency to module-provider 1.1.0, no jahia depends, import package with range [1,2)
**MANIFEST elements:**
Import-Package: org.external.modules.provider;version="[1,2)"  
Require-Capability: (...) osgi.service;filter:="(objectClass=org.external.modules.provider.ProvidedComponent)";effective:=active (...)

#### module-dependent-case27
maven dependency to module-provider 1.1.0, no jahia depends, import package with range [1,1.99)
**MANIFEST elements:**
Import-Package: org.external.modules.provider;version="[1,1.99)"  
Require-Capability: (...) osgi.service;filter:="(objectClass=org.external.modules.provider.ProvidedComponent)";effective:=active (...)


## Cases that we want to detect because avoiding upgrade >= 1.2.x

#### module-dependent-case31

maven dependency to module-provider 1.1.0, no jahia depends, import package with range [1.1, 1.2)  
**MANIFEST elements:**
Import-Package: org.external.modules.provider;version="[1.1,1.2)"  
Require-Capability: (...) osgi.service;filter:="(objectClass=org.external.modules.provider.ProvidedComponent)";effective:=active (...)  

#### module-dependent-case32

maven dependency to module-provider 1.1.0, jahia depends to version [1, 1.2), no import package  
**MANIFEST elements:**
Jahia-Depends: module-provider=[1,1.2)
Import-Package: org.external.modules.provider;version="[1.1,2)",org.apache.naming.java,org.jahia.defaults.config.spring,org.jahia.exceptions,org.jahia.services  
Require-Capability: (...) com.jahia.modules.dependencies;filter:="(&(moduleIdentifier=module-provider)(moduleVersion>=1.0.0)(!(moduleVersion>=1.2.0)))" (...) osgi.service;filter:="(objectClass=org.external.modules.provider.ProvidedComponent)";effective:=active (...)  

#### module-dependent-case33

maven dependency to module-provider 1.1.0, no jahia depends, import package fixed version 1.1.0  
**MANIFEST elements:**
Import-Package: org.external.modules.provider;version=1.1.0  
Require-Capability: (...) osgi.service;filter:="(objectClass=org.external.modules.provider.ProvidedComponent)";effective:=active (...)

#### module-dependent-case34

maven dependency to module-provider 1.1.0, no jahia depends, import package range [1.1.0, 1.2.0)  
**MANIFEST elements:**
Import-Package: org.external.modules.provider;version="[1.1.0,1.2.0)"  
Require-Capability: (...) osgi.service;filter:="(objectClass=org.external.modules.provider.ProvidedComponent)";effective:=active (...)


