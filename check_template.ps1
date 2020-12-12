& .\release.ps1
if ($?) {
  & C:\uppaal-4.1.24\uppaal.exe "H:\srcctrl\github\Uppaal-cosimulation\tools\scenario_verifier\release\algebraic_loop_msd_jac.xml"
}
