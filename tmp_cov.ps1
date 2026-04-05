$files = Get-ChildItem -Recurse -Filter jacocoTestReport.xml
$overallMissed = 0
$overallCovered = 0
$rows = @()

foreach ($f in $files) {
  [xml]$x = Get-Content $f.FullName
  $module = ($f.FullName -replace '^.*CanvasLauncher\\','' -replace '\\build\\reports\\jacoco\\.*$','')
  foreach ($pkg in $x.report.package) {
    foreach ($cls in $pkg.class) {
      $counter = $cls.counter | Where-Object { $_.type -eq 'INSTRUCTION' } | Select-Object -First 1
      if ($null -ne $counter) {
        $missed = [int]$counter.missed
        $covered = [int]$counter.covered
        $overallMissed += $missed
        $overallCovered += $covered
        $rows += [pscustomobject]@{
          Module = $module
          Class = ($pkg.name + '/' + $cls.name)
          Missed = $missed
          Covered = $covered
          Coverage = [math]::Round(100 * $covered / ($covered + $missed), 2)
        }
      }
    }
  }
}

$total = $overallMissed + $overallCovered
$pct = if ($total -gt 0) { [math]::Round(100 * $overallCovered / $total, 2) } else { 0 }
"TOTAL coverage: $pct% (covered=$overallCovered, missed=$overallMissed)"
"Top 50 missed classes:"
$rows | Sort-Object Missed -Descending | Select-Object -First 50 | Format-Table -AutoSize
