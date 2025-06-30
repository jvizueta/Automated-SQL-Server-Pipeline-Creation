//DEFINITION
def dbmaestroJavaCmd = "java -jar \"C:\\Program Files (x86)\\DBmaestro\\TeamWork\\TeamWorkOracleServer\\Automation\\DBmaestroAgent.jar\""
def dbmaestroServer = "server"
def dbmaestroNode = "server.domain.com"
def dbmaestroProjectTemplateFile = "dbmaestro-mssql-project-by-version-template.json"
def dbmaestroProjectFile = "mssql-import-project.json"
def dbmaestroUsername = "username"
def dbmaestroPassword = "password"

def dbmaestroCredential = "-AuthType DBmaestroAccount -UserName \"_USER_\" -Password \"_PASS_\""
dbmaestroCredential = dbmaestroCredential.replaceFirst("_USER_", dbmaestroUsername)
dbmaestroCredential = dbmaestroCredential.replaceFirst("_PASS_", dbmaestroPassword)

def targetDBNames = env.DEV + ", " + env.RS + ", " + env.DRYRUN + ", " + env.QA + ", " + env.UAT1 + ", " + env.UAT2 + ", " + env.PROD
def dbServer = env.DbHost + "," + env.DbPort + "\\" + env.DbInstance

// START THE AUTOMATION

stage("Restore MSSQL DB Envs") {
  node (dbmaestroNode) {
    checkout scm
    mssqlRestoreDBEnvs(
      Server: dbServer
      , Username: env.DbUsername
      , Password: env.DbPassword
      , TargetDBNames: targetDBNames
      , BackupFile: env.BackupFile
      , TargetDBFolder: env.TargetDBFolder
      )
    }
}

stage("Prepare Project File"){
  node (dbmaestroNode) {
    prepareDBmaestroProjectFile(
      DbmaestroProjectTemplateFile: dbmaestroProjectTemplateFile
      , DbmaestroProjectFile: dbmaestroProjectFile
      , DbmaestroProjectName: env.DbmaestroProjectName
      , DbmaestroProjectScriptOutputFolder: env.DbmaestroProjectScriptOutputFolder
      , DbHost: env.DbHost
      , DbUsername: env.DbUsername
      , DbPassword: env.DbPassword
      , DEV: env.DEV
      , RS: env.RS
      , DRYRUN: env.DRYRUN
      , QA: env.QA
      , UAT1: env.UAT1
      , UAT2: env.UAT2
      , PROD: env.PROD
    )
  }
}

stage("Import Project to DBmaestro"){
  node (dbmaestroNode) {
    dbmaestroImportProject(
      dbmaestroJavaCmd: dbmaestroJavaCmd
      , dbmaestroProjectFile: dbmaestroProjectFile
      , dbmaestroServer: dbmaestroServer
      , dbmaestroCredential: dbmaestroCredential
    )
  }
}

def mssqlRestoreDBEnvs(Map config=[:])
{
    msgbox "Restoring DB..."
    bat "sqlcmd -S ${config.Server} -U ${config.Username} -P ${config.Password} -v TargetDBNames=\"${config.TargetDBNames}\"  BackupFile=\"${config.BackupFile}\" TargetDBFolder=\"${config.TargetDBFolder}\" -i restore.sql"
}

def prepareDBmaestroProjectFile(Map config=[:]){
  msgbox "Prep ${config.DbmaestroProjectFile} file to import to DBmaestro"
  copyFile(
    sourceFile: config.DbmaestroProjectTemplateFile
    , destinationFile: config.DbmaestroProjectFile
  )
  replaceInFile(
    file: config.DbmaestroProjectFile
    , search: "@ProjectName"
    , replace: config.DbmaestroProjectName
  )
  replaceInFile(
    file: config.DbmaestroProjectFile
    , search: "@ScriptOutputFolder"
    , replace: config.DbmaestroProjectScriptOutputFolder
  )  
  replaceInFile(
    file: config.DbmaestroProjectFile
    , search: "@host"
    , replace: config.DbHost
  )    
  replaceInFile(
    file: config.DbmaestroProjectFile
    , search: "@userName"
    , replace: config.DbUsername
  )    
  replaceInFile(
    file: config.DbmaestroProjectFile
    , search: "@password"
    , replace: config.DbPassword
  )    
  replaceInFile(
    file: config.DbmaestroProjectFile
    , search: "@DEV"
    , replace: config.DEV
  )    
  replaceInFile(
    file: config.DbmaestroProjectFile
    , search: "@RS"
    , replace: config.RS
  )    
  replaceInFile(
    file: config.DbmaestroProjectFile
    , search: "@DRYRUN"
    , replace: config.DRYRUN
  )    
  replaceInFile(
    file: config.DbmaestroProjectFile
    , search: "@QA"
    , replace: config.QA
  )    
  replaceInFile(
    file: config.DbmaestroProjectFile
    , search: "@UAT1"
    , replace: config.UAT1
  )    
  replaceInFile(
    file: config.DbmaestroProjectFile
    , search: "@UAT2"
    , replace: config.UAT2
  )    
  replaceInFile(
    file: config.DbmaestroProjectFile
    , search: "@PROD"
    , replace: config.PROD
  )    
}

def copyFile(Map config=[:]){
  bat "echo f | xcopy /f /y ${config.sourceFile} ${config.destinationFile}"
}

def replaceInFile(Map config=[:]){
  bat "powershell \"(Get-Content ${config.file}) | ForEach-Object { \$_ -replace '${config.search}', '${config.replace}' } | Set-Content ${config.file} \""
}


def dbmaestroImportProject(Map config=[:])
{
    msgbox "Importing project ${config.dbmaestroProjectFile} to DBmaestro"
    bat "${config.dbmaestroJavaCmd} -ImportProject -FilePath ${config.dbmaestroProjectFile} -Server ${config.dbmaestroServer} ${config.dbmaestroCredential}"
}

def msgbox(msg, def mtype = "nosep") {
  def tot = 80
  def start = ""
  def res = ""
  msg = (msg.size() > 65) ? msg[0..64] : msg
  def ilen = tot - msg.size()
  if (mtype == "sep"){
    start = "#${"-" * (ilen/2).toInteger()} ${msg} "
    res = "${start}${"-" * (tot - start.size() + 1)}#"
  }else{
    res = "#${"-" * tot}#\n"
    start = "#${" " * (ilen/2).toInteger()} ${msg} "
    res += "${start}${" " * (tot - start.size() + 1)}#\n"
    res += "#${"-" * tot}#\n"   
  }
  println res
}

/*
// PARAMS

//  DEPLOY TO UAT
stage("Upgrade") {
    node (dbmNode) {
        dbmaestroUpgrade(
            java_cmd: java_cmd
            , pipeline: pipeline
            , environment: env.Environment
            , version: env.Version
            , server: server
            , credential: credential
        )
   }
}

def dbmaestroUpgrade(Map config=[:])
{
    msgbox "Upgrading ${config.environment} to ${config.version}"
    bat "${config.java_cmd} -Upgrade -ProjectName ${config.pipeline} -EnvName \"${config.environment}\" -PackageName ${config.version} -Server ${config.server} ${config.credential}"
}
*/
