$jarfile = get-item *.jar

Write-Host "-----------------------------Help Test-----------------------------"
java -jar $jarfile --help
if(-Not $?) {
  Write-Host "Error running help test." -ForegroundColor red
}
Write-Host "-------------------------------------------------------------------"

Write-Host "--------------------------Generation test--------------------------"
java -jar $jarfile -m .\simple_master.conf -o uppaal_master.xml
if(-Not $?) {
  Write-Host "Error running generation test." -ForegroundColor red
}
Write-Host "-------------------------------------------------------------------"
