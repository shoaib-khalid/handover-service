node {
   // Mark the code checkout 'stage'....
   stage 'Checkout'

   // Checkout code from repository
   checkout scm

   stage ('Deploy') {
       build job: 'java-handover-service', parameters: [[$class: 'StringParameterValue', name: 'payload', value:"origin/${BRANCH_NAME}" ]]
   }
}