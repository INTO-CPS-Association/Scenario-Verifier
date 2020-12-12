# Clean is necessary because of the twirl templates. They won't be recompiled (even if changed) unless they've been deleted.
& sbt clean assembly
if($?)
{
    Remove-Item -Force -Recurse .\release
    New-Item -Path "." -Name "release" -ItemType "directory"

    Copy-Item -Force -Path .\target\scala-2.13\* -Destination .\release\ -Include "*.jar"
    Copy-Item -Force -Path .\scripts\* -Destination .\release\
    Copy-Item -Force -Path .\src\test\resources\examples\* -Destination .\release\
    Copy-Item -Force -Path .\src\test\resources\log4j2.xml -Destination .\release\
    
    Push-Location .\release
    
    & .\tests.ps1
    
    Pop-Location
} else {
  exit 1
}
