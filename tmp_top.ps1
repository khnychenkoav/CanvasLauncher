$files = Get-ChildItem -Recurse -Filter jacocoTestReport.xml
$rows = @()
foreach ($f in $files) {
  [xml]$x = Get-Content $f.FullName
  $module = ($f.FullName -replace '^.*CanvasLauncher\\','' -replace '\\build\\reports\\jacoco\\.*$','')
  foreach ($pkg in $x.report.package) {
    foreach ($cls in $pkg.class) {
      $c = $cls.counter | Where-Object { $_.type -eq 'INSTRUCTION' } | Select-Object -First 1
      if ($c) {
        $m = [int]$c.missed
        $cov = [int]$c.covered
        $rows += [pscustomobject]@{
          module = $module
          class = [string]$cls.name
          missed = $m
          covered = $cov
          pct = [math]::Round(100 * $cov / ($cov + $m), 2)
        }
      }
    }
  }
}
$rows | Sort-Object missed -Descending | Select-Object -First 120 | ForEach-Object {
  "$($_.module)|$($_.class)|missed=$($_.missed)|pct=$($_.pct)"
}
