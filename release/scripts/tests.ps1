$jarfile = get-item *.jar

Function RunTest($testFile) {
    Write-Host "-----------------Generation of $testFile-----------------"
    java -jar $jarfile -m ".\$testFile.conf" -o "$testFile.xml"
    if(-Not $?) {
      Write-Host "Error running test $testFile." -ForegroundColor red
      Exit 1
    }
    Write-Host "-------------------------------------------------------------------"
}

Write-Host "-----------------------------Help Test-----------------------------"
java -jar $jarfile --help
if(-Not $?) {
  Write-Host "Error running help test." -ForegroundColor red
  Exit 1
}
Write-Host "-------------------------------------------------------------------"

RunTest "simple_master"
RunTest "algebraic_loop_msd_jac"
RunTest "algebraic_loop_msd_gs"
