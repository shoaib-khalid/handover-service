node {
   // Mark the code checkout 'stage'....
   stage 'Checkout'

   // Checkout code from repository
   checkout scm
//
//    // Get the maven tool.
//    // ** NOTE: This 'M3' maven tool must be configured
//    // **       in the global configuration.
//    def mvnHome = tool 'M3'

   // Mark the code build 'stage'....
   stage 'Build'
   // Run the maven build
   withEnv(["JAVA_HOME=/usr/lib/jvm/java-1.8.0"])  {
   sh "env "
   sh "export JAVA_HOME=/usr/lib/jvm/java-1.8.0"
   sh "/opt/maven/apache-maven-3.8.1/bin/mvn clean install"
   }
}