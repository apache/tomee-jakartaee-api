These are the archives used to create the Apache TomEE javaee-api-8.0-x.jar.

This archive exists as a way to replace the single javaee-api-8.0-x.jar for users
who would rather have the individual API jars rather than one large
jar.  This can be useful in situations where one or more spec versions
need to be upgraded or changed.

Simply delete the javaee-api-8.0-x.jar and unzip this zip in its
place.  Typically this will be inside the 'lib' directory of the
TomEE Standalone server or TomEE Webapp.
